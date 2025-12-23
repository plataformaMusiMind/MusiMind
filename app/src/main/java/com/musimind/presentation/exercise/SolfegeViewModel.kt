package com.musimind.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.music.audio.midi.MidiPlayer
import com.musimind.music.audio.pitch.*
import com.musimind.music.notation.model.*
import com.musimind.music.notation.parser.ScoreParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for solfege exercise
 */
@HiltViewModel
class SolfegeViewModel @Inject constructor(
    private val pitchDetector: PitchDetector,
    private val midiPlayer: MidiPlayer
) : ViewModel() {
    
    private val _state = MutableStateFlow(SolfegeState())
    val state: StateFlow<SolfegeState> = _state.asStateFlow()
    
    private var pitchListeningJob: kotlinx.coroutines.Job? = null
    private var consecutiveMatches = 0
    private val requiredMatches = 5 // Number of consecutive good matches to count as correct
    
    /**
     * Load exercise from ID (could be from Firebase or local JSON)
     */
    fun loadExercise(exerciseId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // For demo, create a simple exercise
                val notes = createDemoExercise()
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        exerciseId = exerciseId,
                        notes = notes,
                        totalNotes = notes.size,
                        currentNoteIndex = 0,
                        currentNote = notes.firstOrNull(),
                        hasPermission = pitchDetector.hasPermission()
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
     * Create a demo exercise with simple notes
     */
    private fun createDemoExercise(): List<Note> {
        // C major scale exercise
        val pitches = listOf(
            Pitch(NoteName.C, 4),
            Pitch(NoteName.D, 4),
            Pitch(NoteName.E, 4),
            Pitch(NoteName.F, 4),
            Pitch(NoteName.G, 4),
            Pitch(NoteName.A, 4),
            Pitch(NoteName.B, 4),
            Pitch(NoteName.C, 5)
        )
        
        return pitches.mapIndexed { index, pitch ->
            Note(
                id = "note_$index",
                durationBeats = 1f,
                pitch = pitch
            )
        }
    }
    
    /**
     * Start listening for pitch
     */
    fun startListening() {
        if (!pitchDetector.hasPermission()) {
            _state.update { it.copy(error = "Permissão de microfone necessária") }
            return
        }
        
        val targetPitch = _state.value.currentNote?.pitch ?: return
        
        consecutiveMatches = 0
        
        _state.update { 
            it.copy(
                isListening = true,
                currentNoteState = NoteState.HIGHLIGHTED,
                pitchDetectionState = it.pitchDetectionState.copy(
                    isListening = true,
                    targetPitch = targetPitch
                )
            )
        }
        
        pitchListeningJob = viewModelScope.launch {
            pitchDetector.startListening().collect { result ->
                handlePitchResult(result, targetPitch)
            }
        }
    }
    
    /**
     * Stop listening
     */
    fun stopListening() {
        pitchListeningJob?.cancel()
        pitchListeningJob = null
        pitchDetector.stopListening()
        
        _state.update {
            it.copy(
                isListening = false,
                pitchDetectionState = it.pitchDetectionState.copy(isListening = false)
            )
        }
    }
    
    /**
     * Handle pitch detection result
     */
    private fun handlePitchResult(result: PitchResult, targetPitch: Pitch) {
        val matchQuality = result.matchQuality(targetPitch)
        val isMatching = result.matchesPitch(targetPitch)
        
        _state.update {
            it.copy(
                pitchDetectionState = it.pitchDetectionState.copy(
                    currentPitch = result,
                    targetPitch = targetPitch,
                    matchQuality = matchQuality,
                    isMatching = isMatching,
                    consecutiveMatches = if (isMatching) consecutiveMatches else 0
                )
            )
        }
        
        if (isMatching) {
            consecutiveMatches++
            
            if (consecutiveMatches >= requiredMatches) {
                // Success!
                handleCorrectAnswer()
            }
        } else {
            consecutiveMatches = 0
        }
    }
    
    /**
     * Handle correct answer
     */
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
        
        // Auto advance after delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            nextNote()
        }
    }
    
    /**
     * Move to next note
     */
    fun nextNote() {
        val currentState = _state.value
        val nextIndex = currentState.currentNoteIndex + 1
        
        // Clear feedback
        _state.update { 
            it.copy(feedbackMessage = null, lastResult = null)
        }
        
        if (nextIndex >= currentState.notes.size) {
            // Exercise complete
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
     * Skip current note (costs a life)
     */
    fun skipNote() {
        val currentState = _state.value
        
        if (currentState.lives <= 1) {
            // Game over
            _state.update { it.copy(isComplete = true, lives = 0) }
            return
        }
        
        _state.update {
            it.copy(
                currentNoteState = NoteState.INCORRECT,
                lives = it.lives - 1,
                lastResult = ExerciseResult.SKIPPED,
                feedbackMessage = "Pulou! -1 ❤️"
            )
        }
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            nextNote()
        }
    }
    
    /**
     * Play current note
     */
    fun playCurrentNote() {
        val pitch = _state.value.currentNote?.pitch ?: return
        midiPlayer.playPitch(pitch, durationMs = 800)
    }
    
    /**
     * Toggle note playback setting
     */
    fun togglePlayback() {
        _state.update { it.copy(playbackEnabled = !it.playbackEnabled) }
    }
    
    override fun onCleared() {
        super.onCleared()
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
    val notes: List<Note> = emptyList(),
    val totalNotes: Int = 0,
    val currentNoteIndex: Int = 0,
    val currentNote: Note? = null,
    val currentNoteState: NoteState = NoteState.NORMAL,
    
    val isListening: Boolean = false,
    val playbackEnabled: Boolean = true,
    
    val pitchDetectionState: PitchDetectionState = PitchDetectionState(),
    
    val correctCount: Int = 0,
    val lives: Int = 3,
    
    val lastResult: ExerciseResult? = null,
    val feedbackMessage: String? = null
)

enum class ExerciseResult {
    CORRECT,
    INCORRECT,
    SKIPPED
}
