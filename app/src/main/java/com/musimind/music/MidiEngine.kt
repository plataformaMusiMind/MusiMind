package com.musimind.music

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Simple MIDI-like engine for playing musical notes
 * Uses tone generation or synthesized sounds
 */
@Singleton
class MidiEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Track active notes for stopping
    private val activeNotes = mutableMapOf<Int, android.media.AudioTrack?>()
    
    // Audio parameters
    private val sampleRate = 44100
    
    /**
     * Play a MIDI note
     * @param midiNote MIDI note number (0-127, 60 = Middle C)
     * @param velocity Note velocity (0-127)
     * @param durationMs Duration in milliseconds
     */
    fun playNote(midiNote: Int, velocity: Int = 100, durationMs: Int = 500) {
        scope.launch {
            try {
                val frequency = midiToFrequency(midiNote)
                val amplitude = (velocity / 127.0).coerceIn(0.0, 1.0)
                
                playTone(frequency, amplitude, durationMs)
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
    
    /**
     * Play multiple notes simultaneously (chord)
     */
    fun playNotes(midiNotes: List<Int>, velocity: Int = 100, durationMs: Int = 500) {
        midiNotes.forEach { note ->
            playNote(note, velocity, durationMs)
        }
    }
    
    /**
     * Stop a specific note
     */
    fun stopNote(midiNote: Int) {
        activeNotes[midiNote]?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        activeNotes.remove(midiNote)
    }
    
    /**
     * Stop all notes
     */
    fun stopAllNotes() {
        activeNotes.values.forEach { track ->
            try {
                track?.stop()
                track?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        activeNotes.clear()
    }
    
    /**
     * Convert MIDI note number to frequency
     * A4 (MIDI 69) = 440 Hz
     */
    private fun midiToFrequency(midiNote: Int): Double {
        return 440.0 * 2.0.pow((midiNote - 69) / 12.0)
    }
    
    /**
     * Generate and play a sine wave tone
     */
    private fun playTone(frequency: Double, amplitude: Double, durationMs: Int) {
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        
        // Generate sine wave with envelope
        val attackSamples = (numSamples * 0.05).toInt()
        val releaseSamples = (numSamples * 0.2).toInt()
        
        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val sample = kotlin.math.sin(2.0 * Math.PI * frequency * time)
            
            // Apply envelope (attack and release)
            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i > numSamples - releaseSamples -> (numSamples - i).toDouble() / releaseSamples
                else -> 1.0
            }
            
            samples[i] = (sample * amplitude * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        
        try {
            val audioTrack = android.media.AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                samples.size * 2,
                android.media.AudioTrack.MODE_STATIC
            )
            
            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            
            // Auto-release after playback
            scope.launch {
                delay(durationMs.toLong() + 100)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            // Silent fallback - audio might not be available
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        stopAllNotes()
    }
}
