package com.musimind.music.notation.engine

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musimind.music.notation.model.*
import com.musimind.music.notation.smufl.SMuFLGlyphs

/**
 * Professional music notation rendering engine using SMuFL fonts
 * 
 * This engine renders musical notation using the Bravura font (SMuFL standard)
 * and provides high-quality, vector-based rendering suitable for any resolution.
 */
class NotationEngine(context: Context) {
    
    companion object {
        // Staff dimensions (relative to staff space)
        const val STAFF_LINES = 5
        const val LEDGER_LINE_EXTENSION = 0.5f // Extension beyond notehead
        
        // Default colors
        val COLOR_STAFF = Color(0xFF1A1A1A)
        val COLOR_NOTE = Color(0xFF000000)
        val COLOR_CORRECT = Color(0xFF22C55E)
        val COLOR_INCORRECT = Color(0xFFEF4444)
        val COLOR_HIGHLIGHTED = Color(0xFF7C3AED)
        val COLOR_UPCOMING = Color(0xFF3B82F6)
        val COLOR_PASSED = Color(0xFFA0A0A0)
    }
    
    // SMuFL-compliant music font (Bravura)
    private val bravuraTypeface: Typeface = try {
        Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
    } catch (e: Exception) {
        // Fallback - will use Android's default font
        Typeface.DEFAULT
    }
    
    // Text typeface for lyrics, chord symbols, etc.
    private val textTypeface: Typeface = try {
        Typeface.createFromAsset(context.assets, "fonts/BravuraText.otf")
    } catch (e: Exception) {
        Typeface.DEFAULT
    }
    
    /**
     * Gets the color for a note based on its state
     */
    fun getColorForState(state: NoteState): Color = when (state) {
        NoteState.NORMAL -> COLOR_NOTE
        NoteState.HIGHLIGHTED -> COLOR_HIGHLIGHTED
        NoteState.CORRECT -> COLOR_CORRECT
        NoteState.INCORRECT -> COLOR_INCORRECT
        NoteState.UPCOMING -> COLOR_UPCOMING
        NoteState.PASSED -> COLOR_PASSED
    }
    
    /**
     * Creates the native Paint object for music notation rendering
     */
    fun createMusicPaint(fontSize: Float, color: Color = COLOR_NOTE): android.graphics.Paint {
        return android.graphics.Paint().apply {
            typeface = bravuraTypeface
            textSize = fontSize
            isAntiAlias = true
            this.color = color.toArgb()
        }
    }
    
    /**
     * Creates the native Paint object for text rendering
     */
    fun createTextPaint(fontSize: Float, color: Color = COLOR_NOTE): android.graphics.Paint {
        return android.graphics.Paint().apply {
            typeface = textTypeface
            textSize = fontSize
            isAntiAlias = true
            this.color = color.toArgb()
        }
    }
    
    /**
     * Creates paint for staff lines
     */
    fun createStaffPaint(lineWidth: Float, color: Color = COLOR_STAFF): android.graphics.Paint {
        return android.graphics.Paint().apply {
            this.color = color.toArgb()
            strokeWidth = lineWidth
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
    }
    
    /**
     * Extension function to convert Color to ARGB int
     */
    private fun Color.toArgb(): Int {
        return android.graphics.Color.argb(
            (alpha * 255).toInt(),
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt()
        )
    }
    
    /**
     * Get glyph character for a specific clef
     */
    fun getClefGlyph(clef: ClefType): Char = clef.glyph
    
    /**
     * Get glyph for time signature numerator digit
     */
    fun getTimeSignatureGlyph(digit: Int): Char = SMuFLGlyphs.TimeSignatures.getNumeral(digit)
    
    /**
     * Get notehead glyph based on duration
     */
    fun getNoteheadGlyph(durationBeats: Float): Char = when {
        durationBeats >= 4f -> SMuFLGlyphs.Noteheads.WHOLE
        durationBeats >= 2f -> SMuFLGlyphs.Noteheads.HALF
        else -> SMuFLGlyphs.Noteheads.BLACK
    }
    
    /**
     * Get rest glyph based on duration
     */
    fun getRestGlyph(durationBeats: Float): Char = SMuFLGlyphs.getRestForDuration(durationBeats)
    
    /**
     * Get flag glyph for a note
     */
    fun getFlagGlyph(durationBeats: Float, stemUp: Boolean): Char? = 
        SMuFLGlyphs.getFlagForDuration(durationBeats, stemUp)
    
    /**
     * Calculate if note needs ledger lines
     */
    fun needsLedgerLines(staffPosition: Int): Boolean {
        // Staff positions: 0-8 are on staff (bottom line = 0, top line = 8)
        return staffPosition < 0 || staffPosition > 8
    }
    
    /**
     * Calculate number of ledger lines needed
     */
    fun getLedgerLineCount(staffPosition: Int): Int {
        return when {
            staffPosition < 0 -> (-staffPosition + 1) / 2
            staffPosition > 8 -> (staffPosition - 8 + 1) / 2
            else -> 0
        }
    }
    
    /**
     * Calculate stem direction for a note
     * Returns true for stem up, false for stem down
     */
    fun calculateStemDirection(staffPosition: Int): Boolean {
        // Notes on or below middle line have stems up
        return staffPosition <= 4
    }
    
    /**
     * Get all accidentals needed for a key signature
     */
    fun getKeySignatureAccidentals(keySignature: KeySignatureType): List<Pair<Int, Char>> {
        val positions = if (keySignature.isSharp) {
            // Sharp order: F C G D A E B
            listOf(8, 5, 9, 6, 3, 7, 4) // Treble clef positions
        } else {
            // Flat order: B E A D G C F
            listOf(4, 7, 3, 6, 2, 5, 1) // Treble clef positions
        }
        
        val accidental = if (keySignature.isSharp) 
            SMuFLGlyphs.Accidentals.SHARP 
        else 
            SMuFLGlyphs.Accidentals.FLAT
            
        return positions.take(keySignature.accidentals).map { it to accidental }
    }
}
