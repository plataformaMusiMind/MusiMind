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
        
        val startX = if (showClef) staffSpace * 3f else 0f
        
        // Draw 5 staff lines
        for (i in 0 until 5) {
            val y = i * staffSpace
            canvas.drawLine(startX, y, size.width, y, linePaint)
        }
        
        // Draw clef if requested
        if (showClef) {
            val musicPaint = android.graphics.Paint().apply {
                this.typeface = typeface
                textSize = staffSpace * 4
                color = linePaint.color
                isAntiAlias = true
            }
            
            val clefY = when (clef) {
                ClefType.TREBLE -> staffSpace * 3.5f
                ClefType.BASS -> staffSpace * 1f
                else -> staffSpace * 2f
            }
            
            canvas.drawText(clef.glyph.toString(), staffSpace * 0.3f, clefY, musicPaint)
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
            textSize = staffSpace * 4
            color = linePaint.color
            isAntiAlias = true
        }
        
        val clefY = when (clef) {
            ClefType.TREBLE -> topMargin + staffSpace * 3.5f
            ClefType.BASS -> topMargin + staffSpace * 1f
            else -> topMargin + staffSpace * 2f
        }
        
        canvas.drawText(clef.glyph.toString(), staffSpace * 0.2f, clefY, musicPaint)
    }
}

/**
 * Preview component showing a single note on staff
 * 
 * Useful for lessons and quizzes to show a note example
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
            textSize = staffSpace * 4
            color = linePaint.color
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
        
        val clefWidth = staffSpace * 2.5f
        val startX = clefWidth + staffSpace
        val noteX = size.width / 2
        
        // Draw 5 staff lines
        for (i in 0 until 5) {
            val y = topMargin + i * staffSpace
            canvas.drawLine(startX, y, size.width - staffSpace, y, linePaint)
        }
        
        // Draw clef
        val clefY = when (clef) {
            ClefType.TREBLE -> topMargin + staffSpace * 3.5f
            ClefType.BASS -> topMargin + staffSpace * 1f
            else -> topMargin + staffSpace * 2f
        }
        canvas.drawText(clef.glyph.toString(), staffSpace * 0.2f, clefY, musicPaint)
        
        // Calculate note Y position
        // Position 0 = bottom line (E4), position 8 = top line (F5)
        val noteY = topMargin + (4 - staffPosition / 2f) * staffSpace
        
        // Draw ledger lines if needed
        if (staffPosition < 0) {
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
            canvas.drawText(it.toString(), noteX - staffSpace * 1.2f, noteY, notePaint)
        }
        
        // Draw notehead
        val noteheadGlyph = when {
            duration >= 4f -> SMuFLGlyphs.Noteheads.WHOLE
            duration >= 2f -> SMuFLGlyphs.Noteheads.HALF
            else -> SMuFLGlyphs.Noteheads.BLACK
        }
        canvas.drawText(noteheadGlyph.toString(), noteX, noteY, notePaint)
        
        // Draw stem if needed
        if (duration < 4f) {
            val stemUp = staffPosition <= 4
            val stemPaint = android.graphics.Paint().apply {
                color = notePaint.color
                strokeWidth = staffSpace * 0.1f
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }
            
            val stemX = if (stemUp) noteX + staffSpace * 0.35f else noteX
            val stemLength = staffSpace * 3.5f
            val stemEndY = if (stemUp) noteY - stemLength else noteY + stemLength
            
            canvas.drawLine(stemX, noteY, stemX, stemEndY, stemPaint)
            
            // Draw flag if needed
            if (duration < 1f) {
                val flagGlyph = SMuFLGlyphs.getFlagForDuration(duration, stemUp)
                flagGlyph?.let {
                    canvas.drawText(it.toString(), stemX - staffSpace * 0.05f, stemEndY, notePaint)
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
        
        // Draw clef
        val clefY = when (clef) {
            ClefType.TREBLE -> topMargin + staffSpace * 3.5f
            ClefType.BASS -> topMargin + staffSpace * 1f
            else -> topMargin + staffSpace * 2f
        }
        canvas.drawText(clef.glyph.toString(), staffSpace * 0.2f, clefY, musicPaint)
        
        // Draw both notes
        listOf(lowerPosition, upperPosition).forEach { position ->
            val noteY = topMargin + (4 - position / 2f) * staffSpace
            
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
            
            // Notehead
            canvas.drawText(SMuFLGlyphs.Noteheads.WHOLE.toString(), noteX, noteY, notePaint)
        }
    }
}
