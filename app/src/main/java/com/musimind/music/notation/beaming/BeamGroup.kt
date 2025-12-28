package com.musimind.music.notation.beaming

import com.musimind.music.notation.model.MusicElement
import com.musimind.music.notation.model.Note

/**
 * Represents a group of notes connected by beams with calculated geometry.
 * 
 * This is the advanced version with slope interpolation, used for proper
 * beam rendering according to music engraving standards.
 * 
 * Key positioning concepts (per SMuFL/Behind Bars):
 * - Beam Y positions are calculated at stem endpoints
 * - Beam slope follows the melodic contour (with limits)
 * - Multiple beam levels (for 16th, 32nd, etc.) stack with gaps
 * - Fractional beams (stubs) indicate isolated short notes
 */
data class AdvancedBeamGroup(
    /** Notes in the group (must be consecutive beamable notes) */
    val notes: List<Note>,
    
    /** Stem direction for all notes in the group */
    var stemDirection: StemDirection = StemDirection.UP,
    
    /** All beam segments (primary, secondary, fractional, etc.) */
    val beamSegments: MutableList<BeamSegment> = mutableListOf(),
    
    /** X position of the first note's stem (in pixels) */
    var leftX: Float = 0f,
    
    /** X position of the last note's stem (in pixels) */
    var rightX: Float = 0f,
    
    /** Y position of the beam at the first stem (in pixels) */
    var leftY: Float = 0f,
    
    /** Y position of the beam at the last stem (in pixels) */
    var rightY: Float = 0f
) {
    /**
     * Slope of the beam line.
     * 
     * Per "Behind Bars":
     * - Maximum slope is typically 1 staff space per octave
     * - Steep slopes should be moderated for readability
     */
    val slope: Float get() = if (rightX != leftX) {
        (rightY - leftY) / (rightX - leftX)
    } else 0f
    
    /** Whether the beam is perfectly horizontal */
    val isHorizontal: Boolean get() = leftY == rightY
    
    /** Maximum number of beam lines needed (based on shortest note) */
    val maxBeamLevel: Int get() = notes.maxOfOrNull { 
        DurationType.fromBeatValue(it.durationBeats).beamCount 
    } ?: 0
    
    /**
     * Interpolate the Y position of the beam at a given X position.
     * 
     * This allows calculating where each note's stem should end
     * to meet the angled beam line.
     */
    fun interpolateBeamY(x: Float): Float {
        if (isHorizontal || rightX == leftX) {
            return leftY
        }
        return leftY + (slope * (x - leftX))
    }
    
    /**
     * Get beam Y offset for a specific level.
     * 
     * SMuFL specifications:
     * - Beam thickness: 0.5 staff spaces
     * - Gap between beams: 0.25 staff spaces
     */
    fun getBeamLevelOffset(level: Int, staffSpace: Float): Float {
        if (level <= 1) return 0f
        
        val beamThickness = 0.5f * staffSpace
        val beamGap = 0.25f * staffSpace
        val offset = (level - 1) * (beamThickness + beamGap)
        
        // Invert for stems down (beams stack upward from baseline)
        return if (stemDirection == StemDirection.DOWN) -offset else offset
    }
    
    override fun toString(): String = 
        "AdvancedBeamGroup(notes=${notes.size}, segments=${beamSegments.size}, slope=${String.format("%.3f", slope)})"
}

/**
 * Simple beam group for basic beaming (without advanced geometry).
 * Used for quick grouping before detailed layout.
 */
data class SimpleBeamGroup(
    /** Indices of notes in the original list that belong to this group */
    val noteIndices: List<Int>,
    
    /** The group ID (for linking notes to their beam group) */
    val groupId: Int,
    
    /** Maximum beam level needed (1=eighth, 2=sixteenth, etc.) */
    val maxBeamLevel: Int
) {
    val size: Int get() = noteIndices.size
    val isValid: Boolean get() = noteIndices.size >= 2
}
