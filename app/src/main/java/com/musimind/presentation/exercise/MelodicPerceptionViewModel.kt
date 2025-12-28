package com.musimind.presentation.exercise

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.ExerciseRepository
import com.musimind.domain.gamification.ExerciseLifeResult
import com.musimind.domain.gamification.LivesManager
import com.musimind.domain.gamification.RewardsManager
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
 * Integrated with Lives system - lose life when accuracy < 75%
 */
@HiltViewModel
class MelodicPerceptionViewModel @Inject constructor(
    private val midiPlayer: MidiPlayer,
    private val exerciseRepository: ExerciseRepository,
    private val livesManager: LivesManager,
    private val rewardsManager: RewardsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(MelodicPerceptionState())
    val state: StateFlow<MelodicPerceptionState> = _state.asStateFlow()
    
    // Lives state exposed for UI
    val livesState = livesManager.livesState
    
    // Track if this is first exercise of day for bonus
    private var isFirstExerciseOfDay = false
    
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
     * Select dot type (none, single, double)
     */
    fun selectDotType(dotType: DotType) {
        _state.update { it.copy(selectedDotType = dotType) }
    }
    
    /**
     * Toggle tied note on/off
     */
    fun toggleTied() {
        _state.update { it.copy(selectedTied = !it.selectedTied) }
    }
    
    /**
     * Toggle metronome on/off
     */
    fun toggleMetronome() {
        _state.update { it.copy(metronomeEnabled = !it.metronomeEnabled) }
    }
    
    /**
     * Toggle tie on the CURRENT note (where cursor is)
     * This allows adding a tie from the selected note to the next one
     */
    fun toggleTieOnCurrentNote() {
        val currentState = _state.value
        val index = currentState.currentNoteIndex
        val notes = currentState.userNotes.toMutableList()
        
        if (index < 0 || index >= notes.size) return
        
        val element = notes[index]
        if (element is Note) {
            // Toggle the tied property on the current note
            notes[index] = element.copy(tied = !element.tied)
            _state.update { it.copy(userNotes = notes) }
        }
    }
    
    /**
     * Dismiss feedback modal
     */
    fun dismissFeedbackModal() {
        _state.update { it.copy(showFeedbackModal = false, feedbackMessage = null) }
    }
    
    /**
     * Add a note based on current selection with automatic bar break.
     * If the note exceeds the measure, it's split into tied notes.
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
        
        // Calculate final duration with dots
        val baseDuration = currentState.selectedDuration
        val duration = when (currentState.selectedDotType) {
            DotType.NONE -> baseDuration
            DotType.SINGLE -> baseDuration * 1.5f   // 50% increase
            DotType.DOUBLE -> baseDuration * 1.75f  // 75% increase
        }
        
        val isDotted = currentState.selectedDotType == DotType.SINGLE
        val isDoubleDotted = currentState.selectedDotType == DotType.DOUBLE
        val isTied = currentState.selectedTied
        
        // Calculate total beats used so far
        val totalBeatsUsed = currentState.userNotes.sumOf { it.durationBeats.toDouble() }.toFloat()
        
        // No limit on beats - score will scroll horizontally
        
        // Calculate position within current measure (0-based: 0.0 to 3.999...)
        val beatsPerMeasure = 4f // 4/4 time signature
        val positionInMeasure = totalBeatsUsed % beatsPerMeasure
        val remainingInMeasure = beatsPerMeasure - positionInMeasure
        
        // Beat number for display (1-based)
        val beatNumber = positionInMeasure + 1f
        
        // Check if note fits in current measure
        if (duration <= remainingInMeasure + 0.001f) {
            // Note fits - add normally
            val newNote = Note(
                id = "user_${currentState.userNotes.size}",
                durationBeats = duration,
                pitch = pitch,
                accidental = currentState.selectedAccidental,
                dotted = isDotted,
                doubleDotted = isDoubleDotted,
                tied = isTied,
                beatNumber = beatNumber
            )
            
            _state.update {
                it.copy(
                    userNotes = it.userNotes + newNote,
                    currentNoteIndex = it.userNotes.size,
                    showFeedback = false,
                    feedbackMessage = null
                )
            }
        } else if (remainingInMeasure >= 0.001f) {
            // Note exceeds measure but there's space - split with tie
            val firstPartDuration = remainingInMeasure
            val secondPartDuration = duration - remainingInMeasure
            
            val firstNote = Note(
                id = "user_${currentState.userNotes.size}",
                durationBeats = firstPartDuration,
                pitch = pitch,
                accidental = currentState.selectedAccidental,
                tied = true,  // First part is tied to next
                beatNumber = beatNumber
            )
            
            val secondNote = Note(
                id = "user_${currentState.userNotes.size + 1}",
                durationBeats = secondPartDuration,
                pitch = pitch,
                accidental = null,  // Don't repeat accidental after tie
                tied = false,
                beatNumber = 1f  // Start of new measure
            )
            
            _state.update {
                it.copy(
                    userNotes = it.userNotes + firstNote + secondNote,
                    currentNoteIndex = it.userNotes.size + 1,
                    showFeedback = false,
                    feedbackMessage = "Nota dividida entre compassos (ligadura)"
                )
            }
        } else {
            // Measure is exactly full - add to next measure
            val newNote = Note(
                id = "user_${currentState.userNotes.size}",
                durationBeats = duration,
                pitch = pitch,
                accidental = currentState.selectedAccidental,
                beatNumber = 1f // Start of new measure
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
    }
    
    /**
     * Calculate current beat position within the measure (0-based: 0.0 to 3.999...)
     */
    private fun calculateCurrentBeatInMeasure(notes: List<MusicElement>): Float {
        val totalBeats = notes.sumOf { it.durationBeats.toDouble() }.toFloat()
        return totalBeats % 4f
    }
    
    private fun calculateUserBeatNumber(notes: List<MusicElement>, newDuration: Float): Float {
        val totalBeats = notes.sumOf { it.durationBeats.toDouble() }.toFloat()
        return (totalBeats % 4f) + 1f // 1-based for display
    }
    
    /**
     * Add a rest with automatic bar break
     */
    fun addRest() {
        val currentState = _state.value
        val duration = currentState.selectedDuration
        
        // Calculate total beats used so far
        val totalBeatsUsed = currentState.userNotes.sumOf { it.durationBeats.toDouble() }.toFloat()
        
        // Maximum beats allowed (4 measures in 4/4 = 16 beats)
        val maxBeats = 16f
        
        // Check if adding this rest would exceed the limit
        if (totalBeatsUsed + duration > maxBeats + 0.001f) {
            _state.update {
                it.copy(
                    feedbackMessage = "Limite de compassos atingido!"
                )
            }
            return
        }
        
        // Calculate position within current measure (0-based)
        val beatsPerMeasure = 4f
        val positionInMeasure = totalBeatsUsed % beatsPerMeasure
        val remainingInMeasure = beatsPerMeasure - positionInMeasure
        
        if (duration <= remainingInMeasure + 0.001f) {
            // Rest fits
            val newRest = Rest(
                id = "user_rest_${currentState.userNotes.size}",
                durationBeats = duration
            )
            
            _state.update {
                it.copy(
                    userNotes = it.userNotes + newRest,
                    currentNoteIndex = it.userNotes.size,
                    showFeedback = false,
                    feedbackMessage = null
                )
            }
        } else {
            // Split rest across bar line
            val firstRest = Rest(
                id = "user_rest_${currentState.userNotes.size}",
                durationBeats = remainingInMeasure
            )
            val secondRest = Rest(
                id = "user_rest_${currentState.userNotes.size + 1}",
                durationBeats = duration - remainingInMeasure
            )
            
            _state.update {
                it.copy(
                    userNotes = it.userNotes + firstRest + secondRest,
                    currentNoteIndex = it.userNotes.size + 1,
                    showFeedback = false,
                    feedbackMessage = null
                )
            }
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
     * Integrates with Lives and Rewards systems
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
        
        // Calculate accuracy
        val accuracy = if (targetNotes.isNotEmpty()) {
            (correctCount.toFloat() / targetNotes.size) * 100f
        } else 100f
        
        val allCorrect = correctCount == targetNotes.size && userNotes.size == targetNotes.size
        
        // Process result with Lives and Rewards system
        viewModelScope.launch {
            // Process life loss if accuracy < 75%
            val lossResult = livesManager.processExerciseResult(
                exerciseId = currentState.exerciseId,
                accuracy = accuracy,
                wasCompleted = true
            )
            
            // Determine feedback message
            val baseMessage = when {
                userNotes.size < targetNotes.size -> {
                    val missingCount = targetNotes.size - userNotes.size
                    "Faltam $missingCount nota(s). Você acertou $correctCount de ${targetNotes.size}."
                }
                userNotes.size > targetNotes.size -> {
                    val extraCount = userNotes.size - targetNotes.size
                    "Você adicionou $extraCount nota(s) a mais. Acertou $correctCount de ${targetNotes.size}."
                }
                allCorrect -> "Parabéns! Todas as notas corretas! 🎉"
                else -> "Você acertou $correctCount de ${targetNotes.size}."
            }
            
            // Add life status to message
            val lifeMessage = when (lossResult) {
                is ExerciseLifeResult.LifeLost -> "\n❤️ -1 vida (${lossResult.remainingLives} restantes)"
                is ExerciseLifeResult.LifeLostAndOutOfLives -> "\n💔 Você ficou sem vidas!"
                is ExerciseLifeResult.AlreadyOutOfLives -> "\n💔 Você ficou sem vidas!"
                is ExerciseLifeResult.Passed -> ""
                is ExerciseLifeResult.Error -> "\n⚠️ ${lossResult.message}"
            }
            
            val didLoseLife = lossResult is ExerciseLifeResult.LifeLost || 
                              lossResult is ExerciseLifeResult.LifeLostAndOutOfLives
            val isOutOfLives = lossResult is ExerciseLifeResult.LifeLostAndOutOfLives ||
                               lossResult is ExerciseLifeResult.AlreadyOutOfLives
            
            _state.update {
                it.copy(
                    feedbackResults = feedbackResults,
                    showFeedback = true,
                    correctCount = correctCount,
                    allCorrect = allCorrect,
                    accuracy = accuracy,
                    lifeLost = didLoseLife,
                    outOfLives = isOutOfLives,
                    feedbackMessage = baseMessage + lifeMessage
                )
            }
            
            // Award XP if completed successfully (accuracy >= 75%)
            if (accuracy >= 75f) {
                val baseXp = (10 + (accuracy * 0.2f).toInt()).coerceIn(10, 30)
                rewardsManager.awardExerciseXp(
                    baseXp = baseXp,
                    accuracy = accuracy,
                    isFirstOfDay = isFirstExerciseOfDay,
                    isPerfect = allCorrect
                )
                isFirstExerciseOfDay = false
            }
            
            // Complete exercise if perfect
            if (allCorrect) {
                // Play victory sound - ascending arpeggio/fanfare
                playVictorySound()
                delay(2500)
                _state.update { it.copy(isComplete = true) }
            }
        }
    }
    
    /**
     * Play victory sound - an ascending major chord arpeggio
     */
    private suspend fun playVictorySound() {
        val tempo = 150 // Fast tempo for fanfare
        val msPerNote = (60000 / tempo / 2).toLong() // 16th notes
        
        // C major chord arpeggio: C4, E4, G4, C5
        val victoryNotes = listOf(
            Pitch(NoteName.C, 4),
            Pitch(NoteName.E, 4),
            Pitch(NoteName.G, 4),
            Pitch(NoteName.C, 5)
        )
        
        for (pitch in victoryNotes) {
            midiPlayer.playPitch(pitch, durationMs = msPerNote.toInt(), velocity = 0.9f)
            delay(msPerNote)
        }
        
        // Final chord
        delay(50)
        midiPlayer.playChord(
            pitches = listOf(
                Pitch(NoteName.C, 4),
                Pitch(NoteName.E, 4),
                Pitch(NoteName.G, 4),
                Pitch(NoteName.C, 5)
            ),
            durationMs = 800,
            velocity = 1.0f
        )
    }
    
    /**
     * Play the target melody with metronome countdown
     * Respects ties - tied notes are played as one continuous sound
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
            
            // Play melody with tie handling
            var index = 0
            while (index < notes.size) {
                val note = notes[index]
                
                // Calculate total duration including tied notes
                var totalDuration = note.durationBeats
                var tiedCount = 0
                var currentNote: Note = note
                
                while (currentNote.tied && (index + tiedCount + 1) < notes.size) {
                    val nextNote = notes[index + tiedCount + 1]
                    if (nextNote.pitch.midiPitch == currentNote.pitch.midiPitch) {
                        totalDuration += nextNote.durationBeats
                        tiedCount++
                        currentNote = nextNote
                    } else {
                        break
                    }
                }
                
                // Play the note for the full tied duration
                midiPlayer.playPitch(note.pitch, durationMs = (msPerBeat * totalDuration * 0.95).toInt())
                delay((msPerBeat * totalDuration).toLong())
                
                // Skip tied notes
                index += tiedCount + 1
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
     * Play what the user has written (including rests as silence)
     * With optional metronome and auto-scroll support
     * Implements TIES: when a note has tied=true, it connects to the next note as one continuous sound
     */
    fun playUserNotes() {
        viewModelScope.launch {
            val elements = _state.value.userNotes
            if (elements.isEmpty()) {
                _state.update { it.copy(showFeedbackModal = true, feedbackMessage = "Adicione notas primeiro!") }
                return@launch
            }
            
            val tempo = 80 // BPM
            val msPerBeat = (60000 / tempo).toLong()
            val metronomeEnabled = _state.value.metronomeEnabled
            
            // Set playing state
            _state.update { it.copy(isPlaying = true, playbackNoteIndex = -1) }
            
            // Metronome countdown (always do 4 beats before playing)
            for (beat in 1..4) {
                midiPlayer.playMetronomeClick(isAccented = beat == 1)
                delay(msPerBeat)
            }
            
            // Calculate total beats for metronome timing
            var accumulatedBeats = 0f
            var nextMetronomeBeat = 1f  // Next beat to play click on (1-indexed)
            
            // Process elements to handle ties
            // When a note has tied=true, combine its duration with the next note(s)
            var index = 0
            while (index < elements.size) {
                val element = elements[index]
                
                // Update playback index for auto-scroll
                _state.update { it.copy(playbackNoteIndex = index) }
                
                when (element) {
                    is Note -> {
                        // Calculate total duration including tied notes
                        var totalDuration = element.durationBeats
                        var tiedCount = 0
                        var currentNote: Note = element
                        
                        // If this note is tied, accumulate duration of subsequent tied notes
                        while (currentNote.tied && (index + tiedCount + 1) < elements.size) {
                            val nextElement = elements[index + tiedCount + 1]
                            if (nextElement is Note && nextElement.pitch.midiPitch == currentNote.pitch.midiPitch) {
                                totalDuration += nextElement.durationBeats
                                tiedCount++
                                currentNote = nextElement
                            } else {
                                break
                            }
                        }
                        
                        val noteDurationMs = (msPerBeat * totalDuration).toLong()
                        
                        // Play the note for the full tied duration
                        midiPlayer.playPitch(element.pitch, durationMs = (noteDurationMs * 0.95).toInt())
                        
                        // Wait for the note duration with metronome clicks if enabled
                        if (metronomeEnabled) {
                            // Calculate how many metronome beats fall within this note
                            val noteStartBeat = accumulatedBeats
                            val noteEndBeat = accumulatedBeats + totalDuration
                            
                            var elapsed = 0L
                            while (elapsed < noteDurationMs) {
                                val currentBeatPosition = noteStartBeat + (elapsed.toFloat() / msPerBeat)
                                
                                // Check if we should play a metronome click
                                // Only play if we're at or past the next beat position
                                if (nextMetronomeBeat <= noteEndBeat && currentBeatPosition >= nextMetronomeBeat - 0.02f) {
                                    val beatNumber = nextMetronomeBeat.toInt()
                                    val isDownbeat = beatNumber == 1 || (beatNumber - 1) % 4 == 0
                                    midiPlayer.playMetronomeClick(isAccented = isDownbeat)
                                    nextMetronomeBeat += 1f
                                }
                                
                                delay(10) // Small delay for smooth timing
                                elapsed += 10
                            }
                        } else {
                            delay(noteDurationMs)
                        }
                        
                        accumulatedBeats += totalDuration
                        
                        // Skip the tied notes we already played
                        index += tiedCount
                    }
                    is Rest -> {
                        val restDurationMs = (msPerBeat * element.durationBeats).toLong()
                        
                        // Play metronome clicks during rest if enabled
                        if (metronomeEnabled) {
                            val restStartBeat = accumulatedBeats
                            val restEndBeat = accumulatedBeats + element.durationBeats
                            
                            var elapsed = 0L
                            while (elapsed < restDurationMs) {
                                val currentBeatPosition = restStartBeat + (elapsed.toFloat() / msPerBeat)
                                
                                if (nextMetronomeBeat <= restEndBeat && currentBeatPosition >= nextMetronomeBeat - 0.02f) {
                                    val beatNumber = nextMetronomeBeat.toInt()
                                    val isDownbeat = beatNumber == 1 || (beatNumber - 1) % 4 == 0
                                    midiPlayer.playMetronomeClick(isAccented = isDownbeat)
                                    nextMetronomeBeat += 1f
                                }
                                
                                delay(10)
                                elapsed += 10
                            }
                        } else {
                            delay(restDurationMs)
                        }
                        
                        accumulatedBeats += element.durationBeats
                    }
                    else -> {
                        delay((msPerBeat * element.durationBeats).toLong())
                        accumulatedBeats += element.durationBeats
                    }
                }
                
                index++
            }
            
            // Reset playing state
            _state.update { it.copy(isPlaying = false, playbackNoteIndex = -1) }
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
    
    /**
     * Move the currently selected note UP one step (semitone or staff position)
     */
    fun moveNoteUp() {
        val currentState = _state.value
        val index = currentState.currentNoteIndex
        val existingNote = currentState.userNotes.getOrNull(index) as? Note ?: return
        
        // Calculate new pitch (move up by one diatonic step)
        val currentNoteName = existingNote.pitch.note
        val currentOctave = existingNote.pitch.octave
        
        val noteOrder = listOf(NoteName.C, NoteName.D, NoteName.E, NoteName.F, NoteName.G, NoteName.A, NoteName.B)
        val currentIndex = noteOrder.indexOf(currentNoteName)
        
        val newNoteIndex = (currentIndex + 1) % 7
        val newOctave = if (newNoteIndex == 0) currentOctave + 1 else currentOctave
        
        // Clamp octave
        if (newOctave > 6) return
        
        val newPitch = Pitch(noteOrder[newNoteIndex], newOctave, existingNote.pitch.alteration)
        val updatedNote = existingNote.copy(pitch = newPitch)
        
        val updatedNotes = currentState.userNotes.toMutableList()
        updatedNotes[index] = updatedNote
        
        _state.update {
            it.copy(
                userNotes = updatedNotes,
                selectedNote = noteOrder[newNoteIndex],
                selectedOctave = newOctave,
                showFeedback = false,
                feedbackMessage = null
            )
        }
    }
    
    /**
     * Move the currently selected note DOWN one step (semitone or staff position)
     */
    fun moveNoteDown() {
        val currentState = _state.value
        val index = currentState.currentNoteIndex
        val existingNote = currentState.userNotes.getOrNull(index) as? Note ?: return
        
        // Calculate new pitch (move down by one diatonic step)
        val currentNoteName = existingNote.pitch.note
        val currentOctave = existingNote.pitch.octave
        
        val noteOrder = listOf(NoteName.C, NoteName.D, NoteName.E, NoteName.F, NoteName.G, NoteName.A, NoteName.B)
        val currentIndex = noteOrder.indexOf(currentNoteName)
        
        val newNoteIndex = if (currentIndex == 0) 6 else currentIndex - 1
        val newOctave = if (currentIndex == 0) currentOctave - 1 else currentOctave
        
        // Clamp octave
        if (newOctave < 2) return
        
        val newPitch = Pitch(noteOrder[newNoteIndex], newOctave, existingNote.pitch.alteration)
        val updatedNote = existingNote.copy(pitch = newPitch)
        
        val updatedNotes = currentState.userNotes.toMutableList()
        updatedNotes[index] = updatedNote
        
        _state.update {
            it.copy(
                userNotes = updatedNotes,
                selectedNote = noteOrder[newNoteIndex],
                selectedOctave = newOctave,
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
    val selectedDotType: DotType = DotType.NONE, // none, single, double
    val selectedTied: Boolean = false, // Tie to next note
    
    // Metronome
    val metronomeEnabled: Boolean = false,
    
    // Playback state
    val isPlaying: Boolean = false,
    val playbackNoteIndex: Int = -1, // Which note is currently playing
    val scrollPosition: Float = 0f, // Scroll position for auto-scroll during playback
    
    // Feedback
    val showFeedback: Boolean = false,
    val feedbackResults: Map<Int, Boolean> = emptyMap(),
    val feedbackMessage: String? = null,
    val showFeedbackModal: Boolean = false, // Show feedback as modal instead of inline
    val correctCount: Int = 0,
    val allCorrect: Boolean = false,
    
    // Lives integration
    val accuracy: Float = 0f,
    val lifeLost: Boolean = false,
    val outOfLives: Boolean = false
)

// Dot type enum for augmentation dots
enum class DotType {
    NONE,
    SINGLE,     // Increases duration by 50%
    DOUBLE      // Increases duration by 75%
}

