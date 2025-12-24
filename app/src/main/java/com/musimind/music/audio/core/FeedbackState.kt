package com.musimind.music.audio.core

import androidx.compose.runtime.Immutable

/**
 * Resultado de uma análise de pitch.
 * Cada frame contém informação temporal precisa em SAMPLES.
 */
@Immutable
data class PitchFrame(
    // === INFORMAÇÃO TEMPORAL (Sample-Accurate) ===
    val samplePositionStart: Long,  // Sample onde esta análise começa
    val samplePositionEnd: Long,    // Sample onde termina
    val windowSizeSamples: Int,     // Tamanho da janela de análise
    
    // === RESULTADO DA ANÁLISE ===
    val frequency: Float,           // Frequência fundamental em Hz (0 se não detectado)
    val confidence: Float,          // Confiança [0, 1]
    val midiNote: Int,              // Nota MIDI mais próxima
    val centDeviation: Float,       // Desvio em cents da nota MIDI (-50 a +50)
    
    // === FLAGS ===
    val isVoiced: Boolean,          // true se detectou voz
    val isOnset: Boolean = false,   // true se é início de nota
    val isOffset: Boolean = false   // true se é fim de nota
) {
    companion object {
        val SILENCE = PitchFrame(
            samplePositionStart = 0,
            samplePositionEnd = 0,
            windowSizeSamples = 0,
            frequency = 0f,
            confidence = 0f,
            midiNote = 0,
            centDeviation = 0f,
            isVoiced = false,
            isOnset = false,
            isOffset = false
        )
        
        /** Copia com flags de onset/offset */
        fun PitchFrame.withOnset(isOnset: Boolean): PitchFrame = copy(isOnset = isOnset)
        fun PitchFrame.withOffset(isOffset: Boolean): PitchFrame = copy(isOffset = isOffset)
    }
    
    /** Tempo central da janela em samples */
    val centerSample: Long get() = samplePositionStart + windowSizeSamples / 2
    
    /** Nome da nota (C, D, E, etc) */
    val noteName: String get() {
        if (!isVoiced) return "-"
        val names = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        return names[midiNote % 12]
    }
    
    /** Oitava da nota */
    val octave: Int get() = (midiNote / 12) - 1
    
    /** Nome completo (ex: "C4") */
    val fullNoteName: String get() = if (isVoiced) "$noteName$octave" else "-"
}

/**
 * Frame de timing (onset/offset detection).
 */
@Immutable
data class TimingFrame(
    val samplePosition: Long,
    val energy: Float,           // Energia RMS do frame
    val isOnset: Boolean,        // Detectou início de nota
    val isOffset: Boolean,       // Detectou fim de nota
    val onsetStrength: Float = 0f // Força do onset (0-1)
)

/**
 * Estado de feedback para uma nota específica.
 * Atualizado continuamente pela thread de análise.
 */
@Immutable
data class NoteFeedbackState(
    val noteId: String,
    
    // === FEEDBACK DE PITCH ===
    val pitchStatus: PitchStatus = PitchStatus.NOT_EVALUATED,
    val averageCentDeviation: Float = 0f,
    val pitchScore: Float = 0f,
    
    // === FEEDBACK DE TIMING ===
    val timingStatus: TimingStatus = TimingStatus.NOT_PLAYED,
    val attackDeviationSamples: Long = 0,
    val attackDeviationMs: Float = 0f,
    val durationAccuracy: Float = 0f,
    
    // === ESTADO DE PROGRESSO ===
    val isCurrentNote: Boolean = false,
    val isCompleted: Boolean = false,
    val isPending: Boolean = true
)

enum class PitchStatus {
    CORRECT,        // Dentro de ±20 cents
    FLAT,           // Abaixo de -20 cents
    SHARP,          // Acima de +20 cents
    NOT_EVALUATED   // Ainda não avaliado
}

enum class TimingStatus {
    ON_TIME,        // Ataque correto (±50ms)
    EARLY,          // Ataque antes do tempo
    LATE,           // Ataque depois do tempo
    NOT_PLAYED      // Não cantou
}

/**
 * Estado completo de feedback para a UI.
 * Atualizado por StateFlow, lido pela UI.
 */
@Immutable
data class SolfegeFeedbackState(
    // === PLAYHEAD (sempre atualizado, independente do pitch) ===
    val currentSamplePosition: Long = 0,
    val currentBeatPosition: Double = 0.0,
    val currentNoteIndex: Int = -1,
    
    // === METRÔNOMO VISUAL ===
    val currentBeatInMeasure: Int = 0,      // 1, 2, 3, 4
    val isDownbeat: Boolean = false,
    
    // === FEEDBACK POR NOTA ===
    val noteFeedbacks: List<NoteFeedbackState> = emptyList(),
    
    // === DETECÇÃO EM TEMPO REAL ===
    val currentPitchHz: Float = 0f,
    val currentMidiNote: Int = 0,
    val currentCentDeviation: Float = 0f,
    val isCurrentPitchCorrect: Boolean = false,
    val isVoiceDetected: Boolean = false,
    
    // === SCORES ===
    val overallPitchScore: Float = 0f,
    val overallTimingScore: Float = 0f,
    val overallScore: Float = 0f,
    
    // === ESTADO GERAL ===
    val phase: SolfegePhase = SolfegePhase.IDLE,
    val countdownNumber: Int = 0 // 0 = não está em countdown
)

enum class SolfegePhase {
    IDLE,           // Aguardando início
    COUNTDOWN,      // Contagem regressiva
    PLAYING,        // Reproduzindo melodia (usuário ouve)
    LISTENING,      // Ouvindo usuário cantar
    COMPLETED       // Exercício finalizado
}
