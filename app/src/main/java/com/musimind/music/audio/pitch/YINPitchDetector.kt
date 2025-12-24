package com.musimind.music.audio.pitch

import com.musimind.music.audio.core.PitchFrame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Detector de Pitch baseado no algoritmo YIN.
 * 
 * Características:
 * - Janela Hann pré-aplicada
 * - Overlap de 75% (hop size = windowSize/4)
 * - Interpolação parabólica para precisão sub-sample
 * - Threshold adaptativo
 * 
 * Referência: "YIN, a fundamental frequency estimator for speech and music"
 * de Cheveigné & Kawahara, 2002
 */
class YINPitchDetector(
    private val sampleRate: Int = 44100,
    private val windowSize: Int = 2048,
    private val threshold: Float = 0.15f
) {
    // Buffers pré-alocados (evita GC)
    private val yinBuffer = FloatArray(windowSize / 2)
    private val hannWindow = FloatArray(windowSize)
    private val windowedBuffer = FloatArray(windowSize)
    
    // Configuração de frequência
    private val minFrequency = 80f   // Mínimo ~E2
    private val maxFrequency = 1000f // Máximo ~B5
    private val minPeriod: Int
    private val maxPeriod: Int
    
    init {
        // Pré-calcula janela Hann
        for (i in 0 until windowSize) {
            hannWindow[i] = (0.5 * (1 - cos(2 * PI * i / (windowSize - 1)))).toFloat()
        }
        
        // Limites de período (inverso da frequência)
        minPeriod = (sampleRate / maxFrequency).toInt()
        maxPeriod = (sampleRate / minFrequency).toInt().coerceAtMost(windowSize / 2 - 1)
    }
    
    /**
     * Detecta pitch no buffer de entrada.
     * 
     * @param input Buffer de áudio (deve ter pelo menos windowSize samples)
     * @param samplePosition Posição em samples do início da janela
     * @return PitchFrame com resultado da análise
     */
    fun detect(input: FloatArray, samplePosition: Long): PitchFrame {
        if (input.size < windowSize) {
            return PitchFrame.SILENCE.copy(
                samplePositionStart = samplePosition,
                samplePositionEnd = samplePosition + input.size,
                windowSizeSamples = input.size
            )
        }
        
        // Verifica energia mínima (evita processar silêncio)
        val rms = calculateRMS(input)
        if (rms < 0.01f) {
            return PitchFrame.SILENCE.copy(
                samplePositionStart = samplePosition,
                samplePositionEnd = samplePosition + windowSize,
                windowSizeSamples = windowSize
            )
        }
        
        // 1. Aplica janela Hann
        for (i in 0 until windowSize) {
            windowedBuffer[i] = input[i] * hannWindow[i]
        }
        
        // 2. Calcula função de diferença
        computeDifference(windowedBuffer)
        
        // 3. Normalização cumulativa
        cumulativeMeanNormalize()
        
        // 4. Busca mínimo abaixo do threshold
        val tauEstimate = absoluteThreshold()
        
        if (tauEstimate == -1) {
            return PitchFrame.SILENCE.copy(
                samplePositionStart = samplePosition,
                samplePositionEnd = samplePosition + windowSize,
                windowSizeSamples = windowSize
            )
        }
        
        // 5. Interpolação parabólica para precisão sub-sample
        val betterTau = parabolicInterpolation(tauEstimate)
        
        // 6. Converte para frequência
        val frequency = sampleRate.toFloat() / betterTau
        
        // Verifica se frequência está no range válido
        if (frequency < minFrequency || frequency > maxFrequency) {
            return PitchFrame.SILENCE.copy(
                samplePositionStart = samplePosition,
                samplePositionEnd = samplePosition + windowSize,
                windowSizeSamples = windowSize
            )
        }
        
        // 7. Calcula MIDI note e cent deviation
        val midiNote = frequencyToMidi(frequency)
        val centDeviation = calculateCentDeviation(frequency, midiNote)
        
        // 8. Confiança baseada no valor mínimo do YIN
        val confidence = 1f - yinBuffer[tauEstimate]
        
        return PitchFrame(
            samplePositionStart = samplePosition,
            samplePositionEnd = samplePosition + windowSize,
            windowSizeSamples = windowSize,
            frequency = frequency,
            confidence = confidence.coerceIn(0f, 1f),
            midiNote = midiNote,
            centDeviation = centDeviation,
            isVoiced = true,
            isOnset = false,
            isOffset = false
        )
    }
    
    /**
     * Função de diferença (passo 2 do YIN).
     * d(τ) = Σ (x[j] - x[j + τ])²
     */
    private fun computeDifference(buffer: FloatArray) {
        val halfWindow = windowSize / 2
        
        for (tau in 0 until halfWindow) {
            yinBuffer[tau] = 0f
            for (j in 0 until halfWindow) {
                val delta = buffer[j] - buffer[j + tau]
                yinBuffer[tau] += delta * delta
            }
        }
    }
    
    /**
     * Normalização cumulativa (passo 3 do YIN).
     * d'(τ) = d(τ) / [(1/τ) * Σd(j)]
     */
    private fun cumulativeMeanNormalize() {
        yinBuffer[0] = 1f
        var runningSum = 0f
        
        for (tau in 1 until yinBuffer.size) {
            runningSum += yinBuffer[tau]
            if (runningSum != 0f) {
                yinBuffer[tau] = yinBuffer[tau] * tau / runningSum
            } else {
                yinBuffer[tau] = 1f
            }
        }
    }
    
    /**
     * Busca primeiro mínimo abaixo do threshold (passo 4 do YIN).
     */
    private fun absoluteThreshold(): Int {
        // Começa pelo período mínimo (frequência máxima)
        for (tau in minPeriod until maxPeriod) {
            if (yinBuffer[tau] < threshold) {
                // Procura o mínimo local
                var minTau = tau
                while (minTau + 1 < maxPeriod && yinBuffer[minTau + 1] < yinBuffer[minTau]) {
                    minTau++
                }
                return minTau
            }
        }
        
        // Fallback: retorna o mínimo global
        var minValue = Float.MAX_VALUE
        var minTau = -1
        for (tau in minPeriod until maxPeriod) {
            if (yinBuffer[tau] < minValue) {
                minValue = yinBuffer[tau]
                minTau = tau
            }
        }
        
        // Só retorna se o mínimo for razoavelmente baixo
        return if (minValue < threshold * 2) minTau else -1
    }
    
    /**
     * Interpolação parabólica para precisão sub-sample (passo 5).
     * Ajusta τ usando os valores vizinhos para encontrar o mínimo real.
     */
    private fun parabolicInterpolation(tau: Int): Float {
        if (tau <= 0 || tau >= yinBuffer.size - 1) return tau.toFloat()
        
        val s0 = yinBuffer[tau - 1]
        val s1 = yinBuffer[tau]
        val s2 = yinBuffer[tau + 1]
        
        // Evita divisão por zero
        val denominator = 2 * s1 - s0 - s2
        if (abs(denominator) < 0.0001f) return tau.toFloat()
        
        // Fórmula de interpolação parabólica
        val delta = (s2 - s0) / (2 * denominator)
        
        return tau + delta
    }
    
    /**
     * Converte frequência para nota MIDI.
     * MIDI 69 = A4 = 440Hz
     */
    private fun frequencyToMidi(frequency: Float): Int {
        return (69 + 12 * log2(frequency / 440.0)).roundToInt()
    }
    
    /**
     * Calcula desvio em cents da nota MIDI mais próxima.
     * 100 cents = 1 semitom
     */
    private fun calculateCentDeviation(frequency: Float, midiNote: Int): Float {
        val exactMidi = 69 + 12 * log2(frequency / 440.0)
        return ((exactMidi - midiNote) * 100).toFloat()
    }
    
    /**
     * Calcula RMS do buffer.
     */
    private fun calculateRMS(buffer: FloatArray): Float {
        var sum = 0f
        for (sample in buffer) {
            sum += sample * sample
        }
        return sqrt(sum / buffer.size)
    }
    
    /**
     * Hop size recomendado (75% overlap).
     */
    fun getHopSize(): Int = windowSize / 4
    
    /**
     * Tamanho da janela de análise.
     */
    fun getWindowSize(): Int = windowSize
    
    companion object {
        /**
         * Cria detector otimizado para voz humana.
         */
        fun forVoice(sampleRate: Int = 44100): YINPitchDetector {
            return YINPitchDetector(
                sampleRate = sampleRate,
                windowSize = 2048,  // ~46ms @ 44.1kHz - bom para voz
                threshold = 0.10f   // Threshold mais baixo para melhor precisão
            )
        }
        
        /**
         * Cria detector para instrumentos (range mais amplo).
         */
        fun forInstrument(sampleRate: Int = 44100): YINPitchDetector {
            return YINPitchDetector(
                sampleRate = sampleRate,
                windowSize = 4096,  // ~92ms - melhor resolução de frequência
                threshold = 0.15f
            )
        }
    }
}
