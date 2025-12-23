package com.musimind.music.audio.metronome

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metronome service for rhythm exercises and practice
 * 
 * Features:
 * - Configurable BPM (40-240)
 * - Accent on first beat
 * - Visual beat indicator
 * - Time signature support
 */
@Singleton
class Metronome @Inject constructor(
    private val context: Context
) {
    companion object {
        const val MIN_BPM = 40
        const val MAX_BPM = 240
        const val DEFAULT_BPM = 100
    }
    
    // Sound pool for low-latency audio
    private var soundPool: SoundPool? = null
    private var tickSoundId: Int = 0
    private var tockSoundId: Int = 0
    private var isSoundLoaded = false
    
    // Metronome state
    private val _state = MutableStateFlow(MetronomeState())
    val state: StateFlow<MetronomeState> = _state.asStateFlow()
    
    // Coroutine scope for metronome timing
    private var metronomeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        initializeSoundPool()
    }
    
    /**
     * Initialize sound pool with click sounds
     */
    private fun initializeSoundPool() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attributes)
            .build()
            .apply {
                setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) {
                        isSoundLoaded = true
                    }
                }
            }
        
        // Load sounds from assets or use synthesized clicks
        try {
            context.assets.openFd("sounds/metronome_tick.wav").use { afd ->
                tickSoundId = soundPool?.load(afd, 1) ?: 0
            }
            context.assets.openFd("sounds/metronome_tock.wav").use { afd ->
                tockSoundId = soundPool?.load(afd, 1) ?: 0
            }
        } catch (e: Exception) {
            // If sounds not found, we'll use synthesized clicks
            // For now, just mark as loaded so the visual beat works
            isSoundLoaded = true
        }
    }
    
    /**
     * Start the metronome
     */
    fun start() {
        if (_state.value.isPlaying) return
        
        _state.value = _state.value.copy(isPlaying = true, currentBeat = 0)
        
        metronomeJob = scope.launch {
            var beat = 0
            val beatsPerMeasure = _state.value.beatsPerMeasure
            
            while (isActive) {
                val bpm = _state.value.bpm
                val intervalMs = (60_000L / bpm)
                
                // Update current beat
                _state.value = _state.value.copy(
                    currentBeat = beat + 1 // 1-indexed for display
                )
                
                // Play sound
                if (isSoundLoaded && _state.value.soundEnabled) {
                    val soundId = if (beat == 0) tickSoundId else tockSoundId
                    val volume = if (beat == 0) 1.0f else 0.7f
                    soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
                }
                
                // Wait for next beat
                delay(intervalMs)
                
                // Cycle beat
                beat = (beat + 1) % beatsPerMeasure
            }
        }
    }
    
    /**
     * Stop the metronome
     */
    fun stop() {
        metronomeJob?.cancel()
        metronomeJob = null
        _state.value = _state.value.copy(isPlaying = false, currentBeat = 0)
    }
    
    /**
     * Toggle play/stop
     */
    fun toggle() {
        if (_state.value.isPlaying) stop() else start()
    }
    
    /**
     * Set BPM
     */
    fun setBpm(bpm: Int) {
        val clampedBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
        _state.value = _state.value.copy(bpm = clampedBpm)
    }
    
    /**
     * Increase BPM by amount
     */
    fun increaseBpm(amount: Int = 1) {
        setBpm(_state.value.bpm + amount)
    }
    
    /**
     * Decrease BPM by amount
     */
    fun decreaseBpm(amount: Int = 1) {
        setBpm(_state.value.bpm - amount)
    }
    
    /**
     * Set time signature
     */
    fun setTimeSignature(beatsPerMeasure: Int, beatUnit: Int = 4) {
        _state.value = _state.value.copy(
            beatsPerMeasure = beatsPerMeasure,
            beatUnit = beatUnit,
            currentBeat = 0
        )
    }
    
    /**
     * Toggle sound on/off
     */
    fun toggleSound() {
        _state.value = _state.value.copy(soundEnabled = !_state.value.soundEnabled)
    }
    
    /**
     * Set sound enabled state
     */
    fun setSoundEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(soundEnabled = enabled)
    }
    
    /**
     * Release resources
     */
    fun release() {
        stop()
        scope.cancel()
        soundPool?.release()
        soundPool = null
    }
}

/**
 * Metronome state
 */
data class MetronomeState(
    val isPlaying: Boolean = false,
    val bpm: Int = Metronome.DEFAULT_BPM,
    val beatsPerMeasure: Int = 4,
    val beatUnit: Int = 4,
    val currentBeat: Int = 0, // 0 when stopped, 1-beatsPerMeasure when playing
    val soundEnabled: Boolean = true
) {
    val isAccentBeat: Boolean get() = currentBeat == 1
    val intervalMs: Long get() = 60_000L / bpm
    val timeSignatureDisplay: String get() = "$beatsPerMeasure/$beatUnit"
}
