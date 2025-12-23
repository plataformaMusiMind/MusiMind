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
    val state: NoteState = NoteState.NORMAL // For visual feedback
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
     * Position on staff relative to Middle C (0 = C4)
     */
    fun staffPosition(clef: ClefType): Int {
        val basePosition = (octave - 4) * 7 + note.ordinal
        return when (clef) {
            ClefType.TREBLE -> basePosition - 1 // B4 is on middle line
            ClefType.BASS -> basePosition + 11 // D3 is on middle line
            ClefType.ALTO -> basePosition + 5 // C4 is on middle line
            ClefType.TENOR -> basePosition + 7
            ClefType.PERCUSSION -> 0
        }
    }
}

enum class NoteName(val semitone: Int) {
    C(0), D(2), E(4), F(5), G(7), A(9), B(11)
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
 * Duration utilities
 */
object Duration {
    const val DOUBLE_WHOLE = 8f
    const val WHOLE = 4f
    const val HALF = 2f
    const val QUARTER = 1f
    const val EIGHTH = 0.5f
    const val SIXTEENTH = 0.25f
    const val THIRTY_SECOND = 0.125f
    const val SIXTY_FOURTH = 0.0625f
    
    fun getNoteheadGlyph(beats: Float): Char = when {
        beats >= 4f -> SMuFLGlyphs.Noteheads.WHOLE
        beats >= 2f -> SMuFLGlyphs.Noteheads.HALF
        else -> SMuFLGlyphs.Noteheads.BLACK
    }
    
    fun needsStem(beats: Float): Boolean = beats < 4f
    
    fun needsFlag(beats: Float): Boolean = beats < 1f
    
    fun getNumberOfFlags(beats: Float): Int = when {
        beats >= 0.5f -> 1
        beats >= 0.25f -> 2
        beats >= 0.125f -> 3
        beats >= 0.0625f -> 4
        else -> 5
    }
}
