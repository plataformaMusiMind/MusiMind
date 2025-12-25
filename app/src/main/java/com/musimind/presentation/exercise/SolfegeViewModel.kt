package com.musimind.presentation.exercise

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.ExerciseRepository
import com.musimind.domain.model.SolfegeNote
import com.musimind.music.audio.core.*
import com.musimind.music.audio.engine.SolfegeAudioEngine
import com.musimind.music.audio.midi.MidiPlayer
import com.musimind.music.audio.pitch.PitchDetector
import com.musimind.music.audio.pitch.PitchResult
import com.musimind.music.audio.pitch.PitchUtils
import com.musimind.music.notation.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.math.abs

/**
 * ViewModel for solfege exercise
 * 
 * Loads exercise data from Supabase via ExerciseRepository
 */
@HiltViewModel
class SolfegeViewModel @Inject constructor(
    private val pitchDetector: PitchDetector,
    private val midiPlayer: MidiPlayer,
    private val exerciseRepository: ExerciseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(SolfegeState())
    val state: StateFlow<SolfegeState> = _state.asStateFlow()
    
    // New: Audio Engine for sample-accurate playback and analysis
    private val audioEngine = SolfegeAudioEngine(context, midiPlayer)
    
    // Expose feedback state from audio engine
    val audioFeedbackState: StateFlow<SolfegeFeedbackState> = audioEngine.feedbackState
    
    private var pitchListeningJob: kotlinx.coroutines.Job? = null
    private var consecutiveMatches = 0
    private val requiredMatches = 5
    
    init {
        // Observe audio engine feedback and update state
        viewModelScope.launch {
            audioEngine.feedbackState.collect { feedback ->
                updateStateFromFeedback(feedback)
            }
        }
    }
    
    private fun updateStateFromFeedback(feedback: SolfegeFeedbackState) {
        _state.update { state ->
            // Update notes with feedback colors
            val updatedNotes = if (feedback.noteFeedbacks.isNotEmpty()) {
                state.notes.mapIndexed { index, note ->
                    val noteFeedback = feedback.noteFeedbacks.getOrNull(index)
                    if (noteFeedback != null) {
                        note.copy(
                            pitchFeedback = when (noteFeedback.pitchStatus) {
                                PitchStatus.CORRECT -> FeedbackState.CORRECT
                                PitchStatus.FLAT, PitchStatus.SHARP -> FeedbackState.INCORRECT
                                PitchStatus.NOT_EVALUATED -> FeedbackState.NONE
                            },
                            durationFeedback = when (noteFeedback.timingStatus) {
                                TimingStatus.ON_TIME -> FeedbackState.CORRECT
                                TimingStatus.EARLY, TimingStatus.LATE -> FeedbackState.INCORRECT
                                TimingStatus.NOT_PLAYED -> FeedbackState.NONE
                            }
                        )
                    } else {
                        note
                    }
                }
            } else {
                state.notes
            }
            
            state.copy(
                notes = updatedNotes,
                currentBeat = feedback.currentBeatInMeasure,
                currentNoteIndex = feedback.currentNoteIndex, // Always use feedback value (including -1 for reset)
                isPlaying = feedback.phase == SolfegePhase.PLAYING || feedback.phase == SolfegePhase.COUNTDOWN,
                statusText = when (feedback.phase) {
                    SolfegePhase.IDLE -> "pronto"
                    SolfegePhase.COUNTDOWN -> "contando... ${feedback.countdownNumber}"
                    SolfegePhase.PLAYING -> "tocando..."
                    SolfegePhase.LISTENING -> "ouvindo..."
                    SolfegePhase.COMPLETED -> "concluído!"
                },
                isListening = feedback.phase == SolfegePhase.LISTENING,
                currentPitchResult = if (feedback.isVoiceDetected) {
                    PitchResult.Detected(
                        frequency = feedback.currentPitchHz,
                        note = PitchUtils.frequencyToNoteName(feedback.currentPitchHz),
                        cents = feedback.currentCentDeviation.toInt(),
                        amplitude = 0.9f
                    )
                } else null,
                isMatching = feedback.isCurrentPitchCorrect,
                // Ghost note - show where user is singing only during LISTENING phase
                ghostNoteMidi = if (feedback.isVoiceDetected && feedback.phase == SolfegePhase.LISTENING) {
                    feedback.currentMidiNote
                } else null
            )
        }
    }
    
    /**
     * Check microphone permission
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Load exercise from Supabase
     */
    fun loadExercise(exerciseId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // Load solfege notes from Supabase
                val solfegeNotes = exerciseRepository.getSolfegeNotes(exerciseId)
                
                val notes = if (solfegeNotes.isNotEmpty()) {
                    // Convert SolfegeNote to Note
                    solfegeNotes.map { solfegeNote ->
                        solfegeNoteToNote(solfegeNote)
                    }
                } else {
                    // Fallback to demo exercise if no notes in database
                    createDemoExercise()
                }
                
                // Load exercise metadata
                val exercise = exerciseRepository.getExerciseById(exerciseId)
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        exerciseId = exerciseId,
                        exerciseTitle = exercise?.title ?: "Solfejo",
                        xpReward = exercise?.xpReward ?: 10,
                        notes = notes,
                        totalNotes = notes.size,
                        currentNoteIndex = 0,
                        currentNote = notes.firstOrNull(),
                        hasPermission = hasPermission()
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message
                    ) 
                }
            }
        }
    }
    
    /**
     * Convert SolfegeNote (from Supabase) to Note (internal model)
     */
    private fun solfegeNoteToNote(solfegeNote: SolfegeNote): Note {
        val pitch = midiToPitch(solfegeNote.pitch)
        return Note(
            id = solfegeNote.id,
            durationBeats = solfegeNote.durationBeats,
            pitch = pitch,
            solfegeName = pitchToSolfegeName(pitch),
            beatNumber = null // Will be calculated during layout
        )
    }
    
    /**
     * Convert Pitch to solfege name (Dó, Ré, Mi, etc.)
     */
    private fun pitchToSolfegeName(pitch: Pitch): String {
        val baseName = when (pitch.note) {
            NoteName.C -> "Dó"
            NoteName.D -> "Ré"
            NoteName.E -> "Mi"
            NoteName.F -> "Fá"
            NoteName.G -> "Sol"
            NoteName.A -> "Lá"
            NoteName.B -> "Si"
        }
        val alteration = when (pitch.alteration) {
            1 -> "♯"
            -1 -> "♭"
            else -> ""
        }
        return "$baseName$alteration"
    }
    
    /**
     * Convert MIDI note number to Pitch
     */
    private fun midiToPitch(midiNote: Int): Pitch {
        val octave = (midiNote / 12) - 1
        val noteInOctave = midiNote % 12
        
        val (noteName, alteration) = when (noteInOctave) {
            0 -> NoteName.C to 0
            1 -> NoteName.C to 1  // C#
            2 -> NoteName.D to 0
            3 -> NoteName.D to 1  // D#
            4 -> NoteName.E to 0
            5 -> NoteName.F to 0
            6 -> NoteName.F to 1  // F#
            7 -> NoteName.G to 0
            8 -> NoteName.G to 1  // G#
            9 -> NoteName.A to 0
            10 -> NoteName.A to 1 // A#
            11 -> NoteName.B to 0
            else -> NoteName.C to 0
        }
        
        return Pitch(noteName, octave, alteration)
    }
    
    /**
     * Demo exercise for when no data in database
     */
    private fun createDemoExercise(): List<Note> {
        val pitchData = listOf(
            Triple(Pitch(NoteName.C, 4), "Dó", 1f),
            Triple(Pitch(NoteName.D, 4), "Ré", 2f),
            Triple(Pitch(NoteName.E, 4), "Mi", 3f),
            Triple(Pitch(NoteName.F, 4), "Fá", 4f),
            Triple(Pitch(NoteName.G, 4), "Sol", 1f),
            Triple(Pitch(NoteName.A, 4), "Lá", 2f),
            Triple(Pitch(NoteName.B, 4), "Si", 3f),
            Triple(Pitch(NoteName.C, 5), "Dó", 4f)
        )
        
        return pitchData.mapIndexed { index, (pitch, solfege, beat) ->
            Note(
                id = "note_$index",
                durationBeats = 1f,
                pitch = pitch,
                solfegeName = solfege,
                beatNumber = beat
            )
        }
    }
    
    /**
     * Reset all feedback state before starting a new playback or solfege session.
     * Clears note feedback colors, resets highlight to first note, clears ghost note.
     */
    private fun resetForNewExercise() {
        _state.update { state ->
            state.copy(
                // Reset all notes to clean feedback state
                notes = state.notes.map { note ->
                    note.copy(
                        pitchFeedback = FeedbackState.NONE,
                        durationFeedback = FeedbackState.NONE
                    )
                },
                // Set highlight to first note (0), not -1
                currentNoteIndex = 0,
                // Reset other visual state
                currentBeat = 0,
                ghostNoteMidi = null,
                currentPitchResult = null,
                isMatching = false,
                statusText = "preparando..."
            )
        }
    }
    
    /**
     * Play entire melody with sample-accurate audio engine.
     * Uses the new SolfegeAudioEngine for precise synchronization.
     * Mode: PLAYBACK (com piano)
     */
    fun playMelody() {
        val currentState = _state.value
        if (currentState.isPlaying) return
        
        // Reset all feedback state before starting
        resetForNewExercise()
        
        // Convert Note list to ExpectedNote for audio engine
        val expectedNotes = convertToExpectedNotes(_state.value.notes)
        
        // Configure audio engine
        audioEngine.configure(
            notes = expectedNotes,
            tempo = _state.value.tempo.toDouble(),
            timeSignatureNumerator = 4,
            timeSignatureDenominator = 4,
            octaveOffset = _state.value.currentOctave
        )
        
        // Start playback WITH piano (user listens)
        audioEngine.startPlayback(playPiano = true)
    }
    
    /**
     * Convert internal Note model to ExpectedNote for audio engine.
     */
    private fun convertToExpectedNotes(notes: List<Note>): List<ExpectedNote> {
        var currentBeat = 0.0
        return notes.mapIndexed { index, note ->
            val expectedNote = ExpectedNote(
                id = note.id,
                midiNote = pitchToMidi(note.pitch),
                startBeat = currentBeat,
                durationBeats = note.durationBeats.toDouble(),
                solfegeName = note.solfegeName ?: ""
            )
            currentBeat += note.durationBeats
            expectedNote
        }
    }
    
    /**
     * Convert Pitch to MIDI note number.
     */
    private fun pitchToMidi(pitch: Pitch): Int {
        val noteValues = mapOf(
            NoteName.C to 0, NoteName.D to 2, NoteName.E to 4, NoteName.F to 5,
            NoteName.G to 7, NoteName.A to 9, NoteName.B to 11
        )
        val baseNote = noteValues[pitch.note] ?: 0
        return (pitch.octave + 1) * 12 + baseNote + pitch.alteration
    }
    
    /**
     * Stop playback.
     */
    fun stopPlayback() {
        audioEngine.stop()
        _state.update { it.copy(isPlaying = false, statusText = "pronto", currentBeat = 0) }
    }
    
    /**
     * Navigate to previous note
     */
    fun previousNote() {
        val currentIndex = _state.value.currentNoteIndex
        if (currentIndex > 0) {
            _state.update {
                it.copy(
                    currentNoteIndex = currentIndex - 1,
                    currentNote = it.notes.getOrNull(currentIndex - 1),
                    currentNoteState = NoteState.NORMAL,
                    feedbackMessage = null
                )
            }
        }
    }
    
    /**
     * Change octave offset
     */
    fun changeOctave(delta: Int) {
        _state.update {
            val newOctave = (it.currentOctave + delta).coerceIn(-1, 1)
            it.copy(currentOctave = newOctave)
        }
    }
    
    /**
     * Toggle beat numbers display
     */
    fun toggleBeatNumbers() {
        _state.update { it.copy(showBeatNumbers = !it.showBeatNumbers) }
    }
    
    /**
     * Toggle solfege names display
     */
    fun toggleSolfegeNames() {
        _state.update { it.copy(showSolfegeNames = !it.showSolfegeNames) }
    }
    
    /**
     * Play the reference/tonic note for the current key signature.
     * This helps users find the starting pitch.
     */
    fun playReferenceNote() {
        viewModelScope.launch {
            val keySignature = _state.value.keySignature
            val octaveOffset = _state.value.currentOctave
            
            // Get MIDI note for the tonic of current key
            val baseMidiNote = when (keySignature) {
                "C" -> 60  // C4
                "G" -> 67  // G4
                "D" -> 62  // D4
                "A" -> 69  // A4
                "E" -> 64  // E4
                "B" -> 71  // B4
                "F#", "Gb" -> 66  // F#4
                "C#", "Db" -> 61  // C#4
                "F" -> 65  // F4
                "Bb" -> 70 // Bb4
                "Eb" -> 63 // Eb4
                "Ab" -> 68 // Ab4
                else -> 60 // Default to C4
            }
            
            // Apply octave offset for the reference note
            val adjustedMidiNote = baseMidiNote + (octaveOffset * 12)
            
            midiPlayer.playMidiNote(adjustedMidiNote, durationMs = 1500)
        }
    }
    
    /**
     * Start solfege mode (user sings).
     * Mode: SOLFEGE (sem piano, com deteção de pitch)
     */
    fun startListening() {
        if (!hasPermission()) {
            _state.update { it.copy(
                error = "Permissão de microfone necessária",
                statusText = "Sem mic!" 
            ) }
            return
        }
        
        // Reset all feedback state before starting
        resetForNewExercise()
        
        // Convert notes for audio engine
        val expectedNotes = convertToExpectedNotes(_state.value.notes)
        
        // Configure audio engine for solfege mode
        audioEngine.configure(
            notes = expectedNotes,
            tempo = _state.value.tempo.toDouble(),
            timeSignatureNumerator = 4,
            timeSignatureDenominator = 4,
            octaveOffset = _state.value.currentOctave
        )
        
        // Start listening - engine handles countdown internally
        val success = audioEngine.startListening()
        
        if (!success) {
            _state.update { it.copy(
                error = "Não foi possível iniciar o microfone",
                statusText = "Erro mic!"
            ) }
            return
        }
        
        consecutiveMatches = 0
        
        _state.update { 
            it.copy(
                isListening = true,
                currentNoteState = NoteState.HIGHLIGHTED,
                statusText = "ouvindo..."
            )
        }
    }
    
    fun stopListening() {
        audioEngine.stop()
        pitchListeningJob?.cancel()
        pitchListeningJob = null
        pitchDetector.stopListening()
        
        _state.update {
            it.copy(isListening = false, statusText = "pronto")
        }
    }
    
    private fun handlePitchResult(result: PitchResult, targetNote: Note) {
        when (result) {
            is PitchResult.Detected -> {
                val targetFreq = pitchToFrequency(targetNote.pitch)
                val isMatching = result.matchesPitch(
                    PitchUtils.frequencyToNoteName(targetFreq), 
                    tolerance = 30
                )
                
                _state.update {
                    it.copy(
                        currentPitchResult = result,
                        isMatching = isMatching
                    )
                }
                
                if (isMatching) {
                    consecutiveMatches++
                    if (consecutiveMatches >= requiredMatches) {
                        handleCorrectAnswer()
                    }
                } else {
                    consecutiveMatches = 0
                }
            }
            is PitchResult.NoSound -> {
                _state.update { 
                    it.copy(
                        currentPitchResult = result,
                        isMatching = false
                    )
                }
                consecutiveMatches = 0
            }
            is PitchResult.Error -> {
                _state.update { it.copy(error = result.message) }
            }
        }
    }
    
    private fun pitchToFrequency(pitch: Pitch): Float {
        val noteValues = mapOf(
            NoteName.C to 0, NoteName.D to 2, NoteName.E to 4, NoteName.F to 5,
            NoteName.G to 7, NoteName.A to 9, NoteName.B to 11
        )
        val baseNote = noteValues[pitch.note] ?: 0
        val midiNote = (pitch.octave + 1) * 12 + baseNote + pitch.alteration
        return PitchUtils.midiToFrequency(midiNote)
    }
    
    private fun handleCorrectAnswer() {
        stopListening()
        
        _state.update {
            it.copy(
                currentNoteState = NoteState.CORRECT,
                correctCount = it.correctCount + 1,
                lastResult = ExerciseResult.CORRECT,
                feedbackMessage = "Perfeito! 🎉"
            )
        }
        
        viewModelScope.launch {
            delay(1500)
            nextNote()
        }
    }
    
    fun nextNote() {
        val currentState = _state.value
        val nextIndex = currentState.currentNoteIndex + 1
        
        _state.update { 
            it.copy(feedbackMessage = null, lastResult = null)
        }
        
        if (nextIndex >= currentState.notes.size) {
            // Exercise complete - save progress to Supabase
            saveProgress()
            _state.update { it.copy(isComplete = true) }
        } else {
            _state.update {
                it.copy(
                    currentNoteIndex = nextIndex,
                    currentNote = it.notes.getOrNull(nextIndex),
                    currentNoteState = NoteState.NORMAL
                )
            }
        }
    }
    
    /**
     * Save exercise progress to Supabase
     */
    private fun saveProgress() {
        viewModelScope.launch {
            val currentState = _state.value
            exerciseRepository.saveProgress(
                exerciseId = currentState.exerciseId,
                score = currentState.correctCount,
                totalQuestions = currentState.totalNotes,
                correctAnswers = currentState.correctCount,
                timeSpentSeconds = 0, // TODO: track time
                xpEarned = calculateXpEarned(),
                coinsEarned = currentState.correctCount * 2
            )
        }
    }
    
    private fun calculateXpEarned(): Int {
        val currentState = _state.value
        val accuracy = if (currentState.totalNotes > 0) {
            currentState.correctCount.toFloat() / currentState.totalNotes
        } else 0f
        
        // Base XP * accuracy multiplier
        return (currentState.xpReward * accuracy).toInt()
    }
    
    fun playCurrentNote() {
        val pitch = _state.value.currentNote?.pitch ?: return
        midiPlayer.playPitch(pitch, durationMs = 800)
    }
    
    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        stopListening()
        midiPlayer.stop()
    }
}

/**
 * State for solfege exercise
 */
data class SolfegeState(
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,
    val error: String? = null,
    val hasPermission: Boolean = false,
    
    val exerciseId: String = "",
    val exerciseTitle: String = "Passos Lentos em Dó",
    val xpReward: Int = 10,
    val notes: List<Note> = emptyList(),
    val totalNotes: Int = 0,
    val currentNoteIndex: Int = -1,  // -1 means no note highlighted until playback starts
    val currentNote: Note? = null,
    val currentNoteState: NoteState = NoteState.NORMAL,
    
    val isListening: Boolean = false,
    val playbackEnabled: Boolean = true,
    
    val currentPitchResult: PitchResult? = null,
    val isMatching: Boolean = false,
    
    // Ghost note - shows where user is actually singing (MIDI note number)
    val ghostNoteMidi: Int? = null,
    
    val correctCount: Int = 0,
    val lives: Int = 3,
    
    val lastResult: ExerciseResult? = null,
    val feedbackMessage: String? = null,
    
    // New fields for redesigned screen
    val keySignature: String = "C",
    val timeSignature: String = "4/4",
    val clef: ClefType = ClefType.TREBLE,
    val tempo: Int = 60,
    val currentOctave: Int = 0, // -1, 0, +1 for -8va, normal, +8va
    val showBeatNumbers: Boolean = true,
    val showSolfegeNames: Boolean = true,
    val statusText: String = "pronto",
    val isPlaying: Boolean = false,
    val currentBeat: Int = 0 // Visual Metronome State
)

enum class ExerciseResult {
    CORRECT,
    INCORRECT,
    SKIPPED
}
