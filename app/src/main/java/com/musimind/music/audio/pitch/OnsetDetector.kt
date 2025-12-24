package com.musimind.music.audio.pitch

import com.musimind.music.audio.core.PitchFrame
import com.musimind.music.audio.core.TimingFrame
import kotlin.math.sqrt

/**
 * Detector de Onset/Offset baseado em energia.
 * Identifica quando o usuário COMEÇA e TERMINA de cantar uma nota.
 * 
 * Critérios de Onset:
 * 1. Aumento súbito de energia (attack)
 * 2. Transição de silêncio para voiced
 * 
 * Critérios de Offset:
 * 1. Queda de energia abaixo do threshold
 * 2. Silêncio sustentado por N frames
 */
class OnsetDetector(
    private val sampleRate: Int = 44100,
    private val hopSize: Int = 512,
    private val energyThreshold: Float = 0.01f,
    private val attackRatio: Float = 2.5f, // Energia deve aumentar 2.5x para onset
    private val silenceFramesForOffset: Int = 3 // ~17ms @ 512 hop
) {
    // Estado interno
    private var previousEnergy = 0f
    private var previousPreviousEnergy = 0f
    private var wasVoiced = false
    private var silenceCounter = 0
    private var lastOnsetSample: Long = -1
    
    // Smoothing
    private val energyHistory = FloatArray(5)
    private var historyIndex = 0
    
    /**
     * Analisa frame para detectar onset/offset.
     * 
     * @param frame Buffer de áudio do frame atual
     * @param pitchResult Resultado da detecção de pitch (para saber se é voiced)
     * @param samplePosition Posição em samples
     * @return TimingFrame com informação de onset/offset
     */
    fun analyze(
        frame: FloatArray, 
        pitchResult: PitchFrame,
        samplePosition: Long
    ): TimingFrame {
        // 1. Calcula energia RMS
        val energy = calculateRMS(frame)
        
        // Smoothing da energia
        energyHistory[historyIndex] = energy
        historyIndex = (historyIndex + 1) % energyHistory.size
        val smoothedEnergy = energyHistory.average().toFloat()
        
        // 2. Detecta onset
        val isOnset = detectOnset(smoothedEnergy, pitchResult)
        
        // 3. Detecta offset
        val isOffset = detectOffset(pitchResult)
        
        // 4. Calcula força do onset
        val onsetStrength = if (isOnset && previousEnergy > 0) {
            (smoothedEnergy / previousEnergy).coerceIn(1f, 10f) / 10f
        } else 0f
        
        // Atualiza estado
        previousPreviousEnergy = previousEnergy
        previousEnergy = smoothedEnergy
        wasVoiced = pitchResult.isVoiced
        
        if (isOnset) {
            lastOnsetSample = samplePosition
        }
        
        return TimingFrame(
            samplePosition = samplePosition,
            energy = smoothedEnergy,
            isOnset = isOnset,
            isOffset = isOffset,
            onsetStrength = onsetStrength
        )
    }
    
    private fun detectOnset(energy: Float, pitchResult: PitchFrame): Boolean {
        // Critério 1: Transição silêncio → voz
        val silenceToVoice = !wasVoiced && pitchResult.isVoiced && energy > energyThreshold
        
        // Critério 2: Aumento súbito de energia (attack)
        val suddenAttack = pitchResult.isVoiced && 
                           previousEnergy > 0 && 
                           energy > previousEnergy * attackRatio &&
                           energy > energyThreshold
        
        // Critério 3: Mudança significativa após silêncio prolongado
        val afterSilence = silenceCounter > silenceFramesForOffset && 
                           pitchResult.isVoiced && 
                           energy > energyThreshold
        
        val isOnset = silenceToVoice || suddenAttack || afterSilence
        
        // Reset silence counter on onset
        if (isOnset) {
            silenceCounter = 0
        }
        
        return isOnset
    }
    
    private fun detectOffset(pitchResult: PitchFrame): Boolean {
        if (!pitchResult.isVoiced) {
            silenceCounter++
        } else {
            silenceCounter = 0
        }
        
        // Offset quando estava voiced e agora há silêncio sustentado
        return wasVoiced && silenceCounter >= silenceFramesForOffset
    }
    
    private fun calculateRMS(frame: FloatArray): Float {
        var sum = 0f
        for (sample in frame) {
            sum += sample * sample
        }
        return sqrt(sum / frame.size)
    }
    
    /**
     * Reseta o estado do detector.
     * Chamar ao iniciar um novo exercício.
     */
    fun reset() {
        previousEnergy = 0f
        previousPreviousEnergy = 0f
        wasVoiced = false
        silenceCounter = 0
        lastOnsetSample = -1
        energyHistory.fill(0f)
        historyIndex = 0
    }
    
    /**
     * Retorna o sample position do último onset detectado.
     */
    fun getLastOnsetSample(): Long = lastOnsetSample
    
    /**
     * Verifica se está em estado de voz (após onset, antes de offset).
     */
    fun isInVoicedState(): Boolean = wasVoiced && silenceCounter < silenceFramesForOffset
}
