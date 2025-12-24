package com.musimind.music.audio.core

import androidx.compose.runtime.Immutable

/**
 * Clock Musical Único - Fonte de Verdade Absoluta
 * 
 * Todos os subsistemas LEEM este clock. Nenhum cria o seu próprio.
 * O tempo NUNCA é derivado de System.currentTimeMillis() ou timers.
 * O tempo é SEMPRE derivado da posição de samples.
 */
@Immutable
data class AudioClock(
    val samplePosition: Long,           // Posição absoluta em samples desde o início
    val sampleRate: Int = 44100,        // 44100 ou 48000
    val tempo: Double = 60.0,           // BPM (ex: 60.0, 120.0)
    val timeSignatureNumerator: Int = 4,   // Ex: 4 (em 4/4)
    val timeSignatureDenominator: Int = 4  // Ex: 4 (em 4/4)
) {
    // === DERIVAÇÕES MATEMÁTICAS PURAS ===
    
    /** Tempo absoluto em segundos (derivado, não primário) */
    val timeSeconds: Double get() = samplePosition.toDouble() / sampleRate
    
    /** Samples por beat (derivado do BPM) */
    val samplesPerBeat: Double get() = (sampleRate * 60.0) / tempo
    
    /** Samples por compasso */
    val samplesPerMeasure: Double get() = samplesPerBeat * timeSignatureNumerator
    
    /** Beat atual (0-indexed, fracionário) */
    val beatPosition: Double get() = samplePosition / samplesPerBeat
    
    /** Compasso atual (0-indexed, fracionário) */
    val measurePosition: Double get() = samplePosition / samplesPerMeasure
    
    /** Beat dentro do compasso atual (1 to numerator, inteiro) */
    val beatInMeasure: Int get() = ((beatPosition.toLong() % timeSignatureNumerator) + 1).toInt()
    
    /** Beat fracionário dentro do compasso (0 to numerator-1) */
    val beatInMeasureFractional: Double get() = beatPosition % timeSignatureNumerator
    
    /** Subdivide em 16ths, 32nds, etc */
    fun getSubdivision(divisor: Int): Double = (beatPosition * divisor) % divisor
    
    /** Converte sample position para beat position */
    fun sampleToBeat(sample: Long): Double = sample.toDouble() / samplesPerBeat
    
    /** Converte beat position para sample position */
    fun beatToSample(beat: Double): Long = (beat * samplesPerBeat).toLong()
    
    /** Avança o clock por um número de samples */
    fun advance(samples: Int): AudioClock = copy(samplePosition = samplePosition + samples)
    
    /** Reseta o clock para posição zero */
    fun reset(): AudioClock = copy(samplePosition = 0)
    
    companion object {
        /** Cria um clock parado na posição 0 */
        fun create(
            sampleRate: Int = 44100,
            tempo: Double = 60.0,
            timeSignatureNumerator: Int = 4,
            timeSignatureDenominator: Int = 4
        ): AudioClock = AudioClock(
            samplePosition = 0,
            sampleRate = sampleRate,
            tempo = tempo,
            timeSignatureNumerator = timeSignatureNumerator,
            timeSignatureDenominator = timeSignatureDenominator
        )
    }
}
