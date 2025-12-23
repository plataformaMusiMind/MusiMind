package com.musimind.music.audio.midi

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.musimind.music.notation.model.Pitch
import com.musimind.music.audio.pitch.PitchUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin

/**
 * Simple MIDI-like synthesizer for playing notes
 * 
 * Generates sine wave tones for note playback.
 * For a full MIDI implementation, consider using FluidSynth or a SoundFont library.
 */
@Singleton
class MidiPlayer @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val DEFAULT_DURATION_MS = 500
        private const val FADE_DURATION_MS = 20 // Fade in/out to avoid clicks
    }
    
    private val _state = MutableStateFlow(MidiPlayerState())
    val state: StateFlow<MidiPlayerState> = _state.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentPlayJob: Job? = null
    
    /**
     * Play a single pitch
     */
    fun playPitch(
        pitch: Pitch,
        durationMs: Int = DEFAULT_DURATION_MS,
        velocity: Float = 0.8f
    ) {
        currentPlayJob?.cancel()
        currentPlayJob = scope.launch {
            try {
                _state.value = _state.value.copy(
                    isPlaying = true,
                    currentPitch = pitch
                )
                
                val frequency = PitchUtils.pitchToFrequency(pitch)
                playTone(frequency, durationMs, velocity)
                
            } finally {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    currentPitch = null
                )
            }
        }
    }
    
    /**
     * Play multiple pitches in sequence
     */
    fun playSequence(
        pitches: List<Pitch>,
        durationMsPerNote: Int = DEFAULT_DURATION_MS,
        velocity: Float = 0.8f
    ) {
        currentPlayJob?.cancel()
        currentPlayJob = scope.launch {
            for (pitch in pitches) {
                if (!isActive) break
                
                _state.value = _state.value.copy(
                    isPlaying = true,
                    currentPitch = pitch
                )
                
                val frequency = PitchUtils.pitchToFrequency(pitch)
                playTone(frequency, durationMsPerNote, velocity)
                
                delay(50) // Small gap between notes
            }
            
            _state.value = _state.value.copy(
                isPlaying = false,
                currentPitch = null
            )
        }
    }
    
    /**
     * Play a chord (multiple notes simultaneously)
     */
    fun playChord(
        pitches: List<Pitch>,
        durationMs: Int = DEFAULT_DURATION_MS,
        velocity: Float = 0.8f
    ) {
        currentPlayJob?.cancel()
        currentPlayJob = scope.launch {
            try {
                _state.value = _state.value.copy(
                    isPlaying = true,
                    currentPitch = pitches.firstOrNull()
                )
                
                // Generate combined waveform
                val frequencies = pitches.map { PitchUtils.pitchToFrequency(it) }
                playMultipleTones(frequencies, durationMs, velocity)
                
            } finally {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    currentPitch = null
                )
            }
        }
    }
    
    /**
     * Stop current playback
     */
    fun stop() {
        currentPlayJob?.cancel()
        currentPlayJob = null
        _state.value = _state.value.copy(
            isPlaying = false,
            currentPitch = null
        )
    }
    
    /**
     * Generate and play a sine wave tone
     */
    private suspend fun playTone(
        frequency: Float,
        durationMs: Int,
        velocity: Float
    ) = withContext(Dispatchers.Default) {
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        val fadeInSamples = SAMPLE_RATE * FADE_DURATION_MS / 1000
        val fadeOutStart = numSamples - fadeInSamples
        
        // Generate sine wave with envelope
        for (i in 0 until numSamples) {
            val time = i.toDouble() / SAMPLE_RATE
            val sample = sin(2.0 * Math.PI * frequency * time)
            
            // Apply fade in/out envelope
            val envelope = when {
                i < fadeInSamples -> i.toDouble() / fadeInSamples
                i > fadeOutStart -> (numSamples - i).toDouble() / fadeInSamples
                else -> 1.0
            }
            
            samples[i] = (sample * envelope * velocity * Short.MAX_VALUE).toInt().toShort()
        }
        
        // Play using AudioTrack
        playAudioTrack(samples)
    }
    
    /**
     * Generate and play multiple tones (for chords)
     */
    private suspend fun playMultipleTones(
        frequencies: List<Float>,
        durationMs: Int,
        velocity: Float
    ) = withContext(Dispatchers.Default) {
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        val fadeInSamples = SAMPLE_RATE * FADE_DURATION_MS / 1000
        val fadeOutStart = numSamples - fadeInSamples
        
        // Generate combined sine waves
        for (i in 0 until numSamples) {
            val time = i.toDouble() / SAMPLE_RATE
            
            // Sum all frequencies
            var sample = 0.0
            for (frequency in frequencies) {
                sample += sin(2.0 * Math.PI * frequency * time)
            }
            
            // Normalize by number of tones
            sample /= frequencies.size
            
            // Apply fade envelope
            val envelope = when {
                i < fadeInSamples -> i.toDouble() / fadeInSamples
                i > fadeOutStart -> (numSamples - i).toDouble() / fadeInSamples
                else -> 1.0
            }
            
            samples[i] = (sample * envelope * velocity * Short.MAX_VALUE).toInt().toShort()
        }
        
        playAudioTrack(samples)
    }
    
    /**
     * Play samples through AudioTrack
     */
    private fun playAudioTrack(samples: ShortArray) {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(maxOf(bufferSize, samples.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        
        try {
            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            
            // Wait for playback to complete
            Thread.sleep((samples.size * 1000L / SAMPLE_RATE) + 50)
            
        } finally {
            audioTrack.stop()
            audioTrack.release()
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        stop()
        scope.cancel()
    }
}

/**
 * MIDI player state
 */
data class MidiPlayerState(
    val isPlaying: Boolean = false,
    val currentPitch: Pitch? = null
) {
    val displayNote: String get() = currentPitch?.let {
        PitchUtils.pitchToDisplayString(it)
    } ?: "—"
}
