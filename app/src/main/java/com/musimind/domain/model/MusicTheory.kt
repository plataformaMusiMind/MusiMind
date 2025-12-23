package com.musimind.domain.model

import kotlinx.serialization.Serializable

/**
 * Musical clef types
 */
@Serializable
enum class Clef {
    TREBLE,     // Clave de Sol
    BASS,       // Clave de Fá
    ALTO,       // Clave de Dó na 3ª linha
    TENOR       // Clave de Dó na 4ª linha
}

/**
 * Note duration values
 */
@Serializable
enum class NoteDuration(val beats: Float, val displayName: String) {
    WHOLE(4f, "Semibreve"),
    HALF(2f, "Mínima"),
    QUARTER(1f, "Semínima"),
    EIGHTH(0.5f, "Colcheia"),
    SIXTEENTH(0.25f, "Semicolcheia"),
    THIRTY_SECOND(0.125f, "Fusa"),
    SIXTY_FOURTH(0.0625f, "Semifusa")
}

/**
 * Represents a musical note with pitch and duration
 */
@Serializable
data class MusicNote(
    val pitch: String,          // e.g., "C4", "D#5", "Bb3"
    val duration: NoteDuration,
    val beat: Float,            // Position in the measure
    val isRest: Boolean = false,
    val isTied: Boolean = false,
    val isDotted: Boolean = false,
    val accidental: Accidental? = null
)

/**
 * Accidentals
 */
@Serializable
enum class Accidental {
    SHARP,      // Sustenido
    FLAT,       // Bemol
    NATURAL,    // Bequadro
    DOUBLE_SHARP,
    DOUBLE_FLAT
}

/**
 * Time signature representation
 */
@Serializable
data class TimeSignature(
    val numerator: Int,     // Top number (beats per measure)
    val denominator: Int    // Bottom number (note value that gets one beat)
) {
    override fun toString(): String = "$numerator/$denominator"
    
    companion object {
        val COMMON_TIME = TimeSignature(4, 4)
        val CUT_TIME = TimeSignature(2, 2)
        val THREE_FOUR = TimeSignature(3, 4)
        val SIX_EIGHT = TimeSignature(6, 8)
    }
}

/**
 * Key signature
 */
@Serializable
data class KeySignature(
    val key: String,        // e.g., "C", "G", "F#m"
    val isMinor: Boolean = false,
    val sharps: Int = 0,    // Number of sharps (0-7)
    val flats: Int = 0      // Number of flats (0-7)
) {
    companion object {
        val C_MAJOR = KeySignature("C", false, 0, 0)
        val G_MAJOR = KeySignature("G", false, 1, 0)
        val D_MAJOR = KeySignature("D", false, 2, 0)
        val A_MAJOR = KeySignature("A", false, 3, 0)
        val F_MAJOR = KeySignature("F", false, 0, 1)
        val Bb_MAJOR = KeySignature("Bb", false, 0, 2)
        val Eb_MAJOR = KeySignature("Eb", false, 0, 3)
    }
}

/**
 * A measure containing notes
 */
@Serializable
data class Measure(
    val number: Int,
    val notes: List<MusicNote>
)

/**
 * Complete exercise data for solfege and other exercises
 */
@Serializable
data class MusicExercise(
    val id: String,
    val title: String,
    val description: String = "",
    val category: MusicCategory,
    val difficulty: Int,        // 1-5
    val clef: Clef,
    val timeSignature: TimeSignature,
    val keySignature: KeySignature,
    val tempo: Int,             // BPM
    val measures: List<Measure>,
    val audioUrl: String? = null,
    val xpReward: Int = 10,
    val passingScore: Int = 75  // Minimum score to pass (%)
)

/**
 * Interval types for ear training
 */
@Serializable
enum class Interval(val semitones: Int, val displayName: String) {
    UNISON(0, "Uníssono"),
    MINOR_SECOND(1, "2ª menor"),
    MAJOR_SECOND(2, "2ª maior"),
    MINOR_THIRD(3, "3ª menor"),
    MAJOR_THIRD(4, "3ª maior"),
    PERFECT_FOURTH(5, "4ª justa"),
    TRITONE(6, "Trítono"),
    PERFECT_FIFTH(7, "5ª justa"),
    MINOR_SIXTH(8, "6ª menor"),
    MAJOR_SIXTH(9, "6ª maior"),
    MINOR_SEVENTH(10, "7ª menor"),
    MAJOR_SEVENTH(11, "7ª maior"),
    OCTAVE(12, "8ª justa")
}

/**
 * Chord types for harmonic perception
 */
@Serializable
enum class ChordType(val displayName: String, val symbol: String) {
    MAJOR("Maior", ""),
    MINOR("Menor", "m"),
    DIMINISHED("Diminuto", "dim"),
    AUGMENTED("Aumentado", "aug"),
    DOMINANT_SEVENTH("Dominante com 7ª", "7"),
    MAJOR_SEVENTH("Maior com 7ª", "maj7"),
    MINOR_SEVENTH("Menor com 7ª", "m7"),
    HALF_DIMINISHED("Meio-diminuto", "m7b5"),
    DIMINISHED_SEVENTH("Diminuto com 7ª", "dim7")
}

/**
 * A chord in a harmonic progression
 */
@Serializable
data class Chord(
    val root: String,           // e.g., "C", "F#", "Bb"
    val type: ChordType,
    val inversion: Int = 0,     // 0 = root position, 1 = first inversion, etc.
    val romanNumeral: String = "" // e.g., "I", "IV", "V7"
)

/**
 * Harmonic progression exercise
 */
@Serializable
data class HarmonicExercise(
    val id: String,
    val title: String,
    val key: KeySignature,
    val tempo: Int,
    val chords: List<Chord>,
    val audioUrl: String? = null,
    val xpReward: Int = 15
)
