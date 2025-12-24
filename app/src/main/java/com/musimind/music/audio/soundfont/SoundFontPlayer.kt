package com.musimind.music.audio.soundfont

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * SoundFont based piano player.
 * Loads a General MIDI SoundFont (gm.sf2) from assets
 * and uses it for realistic piano playback.
 * 
 * This implementation parses the SF2 file to extract sample data
 * and uses AudioTrack for playback.
 */
@Singleton
class SoundFontPlayer @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SoundFontPlayer"
        private const val SAMPLE_RATE = 44100
        private const val SOUNDFONT_PATH = "soundfonts/gm.sf2"
        
        // SF2 RIFF chunk IDs
        private const val RIFF_ID = 0x46464952 // "RIFF"
        private const val SFBK_ID = 0x6B626673 // "sfbk"
        private const val LIST_ID = 0x5453494C // "LIST"
        private const val SDTA_ID = 0x61746473 // "sdta"
        private const val SMPL_ID = 0x6C706D73 // "smpl"
        private const val PDTA_ID = 0x61746470 // "pdta"
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Loaded sample data from SoundFont
    private var sampleData: ShortArray? = null
    private var isLoaded = false
    
    // Presets (instruments) mapping
    private val presets = mutableMapOf<Int, Preset>()
    
    /**
     * Initialize and load the SoundFont file.
     * Should be called once at app startup.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext true
        
        try {
            Log.d(TAG, "Loading SoundFont from assets...")
            context.assets.open(SOUNDFONT_PATH).use { inputStream ->
                parseSoundFont(inputStream)
            }
            isLoaded = true
            Log.d(TAG, "SoundFont loaded successfully. Sample data size: ${sampleData?.size ?: 0}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load SoundFont: ${e.message}", e)
            false
        }
    }
    
    /**
     * Parse SF2 file and extract sample data.
     */
    private fun parseSoundFont(input: InputStream) {
        val bytes = input.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        
        // Read RIFF header
        val riffId = buffer.int
        if (riffId != RIFF_ID) {
            throw IllegalArgumentException("Not a valid RIFF file")
        }
        
        val fileSize = buffer.int
        val sfbkId = buffer.int
        if (sfbkId != SFBK_ID) {
            throw IllegalArgumentException("Not a valid SoundFont file")
        }
        
        // Parse chunks
        while (buffer.remaining() > 8) {
            val chunkId = buffer.int
            val chunkSize = buffer.int
            val chunkStart = buffer.position()
            
            when (chunkId) {
                LIST_ID -> {
                    val listType = buffer.int
                    when (listType) {
                        SDTA_ID -> parseSdtaList(buffer, chunkSize - 4)
                        PDTA_ID -> parsePdtaList(buffer, chunkSize - 4)
                    }
                }
            }
            
            // Move to next chunk
            buffer.position(chunkStart + chunkSize)
            if (chunkSize % 2 != 0) buffer.position(buffer.position()) // padding
        }
    }
    
    /**
     * Parse sdta (sample data) list - contains actual audio samples.
     */
    private fun parseSdtaList(buffer: ByteBuffer, size: Int) {
        val end = buffer.position() + size
        
        while (buffer.position() < end) {
            val subChunkId = buffer.int
            val subChunkSize = buffer.int
            
            if (subChunkId == SMPL_ID) {
                // This is the raw sample data
                val numSamples = subChunkSize / 2 // 16-bit samples
                sampleData = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    sampleData!![i] = buffer.short
                }
                Log.d(TAG, "Loaded $numSamples samples from SoundFont")
            } else {
                // Skip other chunks
                buffer.position(buffer.position() + subChunkSize)
            }
            
            if (subChunkSize % 2 != 0) buffer.position(buffer.position()) // padding
        }
    }
    
    /**
     * Parse pdta (preset data) list - contains instrument definitions.
     * For simplicity, we use default piano samples.
     */
    private fun parsePdtaList(buffer: ByteBuffer, size: Int) {
        // Skip detailed parsing for now - we'll use a simplified approach
        buffer.position(buffer.position() + size)
    }
    
    /**
     * Play a MIDI note with piano sound.
     * 
     * @param midiNote MIDI note number (60 = C4)
     * @param durationMs Duration in milliseconds
     * @param velocity Velocity 0.0 to 1.0
     */
    suspend fun playNote(
        midiNote: Int,
        durationMs: Int = 500,
        velocity: Float = 0.8f
    ) {
        if (!isLoaded || sampleData == null) {
            Log.w(TAG, "SoundFont not loaded, skipping playback")
            return
        }
        
        withContext(Dispatchers.Default) {
            // Generate piano sound from SoundFont samples
            val samples = generatePianoSamples(midiNote, durationMs, velocity)
            playAudioSamples(samples)
        }
    }
    
    /**
     * Generate piano samples for a note.
     * Uses sample data from SF2 with pitch shifting.
     */
    private fun generatePianoSamples(
        midiNote: Int,
        durationMs: Int,
        velocity: Float
    ): ShortArray {
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val result = ShortArray(numSamples)
        
        val sfData = sampleData ?: return generateSynthPianoSamples(midiNote, durationMs, velocity)
        
        // Calculate pitch ratio for note transposition
        // Base sample is typically around C4 (MIDI 60)
        val baseNote = 60
        val pitchRatio = 2.0.pow((midiNote - baseNote) / 12.0)
        
        // Find a suitable region in the sample data
        // For General MIDI SF2, piano samples typically start at the beginning
        val sampleStart = 0
        val sampleLength = min(sfData.size, SAMPLE_RATE * 2) // Use up to 2 seconds of sample
        
        // ADSR envelope parameters
        val attackSamples = (SAMPLE_RATE * 0.005).toInt() // 5ms attack
        val decaySamples = (SAMPLE_RATE * 0.1).toInt() // 100ms decay
        val sustainLevel = 0.7f
        val releaseSamples = (SAMPLE_RATE * 0.1).toInt() // 100ms release
        val releaseStart = numSamples - releaseSamples
        
        for (i in 0 until numSamples) {
            // Sample position with pitch shifting
            val srcPos = ((i * pitchRatio) % sampleLength).toInt()
            
            // Get sample from SF2 data
            var sample = if (srcPos + sampleStart < sfData.size) {
                sfData[srcPos + sampleStart].toFloat() / Short.MAX_VALUE
            } else {
                0f
            }
            
            // Apply envelope
            val envelope = when {
                i < attackSamples -> i.toFloat() / attackSamples
                i < attackSamples + decaySamples -> {
                    val decayProgress = (i - attackSamples).toFloat() / decaySamples
                    1f - (1f - sustainLevel) * decayProgress
                }
                i > releaseStart -> {
                    sustainLevel * (numSamples - i).toFloat() / releaseSamples
                }
                else -> sustainLevel
            }
            
            sample *= envelope * velocity
            result[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        return result
    }
    
    /**
     * Fallback: Generate synthesized piano samples if SF2 loading fails.
     */
    private fun generateSynthPianoSamples(
        midiNote: Int,
        durationMs: Int,
        velocity: Float
    ): ShortArray {
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val samples = ShortArray(numSamples)
        val frequency = 440f * 2f.pow((midiNote - 69) / 12f)
        
        for (i in 0 until numSamples) {
            val time = i.toDouble() / SAMPLE_RATE
            
            // Piano-like sound with harmonics
            val fundamental = kotlin.math.sin(2.0 * Math.PI * frequency * time)
            val harmonic2 = kotlin.math.sin(4.0 * Math.PI * frequency * time) * 0.5
            val harmonic3 = kotlin.math.sin(6.0 * Math.PI * frequency * time) * 0.25
            
            var sample = (fundamental + harmonic2 + harmonic3) / 1.75
            
            // Envelope
            val attack = if (i < 220) i / 220.0 else 1.0
            val decay = kotlin.math.exp(-time * 2.0)
            
            sample *= attack * decay * velocity
            samples[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        return samples
    }
    
    private fun playAudioSamples(samples: ShortArray) {
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
    
    /**
     * Check if SoundFont is loaded.
     */
    fun isReady(): Boolean = isLoaded
    
    /**
     * Release resources.
     */
    fun release() {
        scope.cancel()
        sampleData = null
        isLoaded = false
    }
}

/**
 * Preset definition from SF2.
 */
data class Preset(
    val name: String,
    val preset: Int,
    val bank: Int,
    val sampleStart: Int,
    val sampleEnd: Int,
    val loopStart: Int,
    val loopEnd: Int,
    val rootKey: Int
)
