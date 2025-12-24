package com.musimind.presentation.exercise

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.ExerciseRepository
import com.musimind.music.audio.midi.MidiPlayer
import com.musimind.music.notation.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Melodic Perception exercise
 * 
 * Users input notes they hear and the system verifies correctness.
 */
@HiltViewModel
class MelodicPerceptionViewModel @Inject constructor(
    private val midiPlayer: MidiPlayer,
    private val exerciseRepository: ExerciseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(MelodicPerceptionState())
    val state: StateFlow<MelodicPerceptionState> = _state.asStateFlow()
    
    /**
     * Load exercise from repository
     */
    fun loadExercise(exerciseId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // For now, create a demo exercise
                val targetNotes = createDemoTargetNotes()
                
                val exercise = exerciseRepository.getExerciseById(exerciseId)
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        exerciseId = exerciseId,
                        exerciseTitle = exercise?.title ?: "Primeiros Passos (Dó-Ré-Mi)",
                        targetNotes = targetNotes,
                        userNotes = emptyList(),
                        currentNoteIndex = 0
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }
    
    /**
     * Demo target notes for the exercise
     */
    private fun createDemoTargetNotes(): List<Note> {
        val pitchData = listOf(
            Pitch(NoteName.C, 4) to 1f,
            Pitch(NoteName.D, 4) to 1f,
            Pitch(NoteName.E, 4) to 1f,
            Pitch(NoteName.D, 4) to 1f,
            Pitch(NoteName.C, 4) to 2f,
            Pitch(NoteName.E, 4) to 1f,
            Pitch(NoteName.D, 4) to 1f
        )
        
        return pitchData.mapIndexed { index, (pitch, duration) ->
            Note(
                id = "target_$index",
                durationBeats = duration,
                pitch = pitch,
                beatNumber = calculateBeatNumber(pitchData, index)
            )
        }
    }
    
    private fun calculateBeatNumber(notes: List<Pair<Pitch, Float>>, index: Int): Float {
        var beat = 1f
        for (i in 0 until index) {
            beat += notes[i].second
            if (beat > 4f) beat -= 4f
        }
        return beat
    }
    
    /**
     * Select a note for input
     */
    fun selectNote(note: NoteName) {
        _state.update { it.copy(selectedNote = note) }
    }
    
    /**
     * Change the selected octave
     */
    fun changeOctave(delta: Int) {
        _state.update {
            val newOctave = (it.selectedOctave + delta).coerceIn(2, 6)
            it.copy(selectedOctave = newOctave)
        }
    }
    
    /**
     * Select a duration
     */
    fun selectDuration(duration: Float) {
        _state.update { it.copy(selectedDuration = duration) }
    }
    
    /**
     * Select an accidental (or null to clear)
     */
    fun selectAccidental(accidental: AccidentalType?) {
        _state.update { it.copy(selectedAccidental = accidental) }
    }
    
    /**
     * Add a note based on current selection
     */
    fun addNote() {
        val currentState = _state.value
        val noteName = currentState.selectedNote ?: return
        
        val alteration = when (currentState.selectedAccidental) {
            AccidentalType.FLAT -> -1
            AccidentalType.SHARP -> 1
            AccidentalType.DOUBLE_FLAT -> -2
            AccidentalType.DOUBLE_SHARP -> 2
            else -> 0
        }
        
        val pitch = Pitch(noteName, currentState.selectedOctave, alteration)
        val newNote = Note(
            id = "user_${currentState.userNotes.size}",
            durationBeats = currentState.selectedDuration,
            pitch = pitch,
            accidental = currentState.selectedAccidental,
            beatNumber = calculateUserBeatNumber(currentState.userNotes, currentState.selectedDuration)
        )
        
        _state.update {
            it.copy(
                userNotes = it.userNotes + newNote,
                currentNoteIndex = it.userNotes.size,
                showFeedback = false,
                feedbackMessage = null
            )
        }
    }
    
    private fun calculateUserBeatNumber(notes: List<MusicElement>, newDuration: Float): Float {
        var beat = 1f
        for (note in notes) {
            beat += note.durationBeats
            while (beat > 4f) beat -= 4f
        }
        return beat
    }
    
    /**
     * Add a rest
     */
    fun addRest() {
        val currentState = _state.value
        
        val newRest = Rest(
            id = "user_rest_${currentState.userNotes.size}",
            durationBeats = currentState.selectedDuration
        )
        
        _state.update {
            it.copy(
                userNotes = it.userNotes + newRest,
                currentNoteIndex = it.userNotes.size,
                showFeedback = false,
                feedbackMessage = null
            )
        }
    }
    
    /**
     * Undo the last note
     */
    fun undoLastNote() {
        _state.update {
            if (it.userNotes.isNotEmpty()) {
                it.copy(
                    userNotes = it.userNotes.dropLast(1),
                    currentNoteIndex = maxOf(0, it.userNotes.size - 2),
                    showFeedback = false,
                    feedbackMessage = null
                )
            } else it
        }
    }
    
    /**
     * Verify user input against target
     */
    fun verify() {
        val currentState = _state.value
        val targetNotes = currentState.targetNotes
        val userNotes = currentState.userNotes.filterIsInstance<Note>()
        
        if (userNotes.isEmpty()) {
            _state.update {
                it.copy(
                    feedbackMessage = "Adicione as notas antes de verificar",
                    showFeedback = false
                )
            }
            return
        }
        
        val feedbackResults = mutableMapOf<Int, Boolean>()
        var correctCount = 0
        
        userNotes.forEachIndexed { index, userNote ->
            val targetNote = targetNotes.getOrNull(index)
            val isCorrect = if (targetNote != null) {
                userNote.pitch.midiPitch == targetNote.pitch.midiPitch &&
                userNote.durationBeats == targetNote.durationBeats
            } else {
                false // Extra notes are wrong
            }
            
            feedbackResults[index] = isCorrect
            if (isCorrect) correctCount++
        }
        
        // Check for missing notes
        if (userNotes.size < targetNotes.size) {
            val missingCount = targetNotes.size - userNotes.size
            _state.update {
                it.copy(
                    feedbackResults = feedbackResults,
                    showFeedback = true,
                    correctCount = correctCount,
                    allCorrect = false,
                    feedbackMessage = "Faltam $missingCount nota(s). Você acertou $correctCount de ${targetNotes.size}."
                )
            }
        } else if (userNotes.size > targetNotes.size) {
            val extraCount = userNotes.size - targetNotes.size
            _state.update {
                it.copy(
                    feedbackResults = feedbackResults,
                    showFeedback = true,
                    correctCount = correctCount,
                    allCorrect = false,
                    feedbackMessage = "Você adicionou $extraCount nota(s) a mais. Acertou $correctCount de ${targetNotes.size}."
                )
            }
        } else {
            val allCorrect = correctCount == targetNotes.size
            _state.update {
                it.copy(
                    feedbackResults = feedbackResults,
                    showFeedback = true,
                    correctCount = correctCount,
                    allCorrect = allCorrect,
                    feedbackMessage = if (allCorrect) {
                        "Parabéns! Todas as notas corretas! 🎉"
                    } else {
                        "Você acertou $correctCount de ${targetNotes.size}."
                    }
                )
            }
            
            if (allCorrect) {
                viewModelScope.launch {
                    delay(2000)
                    _state.update { it.copy(isComplete = true) }
                }
            }
        }
    }
    
    /**
     * Play the target melody with metronome countdown
     */
    fun playMelody() {
        viewModelScope.launch {
            val notes = _state.value.targetNotes
            val tempo = 80 // BPM
            val msPerBeat = (60000 / tempo).toLong()
            
            // Metronome countdown - 1 measure (4 beats in 4/4)
            for (beat in 1..4) {
                midiPlayer.playMetronomeClick(isAccented = beat == 1)
                delay(msPerBeat)
            }
            
            // Play melody
            for (note in notes) {
                midiPlayer.playPitch(note.pitch, durationMs = (msPerBeat * note.durationBeats * 0.9).toInt())
                delay((msPerBeat * note.durationBeats).toLong())
            }
        }
    }
    
    /**
     * Navigate to previous note
     */
    fun previousNote() {
        _state.update {
            if (it.currentNoteIndex > 0) {
                it.copy(currentNoteIndex = it.currentNoteIndex - 1)
            } else it
        }
    }
    
    /**
     * Navigate to next note
     */
    fun nextNote() {
        _state.update {
            if (it.currentNoteIndex < it.userNotes.size - 1) {
                it.copy(currentNoteIndex = it.currentNoteIndex + 1)
            } else it
        }
    }
    
    /**
     * Show help dialog
     */
    fun showHelp() {
        // TODO: Show help dialog
        _state.update {
            it.copy(feedbackMessage = "Ouça a melodia e digite as notas que você ouvir!")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        midiPlayer.stop()
    }
}

/**
 * State for Melodic Perception exercise
 */
data class MelodicPerceptionState(
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,
    val error: String? = null,
    
    val exerciseId: String = "",
    val exerciseTitle: String = "Primeiros Passos (Dó-Ré-Mi)",
    val clef: ClefType = ClefType.TREBLE,
    
    val targetNotes: List<Note> = emptyList(),
    val userNotes: List<MusicElement> = emptyList(),
    val currentNoteIndex: Int = 0,
    
    // Input state
    val selectedNote: NoteName? = null,
    val selectedOctave: Int = 4,
    val selectedDuration: Float = 1f, // Quarter note default
    val selectedAccidental: AccidentalType? = null,
    
    // Feedback
    val showFeedback: Boolean = false,
    val feedbackResults: Map<Int, Boolean> = emptyMap(),
    val feedbackMessage: String? = null,
    val correctCount: Int = 0,
    val allCorrect: Boolean = false
)
