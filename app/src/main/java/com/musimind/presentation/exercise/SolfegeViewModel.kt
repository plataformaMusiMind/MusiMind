package com.musimind.presentation.exercise

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.ExerciseRepository
import com.musimind.domain.model.SolfegeNote
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
import kotlinx.coroutines.currentCoroutineContext
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
            pitch = pitch
        )
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
     * Play entire melody with metronome countdown
     */
    /**
     * Play entire melody with metronome countdown and continuous click
     */
    /**
     * Play entire melody with metronome countdown and continuous click
     * Uses a single loop to ensure synchronization
     */
    fun playMelody() {
        val currentState = _state.value
        if (currentState.isPlaying) return

        viewModelScope.launch {
            _state.update { it.copy(isPlaying = true, statusText = "contando...", currentBeat = 0) }
            
            val notes = currentState.notes
            val tempo = currentState.tempo
            val msPerBeat = (60000 / tempo).toLong()
            
            // 1. Count-in (1 bar of 4 beats)
            for (beat in 1..4) {
                _state.update { it.copy(currentBeat = beat) }
                midiPlayer.playMetronomeClick(isAccented = beat == 1)
                delay(msPerBeat)
            }
            
            _state.update { it.copy(statusText = "tocando...") }
            
            // 2. Play Melody + Metronome in a unified loop
            // assuming 4/4 signature for now
            // We calculate the total beats needed
            val totalNotesDuration = notes.sumOf { it.durationBeats.toDouble() }.toFloat()
            val totalBeats = kotlin.math.ceil(totalNotesDuration).toInt()
            // Round up to nearest measure (multiple of 4)
            val totalExploreBeats = ((totalBeats + 3) / 4) * 4
            
            // Map notes to their start times (in beats)
            var currentNoteStartTime = 0f
            val noteEvents = notes.associate { note ->
                val start = currentNoteStartTime
                currentNoteStartTime += note.durationBeats
                start to note
            }
            
            // Iterate beat by beat (and sub-beats if needed, but simplified for now)
            // Real-time synchronization is complex with just delays, but single loop prevents drift between metro and notes
            
            var currentBeatTime = 0f
            var beatIndex = 1 // 1-based index for visual metronome
            
            // We iterate by smallest necessary resolution.
            // If we have eighth notes (0.5), we step by 0.5?
            // For now, let's assume we step by BEATS and schedule notes that fall on them.
            // If notes are off-beat, this simple loop needs sub-steps.
            // Let's implement a loop that ticks every 'resolution' (e.g. 0.25 beats)
            val resolution = 0.25f 
            val msPerStep = (msPerBeat * resolution).toLong()
            var stepCount = 0
            
            // We loop until all notes are played
            while (currentBeatTime < totalNotesDuration + 1) { // +1 for decay
                // Metronome click on integer beats
                if (stepCount % (1/resolution).toInt() == 0) {
                     val beatNum = (currentBeatTime.toInt() % 4) + 1
                     val isAccented = beatNum == 1
                     midiPlayer.playMetronomeClick(isAccented = isAccented)
                     _state.update { it.copy(currentBeat = beatNum) }
                }
                
                // Play Note if it starts at this exact time
                // We use a small epsilon for float comparison
                val noteToPlay = noteEvents.filterKeys { abs(it - currentBeatTime) < 0.01f }.values.firstOrNull()
                
                if (noteToPlay != null) {
                    val noteIndex = notes.indexOf(noteToPlay)
                    _state.update { it.copy(currentNoteIndex = noteIndex) }
                    
                    // Duration slightly less than full to articulate
                    val durationMs = (msPerBeat * noteToPlay.durationBeats * 0.95).toInt()
                    midiPlayer.playPitch(noteToPlay.pitch, durationMs = durationMs)
                }
                
                delay(msPerStep)
                currentBeatTime += resolution
                stepCount++
            }
            
            // Cleanup
            _state.update { 
                it.copy(
                    isPlaying = false, 
                    statusText = "pronto",
                    currentNoteIndex = 0,
                    currentBeat = 0
                ) 
            }
        }
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
     * Start listening for pitch
     */
    fun startListening() {
        if (!hasPermission()) {
            _state.update { it.copy(
                error = "Permissão de microfone necessária",
                statusText = "Sem mic!" 
            ) }
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
