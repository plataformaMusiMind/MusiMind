package com.musimind.music.audio.core

import androidx.compose.runtime.Immutable

/**
 * Nota esperada do exercício (carregada do banco de dados).
 * Representa o que o usuário DEVE cantar.
 * 
 * As posições em samples são calculadas a partir do AudioClock.
 */
@Immutable
data class ExpectedNote(
    val id: String,
    val midiNote: Int,                    // Nota MIDI esperada (60 = C4)
    val startBeat: Double,                // Beat de início (relativo ao exercício)
    val durationBeats: Double,            // Duração em beats
    val solfegeName: String = "",         // "Dó", "Ré", etc.
    
    // === CALCULADOS A PARTIR DO AUDIO CLOCK ===
    val startSample: Long = 0,            // Calculado: startBeat * samplesPerBeat
    val endSample: Long = 0,              // Calculado: startSample + durationSamples
    val durationSamples: Long = 0         // Calculado: durationBeats * samplesPerBeat
) {
    companion object {
        /**
         * Prepara a nota calculando posições em samples.
         * Retorna nova instância com valores preenchidos.
         */
        fun ExpectedNote.prepare(clock: AudioClock): ExpectedNote {
            val start = clock.beatToSample(startBeat)
            val duration = (durationBeats * clock.samplesPerBeat).toLong()
            return copy(
                startSample = start,
                durationSamples = duration,
                endSample = start + duration
            )
        }
        
        /**
         * Prepara lista de notas com posições em samples.
         */
        fun List<ExpectedNote>.prepareAll(clock: AudioClock): List<ExpectedNote> {
            return map { it.prepare(clock) }
        }
        
        /**
         * Converte nota MIDI para nome de solfejo (português).
         */
        fun midiToSolfege(midiNote: Int): String {
            val noteNames = listOf("Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá", "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si")
            return noteNames[midiNote % 12]
        }
        
        /**
         * Converte nota MIDI para frequência Hz.
         * A4 (MIDI 69) = 440Hz
         */
        fun midiToFrequency(midiNote: Int): Float {
            return (440.0 * Math.pow(2.0, (midiNote - 69) / 12.0)).toFloat()
        }
    }
    
    /** Verifica se o sample position está dentro desta nota */
    fun containsSample(samplePos: Long): Boolean = samplePos in startSample until endSample
    
    /** Verifica se a nota já passou */
    fun isPast(samplePos: Long): Boolean = samplePos >= endSample
    
    /** Verifica se a nota ainda não começou */
    fun isFuture(samplePos: Long): Boolean = samplePos < startSample
    
    /** Frequência esperada em Hz */
    val frequencyHz: Float get() = midiToFrequency(midiNote)
}
