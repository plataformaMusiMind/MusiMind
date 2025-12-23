package com.musimind.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.music.audio.metronome.Metronome
import com.musimind.music.audio.metronome.MetronomeState
import com.musimind.music.audio.midi.MidiPlayer
import com.musimind.music.notation.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RhythmViewModel @Inject constructor(
    private val metronome: Metronome,
    private val midiPlayer: MidiPlayer
) : ViewModel() {
    
    private val _state = MutableStateFlow(RhythmState())
    val state: StateFlow<RhythmState> = _state.asStateFlow()
    
    val metronomeState: StateFlow<MetronomeState> = metronome.state
    
    private var recordingStartTime: Long = 0
    private var expectedTapCount = 0
    
    fun loadExercise(exerciseId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val patterns = createDemoPatterns()
            
            _state.update {
                it.copy(
                    isLoading = false,
                    patterns = patterns,
                    totalPatterns = patterns.size,
                    currentPatternIndex = 0,
                    currentPattern = patterns.firstOrNull() ?: emptyList()
                )
            }
            
            metronome.setBpm(80)
        }
    }
    
    private fun createDemoPatterns(): List<List<Float>> {
        return listOf(
            // Pattern 1: Quarter notes
            listOf(1f, 1f, 1f, 1f),
            // Pattern 2: Half notes
            listOf(2f, 2f),
            // Pattern 3: Eighth notes
            listOf(0.5f, 0.5f, 0.5f, 0.5f, 1f, 1f),
            // Pattern 4: Mixed
            listOf(1f, 0.5f, 0.5f, 1f, 1f),
            // Pattern 5: With rest (negative = rest)
            listOf(1f, -1f, 1f, 1f)
        )
    }
    
    fun startMetronome() {
        metronome.start()
        _state.update { 
            it.copy(
                isRecording = true,
                userTaps = emptyList()
            )
        }
        recordingStartTime = System.currentTimeMillis()
        expectedTapCount = _state.value.currentPattern.count { it > 0 }
    }
    
    fun stopMetronome() {
        metronome.stop()
        
        if (_state.value.isRecording) {
            evaluateTaps()
        }
        
        _state.update { it.copy(isRecording = false) }
    }
    
    fun onTap() {
        if (!_state.value.isRecording) return
        
        val tapTime = System.currentTimeMillis() - recordingStartTime
        
        _state.update {
            it.copy(userTaps = it.userTaps + tapTime)
        }
        
        // Check if we have enough taps
        if (_state.value.userTaps.size >= expectedTapCount) {
            viewModelScope.launch {
                delay(500)
                stopMetronome()
            }
        }
    }
    
    private fun evaluateTaps() {
        val state = _state.value
        val pattern = state.currentPattern.filter { it > 0 }
        val taps = state.userTaps
        
        if (taps.size != pattern.size) {
            // Wrong number of taps
            _state.update {
                it.copy(
                    lastResult = ExerciseResult.INCORRECT,
                    feedbackMessage = "Número de batidas errado"
                )
            }
            return
        }
        
        // Calculate expected intervals based on BPM
        val bpm = metronome.state.value.bpm
        val beatDurationMs = 60_000L / bpm
        
        var isCorrect = true
        var totalError = 0L
        
        for (i in 1 until taps.size) {
            val expectedInterval = (pattern[i - 1] * beatDurationMs).toLong()
            val actualInterval = taps[i] - taps[i - 1]
            val error = kotlin.math.abs(actualInterval - expectedInterval)
            totalError += error
            
            if (error > beatDurationMs * 0.3) { // 30% tolerance
                isCorrect = false
            }
        }
        
        if (isCorrect) {
            _state.update {
                it.copy(
                    correctCount = it.correctCount + 1,
                    lastResult = ExerciseResult.CORRECT,
                    feedbackMessage = "Perfeito! 🎵"
                )
            }
        } else {
            _state.update {
                it.copy(
                    lastResult = ExerciseResult.INCORRECT,
                    feedbackMessage = "Ritmo incorreto"
                )
            }
        }
    }
    
    fun playPattern() {
        viewModelScope.launch {
            val pattern = _state.value.currentPattern
            val bpm = metronome.state.value.bpm
            val beatDurationMs = 60_000 / bpm
            
            for ((index, duration) in pattern.withIndex()) {
                _state.update { it.copy(recordingBeat = index) }
                
                if (duration > 0) {
                    // Play note
                    midiPlayer.playPitch(
                        Pitch(NoteName.C, 4),
                        durationMs = (duration * beatDurationMs * 0.8).toInt()
                    )
                }
                
                delay((kotlin.math.abs(duration) * beatDurationMs).toLong())
            }
            
            _state.update { it.copy(recordingBeat = -1) }
        }
    }
    
    fun nextPattern() {
        val currentIndex = _state.value.currentPatternIndex + 1
        
        _state.update { it.copy(feedbackMessage = null, lastResult = null) }
        
        if (currentIndex >= _state.value.patterns.size) {
            _state.update { it.copy(isComplete = true) }
        } else {
            _state.update {
                it.copy(
                    currentPatternIndex = currentIndex,
                    currentPattern = it.patterns[currentIndex],
                    userTaps = emptyList()
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        metronome.stop()
    }
}

data class RhythmState(
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,
    val patterns: List<List<Float>> = emptyList(),
    val totalPatterns: Int = 0,
    val currentPatternIndex: Int = 0,
    val currentPattern: List<Float> = emptyList(),
    val isRecording: Boolean = false,
    val recordingBeat: Int = -1,
    val userTaps: List<Long> = emptyList(),
    val correctCount: Int = 0,
    val lastResult: ExerciseResult? = null,
    val feedbackMessage: String? = null
)
