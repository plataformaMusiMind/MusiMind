package com.musimind.music.audio.scoring

import com.musimind.music.audio.core.*
import com.musimind.music.audio.pitch.OnsetDetector
import com.musimind.music.audio.pitch.YINPitchDetector

/**
 * Thread de análise de áudio.
 * Processa samples do ring buffer e produz feedback em tempo real.
 * 
 * Esta classe roda em uma thread separada da thread de áudio
 * para não causar glitches.
 */
class AnalysisEngine(
    private val sampleRate: Int = 44100,
    private val onFeedbackUpdate: (SolfegeFeedbackState) -> Unit
) {
    // Detectores
    private val pitchDetector = YINPitchDetector.forVoice(sampleRate)
    private val onsetDetector = OnsetDetector(sampleRate = sampleRate)
    
    // Configuração
    private val windowSize = pitchDetector.getWindowSize()
    private val hopSize = pitchDetector.getHopSize()
    
    // Buffers pré-alocados
    private val analysisBuffer = FloatArray(windowSize)
    
    // Estado
    private var currentNoteIndex = 0
    private var expectedNotes: List<ExpectedNote> = emptyList()
    private var audioClock: AudioClock = AudioClock.create()
    
    // Tracking por nota
    private val pitchFramesPerNote = mutableMapOf<Int, MutableList<PitchFrame>>()
    private val onsetSamplePerNote = mutableMapOf<Int, Long?>()
    private val offsetSamplePerNote = mutableMapOf<Int, Long?>()
    
    // Estado atual
    private var lastPitchFrame: PitchFrame = PitchFrame.SILENCE
    private var phase: SolfegePhase = SolfegePhase.IDLE
    
    /**
     * Configura o engine para um novo exercício.
     */
    fun configure(
        notes: List<ExpectedNote>,
        clock: AudioClock
    ) {
        expectedNotes = notes.map { it.copy() }
        audioClock = clock
        
        // Prepara notas com sample positions
        expectedNotes = expectedNotes.map { note ->
            note.copy(
                startSample = clock.beatToSample(note.startBeat),
                durationSamples = (note.durationBeats * clock.samplesPerBeat).toLong(),
                endSample = clock.beatToSample(note.startBeat) + 
                    (note.durationBeats * clock.samplesPerBeat).toLong()
            )
        }
        
        reset()
    }
    
    /**
     * Reseta o estado do engine.
     */
    fun reset() {
        currentNoteIndex = 0
        pitchFramesPerNote.clear()
        onsetSamplePerNote.clear()
        offsetSamplePerNote.clear()
        onsetDetector.reset()
        lastPitchFrame = PitchFrame.SILENCE
        phase = SolfegePhase.IDLE
    }
    
    /**
     * Processa um buffer de samples.
     * Chamado pela thread de análise.
     * 
     * @param samples Buffer de samples do microfone
     * @param samplePosition Posição em samples do início do buffer
     */
    fun process(samples: FloatArray, samplePosition: Long) {
        if (samples.size < windowSize) return
        
        // Copia samples para buffer de análise
        samples.copyInto(analysisBuffer, 0, 0, minOf(samples.size, windowSize))
        
        // 1. Detecção de Pitch (YIN)
        val pitchFrame = pitchDetector.detect(analysisBuffer, samplePosition)
        
        // 2. Detecção de Onset/Offset
        val timingFrame = onsetDetector.analyze(analysisBuffer, pitchFrame, samplePosition)
        
        // Atualiza pitch frame com informação de onset/offset
        val updatedPitchFrame = pitchFrame.copy(
            isOnset = timingFrame.isOnset,
            isOffset = timingFrame.isOffset
        )
        
        lastPitchFrame = updatedPitchFrame
        
        // 3. Encontra nota atual baseado na posição
        updateCurrentNoteIndex(samplePosition)
        
        // 4. Armazena resultado para a nota atual
        if (currentNoteIndex in expectedNotes.indices) {
            val noteFrames = pitchFramesPerNote.getOrPut(currentNoteIndex) { mutableListOf() }
            noteFrames.add(updatedPitchFrame)
            
            // Registra onset
            if (timingFrame.isOnset && onsetSamplePerNote[currentNoteIndex] == null) {
                onsetSamplePerNote[currentNoteIndex] = samplePosition
            }
            
            // Registra offset
            if (timingFrame.isOffset && onsetSamplePerNote[currentNoteIndex] != null) {
                offsetSamplePerNote[currentNoteIndex] = samplePosition
            }
        }
        
        // 5. Gera estado de feedback atualizado
        val feedbackState = buildFeedbackState(samplePosition)
        onFeedbackUpdate(feedbackState)
    }
    
    private fun updateCurrentNoteIndex(samplePosition: Long) {
        // Encontra a nota que contém o sample position atual
        for (i in expectedNotes.indices) {
            if (expectedNotes[i].containsSample(samplePosition)) {
                currentNoteIndex = i
                return
            }
        }
        
        // Se passou do fim, mantém no último
        if (expectedNotes.isNotEmpty() && samplePosition >= expectedNotes.last().endSample) {
            currentNoteIndex = expectedNotes.size - 1
            phase = SolfegePhase.COMPLETED
        }
    }
    
    private fun buildFeedbackState(samplePosition: Long): SolfegeFeedbackState {
        val beatPosition = audioClock.sampleToBeat(samplePosition)
        val beatInMeasure = ((beatPosition.toLong() % audioClock.timeSignatureNumerator) + 1).toInt()
        
        // Constrói feedback por nota
        val noteFeedbacks = expectedNotes.mapIndexed { index, note ->
            buildNoteFeedback(index, note, samplePosition)
        }
        
        // Calcula scores gerais
        val completedFeedbacks = noteFeedbacks.filter { it.isCompleted }
        val overallPitchScore = if (completedFeedbacks.isNotEmpty()) {
            completedFeedbacks.map { it.pitchScore }.average().toFloat()
        } else 0f
        
        val overallTimingScore = if (completedFeedbacks.isNotEmpty()) {
            completedFeedbacks.map { it.durationAccuracy * 100 }.average().toFloat()
        } else 0f
        
        return SolfegeFeedbackState(
            currentSamplePosition = samplePosition,
            currentBeatPosition = beatPosition,
            currentNoteIndex = currentNoteIndex,
            currentBeatInMeasure = beatInMeasure,
            isDownbeat = beatInMeasure == 1,
            noteFeedbacks = noteFeedbacks,
            currentPitchHz = lastPitchFrame.frequency,
            currentMidiNote = lastPitchFrame.midiNote,
            currentCentDeviation = lastPitchFrame.centDeviation,
            isCurrentPitchCorrect = isCurrentPitchCorrect(),
            isVoiceDetected = lastPitchFrame.isVoiced,
            overallPitchScore = overallPitchScore,
            overallTimingScore = overallTimingScore,
            overallScore = (overallPitchScore + overallTimingScore) / 2,
            phase = phase,
            countdownNumber = 0
        )
    }
    
    private fun buildNoteFeedback(
        index: Int, 
        note: ExpectedNote,
        currentSample: Long
    ): NoteFeedbackState {
        val pitchFrames = pitchFramesPerNote[index] ?: emptyList()
        val onsetSample = onsetSamplePerNote[index]
        val offsetSample = offsetSamplePerNote[index]
        
        // Scoring de pitch
        val pitchResult = PitchScorer.score(pitchFrames, note.midiNote)
        
        // Scoring de timing
        val timingResult = TimingScorer.score(
            detectedOnsetSample = onsetSample,
            detectedOffsetSample = offsetSample,
            expectedStartSample = note.startSample,
            expectedEndSample = note.endSample,
            sampleRate = sampleRate
        )
        
        val isCompleted = currentSample >= note.endSample
        val isPending = currentSample < note.startSample
        val isCurrent = index == currentNoteIndex
        
        return NoteFeedbackState(
            noteId = note.id,
            pitchStatus = pitchResult.status,
            averageCentDeviation = pitchResult.avgCentDeviation,
            pitchScore = pitchResult.score,
            timingStatus = timingResult.status,
            attackDeviationSamples = timingResult.attackDeviationSamples,
            attackDeviationMs = timingResult.attackDeviationMs,
            durationAccuracy = timingResult.durationAccuracy,
            isCurrentNote = isCurrent,
            isCompleted = isCompleted,
            isPending = isPending
        )
    }
    
    private fun isCurrentPitchCorrect(): Boolean {
        if (!lastPitchFrame.isVoiced) return false
        if (currentNoteIndex !in expectedNotes.indices) return false
        
        val expectedNote = expectedNotes[currentNoteIndex]
        return lastPitchFrame.midiNote == expectedNote.midiNote &&
               kotlin.math.abs(lastPitchFrame.centDeviation) <= 25f
    }
    
    /**
     * Define a fase atual do exercício.
     */
    fun setPhase(newPhase: SolfegePhase) {
        phase = newPhase
    }
}
