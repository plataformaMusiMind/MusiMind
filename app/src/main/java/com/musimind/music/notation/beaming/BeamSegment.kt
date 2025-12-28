package com.musimind.music.notation.beaming

/**
 * Represents a segment of a beam (complete or fractional/broken).
 * 
 * A beam group may have multiple segments at different levels:
 * - Level 1: Primary beam (connects all notes in group)
 * - Level 2: Secondary beam (for sixteenth notes)
 * - Level 3: Tertiary beam (for thirty-second notes)
 * - etc.
 * 
 * Fractional (broken) beams are short stubs used when a note at a given
 * subdivision level is isolated within the group.
 */
data class BeamSegment(
    /** 
     * Level of the beam (1 = primary, 2 = secondary for 16th, 3 = tertiary for 32nd, etc.)
     */
    val level: Int,
    
    /** 
     * Index of the first note in this segment (within the parent beam group)
     */
    val startNoteIndex: Int,
    
    /** 
     * Index of the last note in this segment (same as start for fractional beams)
     */
    val endNoteIndex: Int,
    
    /**
     * Whether this is a fractional (broken/stub) beam.
     * 
     * Per "Behind Bars" by Elaine Gould:
     * - Fractional beams indicate a single note at a subdivision level
     * - They point toward the shorter note value
     */
    val isFractional: Boolean = false,
    
    /**
     * Direction of the fractional beam stub (only for isFractional = true)
     */
    val fractionalSide: FractionalBeamSide? = null,
    
    /**
     * Length of the fractional beam in staff spaces.
     * Default is approximately one notehead width.
     */
    val fractionalLength: Float? = null
) {
    override fun toString(): String = if (isFractional) {
        "BeamSegment(level=$level, note=$startNoteIndex, fractional=$fractionalSide)"
    } else {
        "BeamSegment(level=$level, notes=$startNoteIndex-$endNoteIndex)"
    }
}
