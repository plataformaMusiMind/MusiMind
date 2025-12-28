package com.musimind.music.notation.beaming

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.musimind.music.notation.model.Note

/**
 * Renders beams (beam lines) on an Android Canvas.
 * 
 * This renderer draws:
 * - Stems for all notes in the beam group
 * - Primary beams (connecting all notes)
 * - Secondary beams (for 16th notes)
 * - Tertiary beams (for 32nd notes)
 * - Fractional (broken) beams for isolated short notes
 * 
 * Based on SMuFL specifications:
 * - Beam thickness: 0.5 staff spaces
 * - Gap between beams: 0.25 staff spaces
 * - Stem thickness: 0.12 staff spaces
 */
class BeamRenderer(
    /** Staff space in pixels */
    private val staffSpace: Float,
    
    /** Width of a notehead in pixels */
    private val noteheadWidth: Float,
    
    /** Color for beams and stems */
    private val beamColor: Int = android.graphics.Color.BLACK,
    
    /** Color for stems (usually same as beam) */
    private val stemColor: Int = android.graphics.Color.BLACK
) {
    // SMuFL-standard measurements (converted to pixels)
    private val beamThickness = 0.5f * staffSpace
    private val beamGap = 0.25f * staffSpace
    private val stemThickness = 0.12f * staffSpace
    
    // Pre-configured paint objects for performance
    private val beamPaint = Paint().apply {
        color = beamColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val stemPaint = Paint().apply {
        color = stemColor
        strokeWidth = stemThickness
        strokeCap = Paint.Cap.BUTT
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    /**
     * Render a complete beam group (stems + all beam levels).
     * 
     * @param canvas The Android Canvas to draw on
     * @param group The AdvancedBeamGroup with calculated geometry
     * @param noteXPositions Map of note to its X position
     * @param noteYPositions Map of note to its Y position (notehead center)
     */
    fun renderBeamGroup(
        canvas: Canvas,
        group: AdvancedBeamGroup,
        noteXPositions: Map<Note, Float>,
        noteYPositions: Map<Note, Float>
    ) {
        // Step 1: Render stems for all notes
        renderStems(canvas, group, noteXPositions, noteYPositions)
        
        // Step 2: Render all beam segments (sorted by level for proper layering)
        val sortedSegments = group.beamSegments.sortedBy { it.level }
        for (segment in sortedSegments) {
            renderBeamSegment(canvas, group, segment, noteXPositions)
        }
    }
    
    /**
     * Render stems for all notes in the beam group.
     */
    private fun renderStems(
        canvas: Canvas,
        group: AdvancedBeamGroup,
        noteXPositions: Map<Note, Float>,
        noteYPositions: Map<Note, Float>
    ) {
        for (note in group.notes) {
            val noteX = noteXPositions[note] ?: continue
            val noteY = noteYPositions[note] ?: continue
            
            // Calculate stem X position based on stem direction
            val stemX = if (group.stemDirection == StemDirection.UP) {
                noteX + noteheadWidth * 0.92f  // Right edge for stems up
            } else {
                noteX + noteheadWidth * 0.08f  // Left edge for stems down
            }
            
            // Calculate where stem meets the beam (interpolate along beam line)
            val beamY = group.interpolateBeamY(stemX)
            
            // Adjust stem attachment to notehead
            val stemStartY = if (group.stemDirection == StemDirection.UP) {
                noteY - staffSpace * 0.15f  // Slight overlap with notehead
            } else {
                noteY + staffSpace * 0.15f
            }
            
            // Draw the stem
            canvas.drawLine(stemX, stemStartY, stemX, beamY, stemPaint)
        }
    }
    
    /**
     * Render a single beam segment.
     */
    private fun renderBeamSegment(
        canvas: Canvas,
        group: AdvancedBeamGroup,
        segment: BeamSegment,
        noteXPositions: Map<Note, Float>
    ) {
        // Calculate vertical offset for this beam level
        val levelOffset = calculateLevelOffset(segment.level, group.stemDirection)
        
        if (segment.isFractional) {
            renderFractionalBeam(canvas, group, segment, levelOffset, noteXPositions)
        } else {
            renderFullBeam(canvas, group, segment, levelOffset, noteXPositions)
        }
    }
    
    /**
     * Calculate Y offset for a beam level.
     * 
     * Level 1 has no offset.
     * Level 2+ are offset by (beamThickness + beamGap) per level.
     */
    private fun calculateLevelOffset(level: Int, stemDirection: StemDirection): Float {
        val offset = (level - 1) * (beamThickness + beamGap)
        
        // Invert for stems down (secondary beams stack upward)
        return if (stemDirection == StemDirection.DOWN) -offset else offset
    }
    
    /**
     * Render a full beam (connecting multiple notes).
     */
    private fun renderFullBeam(
        canvas: Canvas,
        group: AdvancedBeamGroup,
        segment: BeamSegment,
        levelOffset: Float,
        noteXPositions: Map<Note, Float>
    ) {
        val startNote = group.notes[segment.startNoteIndex]
        val endNote = group.notes[segment.endNoteIndex]
        
        // Get X positions of the notes
        val startNoteX = noteXPositions[startNote] ?: return
        val endNoteX = noteXPositions[endNote] ?: return
        
        // Apply stem direction offset to get stem X
        val stemOffset = if (group.stemDirection == StemDirection.UP) {
            noteheadWidth * 0.92f
        } else {
            noteheadWidth * 0.08f
        }
        
        val leftX = startNoteX + stemOffset
        val rightX = endNoteX + stemOffset
        
        // Interpolate Y positions along the beam line
        val leftY = group.interpolateBeamY(leftX) + levelOffset
        val rightY = group.interpolateBeamY(rightX) + levelOffset
        
        // Draw the beam as a filled parallelogram (to handle slope)
        val beamPath = Path()
        
        if (group.stemDirection == StemDirection.UP) {
            // Stems up: beam hangs below the Y line
            beamPath.moveTo(leftX, leftY)
            beamPath.lineTo(rightX, rightY)
            beamPath.lineTo(rightX, rightY + beamThickness)
            beamPath.lineTo(leftX, leftY + beamThickness)
        } else {
            // Stems down: beam sits above the Y line
            beamPath.moveTo(leftX, leftY - beamThickness)
            beamPath.lineTo(rightX, rightY - beamThickness)
            beamPath.lineTo(rightX, rightY)
            beamPath.lineTo(leftX, leftY)
        }
        
        beamPath.close()
        canvas.drawPath(beamPath, beamPaint)
    }
    
    /**
     * Render a fractional (broken/stub) beam.
     */
    private fun renderFractionalBeam(
        canvas: Canvas,
        group: AdvancedBeamGroup,
        segment: BeamSegment,
        levelOffset: Float,
        noteXPositions: Map<Note, Float>
    ) {
        val note = group.notes[segment.startNoteIndex]
        val noteX = noteXPositions[note] ?: return
        
        // Calculate center position (stem X)
        val stemOffset = if (group.stemDirection == StemDirection.UP) {
            noteheadWidth * 0.92f
        } else {
            noteheadWidth * 0.08f
        }
        
        val centerX = noteX + stemOffset
        
        // Fractional beam length (default: one notehead width)
        val length = segment.fractionalLength ?: noteheadWidth
        
        // Calculate left and right X based on direction
        val leftX: Float
        val rightX: Float
        
        when (segment.fractionalSide) {
            FractionalBeamSide.RIGHT -> {
                leftX = centerX
                rightX = centerX + length
            }
            FractionalBeamSide.LEFT, null -> {
                leftX = centerX - length
                rightX = centerX
            }
        }
        
        // Interpolate Y along beam slope
        val leftY = group.interpolateBeamY(leftX) + levelOffset
        val rightY = group.interpolateBeamY(rightX) + levelOffset
        
        // Draw as parallelogram
        val beamPath = Path()
        
        if (group.stemDirection == StemDirection.UP) {
            beamPath.moveTo(leftX, leftY)
            beamPath.lineTo(rightX, rightY)
            beamPath.lineTo(rightX, rightY + beamThickness)
            beamPath.lineTo(leftX, leftY + beamThickness)
        } else {
            beamPath.moveTo(leftX, leftY - beamThickness)
            beamPath.lineTo(rightX, rightY - beamThickness)
            beamPath.lineTo(rightX, rightY)
            beamPath.lineTo(leftX, leftY)
        }
        
        beamPath.close()
        canvas.drawPath(beamPath, beamPaint)
    }
    
    /**
     * Calculate total height needed for multiple beams.
     * Useful for stem length calculations.
     */
    fun calculateTotalBeamHeight(beamCount: Int): Float {
        if (beamCount <= 0) return 0f
        return beamThickness + ((beamCount - 1) * (beamThickness + beamGap))
    }
    
    /**
     * Update beam and stem colors.
     */
    fun setColors(beamColor: Int, stemColor: Int = beamColor) {
        beamPaint.color = beamColor
        stemPaint.color = stemColor
    }
}
