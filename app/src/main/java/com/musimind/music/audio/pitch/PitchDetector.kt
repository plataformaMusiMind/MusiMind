package com.musimind.music.audio.pitch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Native Android pitch detector using YIN algorithm
 * No external dependencies needed - uses Android AudioRecord directly
 */
@Singleton
class PitchDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 4
        
        // YIN algorithm parameters
        private const val YIN_THRESHOLD = 0.15f
        private const val MIN_FREQUENCY = 60.0 // Hz
        private const val MAX_FREQUENCY = 1500.0 // Hz
    }
    
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    
    /**
     * Start listening for pitch and emit results as a Flow
     */
    fun startListening(): Flow<PitchResult> = flow {
        if (!hasPermission()) {
            emit(PitchResult.Error("Permissão de áudio não concedida"))
            return@flow
        }
        
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        ) * BUFFER_SIZE_MULTIPLIER
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                emit(PitchResult.Error("Falha ao inicializar gravação"))
                return@flow
            }
            
            audioRecord?.startRecording()
            isListening = true
            
            val buffer = ShortArray(bufferSize / 2)
            val floatBuffer = FloatArray(bufferSize / 2)
            
            while (isListening && kotlinx.coroutines.currentCoroutineContext().isActive) {
                val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (readCount > 0) {
                    // Convert to float
                    for (i in 0 until readCount) {
                        floatBuffer[i] = buffer[i] / 32768f
                    }
                    
                    // Calculate RMS for amplitude
                    val rms = calculateRMS(floatBuffer, readCount)
                    
                    if (rms > 0.01f) { // Minimum volume threshold
                        // Detect pitch using YIN algorithm
                        val frequency = detectPitchYIN(floatBuffer, readCount)
                        
                        if (frequency > MIN_FREQUENCY && frequency < MAX_FREQUENCY) {
                            emit(PitchResult.Detected(
                                frequency = frequency.toFloat(),
                                amplitude = rms,
                                note = PitchUtils.frequencyToNoteName(frequency.toFloat()),
                                cents = PitchUtils.calculateCents(frequency.toFloat())
                            ))
                        } else {
                            emit(PitchResult.NoSound)
                        }
                    } else {
                        emit(PitchResult.NoSound)
                    }
                }
            }
        } catch (e: SecurityException) {
            emit(PitchResult.Error("Permissão de áudio negada"))
        } catch (e: Exception) {
            emit(PitchResult.Error("Erro: ${e.message}"))
        } finally {
            stopListening()
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Stop listening
     */
    fun stopListening() {
        isListening = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        audioRecord = null
    }
    
    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun calculateRMS(buffer: FloatArray, size: Int): Float {
        var sum = 0f
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        return sqrt(sum / size)
    }
    
    /**
     * YIN pitch detection algorithm
     * Reference: De Cheveigné, A., & Kawahara, H. (2002). YIN, a fundamental frequency estimator for speech and music.
     */
    private fun detectPitchYIN(buffer: FloatArray, size: Int): Double {
        val yinBufferSize = size / 2
        val yinBuffer = FloatArray(yinBufferSize)
        
        // Step 1: Calculate difference function
        for (tau in 0 until yinBufferSize) {
            yinBuffer[tau] = 0f
            for (i in 0 until yinBufferSize) {
                val delta = buffer[i] - buffer[i + tau]
                yinBuffer[tau] += delta * delta
            }
        }
        
        // Step 2: Cumulative mean normalized difference function
        yinBuffer[0] = 1f
        var runningSum = 0f
        for (tau in 1 until yinBufferSize) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] = yinBuffer[tau] * tau / runningSum
        }
        
        // Step 3: Absolute threshold
        var tauEstimate = -1
        for (tau in 2 until yinBufferSize) {
            if (yinBuffer[tau] < YIN_THRESHOLD) {
                while (tau + 1 < yinBufferSize && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tauEstimate = tau + 1
                }
                if (tauEstimate == -1) {
                    tauEstimate = tau
                }
                break
            }
        }
        
        if (tauEstimate == -1) {
            return -1.0
        }
        
        // Step 4: Parabolic interpolation for better accuracy
        val betterTau = if (tauEstimate > 0 && tauEstimate < yinBufferSize - 1) {
            val s0 = yinBuffer[tauEstimate - 1]
            val s1 = yinBuffer[tauEstimate]
            val s2 = yinBuffer[tauEstimate + 1]
            tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0))
        } else {
            tauEstimate.toFloat()
        }
        
        return SAMPLE_RATE / betterTau.toDouble()
    }
}

/**
 * Pitch detection result
 */
sealed class PitchResult {
    data class Detected(
        val frequency: Float,
        val amplitude: Float,
        val note: String,
        val cents: Int
    ) : PitchResult() {
        fun matchesPitch(targetNote: String, tolerance: Int = 50): Boolean {
            return note.take(note.length - 1) == targetNote.take(targetNote.length - 1) &&
                   abs(cents) <= tolerance
        }
    }
    
    data object NoSound : PitchResult()
    data class Error(val message: String) : PitchResult()
}

/**
 * Utility functions for pitch calculations
 */
object PitchUtils {
    private const val A4_FREQUENCY = 440.0f
    private const val A4_MIDI = 69
    
    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    
    /**
     * Convert frequency to note name (e.g., "A4", "C#5")
     */
    fun frequencyToNoteName(frequency: Float): String {
        if (frequency <= 0) return ""
        
        val midiNote = frequencyToMidi(frequency)
        val noteName = NOTE_NAMES[midiNote % 12]
        val octave = midiNote / 12 - 1
        return "$noteName$octave"
    }
    
    /**
     * Convert frequency to MIDI note number
     */
    fun frequencyToMidi(frequency: Float): Int {
        return (A4_MIDI + 12 * log2(frequency / A4_FREQUENCY)).toInt()
    }
    
    /**
     * Calculate cents deviation from nearest note
     */
    fun calculateCents(frequency: Float): Int {
        if (frequency <= 0) return 0
        
        val midiNote = frequencyToMidi(frequency)
        val exactFrequency = midiToFrequency(midiNote)
        
        return (1200 * log2(frequency / exactFrequency)).toInt()
    }
    
    /**
     * Convert MIDI note to frequency
     */
    fun midiToFrequency(midiNote: Int): Float {
        return A4_FREQUENCY * 2f.pow((midiNote - A4_MIDI) / 12f)
    }
    
    /**
     * Get frequency for a note name (e.g., "A4" -> 440.0)
     */
    fun noteNameToFrequency(noteName: String): Float {
        val regex = Regex("([A-G]#?)(\\d)")
        val match = regex.find(noteName) ?: return 0f
        
        val note = match.groupValues[1]
        val octave = match.groupValues[2].toIntOrNull() ?: return 0f
        
        val noteIndex = NOTE_NAMES.indexOf(note)
        if (noteIndex == -1) return 0f
        
        val midiNote = (octave + 1) * 12 + noteIndex
        return midiToFrequency(midiNote)
    }
}
