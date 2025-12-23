package com.musimind.music.notation.ui

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musimind.music.notation.model.*
import com.musimind.music.notation.smufl.SMuFLGlyphs

/**
 * Reusable composable to display any SMuFL music symbol
 * 
 * This can be used anywhere in the app to display music symbols
 * without loading images - perfect for lessons, quizzes, and UI elements.
 */
@Composable
fun MusicSymbol(
    glyph: Char,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    
    val typeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    val paint = remember(color, sizePx) {
        android.graphics.Paint().apply {
            this.typeface = typeface
            this.textSize = sizePx
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    
    Canvas(
        modifier = modifier.size(size)
    ) {
        drawContext.canvas.nativeCanvas.drawText(
            glyph.toString(),
            size.toPx() / 2,
            size.toPx() * 0.75f, // Vertical center adjustment
            paint
        )
    }
}

/**
 * Display a clef symbol
 */
@Composable
fun ClefSymbol(
    clef: ClefType,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    MusicSymbol(
        glyph = clef.glyph,
        modifier = modifier,
        size = size,
        color = color,
        contentDescription = "Clef ${clef.name}"
    )
}

/**
 * Display a note symbol (notehead + optional stem)
 */
@Composable
fun NoteSymbol(
    duration: Float,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    stemUp: Boolean = true,
    showStem: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    
    val typeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    Canvas(
        modifier = modifier.size(width = size * 1.5f, height = size * 2f)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        val paint = android.graphics.Paint().apply {
            this.typeface = typeface
            this.textSize = sizePx
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            isAntiAlias = true
        }
        
        val noteheadGlyph = Duration.getNoteheadGlyph(duration)
        val noteY = this.size.height / 2
        val noteX = this.size.width / 3
        
        // Draw notehead
        canvas.drawText(noteheadGlyph.toString(), noteX, noteY, paint)
        
        // Draw stem if needed
        if (showStem && Duration.needsStem(duration)) {
            val stemPaint = android.graphics.Paint().apply {
                this.color = paint.color
                strokeWidth = sizePx * 0.04f
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }
            
            val stemX = if (stemUp) noteX + sizePx * 0.35f else noteX
            val stemHeight = sizePx * 1.1f
            val stemEndY = if (stemUp) noteY - stemHeight else noteY + stemHeight
            
            canvas.drawLine(stemX, noteY, stemX, stemEndY, stemPaint)
            
            // Draw flag if needed
            if (Duration.needsFlag(duration)) {
                val flagGlyph = SMuFLGlyphs.getFlagForDuration(duration, stemUp)
                flagGlyph?.let {
                    canvas.drawText(it.toString(), stemX - sizePx * 0.05f, stemEndY, paint)
                }
            }
        }
    }
}

/**
 * Display a rest symbol
 */
@Composable
fun RestSymbol(
    duration: Float,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val glyph = SMuFLGlyphs.getRestForDuration(duration)
    MusicSymbol(
        glyph = glyph,
        modifier = modifier,
        size = size,
        color = color,
        contentDescription = "Rest"
    )
}

/**
 * Display an accidental symbol
 */
@Composable
fun AccidentalSymbol(
    accidental: AccidentalType,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    MusicSymbol(
        glyph = accidental.glyph,
        modifier = modifier,
        size = size,
        color = color,
        contentDescription = accidental.name
    )
}

/**
 * Display a dynamic symbol (p, f, mf, etc.)
 */
@Composable
fun DynamicSymbol(
    dynamic: DynamicType,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    MusicSymbol(
        glyph = dynamic.glyph,
        modifier = modifier,
        size = size,
        color = color,
        contentDescription = dynamic.name
    )
}

/**
 * Display an articulation symbol
 */
@Composable
fun ArticulationSymbol(
    articulation: ArticulationType,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    MusicSymbol(
        glyph = articulation.glyph,
        modifier = modifier,
        size = size,
        color = color,
        contentDescription = articulation.name
    )
}

/**
 * Display an ornament symbol
 */
@Composable
fun OrnamentSymbol(
    ornament: OrnamentType,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    MusicSymbol(
        glyph = ornament.glyph,
        modifier = modifier,
        size = size,
        color = color,
        contentDescription = ornament.name
    )
}

/**
 * Display a time signature
 */
@Composable
fun TimeSignatureSymbol(
    numerator: Int,
    denominator: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    
    val typeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    Canvas(
        modifier = modifier.size(width = size * 0.8f, height = size)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        val paint = android.graphics.Paint().apply {
            this.typeface = typeface
            this.textSize = sizePx * 0.45f
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        val centerX = this.size.width / 2
        
        // Draw numerator
        val numGlyph = SMuFLGlyphs.TimeSignatures.getNumeral(numerator)
        canvas.drawText(numGlyph.toString(), centerX, this.size.height * 0.35f, paint)
        
        // Draw denominator
        val denGlyph = SMuFLGlyphs.TimeSignatures.getNumeral(denominator)
        canvas.drawText(denGlyph.toString(), centerX, this.size.height * 0.85f, paint)
    }
}

/**
 * Display a key signature
 */
@Composable
fun KeySignatureSymbol(
    keySignature: KeySignatureType,
    clef: ClefType = ClefType.TREBLE,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    
    val typeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    val width = (keySignature.accidentals * 0.5f * size.value).dp + size / 2
    
    Canvas(
        modifier = modifier.size(width = width, height = size)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        val paint = android.graphics.Paint().apply {
            this.typeface = typeface
            this.textSize = sizePx * 0.7f
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            isAntiAlias = true
        }
        
        val accidentalChar = if (keySignature.isSharp) 
            SMuFLGlyphs.Accidentals.SHARP 
        else 
            SMuFLGlyphs.Accidentals.FLAT
        
        // Sharp/flat positions for treble clef
        val positions = if (keySignature.isSharp) {
            listOf(0.3f, 0.6f, 0.2f, 0.5f, 0.8f, 0.4f, 0.7f)
        } else {
            listOf(0.7f, 0.4f, 0.8f, 0.5f, 0.9f, 0.6f, 1.0f)
        }
        
        for (i in 0 until keySignature.accidentals) {
            val x = i * sizePx * 0.4f + sizePx * 0.2f
            val y = positions[i] * this.size.height
            canvas.drawText(accidentalChar.toString(), x, y, paint)
        }
    }
}

/**
 * Convenience composable to display a complete note name with pitch
 * Example: Shows "C4" with a visual representation
 */
@Composable
fun PitchDisplay(
    pitch: Pitch,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showName: Boolean = true,
    noteColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NoteSymbol(
            duration = Duration.QUARTER,
            size = size,
            color = noteColor,
            showStem = true
        )
        
        if (showName) {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                androidx.compose.material3.Text(
                    text = "${pitch.note}${pitch.octave}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (pitch.alteration != 0) {
                    val accType = when (pitch.alteration) {
                        -2 -> AccidentalType.DOUBLE_FLAT
                        -1 -> AccidentalType.FLAT
                        1 -> AccidentalType.SHARP
                        2 -> AccidentalType.DOUBLE_SHARP
                        else -> null
                    }
                    accType?.let {
                        AccidentalSymbol(
                            accidental = it,
                            size = size / 2,
                            color = noteColor
                        )
                    }
                }
            }
        }
    }
}
