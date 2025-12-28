package com.musimind.music.notation.model

import com.musimind.music.notation.smufl.SMuFLGlyphs

/**
 * Represents a complete musical score that can be rendered
 */
data class Score(
    val id: String,
    val title: String,
    val composer: String? = null,
    val measures: List<Measure>,
    val staves: Int = 1,
    val clef: ClefType = ClefType.TREBLE,
    val keySignature: KeySignatureType = KeySignatureType.C_MAJOR,
    val timeSignature: TimeSignatureModel = TimeSignatureModel(4, 4),
    val tempo: Int? = null,
    val staffSpacing: Float = 8f // Space between staff lines
)

/**
 * Represents a single measure/bar
 */
data class Measure(
    val number: Int,
    val elements: List<MusicElement>,
    val barlineType: BarlineType = BarlineType.SINGLE
)

/**
 * Base sealed class for all music elements
 */
sealed class MusicElement {
    abstract val id: String
    abstract val durationBeats: Float
    abstract val positionX: Float? // Calculated during layout
}

/**
 * Represents a note
 */
data class Note(
    override val id: String,
    override val durationBeats: Float,
    override val positionX: Float? = null,
    val pitch: Pitch,
    val accidental: AccidentalType? = null,
    val tied: Boolean = false,
    val slurred: Boolean = false,
    val dotted: Boolean = false,
    val doubleDotted: Boolean = false,
    val articulations: List<ArticulationType> = emptyList(),
    val dynamic: DynamicType? = null,
    val ornament: OrnamentType? = null,
    val beamGroup: Int? = null, // Notes with same group are beamed together
    val voice: Int = 1,
    val finger: Int? = null, // Fingering 1-5
    val isGrace: Boolean = false,
    val state: NoteState = NoteState.NORMAL, // For visual feedback
    val pitchFeedback: FeedbackState = FeedbackState.NONE,    // Solfege: feedback for pitch (notehead)
    val durationFeedback: FeedbackState = FeedbackState.NONE, // Solfege: feedback for duration (stem/beam)
    val beatNumber: Float? = null,   // Beat start time (e.g., 1, 2, 2.5, 3)
    val solfegeName: String? = null  // Solfege name (Dó, Ré, Mi...)
) : MusicElement()

/**
 * Represents a chord (multiple notes at same time)
 */
data class Chord(
    override val id: String,
    override val durationBeats: Float,
    override val positionX: Float? = null,
    val notes: List<Note>,
    val arpeggio: Boolean = false
) : MusicElement()

/**
 * Represents a rest
 */
data class Rest(
    override val id: String,
    override val durationBeats: Float,
    override val positionX: Float? = null,
    val isWholeMeasure: Boolean = false
) : MusicElement()

/**
 * Represents pitch with note name and octave
 */
data class Pitch(
    val note: NoteName,
    val octave: Int, // Middle C = C4
    val alteration: Int = 0 // -2 double flat, -1 flat, 0 natural, 1 sharp, 2 double sharp
) {
    /**
     * MIDI pitch number (60 = Middle C)
     */
    val midiPitch: Int get() = (octave + 1) * 12 + note.semitone + alteration
    
    /**
     * Convert note name to diatonic position (0-6 for C-B)
     * This ignores chromatic alterations for staff position calculation
     */
    private fun diatonicPosition(): Int = when (note) {
        NoteName.C, NoteName.C_SHARP -> 0
        NoteName.D, NoteName.D_SHARP -> 1
        NoteName.E -> 2
        NoteName.F, NoteName.F_SHARP -> 3
        NoteName.G, NoteName.G_SHARP -> 4
        NoteName.A, NoteName.A_SHARP -> 5
        NoteName.B -> 6
    }
    
    /**
     * Position on staff relative to Middle C (0 = C4)
     * 
     * Staff position determines vertical placement on the staff:
     * - Position 0 = first ledger line below staff (C4 in treble clef)
     * - Position 2 = first staff line (E4 in treble clef)
     * - Position 4 = second staff line (G4 in treble clef)
     * - etc.
     */
    fun staffPosition(clef: ClefType): Int {
        // Use diatonic position (0-6) instead of chromatic ordinal (0-11)
        val basePosition = (octave - 4) * 7 + diatonicPosition()
        return when (clef) {
            ClefType.TREBLE -> basePosition       // C4 = position 0 (first ledger line below)
            ClefType.BASS -> basePosition + 12    // C4 = position 12 (above staff in bass clef)
            ClefType.ALTO -> basePosition + 6     // C4 = position 6 (middle line)
            ClefType.TENOR -> basePosition + 8
            ClefType.PERCUSSION -> 4              // Center of staff
        }
    }
}

enum class NoteName(val semitone: Int) {
    C(0), C_SHARP(1), D(2), D_SHARP(3), E(4), F(5), F_SHARP(6), G(7), G_SHARP(8), A(9), A_SHARP(10), B(11)
}

enum class ClefType(val glyph: Char, val staffLine: Int) {
    TREBLE(SMuFLGlyphs.Clefs.TREBLE, 1),      // Line 2 from bottom (G4)
    BASS(SMuFLGlyphs.Clefs.BASS, 3),           // Line 4 from bottom (F3)
    ALTO(SMuFLGlyphs.Clefs.ALTO, 2),           // Line 3 (C4)
    TENOR(SMuFLGlyphs.Clefs.ALTO, 3),          // Line 4 (C4)
    PERCUSSION(SMuFLGlyphs.Clefs.PERCUSSION, 2)
}

enum class KeySignatureType(val accidentals: Int, val isSharp: Boolean) {
    C_MAJOR(0, true),
    G_MAJOR(1, true), D_MAJOR(2, true), A_MAJOR(3, true),
    E_MAJOR(4, true), B_MAJOR(5, true), F_SHARP_MAJOR(6, true), C_SHARP_MAJOR(7, true),
    F_MAJOR(1, false), B_FLAT_MAJOR(2, false), E_FLAT_MAJOR(3, false),
    A_FLAT_MAJOR(4, false), D_FLAT_MAJOR(5, false), G_FLAT_MAJOR(6, false), C_FLAT_MAJOR(7, false)
}

data class TimeSignatureModel(
    val numerator: Int,
    val denominator: Int
) {
    val isCommon: Boolean get() = numerator == 4 && denominator == 4
    val isCut: Boolean get() = numerator == 2 && denominator == 2
    val beatsPerMeasure: Float get() = numerator.toFloat() * (4f / denominator)
}

enum class AccidentalType(val glyph: Char) {
    FLAT(SMuFLGlyphs.Accidentals.FLAT),
    NATURAL(SMuFLGlyphs.Accidentals.NATURAL),
    SHARP(SMuFLGlyphs.Accidentals.SHARP),
    DOUBLE_FLAT(SMuFLGlyphs.Accidentals.DOUBLE_FLAT),
    DOUBLE_SHARP(SMuFLGlyphs.Accidentals.DOUBLE_SHARP)
}

enum class ArticulationType(val glyph: Char) {
    STACCATO(SMuFLGlyphs.Articulations.STACCATO),
    STACCATISSIMO(SMuFLGlyphs.Articulations.STACCATISSIMO),
    ACCENT(SMuFLGlyphs.Articulations.ACCENT),
    TENUTO(SMuFLGlyphs.Articulations.TENUTO),
    MARCATO(SMuFLGlyphs.Articulations.MARCATO),
    FERMATA(SMuFLGlyphs.HoldsAndPauses.FERMATA)
}

enum class DynamicType(val glyph: Char) {
    PP(SMuFLGlyphs.Dynamics.PP),
    P(SMuFLGlyphs.Dynamics.P),
    MP(SMuFLGlyphs.Dynamics.MP),
    MF(SMuFLGlyphs.Dynamics.MF),
    F(SMuFLGlyphs.Dynamics.F),
    FF(SMuFLGlyphs.Dynamics.FF),
    SF(SMuFLGlyphs.Dynamics.SF),
    FP(SMuFLGlyphs.Dynamics.FP)
}

enum class OrnamentType(val glyph: Char) {
    TRILL(SMuFLGlyphs.Ornaments.TRILL),
    MORDENT(SMuFLGlyphs.Ornaments.MORDENT),
    INVERTED_MORDENT(SMuFLGlyphs.Ornaments.INVERTED_MORDENT),
    TURN(SMuFLGlyphs.Ornaments.TURN),
    INVERTED_TURN(SMuFLGlyphs.Ornaments.INVERTED_TURN)
}

enum class BarlineType(val glyph: Char) {
    SINGLE(SMuFLGlyphs.Barlines.SINGLE),
    DOUBLE(SMuFLGlyphs.Barlines.DOUBLE),
    FINAL(SMuFLGlyphs.Barlines.FINAL),
    REPEAT_LEFT(SMuFLGlyphs.Repeats.REPEAT_LEFT),
    REPEAT_RIGHT(SMuFLGlyphs.Repeats.REPEAT_RIGHT),
    REPEAT_BOTH(SMuFLGlyphs.Repeats.REPEAT_BOTH)
}

/**
 * Visual state for interactive feedback
 */
enum class NoteState {
    NORMAL,      // Default appearance
    HIGHLIGHTED, // Currently selected or being worked on
    CORRECT,     // User sang/played correctly
    INCORRECT,   // User made a mistake
    UPCOMING,    // Next note to play
    PASSED       // Already played in sequence
}

/**
 * Feedback state for solfege exercises
 * Allows separate feedback for pitch (notehead) and duration (stem/beam/flag)
 */
enum class FeedbackState {
    NONE,      // No feedback yet
    CORRECT,   // Green - correct
    INCORRECT  // Red - incorrect
}

/**
 * Duration utilities
 */
object Duration {
    const val DOUBLE_WHOLE = 8f
    const val WHOLE = 4f           // Semibreve
    const val HALF = 2f            // Mínima
    const val QUARTER = 1f         // Semínima
    const val EIGHTH = 0.5f        // Colcheia
    const val SIXTEENTH = 0.25f    // Semicolcheia
    const val THIRTY_SECOND = 0.125f  // Fusa
    const val SIXTY_FOURTH = 0.0625f  // Semifusa
    
    /**
     * Calculate duration with augmentation dots
     * Formula: base + base/2 + base/4 + ... (for each dot)
     * 1 dot = base * 1.5
     * 2 dots = base * 1.75
     */
    fun getDottedDuration(base: Float, dots: Int = 1): Float {
        var total = base
        var add = base / 2f
        repeat(dots) {
            total += add
            add /= 2f
        }
        return total
    }
    
    fun getNoteheadGlyph(beats: Float): Char = when {
        beats >= 4f -> SMuFLGlyphs.Noteheads.WHOLE
        beats >= 2f -> SMuFLGlyphs.Noteheads.HALF
        else -> SMuFLGlyphs.Noteheads.BLACK
    }
    
    fun needsStem(beats: Float): Boolean = beats < 4f
    
    fun needsFlag(beats: Float): Boolean = beats < 1f
    
    /**
     * Get number of flags/beams for a duration
     * Colcheia = 1, Semicolcheia = 2, Fusa = 3, Semifusa = 4
     */
    fun getNumberOfFlags(beats: Float): Int = when {
        beats >= 1f -> 0      // Quarter and longer have no flags
        beats >= 0.5f -> 1    // Eighth = 1 flag
        beats >= 0.25f -> 2   // Sixteenth = 2 flags
        beats >= 0.125f -> 3  // 32nd = 3 flags
        beats >= 0.0625f -> 4 // 64th = 4 flags
        else -> 5
    }
    
    /**
     * Check if a duration should be beamed (grouped with adjacent notes)
     */
    fun shouldBeam(beats: Float): Boolean = beats < 1f
}
