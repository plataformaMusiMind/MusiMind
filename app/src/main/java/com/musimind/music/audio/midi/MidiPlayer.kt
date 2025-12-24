package com.musimind.music.audio.midi

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.musimind.music.notation.model.Pitch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sin

/**
 * Simple MIDI-like synthesizer for playing notes
 */
@Singleton
class MidiPlayer @Inject constructor() {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val DEFAULT_DURATION_MS = 500
        private const val FADE_DURATION_MS = 20
        private const val A4_FREQUENCY = 440.0f
    }
    
    private val _state = MutableStateFlow(MidiPlayerState())
    val state: StateFlow<MidiPlayerState> = _state.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentPlayJob: Job? = null
    
    /**
     * Play a single pitch by MIDI note number
     */
    fun playMidiNote(
        midiNote: Int,
        durationMs: Int = DEFAULT_DURATION_MS,
        velocity: Float = 0.8f
    ) {
        currentPlayJob?.cancel()
        currentPlayJob = scope.launch {
            try {
                _state.value = _state.value.copy(
                    isPlaying = true,
                    currentMidiNote = midiNote
                )
                
                val frequency = midiToFrequency(midiNote)
                playTone(frequency, durationMs, velocity)
                
            } finally {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    currentMidiNote = null
                )
            }
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
        
        for (i in 0 until numSamples) {
            val time = i.toDouble() / SAMPLE_RATE
            val sample = sin(2.0 * Math.PI * frequency * time)
            
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
