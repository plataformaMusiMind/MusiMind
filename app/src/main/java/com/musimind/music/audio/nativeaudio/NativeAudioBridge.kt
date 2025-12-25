package com.musimind.music.audio.nativeaudio

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kotlin bridge to native Oboe + TinySoundFont audio engine.
 * 
 * This provides industry-standard, low-latency audio playback
 * using Google's Oboe library and TinySoundFont for SoundFont synthesis.
 */
@Singleton
class NativeAudioBridge @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NativeAudioBridge"
        private const val SOUNDFONT_PATH = "soundfonts/gm.sf2"
        
        init {
            try {
                System.loadLibrary("native-audio")
                Log.i(TAG, "Native audio library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native audio library: ${e.message}")
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isInitialized = false
    
    /**
     * Initialize the native audio engine.
     * Should be called once at app startup.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true
        
        try {
            val assetManager = context.assets
            isInitialized = nativeInitialize(assetManager, SOUNDFONT_PATH)
            Log.i(TAG, "Native audio engine initialized: $isInitialized")
            isInitialized
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize native audio: ${e.message}")
            false
        }
    }
    
    /**
     * Play a MIDI note with the piano sound.
     * 
     * @param midiNote MIDI note number (60 = C4)
     * @param velocity Note velocity (0.0 to 1.0)
     * @param durationMs Duration in milliseconds
     */
    fun playNote(midiNote: Int, velocity: Float = 0.8f, durationMs: Int = 500) {
        if (!isReady()) {
            Log.w(TAG, "Native audio not ready, skipping playNote")
            return
        }
        
        Log.d(TAG, "Playing note: midi=$midiNote, velocity=$velocity, duration=$durationMs")
        nativeNoteOn(0, midiNote, velocity)
        
        // Schedule note off
        scope.launch {
            delay(durationMs.toLong())
            nativeNoteOff(0, midiNote)
        }
    }
    
    /**
     * Play a metronome click.
     * 
     * @param isAccented True for first beat (louder)
     */
    fun playMetronome(isAccented: Boolean = false) {
        if (!isReady()) {
            Log.w(TAG, "Native audio not ready, skipping metronome")
            return
        }
        nativePlayMetronome(isAccented)
    }
    
    /**
     * Set the instrument preset (0 = Grand Piano by default).
     */
    fun setPreset(channel: Int, preset: Int) {
        if (isReady()) {
            nativeSetPreset(channel, preset)
        }
    }
    
    /**
     * Check if the engine is ready to play.
     */
    fun isReady(): Boolean = try {
        nativeIsReady()
    } catch (e: UnsatisfiedLinkError) {
        false
    }
    
    /**
     * Get the native sample rate.
     */
    fun getSampleRate(): Int = try {
        nativeGetSampleRate()
    } catch (e: UnsatisfiedLinkError) {
        44100
    }
    
    /**
     * Release all native resources.
     */
    fun release() {
        scope.cancel()
        try {
            nativeRelease()
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing native audio: ${e.message}")
        }
    }
    
    // Native methods
    private external fun nativeInitialize(assetManager: AssetManager, soundFontPath: String): Boolean
    private external fun nativeNoteOn(channel: Int, midiNote: Int, velocity: Float)
    private external fun nativeNoteOff(channel: Int, midiNote: Int)
    private external fun nativePlayMetronome(isAccented: Boolean)
    private external fun nativeSetPreset(channel: Int, preset: Int)
    private external fun nativeIsReady(): Boolean
    private external fun nativeGetSampleRate(): Int
    private external fun nativeRelease()
}
