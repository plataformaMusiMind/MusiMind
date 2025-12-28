package com.musimind.music.notation.beaming

/**
 * Beam-related enums and types for music notation.
 * 
 * Based on SMuFL (Standard Music Font Layout) specifications:
 * - Stems are positioned using stemUpSE and stemDownNW anchors
 * - All measurements are in staff spaces (not pixels)
 * - y=0 represents the center of a notehead on a staff line
 */

/**
 * Direction of the stem attached to a note
 */
enum class StemDirection {
    /** Stem points upward (attached to right side of notehead) */
    UP,
    
    /** Stem points downward (attached to left side of notehead) */
    DOWN,
    
    /** No stem (for whole notes/breves) */
    NONE,
    
    /** Automatic determination based on staff position */
    AUTO
}

/**
 * Type of beam slope (inclination)
 */
enum class BeamSlope {
    /** Horizontal beam (same height at both ends) */
    HORIZONTAL,
    
    /** Ascending beam (rising from left to right) */
    ASCENDING,
    
    /** Descending beam (falling from left to right) */
    DESCENDING
}

/**
 * Direction of a fractional beam (stub/broken beam)
 * 
 * Per Elaine Gould's "Behind Bars":
 * - Right-pointing stub is used before a longer note (dotted rhythms)
 * - Left-pointing stub is used after a longer note
 */
enum class FractionalBeamSide {
    /** Beam stub points to the left */
    LEFT,
    
    /** Beam stub points to the right (default for dotted rhythms) */
    RIGHT
}

/**
 * Represents the duration type with mathematical and beat-based values.
 * 
 * Two value systems:
 * - mathematicalValue: Semibreve = 1.0 (SMuFL/Flutter convention)
 * - beatValue: Quarter note = 1.0 (common playback convention)
 * 
 * SMuFL glyph names follow the specification.
 */
enum class DurationType(
    val mathematicalValue: Float,  // Semibreve = 1.0
    val beatValue: Float,          // Quarter = 1.0
    val beamCount: Int,            // Number of beams/flags
    val noteheadGlyph: String,     // SMuFL glyph name
    val restGlyph: String,         // SMuFL rest glyph name
    val flagUpGlyph: String?,      // SMuFL flag glyph (stem up)
    val flagDownGlyph: String?     // SMuFL flag glyph (stem down)
) {
    DOUBLE_WHOLE(
        mathematicalValue = 2.0f,
        beatValue = 8f,
        beamCount = 0,
        noteheadGlyph = "noteheadDoubleWhole",
        restGlyph = "restDoubleWhole",
        flagUpGlyph = null,
        flagDownGlyph = null
    ),
    
    WHOLE(
        mathematicalValue = 1.0f,
        beatValue = 4f,
        beamCount = 0,
        noteheadGlyph = "noteheadWhole",
        restGlyph = "restWhole",
        flagUpGlyph = null,
        flagDownGlyph = null
    ),
    
    HALF(
        mathematicalValue = 0.5f,
        beatValue = 2f,
        beamCount = 0,
        noteheadGlyph = "noteheadHalf",
        restGlyph = "restHalf",
        flagUpGlyph = null,
        flagDownGlyph = null
    ),
    
    QUARTER(
        mathematicalValue = 0.25f,
        beatValue = 1f,
        beamCount = 0,
        noteheadGlyph = "noteheadBlack",
        restGlyph = "restQuarter",
        flagUpGlyph = null,
        flagDownGlyph = null
    ),
    
    EIGHTH(
        mathematicalValue = 0.125f,
        beatValue = 0.5f,
        beamCount = 1,
        noteheadGlyph = "noteheadBlack",
        restGlyph = "rest8th",
        flagUpGlyph = "flag8thUp",
        flagDownGlyph = "flag8thDown"
    ),
    
    SIXTEENTH(
        mathematicalValue = 0.0625f,
        beatValue = 0.25f,
        beamCount = 2,
        noteheadGlyph = "noteheadBlack",
        restGlyph = "rest16th",
        flagUpGlyph = "flag16thUp",
        flagDownGlyph = "flag16thDown"
    ),
    
    THIRTY_SECOND(
        mathematicalValue = 0.03125f,
        beatValue = 0.125f,
        beamCount = 3,
        noteheadGlyph = "noteheadBlack",
        restGlyph = "rest32nd",
        flagUpGlyph = "flag32ndUp",
        flagDownGlyph = "flag32ndDown"
    ),
    
    SIXTY_FOURTH(
        mathematicalValue = 0.015625f,
        beatValue = 0.0625f,
        beamCount = 4,
        noteheadGlyph = "noteheadBlack",
        restGlyph = "rest64th",
        flagUpGlyph = "flag64thUp",
        flagDownGlyph = "flag64thDown"
    ),
    
    ONE_TWENTY_EIGHTH(
        mathematicalValue = 0.0078125f,
        beatValue = 0.03125f,
        beamCount = 5,
        noteheadGlyph = "noteheadBlack",
        restGlyph = "rest128th",
        flagUpGlyph = "flag128thUp",
        flagDownGlyph = "flag128thDown"
    );
    
    /** Whether this duration requires a stem */
    val hasStem: Boolean get() = this != WHOLE && this != DOUBLE_WHOLE
    
    /** Whether this duration can be beamed (eighth note or shorter) */
    val canBeam: Boolean get() = beamCount > 0
    
    companion object {
        /**
         * Get DurationType from beat value (quarter = 1.0 system)
         */
        fun fromBeatValue(beats: Float): DurationType = when {
            beats >= 8f -> DOUBLE_WHOLE
            beats >= 4f -> WHOLE
            beats >= 2f -> HALF
            beats >= 1f -> QUARTER
            beats >= 0.5f -> EIGHTH
            beats >= 0.25f -> SIXTEENTH
            beats >= 0.125f -> THIRTY_SECOND
            beats >= 0.0625f -> SIXTY_FOURTH
            else -> ONE_TWENTY_EIGHTH
        }
        
        /**
         * Get DurationType from mathematical value (semibreve = 1.0 system)
         */
        fun fromMathematicalValue(value: Float): DurationType = when {
            value >= 2.0f -> DOUBLE_WHOLE
            value >= 1.0f -> WHOLE
            value >= 0.5f -> HALF
            value >= 0.25f -> QUARTER
            value >= 0.125f -> EIGHTH
            value >= 0.0625f -> SIXTEENTH
            value >= 0.03125f -> THIRTY_SECOND
            value >= 0.015625f -> SIXTY_FOURTH
            else -> ONE_TWENTY_EIGHTH
        }
    }
}
