package com.musimind.music.notation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.Typeface
import com.musimind.music.notation.model.ClefType
import com.musimind.music.notation.smufl.SMuFLGlyphs

/**
 * SMuFL Font Positioning Constants
 * 
 * In SMuFL, all measurements are relative to the staff space (the distance between two staff lines).
 * The font size should be set to 4 * staffSpace to ensure correct proportions.
 * 
 * Reference: https://w3c.github.io/smufl/latest/
 */
object SMuFLPositioning {
    // Treble clef: origin is at G4 (second line from bottom)
    // The glyph's baseline should touch the second line
    const val TREBLE_CLEF_LINE = 3 // 0-indexed from bottom, so line 1 (G4) = position 3 staff spaces from top
    
    // Bass clef: origin is at F3 (fourth line from bottom)
    const val BASS_CLEF_LINE = 1 // Line 4 (F3) = position 1 staff space from top
    
    // Alto clef: origin is at middle C (third line)
    const val ALTO_CLEF_LINE = 2 // Line 3 (C4) = position 2 staff spaces from top
}

/**
 * Simple staff lines view without any notes
 * 
 * Useful for showing empty staff for exercises or as a background
 */
@Composable
fun StaffLinesView(
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    lineColor: Color = Color(0xFF1A1A1A),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    showClef: Boolean = false,
    clef: ClefType = ClefType.TREBLE
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val heightPx = with(density) { height.toPx() }
    val staffSpace = heightPx / 4
    
    val typeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        
        val linePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (lineColor.alpha * 255).toInt(),
                (lineColor.red * 255).toInt(),
                (lineColor.green * 255).toInt(),
                (lineColor.blue * 255).toInt()
            )
            strokeWidth = staffSpace * 0.1f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        
        val clefWidth = if (showClef) staffSpace * 3f else 0f
        val startX = clefWidth
        
        // Draw 5 staff lines
        for (i in 0 until 5) {
            val y = i * staffSpace
            canvas.drawLine(startX, y, size.width, y, linePaint)
        }
        
        // Draw clef if requested
        if (showClef) {
            val musicPaint = android.graphics.Paint().apply {
                this.typeface = typeface
                textSize = staffSpace * 4  // SMuFL standard: font size = 4 * staff space
                color = linePaint.color
                isAntiAlias = true
            }
            
            // SMuFL clef positions - the baseline of the glyph sits on specific lines
            val clefY = when (clef) {
                ClefType.TREBLE -> staffSpace * 3  // G4 line (second from bottom = line index 3 from top)
                ClefType.BASS -> staffSpace * 1    // F3 line (second from top = line index 1 from top)
                ClefType.ALTO -> staffSpace * 2    // C4 line (middle line)
                else -> staffSpace * 2
            }
            
            canvas.drawText(clef.glyph.toString(), staffSpace * 0.2f, clefY, musicPaint)
        }
    }
}

/**
 * Interactive staff for note input
 * 
 * Displays staff lines with touch detection for placing notes
 */
@Composable
fun InteractiveStaff(
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    clef: ClefType = ClefType.TREBLE,
    lineColor: Color = Color(0xFF1A1A1A),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onPositionSelected: ((Int) -> Unit)? = null // Staff position (0 = bottom line)
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val heightPx = with(density) { height.toPx() }
    val staffSpace = heightPx / 6 // Extra space for ledger lines
    val staffHeight = staffSpace * 4
    val topMargin = staffSpace
    
    val typeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        
        val linePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (lineColor.alpha * 255).toInt(),
                (lineColor.red * 255).toInt(),
                (lineColor.green * 255).toInt(),
                (lineColor.blue * 255).toInt()
            )
            strokeWidth = staffSpace * 0.08f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        
        val clefWidth = staffSpace * 2.5f
        val startX = clefWidth + staffSpace
        
        // Draw 5 staff lines
        for (i in 0 until 5) {
            val y = topMargin + i * staffSpace
            canvas.drawLine(startX, y, size.width - staffSpace, y, linePaint)
        }
        
        // Draw clef
        val musicPaint = android.graphics.Paint().apply {
            this.typeface = typeface
            textSize = staffSpace * 4  // SMuFL standard
            color = linePaint.color
            isAntiAlias = true
        }
        
        // Correct clef positioning using SMuFL standard
        val clefY = when (clef) {
            ClefType.TREBLE -> topMargin + staffSpace * 3  // Line 3 (G4)
            ClefType.BASS -> topMargin + staffSpace * 1    // Line 1 (F3)  
            ClefType.ALTO -> topMargin + staffSpace * 2    // Line 2 (C4)
            else -> topMargin + staffSpace * 2
        }
        
        canvas.drawText(clef.glyph.toString(), staffSpace * 0.2f, clefY, musicPaint)
    }
}

/**
 * Preview component showing a single note on staff
 * 
 * Useful for lessons and quizzes to show a note example
 * 
 * SMuFL stem conventions:
 * - Notes on or above the middle line: stem goes DOWN (to the left of notehead)
 * - Notes below the middle line: stem goes UP (to the right of notehead)
 */
@Composable
fun NoteOnStaff(
    staffPosition: Int, // 0 = bottom line (E4 in treble), 8 = top line (F5 in treble)
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    clef: ClefType = ClefType.TREBLE,
    noteColor: Color = MaterialTheme.colorScheme.primary,
    lineColor: Color = Color(0xFF1A1A1A),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    duration: Float = 1f, // Quarter note by default
    accidental: Char? = null
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val heightPx = with(density) { height.toPx() }
    val staffSpace = heightPx / 6
    val staffHeight = staffSpace * 4
    val topMargin = staffSpace
    
    val typeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        
        val linePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (lineColor.alpha * 255).toInt(),
                (lineColor.red * 255).toInt(),
                (lineColor.green * 255).toInt(),
                (lineColor.blue * 255).toInt()
            )
            strokeWidth = staffSpace * 0.08f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        
        val musicPaint = android.graphics.Paint().apply {
            this.typeface = typeface
            textSize = staffSpace * 4  // SMuFL standard
            color = linePaint.color
            isAntiAlias = true
        }
        
        val notePaint = android.graphics.Paint().apply {
            this.typeface = typeface
            textSize = staffSpace * 4  // SMuFL standard
            color = android.graphics.Color.argb(
                (noteColor.alpha * 255).toInt(),
                (noteColor.red * 255).toInt(),
                (noteColor.green * 255).toInt(),
                (noteColor.blue * 255).toInt()
            )
            isAntiAlias = true
        }
        
        val clefWidth = staffSpace * 2.5f
        val startX = clefWidth + staffSpace
        val noteX = size.width / 2
        
        // Draw 5 staff lines
        for (i in 0 until 5) {
            val y = topMargin + i * staffSpace
            canvas.drawLine(startX, y, size.width - staffSpace, y, linePaint)
        }
        
        // Draw clef with correct SMuFL positioning
        val clefY = when (clef) {
            ClefType.TREBLE -> topMargin + staffSpace * 3  // Line 3 (G4)
            ClefType.BASS -> topMargin + staffSpace * 1    // Line 1 (F3)
            ClefType.ALTO -> topMargin + staffSpace * 2    // Line 2 (C4)
            else -> topMargin + staffSpace * 2
        }
        canvas.drawText(clef.glyph.toString(), staffSpace * 0.2f, clefY, musicPaint)
        
        // Calculate note Y position
        // Position 0 = bottom line (E4), position 8 = top line (F5)
        // Staff lines are at positions 0, 2, 4, 6, 8 (even numbers)
        // Spaces are at positions 1, 3, 5, 7 (odd numbers)
        val noteY = topMargin + staffHeight - (staffPosition * staffSpace / 2f)
        
        // Draw ledger lines if needed
        if (staffPosition < 0) {
            // Below the staff
            val ledgerCount = (-staffPosition + 1) / 2
            for (i in 1..ledgerCount) {
                val ledgerY = topMargin + staffHeight + i * staffSpace
                canvas.drawLine(
                    noteX - staffSpace,
                    ledgerY,
                    noteX + staffSpace * 1.5f,
                    ledgerY,
                    linePaint
                )
            }
        } else if (staffPosition > 8) {
            // Above the staff
            val ledgerCount = (staffPosition - 8 + 1) / 2
            for (i in 1..ledgerCount) {
                val ledgerY = topMargin - i * staffSpace
                canvas.drawLine(
                    noteX - staffSpace,
                    ledgerY,
                    noteX + staffSpace * 1.5f,
                    ledgerY,
                    linePaint
                )
            }
        }
        
        // Draw accidental if present
        accidental?.let {
            canvas.drawText(it.toString(), noteX - staffSpace * 1.5f, noteY, notePaint)
        }
        
        // Draw notehead
        val noteheadGlyph = when {
            duration >= 4f -> SMuFLGlyphs.Noteheads.WHOLE
            duration >= 2f -> SMuFLGlyphs.Noteheads.HALF
            else -> SMuFLGlyphs.Noteheads.BLACK
        }
        
        // Get notehead width for stem positioning
        val noteheadWidth = staffSpace * 1.2f  // Approximate notehead width
        
        canvas.drawText(noteheadGlyph.toString(), noteX, noteY, notePaint)
        
        // Draw stem if needed (not for whole notes)
        if (duration < 4f) {
            // Stem direction: UP for notes on/below middle line (position <= 4), DOWN for above
            // Middle line = position 4 (B4 in treble clef)
            val stemUp = staffPosition < 4  // Below middle line = stem up
            
            val stemPaint = android.graphics.Paint().apply {
                color = notePaint.color
                strokeWidth = staffSpace * 0.12f  // Stem width per SMuFL
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }
            
            val stemLength = staffSpace * 3.5f
            
            if (stemUp) {
                // Stem UP: stem is on the RIGHT side of the notehead
                val stemX = noteX + noteheadWidth
                val stemStartY = noteY - staffSpace * 0.1f
                val stemEndY = stemStartY - stemLength
                canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
                
                // Draw flag if needed
                if (duration < 1f) {
                    val flagGlyph = SMuFLGlyphs.getFlagForDuration(duration, true)
                    flagGlyph?.let {
                        canvas.drawText(it.toString(), stemX - staffSpace * 0.1f, stemEndY, notePaint)
                    }
                }
            } else {
                // Stem DOWN: stem is on the LEFT side of the notehead
                val stemX = noteX
                val stemStartY = noteY + staffSpace * 0.1f
                val stemEndY = stemStartY + stemLength
                canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
                
                // Draw flag if needed
                if (duration < 1f) {
                    val flagGlyph = SMuFLGlyphs.getFlagForDuration(duration, false)
                    flagGlyph?.let {
                        canvas.drawText(it.toString(), stemX - staffSpace * 0.1f, stemEndY, notePaint)
                    }
                }
            }
        }
    }
}

/**
 * Display multiple notes for interval/chord recognition
 */
@Composable
fun IntervalOnStaff(
    lowerPosition: Int,
    upperPosition: Int,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    clef: ClefType = ClefType.TREBLE,
    noteColor: Color = MaterialTheme.colorScheme.primary,
    lineColor: Color = Color(0xFF1A1A1A),
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val heightPx = with(density) { height.toPx() }
    val staffSpace = heightPx / 8
    val staffHeight = staffSpace * 4
    val topMargin = staffSpace * 2
    
    val typeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        
        val linePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (lineColor.alpha * 255).toInt(),
                (lineColor.red * 255).toInt(),
                (lineColor.green * 255).toInt(),
                (lineColor.blue * 255).toInt()
            )
            strokeWidth = staffSpace * 0.08f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        
        val notePaint = android.graphics.Paint().apply {
            this.typeface = typeface
            textSize = staffSpace * 4
            color = android.graphics.Color.argb(
                (noteColor.alpha * 255).toInt(),
                (noteColor.red * 255).toInt(),
                (noteColor.green * 255).toInt(),
                (noteColor.blue * 255).toInt()
            )
            isAntiAlias = true
        }
        
        val musicPaint = android.graphics.Paint().apply {
            this.typeface = typeface
            textSize = staffSpace * 4
            color = linePaint.color
            isAntiAlias = true
        }
        
        val clefWidth = staffSpace * 2.5f
        val startX = clefWidth + staffSpace
        val noteX = size.width / 2
        
        // Draw 5 staff lines
        for (i in 0 until 5) {
            val y = topMargin + i * staffSpace
            canvas.drawLine(startX, y, size.width - staffSpace, y, linePaint)
        }
        
        // Draw clef with correct SMuFL positioning
        val clefY = when (clef) {
            ClefType.TREBLE -> topMargin + staffSpace * 3
            ClefType.BASS -> topMargin + staffSpace * 1
            ClefType.ALTO -> topMargin + staffSpace * 2
            else -> topMargin + staffSpace * 2
        }
        canvas.drawText(clef.glyph.toString(), staffSpace * 0.2f, clefY, musicPaint)
        
        // Draw both notes
        listOf(lowerPosition, upperPosition).forEach { position ->
            val noteY = topMargin + staffHeight - (position * staffSpace / 2f)
            
            // Ledger lines
            if (position < 0) {
                val ledgerCount = (-position + 1) / 2
                for (i in 1..ledgerCount) {
                    val ledgerY = topMargin + staffSpace * 4 + i * staffSpace
                    canvas.drawLine(noteX - staffSpace, ledgerY, noteX + staffSpace * 1.5f, ledgerY, linePaint)
                }
            } else if (position > 8) {
                val ledgerCount = (position - 8 + 1) / 2
                for (i in 1..ledgerCount) {
                    val ledgerY = topMargin - i * staffSpace
                    canvas.drawLine(noteX - staffSpace, ledgerY, noteX + staffSpace * 1.5f, ledgerY, linePaint)
                }
            }
            
            // Notehead (whole notes for intervals)
            canvas.drawText(SMuFLGlyphs.Noteheads.WHOLE.toString(), noteX, noteY, notePaint)
        }
    }
}

/**
 * Feedback colors for exercises
 */
object FeedbackColors {
    val CORRECT = Color(0xFF22C55E)   // Green
    val INCORRECT = Color(0xFFEF4444) // Red
    val NEUTRAL = Color(0xFF6B7280)   // Gray
}

/**
 * Note on staff with separate feedback for pitch and duration
 * 
 * Used in solfege exercises where:
 * - Notehead color reflects pitch accuracy (green/red)
 * - Stem/beam/flag color reflects duration accuracy (green/red)
 * 
 * Also supports:
 * - Beat number displayed above the note
 * - Solfege name displayed below the note
 */
@Composable
fun NoteOnStaffWithFeedback(
    staffPosition: Int,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    clef: ClefType = ClefType.TREBLE,
    duration: Float = 1f,
    accidental: Char? = null,
    pitchFeedback: com.musimind.music.notation.model.FeedbackState = com.musimind.music.notation.model.FeedbackState.NONE,
    durationFeedback: com.musimind.music.notation.model.FeedbackState = com.musimind.music.notation.model.FeedbackState.NONE,
    beatNumber: Float? = null,
    solfegeName: String? = null,
    lineColor: Color = Color(0xFF1A1A1A),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    defaultNoteColor: Color = MaterialTheme.colorScheme.primary
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val heightPx = with(density) { height.toPx() }
    val staffSpace = heightPx / 8 // More room for labels above/below
    val staffHeight = staffSpace * 4
    val topMargin = staffSpace * 2 // Room for beat number
    
    val typeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    // Determine colors based on feedback state
    val noteheadColor = when (pitchFeedback) {
        com.musimind.music.notation.model.FeedbackState.CORRECT -> FeedbackColors.CORRECT
        com.musimind.music.notation.model.FeedbackState.INCORRECT -> FeedbackColors.INCORRECT
        else -> defaultNoteColor
    }
    
    val stemColor = when (durationFeedback) {
        com.musimind.music.notation.model.FeedbackState.CORRECT -> FeedbackColors.CORRECT
        com.musimind.music.notation.model.FeedbackState.INCORRECT -> FeedbackColors.INCORRECT
        else -> defaultNoteColor
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        
        val linePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (lineColor.alpha * 255).toInt(),
                (lineColor.red * 255).toInt(),
                (lineColor.green * 255).toInt(),
                (lineColor.blue * 255).toInt()
            )
            strokeWidth = staffSpace * 0.08f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        
        val musicPaint = android.graphics.Paint().apply {
            this.typeface = typeface
            textSize = staffSpace * 4
            color = linePaint.color
            isAntiAlias = true
        }
        
        val noteheadPaint = android.graphics.Paint().apply {
            this.typeface = typeface
            textSize = staffSpace * 4
            color = android.graphics.Color.argb(
                (noteheadColor.alpha * 255).toInt(),
                (noteheadColor.red * 255).toInt(),
                (noteheadColor.green * 255).toInt(),
                (noteheadColor.blue * 255).toInt()
            )
            isAntiAlias = true
        }
        
        val stemPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (stemColor.alpha * 255).toInt(),
                (stemColor.red * 255).toInt(),
                (stemColor.green * 255).toInt(),
                (stemColor.blue * 255).toInt()
            )
            strokeWidth = staffSpace * 0.12f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        
        val flagPaint = android.graphics.Paint().apply {
            this.typeface = typeface
            textSize = staffSpace * 4
            color = stemPaint.color
            isAntiAlias = true
        }
        
        val textPaint = android.graphics.Paint().apply {
            textSize = staffSpace * 1.2f
            color = linePaint.color
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val clefWidth = staffSpace * 2.5f
        val startX = clefWidth + staffSpace
        val noteX = size.width / 2
        val noteheadWidth = staffSpace * 1.2f
        
        // Draw 5 staff lines
        for (i in 0 until 5) {
            val y = topMargin + i * staffSpace
            canvas.drawLine(startX, y, size.width - staffSpace, y, linePaint)
        }
        
        // Draw clef
        val clefY = when (clef) {
            ClefType.TREBLE -> topMargin + staffSpace * 3
            ClefType.BASS -> topMargin + staffSpace * 1
            ClefType.ALTO -> topMargin + staffSpace * 2
            else -> topMargin + staffSpace * 2
        }
        canvas.drawText(clef.glyph.toString(), staffSpace * 0.2f, clefY, musicPaint)
        
        // Calculate note Y position
        val noteY = topMargin + staffHeight - (staffPosition * staffSpace / 2f)
        
        // Draw beat number above the note
        beatNumber?.let {
            val beatText = if (it == it.toInt().toFloat()) {
                it.toInt().toString()
            } else {
                String.format("%.1f", it)
            }
            val beatY = minOf(noteY - staffSpace * 1.5f, topMargin - staffSpace * 0.5f)
            canvas.drawText(beatText, noteX + noteheadWidth / 2, beatY, textPaint)
        }
        
        // Draw ledger lines if needed
        if (staffPosition < 0) {
            val ledgerCount = (-staffPosition + 1) / 2
            for (i in 1..ledgerCount) {
                val ledgerY = topMargin + staffHeight + i * staffSpace
                canvas.drawLine(noteX - staffSpace, ledgerY, noteX + staffSpace * 1.5f, ledgerY, linePaint)
            }
        } else if (staffPosition > 8) {
            val ledgerCount = (staffPosition - 8 + 1) / 2
            for (i in 1..ledgerCount) {
                val ledgerY = topMargin - i * staffSpace
                canvas.drawLine(noteX - staffSpace, ledgerY, noteX + staffSpace * 1.5f, ledgerY, linePaint)
            }
        }
        
        // Draw accidental if present
        accidental?.let {
            canvas.drawText(it.toString(), noteX - staffSpace * 1.5f, noteY, noteheadPaint)
        }
        
        // Draw notehead (with pitch feedback color)
        val noteheadGlyph = when {
            duration >= 4f -> SMuFLGlyphs.Noteheads.WHOLE
            duration >= 2f -> SMuFLGlyphs.Noteheads.HALF
            else -> SMuFLGlyphs.Noteheads.BLACK
        }
        canvas.drawText(noteheadGlyph.toString(), noteX, noteY, noteheadPaint)
        
        // Draw stem if needed (with duration feedback color)
        if (duration < 4f) {
            val stemUp = staffPosition < 4
            val stemLength = staffSpace * 3.5f
            
            if (stemUp) {
                val stemX = noteX + noteheadWidth
                val stemStartY = noteY - staffSpace * 0.1f
                val stemEndY = stemStartY - stemLength
                canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
                
                // Draw flag if needed
                if (duration < 1f) {
                    val flagGlyph = SMuFLGlyphs.getFlagForDuration(duration, true)
                    flagGlyph?.let {
                        canvas.drawText(it.toString(), stemX - staffSpace * 0.1f, stemEndY, flagPaint)
                    }
                }
            } else {
                val stemX = noteX
                val stemStartY = noteY + staffSpace * 0.1f
                val stemEndY = stemStartY + stemLength
                canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
                
                // Draw flag if needed
                if (duration < 1f) {
                    val flagGlyph = SMuFLGlyphs.getFlagForDuration(duration, false)
                    flagGlyph?.let {
                        canvas.drawText(it.toString(), stemX - staffSpace * 0.1f, stemEndY, flagPaint)
                    }
                }
            }
        }
        
        // Draw solfege name below the note
        solfegeName?.let {
            val solfegeY = maxOf(noteY + staffSpace * 2f, topMargin + staffHeight + staffSpace * 1.5f)
            canvas.drawText(it, noteX + noteheadWidth / 2, solfegeY, textPaint)
        }
    }
}

/**
 * Note on staff with unified feedback color (for melodic perception)
 * 
 * Used in melodic perception exercises where the entire note is colored
 * green or red based on correctness.
 */
@Composable
fun NoteOnStaffWithUnifiedFeedback(
    staffPosition: Int,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    clef: ClefType = ClefType.TREBLE,
    duration: Float = 1f,
    accidental: Char? = null,
    isCorrect: Boolean? = null, // null = no feedback, true = correct, false = incorrect
    beatNumber: Float? = null,
    lineColor: Color = Color(0xFF1A1A1A),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    defaultNoteColor: Color = MaterialTheme.colorScheme.primary
) {
    val noteColor = when (isCorrect) {
        true -> FeedbackColors.CORRECT
        false -> FeedbackColors.INCORRECT
        null -> defaultNoteColor
    }
    
    // Use feedback for both pitch and duration when unified
    val feedbackState = when (isCorrect) {
        true -> com.musimind.music.notation.model.FeedbackState.CORRECT
        false -> com.musimind.music.notation.model.FeedbackState.INCORRECT
        null -> com.musimind.music.notation.model.FeedbackState.NONE
    }
    
    NoteOnStaffWithFeedback(
        staffPosition = staffPosition,
        modifier = modifier,
        height = height,
        clef = clef,
        duration = duration,
        accidental = accidental,
        pitchFeedback = feedbackState,
        durationFeedback = feedbackState,
        beatNumber = beatNumber,
        solfegeName = null,
        lineColor = lineColor,
        backgroundColor = backgroundColor,
        defaultNoteColor = noteColor
    )
}
