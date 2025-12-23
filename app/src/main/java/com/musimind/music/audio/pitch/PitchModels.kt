package com.musimind.music.audio.pitch

import com.musimind.music.notation.model.NoteName
import com.musimind.music.notation.model.Pitch

/**
 * Pitch detection result from audio analysis
 */
data class PitchResult(
    val frequency: Float,           // Frequency in Hz
    val probability: Float,         // Detection confidence (0-1)
    val pitch: Pitch?,              // Detected musical pitch
    val centsDeviation: Float,      // Deviation from perfect pitch in cents
    val amplitude: Float            // Signal amplitude (for volume/silence detection)
) {
    companion object {
        val SILENT = PitchResult(0f, 0f, null, 0f, 0f)
    }
    
    val isValid: Boolean get() = probability > 0.8f && frequency > 50f && frequency < 2000f
    val isSilent: Boolean get() = amplitude < 0.01f
    
    /**
     * Returns true if the detected pitch matches the target pitch within tolerance
     */
    fun matchesPitch(target: Pitch, centsTolerance: Float = 50f): Boolean {
        if (!isValid || pitch == null) return false
        
        val targetMidi = target.midiPitch
        val detectedMidi = pitch.midiPitch
        
        // Same note
        if (detectedMidi == targetMidi && kotlin.math.abs(centsDeviation) <= centsTolerance) {
            return true
        }
        
        return false
    }
    
    /**
     * Returns match quality as percentage (0-100)
     */
    fun matchQuality(target: Pitch, centsTolerance: Float = 50f): Float {
        if (!isValid || pitch == null) return 0f
        
        val targetMidi = target.midiPitch
        val detectedMidi = pitch.midiPitch
        
        // Wrong note entirely
        if (detectedMidi != targetMidi) return 0f
        
        // Calculate quality based on cents deviation
        val deviation = kotlin.math.abs(centsDeviation)
        return when {
            deviation <= 10f -> 100f  // Perfect
            deviation <= 25f -> 90f   // Excellent
            deviation <= 40f -> 75f   // Good
            deviation <= 50f -> 60f   // Fair
            else -> 40f               // Poor but correct note
        }
    }
}

/**
 * Utility object for pitch calculations
 */
object PitchUtils {
    // A4 = 440 Hz (concert pitch)
    const val A4_FREQUENCY = 440f
    const val A4_MIDI = 69
    
    // Frequency range for voice
    const val MIN_VOICE_FREQUENCY = 80f   // Below bass voice
    const val MAX_VOICE_FREQUENCY = 1200f // Above soprano voice
    
    /**
     * Convert frequency to MIDI note number
     */
    fun frequencyToMidi(frequency: Float): Float {
        if (frequency <= 0) return 0f
        return 12f * (kotlin.math.log2(frequency / A4_FREQUENCY)) + A4_MIDI
    }
    
    /**
     * Convert MIDI note number to frequency
     */
    fun midiToFrequency(midi: Int): Float {
        return A4_FREQUENCY * kotlin.math.pow(2.0, (midi - A4_MIDI) / 12.0).toFloat()
    }
    
    /**
     * Convert frequency to Pitch object
     */
    fun frequencyToPitch(frequency: Float): Pair<Pitch, Float>? {
        if (frequency <= 0) return null
        
        val midiFloat = frequencyToMidi(frequency)
        val midiRounded = kotlin.math.round(midiFloat).toInt()
        val centsDeviation = (midiFloat - midiRounded) * 100f
        
        val pitch = midiToPitch(midiRounded)
        return pitch to centsDeviation
    }
    
    /**
     * Convert MIDI note number to Pitch
     */
    fun midiToPitch(midi: Int): Pitch {
        val octave = (midi / 12) - 1
        val noteIndex = midi % 12
        
        // Map note index to NoteName and alteration
        val (noteName, alteration) = when (noteIndex) {
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
     * Get frequency for a specific pitch
     */
    fun pitchToFrequency(pitch: Pitch): Float {
        return midiToFrequency(pitch.midiPitch)
    }
    
    /**
     * Calculate cents difference between two frequencies
     */
    fun centsDifference(frequency1: Float, frequency2: Float): Float {
        if (frequency1 <= 0 || frequency2 <= 0) return 0f
        return 1200f * kotlin.math.log2(frequency1 / frequency2).toFloat()
    }
    
    /**
     * Get note name with accidental for display
     */
    fun pitchToDisplayString(pitch: Pitch): String {
        val noteName = pitch.note.name
        val accidental = when (pitch.alteration) {
            1 -> "♯"
            -1 -> "♭"
            2 -> "♯♯"
            -2 -> "♭♭"
            else -> ""
        }
        return "$noteName$accidental${pitch.octave}"
    }
    
    /**
     * Get solfege name for a pitch
     */
    fun pitchToSolfege(pitch: Pitch): String {
        return when (pitch.note) {
            NoteName.C -> "Dó"
            NoteName.D -> "Ré"
            NoteName.E -> "Mi"
            NoteName.F -> "Fá"
            NoteName.G -> "Sol"
            NoteName.A -> "Lá"
            NoteName.B -> "Si"
        } + when (pitch.alteration) {
            1 -> "♯"
            -1 -> "♭"
            else -> ""
        }
    }
}
