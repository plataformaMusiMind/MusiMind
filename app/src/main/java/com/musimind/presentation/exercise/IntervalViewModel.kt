package com.musimind.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.music.audio.midi.MidiPlayer
import com.musimind.music.notation.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntervalViewModel @Inject constructor(
    private val midiPlayer: MidiPlayer
) : ViewModel() {
    
    private val _state = MutableStateFlow(IntervalState())
    val state: StateFlow<IntervalState> = _state.asStateFlow()
    
    fun loadExercise(exerciseId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val intervals = createDemoIntervals()
            val options = IntervalType.entries.take(9) // First 9 intervals
            
            _state.update {
                it.copy(
                    isLoading = false,
                    intervals = intervals,
                    totalIntervals = intervals.size,
                    currentIntervalIndex = 0,
                    currentInterval = intervals.firstOrNull(),
                    answerOptions = options
                )
            }
        }
    }
    
    private fun createDemoIntervals(): List<IntervalQuestion> {
        return listOf(
            IntervalQuestion(
                type = IntervalType.MAJOR_SECOND,
                lowerPosition = 0,  // C4
                upperPosition = 1   // D4
            ),
            IntervalQuestion(
                type = IntervalType.MAJOR_THIRD,
                lowerPosition = 0,  // C4
                upperPosition = 2   // E4
            ),
            IntervalQuestion(
                type = IntervalType.PERFECT_FOURTH,
                lowerPosition = 0,  // C4
                upperPosition = 3   // F4
            ),
            IntervalQuestion(
                type = IntervalType.PERFECT_FIFTH,
                lowerPosition = 0,  // C4
                upperPosition = 4   // G4
            ),
            IntervalQuestion(
                type = IntervalType.MAJOR_SIXTH,
                lowerPosition = 0,  // C4
                upperPosition = 5   // A4
            ),
            IntervalQuestion(
                type = IntervalType.MINOR_THIRD,
                lowerPosition = 2,  // E4
                upperPosition = 4   // G4
            ),
            IntervalQuestion(
                type = IntervalType.PERFECT_OCTAVE,
                lowerPosition = 0,  // C4
                upperPosition = 7   // C5
            ),
            IntervalQuestion(
                type = IntervalType.MINOR_SECOND,
                lowerPosition = 2,  // E4
                upperPosition = 3   // F4
            )
        ).shuffled()
    }
    
    fun playInterval() {
        val interval = _state.value.currentInterval ?: return
        
        viewModelScope.launch {
            // Get pitches from staff positions
            val lowerPitch = staffPositionToPitch(interval.lowerPosition)
            val upperPitch = staffPositionToPitch(interval.upperPosition)
            
            // Play melodically (one after another)
            midiPlayer.playPitch(lowerPitch, 600)
            delay(700)
            midiPlayer.playPitch(upperPitch, 600)
        }
    }
    
    private fun staffPositionToPitch(position: Int): Pitch {
        // Treble clef: position 0 = E4, position 4 = B4, etc.
        val notes = listOf(
            NoteName.E, NoteName.F, NoteName.G, NoteName.A, 
            NoteName.B, NoteName.C, NoteName.D, NoteName.E,
            NoteName.F, NoteName.G, NoteName.A, NoteName.B
        )
        val octaves = listOf(4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5)
        
        val safePosition = position.coerceIn(0, notes.size - 1)
        return Pitch(notes[safePosition], octaves[safePosition])
    }
    
    fun selectAnswer(answer: IntervalType) {
        if (_state.value.answerState != AnswerState.PENDING) return
        
        val correct = _state.value.currentInterval?.type == answer
        
        _state.update {
            it.copy(
                selectedAnswer = answer,
                answerState = if (correct) AnswerState.CORRECT else AnswerState.INCORRECT,
                correctCount = if (correct) it.correctCount + 1 else it.correctCount,
                lives = if (!correct) it.lives - 1 else it.lives,
                feedbackMessage = if (correct) "Correto! 🎵" else "Incorreto"
            )
        }
        
        // Check game over
        if (_state.value.lives <= 0) {
            viewModelScope.launch {
                delay(1500)
                _state.update { it.copy(isComplete = true) }
            }
        }
    }
    
    fun nextInterval() {
        val nextIndex = _state.value.currentIntervalIndex + 1
        
        if (nextIndex >= _state.value.intervals.size) {
            _state.update { it.copy(isComplete = true) }
        } else {
            _state.update {
                it.copy(
                    currentIntervalIndex = nextIndex,
                    currentInterval = it.intervals[nextIndex],
                    selectedAnswer = null,
                    answerState = AnswerState.PENDING,
                    feedbackMessage = null
                )
            }
        }
    }
}

data class IntervalState(
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,
    val intervals: List<IntervalQuestion> = emptyList(),
    val totalIntervals: Int = 0,
    val currentIntervalIndex: Int = 0,
    val currentInterval: IntervalQuestion? = null,
    val answerOptions: List<IntervalType> = emptyList(),
    val selectedAnswer: IntervalType? = null,
    val answerState: AnswerState = AnswerState.PENDING,
    val correctCount: Int = 0,
    val lives: Int = 3,
    val feedbackMessage: String? = null
)

data class IntervalQuestion(
    val type: IntervalType,
    val lowerPosition: Int,
    val upperPosition: Int
)

enum class AnswerState {
    PENDING, CORRECT, INCORRECT
}

enum class IntervalType(
    val semitones: Int,
    val shortName: String,
    val displayName: String
) {
    UNISON(0, "1J", "Uníssono"),
    MINOR_SECOND(1, "2m", "2ª menor"),
    MAJOR_SECOND(2, "2M", "2ª maior"),
    MINOR_THIRD(3, "3m", "3ª menor"),
    MAJOR_THIRD(4, "3M", "3ª maior"),
    PERFECT_FOURTH(5, "4J", "4ª justa"),
    TRITONE(6, "4A", "Trítono"),
    PERFECT_FIFTH(7, "5J", "5ª justa"),
    MINOR_SIXTH(8, "6m", "6ª menor"),
    MAJOR_SIXTH(9, "6M", "6ª maior"),
    MINOR_SEVENTH(10, "7m", "7ª menor"),
    MAJOR_SEVENTH(11, "7M", "7ª maior"),
    PERFECT_OCTAVE(12, "8J", "8ª justa")
}
