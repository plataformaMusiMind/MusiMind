package com.musimind.music.notation.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.musimind.music.notation.model.Note

/**
 * Professional Tie and Slur Renderer using Bézier curves
 * 
 * Supports:
 * - Ties (connecting same-pitch notes)
 * - Slurs (connecting different notes for legato)
 * - Phrase marks (longer slurs over musical phrases)
 * 
 * Based on professional engraving standards:
 * - Elaine Gould's "Behind Bars"
 * - SMuFL curve specifications
 */
class TieSlurRenderer(
    private val staffSpace: Float = 8f,
    private val lineWidth: Float = 2f,
    curveColor: Color = Color.Black
) {
    
    private val curvePaint = Paint().apply {
        color = curveColor.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = lineWidth
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val fillPaint = Paint().apply {
        color = curveColor.toArgb()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    companion object {
        // Standard curve dimensions relative to staff space
        const val TIE_HEIGHT_FACTOR = 0.4f       // Height = staffSpace * factor
        const val SLUR_HEIGHT_FACTOR = 0.6f      // Slurs are slightly higher
        const val PHRASE_HEIGHT_FACTOR = 0.8f    // Phrase marks are even higher
        
        const val CURVE_THICKNESS = 0.15f        // Thickness of the curve
        const val MIN_CURVE_LENGTH = 2f          // Minimum curve length in staff spaces
        
        // Bézier control point positions (as fraction of length)
        const val CONTROL_POINT_X_OFFSET = 0.33f // Control points at 1/3 and 2/3
        const val CONTROL_POINT_Y_FACTOR = 1.5f  // Control point height multiplier
    }
    
    /**
     * Render a tie between two notes of the same pitch
     * Ties are always curved toward the stem (away from the notehead)
     */
    fun renderTie(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        curveUp: Boolean = true // Default curve direction
    ) {
        val length = endX - startX
        if (length < staffSpace * MIN_CURVE_LENGTH) return
        
        val height = staffSpace * TIE_HEIGHT_FACTOR * if (curveUp) -1 else 1
        
        drawCurve(canvas, startX, startY, endX, endY, height)
    }
    
    /**
     * Render a slur between two different notes
     * Slurs indicate legato playing
     */
    fun renderSlur(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        curveUp: Boolean = true
    ) {
        val length = endX - startX
        if (length < staffSpace * MIN_CURVE_LENGTH) return
        
        val height = staffSpace * SLUR_HEIGHT_FACTOR * if (curveUp) -1 else 1
        
        drawCurve(canvas, startX, startY, endX, endY, height)
    }
    
    /**
     * Render a phrase mark (longer slur over a musical phrase)
     */
    fun renderPhraseMark(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        curveUp: Boolean = true
    ) {
        val length = endX - startX
        if (length < staffSpace * MIN_CURVE_LENGTH) return
        
        val height = staffSpace * PHRASE_HEIGHT_FACTOR * if (curveUp) -1 else 1
        
        drawCurve(canvas, startX, startY, endX, endY, height)
    }
    
    /**
     * Draw a curved line using cubic Bézier curves
     * This creates a professional-looking curved shape with varying thickness
     */
    private fun drawCurve(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        height: Float
    ) {
        val length = endX - startX
        val midX = startX + length / 2f
        val midY = (startY + endY) / 2f
        
        // Calculate control points for smooth Bézier curve
        val cp1X = startX + length * CONTROL_POINT_X_OFFSET
        val cp2X = endX - length * CONTROL_POINT_X_OFFSET
        
        val cp1Y = startY + height * CONTROL_POINT_Y_FACTOR
        val cp2Y = endY + height * CONTROL_POINT_Y_FACTOR
        
        // Calculate apex of the curve
        val apexY = midY + height
        
        // Create the curve path
        val path = Path()
        
        // Outer curve (top edge)
        path.moveTo(startX, startY)
        path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, endX, endY)
        
        // Draw outer curve
        canvas.drawPath(path, curvePaint)
        
        // For a more professional look, draw a filled curved shape
        // with tapered ends (thicker in the middle)
        drawTaperedCurve(canvas, startX, startY, endX, endY, height)
    }
    
    /**
     * Draw a tapered curve that's thicker in the middle
     * This matches professional engraving standards
     */
    private fun drawTaperedCurve(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        height: Float
    ) {
        val length = endX - startX
        val thickness = staffSpace * CURVE_THICKNESS
        
        // Calculate control points
        val cp1X = startX + length * CONTROL_POINT_X_OFFSET
        val cp2X = endX - length * CONTROL_POINT_X_OFFSET
        val cp1Y = startY + height * CONTROL_POINT_Y_FACTOR
        val cp2Y = endY + height * CONTROL_POINT_Y_FACTOR
        
        // Create filled area between two curves
        val path = Path()
        
        // Outer curve (away from notes)
        path.moveTo(startX, startY)
        path.cubicTo(
            cp1X, cp1Y - thickness / 2,
            cp2X, cp2Y - thickness / 2,
            endX, endY
        )
        
        // Inner curve (closer to notes) - reversed
        path.cubicTo(
            cp2X, cp2Y + thickness / 2,
            cp1X, cp1Y + thickness / 2,
            startX, startY
        )
        
        path.close()
        
        canvas.drawPath(path, fillPaint)
    }
    
    /**
     * Render tie for a specific note to its tied destination
     * Automatically determines curve direction based on stem direction
     */
    fun renderTieForNote(
        canvas: Canvas,
        note: Note,
        nextNote: Note,
        noteX: Float,
        nextNoteX: Float,
        noteY: Float,
        stemUp: Boolean
    ) {
        // Ties curve away from stems (opposite direction)
        val curveUp = !stemUp
        
        // Tie starts at right edge of notehead
        val startX = noteX + staffSpace * 0.5f
        
        // Tie ends at left edge of next notehead
        val endX = nextNoteX - staffSpace * 0.5f
        
        // Y position is at the notehead center, offset slightly
        val yOffset = if (curveUp) -staffSpace * 0.25f else staffSpace * 0.25f
        val startY = noteY + yOffset
        val endY = noteY + yOffset // Same pitch, same Y
        
        renderTie(canvas, startX, startY, endX, endY, curveUp)
    }
    
    /**
     * Render slur connecting multiple notes
     */
    fun renderSlurForNotes(
        canvas: Canvas,
        notes: List<Note>,
        notePositions: Map<Note, PointF>,
        stemUp: Boolean
    ) {
        if (notes.size < 2) return
        
        val firstNote = notes.first()
        val lastNote = notes.last()
        
        val firstPos = notePositions[firstNote] ?: return
        val lastPos = notePositions[lastNote] ?: return
        
        // Slurs curve away from stems
        val curveUp = !stemUp
        
        // Start slightly offset from notehead
        val startX = firstPos.x + staffSpace * 0.3f
        val endX = lastPos.x + staffSpace * 0.3f
        
        // Find highest/lowest point in the slur to adjust curve
        val extremeY = if (curveUp) {
            notePositions.values.minOfOrNull { it.y } ?: firstPos.y
        } else {
            notePositions.values.maxOfOrNull { it.y } ?: firstPos.y
        }
        
        val yOffset = staffSpace * if (curveUp) -0.5f else 0.5f
        val startY = extremeY + yOffset
        val endY = extremeY + yOffset
        
        renderSlur(canvas, startX, startY, endX, endY, curveUp)
    }
    
    /**
     * Set curve color
     */
    fun setColor(color: Color) {
        curvePaint.color = color.toArgb()
        fillPaint.color = color.toArgb()
    }
    
    /**
     * Set curve thickness
     */
    fun setThickness(thickness: Float) {
        curvePaint.strokeWidth = thickness
    }
}

/**
 * Extension function to calculate optimal curve direction for a group of notes
 */
fun List<Note>.optimalCurveDirection(): Boolean {
    if (isEmpty()) return true
    
    // Count stems up vs down (approximation based on pitch)
    // Higher pitches (MIDI > 71) typically have stems down
    val stemsDown = count { note -> note.pitch.midiPitch > 71 }
    val stemsUp = size - stemsDown
    
    // Curve goes opposite of majority stem direction
    return stemsDown >= stemsUp
}
