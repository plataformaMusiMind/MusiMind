package com.musimind.presentation.exercise

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 */
@HiltViewModel
class SolfegeViewModel @Inject constructor(
    private val pitchDetector: PitchDetector,
    private val midiPlayer: MidiPlayer,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(SolfegeState())
    val state: StateFlow<SolfegeState> = _state.asStateFlow()
    
    private var pitchListeningJob: kotlinx.coroutines.Job? = null
    private var consecutiveMatches = 0
    private val requiredMatches = 5
    
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
     * Load exercise
     */
    fun loadExercise(exerciseId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val notes = createDemoExercise()
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        exerciseId = exerciseId,
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
    
    private fun createDemoExercise(): List<Note> {
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
        if (!hasPermission()) {
            _state.update { it.copy(error = "Permissão de microfone necessária") }
            return
        }
        
        val targetNote = _state.value.currentNote ?: return
        consecutiveMatches = 0
        
        _state.update { 
            it.copy(
                isListening = true,
                currentNoteState = NoteState.HIGHLIGHTED
            )
        }
        
        pitchListeningJob = viewModelScope.launch {
            pitchDetector.startListening().collect { result ->
                handlePitchResult(result, targetNote)
            }
        }
    }
    
    fun stopListening() {
        pitchListeningJob?.cancel()
        pitchListeningJob = null
        pitchDetector.stopListening()
        
        _state.update {
            it.copy(isListening = false)
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
    
    fun skipNote() {
        val currentState = _state.value
        
        if (currentState.lives <= 1) {
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
            delay(1000)
            nextNote()
        }
    }
    
    fun playCurrentNote() {
        val pitch = _state.value.currentNote?.pitch ?: return
        midiPlayer.playPitch(pitch, durationMs = 800)
    }
    
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
    
    val currentPitchResult: PitchResult? = null,
    val isMatching: Boolean = false,
    
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
