package com.musimind.presentation.exercise

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.ExerciseRepository
import com.musimind.domain.model.SolfegeNote
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
                // Get exercise metadata
                val exercise = exerciseRepository.getExerciseById(exerciseId)
                
                // Try to fetch notes from Supabase first
                val supabaseNotes = exerciseRepository.getMelodicNotes(exerciseId)
                
                // If Supabase has notes, convert them; otherwise use hardcoded fallback
                val targetNotes = if (supabaseNotes.isNotEmpty()) {
                    convertSupabaseNotes(supabaseNotes)
                } else {
                    createExerciseNotes(exerciseId)
                }
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        exerciseId = exerciseId,
                        exerciseTitle = exercise?.title ?: "Percepção Melódica",
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
     * Convert SolfegeNote from Supabase to Note for UI
     */
    private fun convertSupabaseNotes(solfegeNotes: List<SolfegeNote>): List<Note> {
        var currentBeat = 1f
        return solfegeNotes.mapIndexed { index, sn ->
            val note = Note(
                id = sn.id.ifEmpty { "note_$index" },
                durationBeats = sn.durationBeats,
                pitch = midiToPitch(sn.pitch),
                beatNumber = currentBeat
            )
            currentBeat += sn.durationBeats
            if (currentBeat > 4f) currentBeat -= 4f
            note
        }
    }
    
    /**
     * Convert MIDI pitch number to Pitch object
     */
    private fun midiToPitch(midi: Int): Pitch {
        val noteNames = listOf(NoteName.C, NoteName.C, NoteName.D, NoteName.D, NoteName.E, 
                               NoteName.F, NoteName.F, NoteName.G, NoteName.G, NoteName.A, 
                               NoteName.A, NoteName.B)
        val alterations = listOf(0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0) // 0 = natural, 1 = sharp
        
        val noteInOctave = midi % 12
        val octave = (midi / 12) - 1
        
        return Pitch(
            note = noteNames[noteInOctave],
            octave = octave,
            alteration = alterations[noteInOctave]
        )
    }
    
    /**
     * Create target notes for the exercise based on exerciseId
     */
    private fun createExerciseNotes(exerciseId: String): List<Note> {
        // Select melody based on exercise ID
        val melodyData = when {
            exerciseId.contains("melodic_1") -> listOf(
                // Exercise 1: Dó-Ré-Mi stepwise
                Pitch(NoteName.C, 4) to 1f,
                Pitch(NoteName.D, 4) to 1f,
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.D, 4) to 1f,
                Pitch(NoteName.C, 4) to 2f
            )
            exerciseId.contains("melodic_2") -> listOf(
                // Exercise 2: Mi-Ré-Dó descending
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.D, 4) to 1f,
                Pitch(NoteName.C, 4) to 2f
            )
            exerciseId.contains("melodic_3") -> listOf(
                // Exercise 3: Dó-Mi-Sol arpeggio
                Pitch(NoteName.C, 4) to 1f,
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.G, 4) to 2f
            )
            exerciseId.contains("melodic_4") -> listOf(
                // Exercise 4: Sol-Mi-Dó descending arpeggio
                Pitch(NoteName.G, 4) to 1f,
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.C, 4) to 2f
            )
            exerciseId.contains("melodic_5") -> listOf(
                // Exercise 5: Dó-Ré-Mi-Fá-Sol ascending scale
                Pitch(NoteName.C, 4) to 1f,
                Pitch(NoteName.D, 4) to 1f,
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.F, 4) to 1f,
                Pitch(NoteName.G, 4) to 2f
            )
            exerciseId.contains("melodic_6") -> listOf(
                // Exercise 6: Sol-Fá-Mi-Ré-Dó descending scale
                Pitch(NoteName.G, 4) to 1f,
                Pitch(NoteName.F, 4) to 1f,
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.D, 4) to 1f,
                Pitch(NoteName.C, 4) to 2f
            )
            exerciseId.contains("melodic_7") -> listOf(
                // Exercise 7: Dó-Mi-Dó-Sol thirds and fifth
                Pitch(NoteName.C, 4) to 1f,
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.C, 4) to 1f,
                Pitch(NoteName.G, 4) to 1f,
                Pitch(NoteName.C, 4) to 2f
            )
            exerciseId.contains("melodic_8") -> listOf(
                // Exercise 8: Dó-Fá-Mi-Ré-Dó with fourth
                Pitch(NoteName.C, 4) to 1f,
                Pitch(NoteName.F, 4) to 1f,
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.D, 4) to 1f,
                Pitch(NoteName.C, 4) to 2f
            )
            exerciseId.contains("melodic_9") -> listOf(
                // Exercise 9: Mi-Sol-Mi-Dó repeated pattern
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.G, 4) to 1f,
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.C, 4) to 2f
            )
            exerciseId.contains("melodic_10") -> listOf(
                // Exercise 10: Dó-Sol-Mi-Dó melodic phrase
                Pitch(NoteName.C, 4) to 1f,
                Pitch(NoteName.G, 4) to 1f,
                Pitch(NoteName.E, 4) to 1f,
                Pitch(NoteName.C, 4) to 2f
            )
            else -> listOf(
                // Default: simple Dó-Ré-Mi
                Pitch(NoteName.C, 4) to 1f,
                Pitch(NoteName.D, 4) to 1f,
                Pitch(NoteName.E, 4) to 2f
            )
        }
        
        return melodyData.mapIndexed { index, (pitch, duration) ->
            Note(
                id = "target_$index",
                durationBeats = duration,
                pitch = pitch,
                beatNumber = calculateBeatNumber(melodyData, index)
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
        _state.update {
            it.copy(feedbackMessage = "Ouça a melodia e digite as notas que você ouvir!")
        }
    }
    
    /**
     * Play what the user has written
     */
    fun playUserNotes() {
        viewModelScope.launch {
            val notes = _state.value.userNotes.filterIsInstance<Note>()
            if (notes.isEmpty()) {
                _state.update { it.copy(feedbackMessage = "Adicione notas primeiro!") }
                return@launch
            }
            
            val tempo = 80 // BPM
            val msPerBeat = (60000 / tempo).toLong()
            
            // Metronome countdown
            for (beat in 1..4) {
                midiPlayer.playMetronomeClick(isAccented = beat == 1)
                delay(msPerBeat)
            }
            
            // Play user's notes
            for (note in notes) {
                midiPlayer.playPitch(note.pitch, durationMs = (msPerBeat * note.durationBeats * 0.9).toInt())
                delay((msPerBeat * note.durationBeats).toLong())
            }
        }
    }
    
    /**
     * Play the target melody starting from a specific beat
     */
    fun playMelodyFromBeat(startBeat: Float) {
        viewModelScope.launch {
            val notes = _state.value.targetNotes
            val tempo = 80 // BPM
            val msPerBeat = (60000 / tempo).toLong()
            
            // Calculate which note to start from
            var currentBeat = 1f
            var startIndex = 0
            for ((index, note) in notes.withIndex()) {
                if (currentBeat >= startBeat) {
                    startIndex = index
                    break
                }
                currentBeat += note.durationBeats
            }
            
            // Play from that point without countdown
            for (i in startIndex until notes.size) {
                val note = notes[i]
                midiPlayer.playPitch(note.pitch, durationMs = (msPerBeat * note.durationBeats * 0.9).toInt())
                delay((msPerBeat * note.durationBeats).toLong())
            }
        }
    }
    
    /**
     * Play user's notes starting from a specific beat
     */
    fun playUserNotesFromBeat(startBeat: Float) {
        viewModelScope.launch {
            val notes = _state.value.userNotes.filterIsInstance<Note>()
            if (notes.isEmpty()) return@launch
            
            val tempo = 80 // BPM
            val msPerBeat = (60000 / tempo).toLong()
            
            // Calculate which note to start from
            var currentBeat = 1f
            var startIndex = 0
            for ((index, note) in notes.withIndex()) {
                if (currentBeat >= startBeat) {
                    startIndex = index
                    break
                }
                currentBeat += note.durationBeats
            }
            
            // Play from that point
            for (i in startIndex until notes.size) {
                val note = notes[i]
                midiPlayer.playPitch(note.pitch, durationMs = (msPerBeat * note.durationBeats * 0.9).toInt())
                delay((msPerBeat * note.durationBeats).toLong())
            }
        }
    }
    
    /**
     * Select a note for editing by clicking on it
     */
    fun selectNoteForEditing(index: Int) {
        val note = _state.value.userNotes.getOrNull(index) as? Note ?: return
        _state.update {
            it.copy(
                currentNoteIndex = index,
                selectedNote = note.pitch.note,
                selectedOctave = note.pitch.octave,
                selectedDuration = note.durationBeats,
                selectedAccidental = note.accidental
            )
        }
    }
    
    /**
     * Update the currently selected note with new values
     */
    fun updateSelectedNote() {
        val currentState = _state.value
        val index = currentState.currentNoteIndex
        val existingNote = currentState.userNotes.getOrNull(index) as? Note ?: return
        val noteName = currentState.selectedNote ?: return
        
        val alteration = when (currentState.selectedAccidental) {
            AccidentalType.FLAT -> -1
            AccidentalType.SHARP -> 1
            AccidentalType.DOUBLE_FLAT -> -2
            AccidentalType.DOUBLE_SHARP -> 2
            else -> 0
        }
        
        val newPitch = Pitch(noteName, currentState.selectedOctave, alteration)
        val updatedNote = existingNote.copy(
            pitch = newPitch,
            durationBeats = currentState.selectedDuration,
            accidental = currentState.selectedAccidental
        )
        
        val updatedNotes = currentState.userNotes.toMutableList()
        updatedNotes[index] = updatedNote
        
        _state.update {
            it.copy(
                userNotes = updatedNotes,
                showFeedback = false,
                feedbackMessage = null
            )
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
