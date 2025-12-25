package com.musimind.music.audio.midi

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.musimind.music.audio.nativeaudio.NativeAudioBridge
import com.musimind.music.notation.model.Pitch
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sin

/**
 * MIDI-like synthesizer for playing notes.
 * Uses native Oboe + TinySoundFont for low-latency, high-quality audio.
 * Falls back to synthesized tones if native engine is not available.
 */
@Singleton
class MidiPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nativeAudio: NativeAudioBridge
) {
    companion object {
        private const val TAG = "MidiPlayer"
        private const val SAMPLE_RATE = 44100
        private const val DEFAULT_DURATION_MS = 500
        private const val FADE_DURATION_MS = 20
        private const val A4_FREQUENCY = 440.0f
    }
    
    private val _state = MutableStateFlow(MidiPlayerState())
    val state: StateFlow<MidiPlayerState> = _state.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentPlayJob: Job? = null
    private var isNativeReady = false
    
    init {
        // Initialize native audio in background
        scope.launch {
            isNativeReady = nativeAudio.initialize()
            Log.d(TAG, "Native audio initialized: $isNativeReady")
        }
    }
    
    /**
     * Play a single pitch by MIDI note number.
     * Uses native Oboe engine for low-latency playback.
     */
    fun playMidiNote(
        midiNote: Int,
        durationMs: Int = DEFAULT_DURATION_MS,
        velocity: Float = 0.8f
    ) {
        Log.d(TAG, "playMidiNote: midi=$midiNote, duration=${durationMs}ms, velocity=$velocity")
        
        _state.value = _state.value.copy(
            isPlaying = true,
            currentMidiNote = midiNote
        )
        
        // Use native audio if available (non-blocking, fire-and-forget)
        if (isNativeReady && nativeAudio.isReady()) {
            Log.d(TAG, "Using native Oboe+TinySoundFont for playback")
            nativeAudio.playNote(midiNote, velocity, durationMs)
        } else {
            Log.d(TAG, "Using synth fallback for playback")
            scope.launch {
                val frequency = midiToFrequency(midiNote)
                playTone(frequency, durationMs, velocity)
            }
        }
        
        // Schedule state update after note ends
        scope.launch {
            delay(durationMs.toLong())
            _state.value = _state.value.copy(
                isPlaying = false,
                currentMidiNote = null
            )
        }
    }
    
    /**
     * Play a pitch from notation model
     */
    fun playPitch(
        pitch: Pitch,
        durationMs: Int = DEFAULT_DURATION_MS,
        velocity: Float = 0.8f
    ) {
        val midiNote = pitchToMidi(pitch)
        playMidiNote(midiNote, durationMs, velocity)
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
                
                val midiNote = pitchToMidi(pitch)
                _state.value = _state.value.copy(
                    isPlaying = true,
                    currentMidiNote = midiNote
                )
                
                val frequency = midiToFrequency(midiNote)
                playTone(frequency, durationMsPerNote, velocity)
                
                delay(50) // Small gap between notes
            }
            
            _state.value = _state.value.copy(
                isPlaying = false,
                currentMidiNote = null
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
                    currentMidiNote = pitches.firstOrNull()?.let { pitchToMidi(it) }
                )
                
                val frequencies = pitches.map { midiToFrequency(pitchToMidi(it)) }
                playMultipleTones(frequencies, durationMs, velocity)
                
            } finally {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    currentMidiNote = null
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
            currentMidiNote = null
        )
    }
    
    /**
     * Convert MIDI note to frequency
     */
    private fun midiToFrequency(midiNote: Int): Float {
        return A4_FREQUENCY * 2f.pow((midiNote - 69) / 12f)
    }
    
    /**
     * Convert Pitch to MIDI note number
     */
    private fun pitchToMidi(pitch: Pitch): Int {
        val noteValues = mapOf(
            "C" to 0, "D" to 2, "E" to 4, "F" to 5, 
            "G" to 7, "A" to 9, "B" to 11
        )
        val baseNote = noteValues[pitch.note.name] ?: 0
        val octave = pitch.octave
        val alteration = pitch.alteration
        
        return (octave + 1) * 12 + baseNote + alteration
    }
    
    /**
     * Play metronome click sound.
     * Uses native audio for low-latency clicks.
     * @param isAccented true for first beat (louder), false for other beats
     */
    suspend fun playMetronomeClick(isAccented: Boolean = false) {
        // Use native audio if available (non-blocking)
        if (isNativeReady && nativeAudio.isReady()) {
            nativeAudio.playMetronome(isAccented)
            return
        }
        
        // Fallback to synthesized click
        withContext(Dispatchers.Default) {
            val frequency = if (isAccented) 1500f else 1200f
            val durationMs = if (isAccented) 80 else 50
            val velocity = if (isAccented) 1.0f else 0.6f
            
            val numSamples = (SAMPLE_RATE * durationMs / 1000)
            val samples = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                val time = i.toDouble() / SAMPLE_RATE
                val decay = kotlin.math.exp(-time * 50.0)
                val sample = kotlin.math.sin(2.0 * Math.PI * frequency * time) * decay
                samples[i] = (sample * velocity * Short.MAX_VALUE).toInt().toShort()
            }
            
            playAudioTrack(samples)
        }
    }
    
    /**
     * Generate and play a piano-like tone with harmonics
     */
    private suspend fun playTone(
        frequency: Float,
        durationMs: Int,
        velocity: Float
    ) = withContext(Dispatchers.Default) {
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        val fadeInSamples = SAMPLE_RATE * 5 / 1000 // Quick attack (5ms)
        
        for (i in 0 until numSamples) {
            val time = i.toDouble() / SAMPLE_RATE
            
            // Piano-like sound: fundamental + harmonics with decay
            val fundamental = sin(2.0 * Math.PI * frequency * time)
            val harmonic2 = sin(2.0 * Math.PI * frequency * 2 * time) * 0.5
            val harmonic3 = sin(2.0 * Math.PI * frequency * 3 * time) * 0.25
            val harmonic4 = sin(2.0 * Math.PI * frequency * 4 * time) * 0.125
            val harmonic5 = sin(2.0 * Math.PI * frequency * 5 * time) * 0.0625
            
            var sample = (fundamental + harmonic2 + harmonic3 + harmonic4 + harmonic5) / 1.9375
            
            // ADSR-like envelope: quick attack, slight decay, sustain, release
            val attackEnv = if (i < fadeInSamples) i.toDouble() / fadeInSamples else 1.0
            val decayEnv = kotlin.math.exp(-time * 2.0) * 0.3 + 0.7 // Decay to 70%
            val releaseStart = numSamples - SAMPLE_RATE * 50 / 1000
            val releaseEnv = if (i > releaseStart) {
                (numSamples - i).toDouble() / (numSamples - releaseStart)
            } else 1.0
            
            val envelope = attackEnv * decayEnv * releaseEnv
            
            samples[i] = (sample * envelope * velocity * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
        
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
        
        for (i in 0 until numSamples) {
            val time = i.toDouble() / SAMPLE_RATE
            
            var sample = 0.0
            for (frequency in frequencies) {
                sample += sin(2.0 * Math.PI * frequency * time)
            }
            sample /= frequencies.size
            
            val envelope = when {
                i < fadeInSamples -> i.toDouble() / fadeInSamples
                i > fadeOutStart -> (numSamples - i).toDouble() / fadeInSamples
                else -> 1.0
            }
            
            samples[i] = (sample * envelope * velocity * Short.MAX_VALUE).toInt().toShort()
        }
        
        playAudioTrack(samples)
    }
    
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
            Thread.sleep((samples.size * 1000L / SAMPLE_RATE) + 50)
        } finally {
            audioTrack.stop()
            audioTrack.release()
        }
    }
    
    fun release() {
        stop()
        nativeAudio.release()
        scope.cancel()
    }
}

/**
 * MIDI player state
 */
data class MidiPlayerState(
    val isPlaying: Boolean = false,
    val currentMidiNote: Int? = null
) {
    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    
    val displayNote: String get() = currentMidiNote?.let { midi ->
        val noteName = noteNames[midi % 12]
        val octave = midi / 12 - 1
        "$noteName$octave"
    } ?: "—"
}
