package com.musimind.music.audio.core

import androidx.compose.runtime.Immutable

/**
 * Evento de áudio agendado.
 * Todos os eventos são agendados por SAMPLE POSITION, não por tempo.
 */
sealed class ScheduledAudioEvent(
    open val samplePosition: Long,  // Quando o evento começa (em samples)
    open val durationSamples: Long  // Duração do evento (em samples)
) : Comparable<ScheduledAudioEvent> {
    
    override fun compareTo(other: ScheduledAudioEvent): Int {
        return samplePosition.compareTo(other.samplePosition)
    }
    
    /** Verifica se o evento cai dentro de um frame de áudio */
    fun isInFrame(frameStart: Long, frameEnd: Long): Boolean {
        // Evento começa após o frame
        if (samplePosition >= frameEnd) return false
        // Evento termina antes do frame
        if (samplePosition + durationSamples <= frameStart) return false
        return true
    }
    
    /** Verifica se o evento já terminou */
    fun isComplete(currentSample: Long): Boolean {
        return currentSample >= samplePosition + durationSamples
    }
    
    /** Nota de piano/instrumento */
    @Immutable
    data class NoteEvent(
        override val samplePosition: Long,
        override val durationSamples: Long,
        val midiNote: Int,
        val velocity: Float = 0.8f
    ) : ScheduledAudioEvent(samplePosition, durationSamples)
    
    /** Click do metrônomo */
    @Immutable
    data class MetronomeClick(
        override val samplePosition: Long,
        override val durationSamples: Long = 661, // ~15ms @ 44.1kHz
        val isAccented: Boolean = false,
        val beatNumber: Int = 1 // 1-4 para 4/4
    ) : ScheduledAudioEvent(samplePosition, durationSamples)
    
    /** Countdown (contagem antes do exercício) */
    @Immutable
    data class CountdownEvent(
        override val samplePosition: Long,
        override val durationSamples: Long = 4410, // ~100ms @ 44.1kHz
        val countNumber: Int // 1, 2, 3, 4
    ) : ScheduledAudioEvent(samplePosition, durationSamples)
}

/**
 * Estado de reprodução de um evento.
 * Usado para tracking de eventos em andamento.
 */
@Immutable
data class EventPlaybackState(
    val event: ScheduledAudioEvent,
    val currentOffset: Int = 0, // Posição atual dentro do evento
    val isComplete: Boolean = false
)
