package com.musimind.music.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.musimind.music.MidiEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized Audio Manager for all MusiMind games
 * Provides a unified interface for playing notes, chords, and sound effects
 */
@Singleton
class GameAudioManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val midiEngine: MidiEngine
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // SoundPool for short sound effects
    private val soundPool: SoundPool by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()
    }
    
    // Sound effect IDs (loaded from raw resources)
    private var correctSoundId: Int = 0
    private var wrongSoundId: Int = 0
    private var tickSoundId: Int = 0
    private var completeSoundId: Int = 0
    private var comboSoundId: Int = 0
    private var levelUpSoundId: Int = 0
    
    private var isInitialized = false
    
    /**
     * Initialize audio resources
     * Call this once when the app starts
     */
    fun initialize() {
        if (isInitialized) return
        
        scope.launch {
            try {
                // Load sound effects from resources
                // Note: These resource IDs would need to exist in res/raw/
                // For now, we'll set them to 0 and handle gracefully
                
                // correctSoundId = soundPool.load(context, R.raw.correct, 1)
                // wrongSoundId = soundPool.load(context, R.raw.wrong, 1)
                // tickSoundId = soundPool.load(context, R.raw.tick, 1)
                // completeSoundId = soundPool.load(context, R.raw.complete, 1)
                // comboSoundId = soundPool.load(context, R.raw.combo, 1)
                // levelUpSoundId = soundPool.load(context, R.raw.level_up, 1)
                
                isInitialized = true
            } catch (e: Exception) {
                // Fallback: sounds won't play but app won't crash
            }
        }
    }
    
    /**
     * Play a single musical note
     * @param noteName Note in scientific notation (e.g., "C4", "A#3", "Bb5")
     * @param durationMs Duration in milliseconds
     * @param velocity Note velocity (0.0 to 1.0)
     */
    fun playNote(noteName: String, durationMs: Long = 500, velocity: Float = 0.8f) {
        scope.launch {
            try {
                val midiNote = noteNameToMidi(noteName)
                midiEngine.playNote(midiNote, (velocity * 127).toInt(), durationMs.toInt())
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
    
    /**
     * Play multiple notes as a chord
     * @param noteNames List of notes in scientific notation
     * @param durationMs Duration in milliseconds
     */
    fun playChord(noteNames: List<String>, durationMs: Long = 800) {
        scope.launch {
            try {
                noteNames.forEach { noteName ->
                    val midiNote = noteNameToMidi(noteName)
                    midiEngine.playNote(midiNote, 100, durationMs.toInt())
                }
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
    
    /**
     * Play a chord by name (e.g., "C", "Am", "G7")
     * @param chordName Chord name
     * @param octave Base octave (default 4)
     * @param durationMs Duration in milliseconds
     */
    fun playChordByName(chordName: String, octave: Int = 4, durationMs: Long = 800) {
        val notes = chordToNotes(chordName, octave)
        playChord(notes, durationMs)
    }
    
    /**
     * Play an interval
     * @param baseNote Base note
     * @param intervalSemitones Number of semitones for interval
     * @param durationMs Duration of each note
     * @param sequential Whether to play notes sequentially or simultaneously
     */
    fun playInterval(
        baseNote: String, 
        intervalSemitones: Int, 
        durationMs: Long = 500,
        sequential: Boolean = true
    ) {
        scope.launch {
            try {
                val baseMidi = noteNameToMidi(baseNote)
                val secondMidi = baseMidi + intervalSemitones
                
                if (sequential) {
                    midiEngine.playNote(baseMidi, 100, durationMs.toInt())
                    kotlinx.coroutines.delay(durationMs)
                    midiEngine.playNote(secondMidi, 100, durationMs.toInt())
                } else {
                    midiEngine.playNote(baseMidi, 100, durationMs.toInt())
                    midiEngine.playNote(secondMidi, 100, durationMs.toInt())
                }
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
    
    /**
     * Play a scale
     * @param rootNote Root note of the scale
     * @param scaleType Type of scale ("major", "minor", "pentatonic", etc.)
     * @param ascending Whether to play ascending or descending
     * @param noteDurationMs Duration of each note
     */
    fun playScale(
        rootNote: String,
        scaleType: String = "major",
        ascending: Boolean = true,
        noteDurationMs: Long = 300
    ) {
        scope.launch {
            try {
                val intervals = when (scaleType.lowercase()) {
                    "major" -> listOf(0, 2, 4, 5, 7, 9, 11, 12)
                    "minor", "natural_minor" -> listOf(0, 2, 3, 5, 7, 8, 10, 12)
                    "harmonic_minor" -> listOf(0, 2, 3, 5, 7, 8, 11, 12)
                    "melodic_minor" -> listOf(0, 2, 3, 5, 7, 9, 11, 12)
                    "pentatonic" -> listOf(0, 2, 4, 7, 9, 12)
                    "blues" -> listOf(0, 3, 5, 6, 7, 10, 12)
                    "chromatic" -> (0..12).toList()
                    else -> listOf(0, 2, 4, 5, 7, 9, 11, 12)
                }
                
                val baseMidi = noteNameToMidi(rootNote)
                val orderedIntervals = if (ascending) intervals else intervals.reversed()
                
                for (interval in orderedIntervals) {
                    midiEngine.playNote(baseMidi + interval, 100, noteDurationMs.toInt())
                    kotlinx.coroutines.delay(noteDurationMs)
                }
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
    
    /**
     * Play metronome tick
     * @param isStrong Whether this is a strong beat (downbeat)
     */
    fun playMetronomeTick(isStrong: Boolean = false) {
        scope.launch {
            try {
                // Use different pitches for strong/weak beats
                val pitch = if (isStrong) 76 else 77 // High wood block sounds
                midiEngine.playNote(pitch, if (isStrong) 110 else 80, 50)
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
    
    /**
     * Play success/correct sound effect
     */
    fun playSuccessSound() {
        scope.launch {
            try {
                // Play a pleasant ascending arpeggio
                val notes = listOf(60, 64, 67) // C major chord
                for (note in notes) {
                    midiEngine.playNote(note, 80, 100)
                    kotlinx.coroutines.delay(80)
                }
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
    
    /**
     * Play error/wrong sound effect
     */
    fun playErrorSound() {
        scope.launch {
            try {
                // Play a dissonant interval
                midiEngine.playNote(60, 80, 200) // C
                midiEngine.playNote(61, 80, 200) // C#
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
    
    /**
     * Play level complete fanfare
     */
    fun playLevelComplete() {
        scope.launch {
            try {
                // Victory fanfare
                val melody = listOf(
                    Pair(67, 150), // G
                    Pair(72, 150), // C
                    Pair(76, 150), // E
                    Pair(79, 300)  // G (high)
                )
                for ((note, duration) in melody) {
                    midiEngine.playNote(note, 100, duration)
                    kotlinx.coroutines.delay(duration.toLong())
                }
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
    
    /**
     * Play combo achievement sound
     */
    fun playComboSound(comboCount: Int) {
        scope.launch {
            try {
                // Higher pitch for higher combos
                val basePitch = 72 + (comboCount.coerceAtMost(10) * 2)
                midiEngine.playNote(basePitch, 100, 100)
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
    
    /**
     * Convert note name to MIDI note number
     */
    private fun noteNameToMidi(noteName: String): Int {
        val pattern = Regex("([A-Ga-g])([#b]?)(\\d+)")
        val match = pattern.matchEntire(noteName.trim())
            ?: return 60 // Default to middle C
        
        val (noteLetter, accidental, octaveStr) = match.destructured
        val octave = octaveStr.toIntOrNull() ?: 4
        
        val baseNote = when (noteLetter.uppercase()) {
            "C" -> 0
            "D" -> 2
            "E" -> 4
            "F" -> 5
            "G" -> 7
            "A" -> 9
            "B" -> 11
            else -> 0
        }
        
        val accidentalOffset = when (accidental) {
            "#" -> 1
            "b" -> -1
            else -> 0
        }
        
        return 12 * (octave + 1) + baseNote + accidentalOffset
    }
    
    /**
     * Convert chord name to list of notes
     */
    private fun chordToNotes(chordName: String, octave: Int): List<String> {
        val cleaned = chordName.trim()
        
        // Parse root note
        val rootPattern = Regex("^([A-Ga-g][#b]?)")
        val rootMatch = rootPattern.find(cleaned)
        val root = rootMatch?.value?.uppercase() ?: "C"
        
        // Determine chord type
        val type = cleaned.substring(root.length).lowercase()
        
        // Get intervals for chord type
        val intervals = when {
            type.isEmpty() || type == "maj" -> listOf(0, 4, 7) // Major
            type == "m" || type == "min" -> listOf(0, 3, 7) // Minor
            type == "dim" -> listOf(0, 3, 6) // Diminished
            type == "aug" -> listOf(0, 4, 8) // Augmented
            type == "7" -> listOf(0, 4, 7, 10) // Dominant 7
            type == "maj7" -> listOf(0, 4, 7, 11) // Major 7
            type == "m7" || type == "min7" -> listOf(0, 3, 7, 10) // Minor 7
            type == "sus4" -> listOf(0, 5, 7) // Suspended 4
            type == "sus2" -> listOf(0, 2, 7) // Suspended 2
            else -> listOf(0, 4, 7) // Default to major
        }
        
        // Convert intervals to note names
        val rootMidi = noteNameToMidi("$root$octave")
        return intervals.map { interval ->
            midiToNoteName(rootMidi + interval)
        }
    }
    
    /**
     * Convert MIDI note number back to note name
     */
    private fun midiToNoteName(midi: Int): String {
        val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val octave = (midi / 12) - 1
        val noteIndex = midi % 12
        return "${noteNames[noteIndex]}$octave"
    }
    
    /**
     * Release audio resources
     */
    fun release() {
        soundPool.release()
    }
}
