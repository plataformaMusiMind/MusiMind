package com.musimind.music.notation.ui

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musimind.music.notation.engine.NotationEngine
import com.musimind.music.notation.layout.*
import com.musimind.music.notation.model.*
import com.musimind.music.notation.smufl.SMuFLGlyphs

/**
 * Main composable for rendering a complete musical score
 * 
 * This component renders professional-quality music notation using SMuFL fonts.
 * It supports scrolling, zooming, and tap interaction with notes.
 */
@Composable
fun ScoreView(
    score: Score,
    modifier: Modifier = Modifier,
    staffHeight: Dp = 64.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    staffColor: Color = Color(0xFF1A1A1A),
    noteStates: Map<String, NoteState> = emptyMap(),
    onNoteClick: ((String) -> Unit)? = null,
    showMeasureNumbers: Boolean = true,
    horizontalPadding: Dp = 24.dp
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    
    // Create notation engine
    val notationEngine = remember { NotationEngine(context) }
    val layoutEngine = remember { ScoreLayoutEngine() }
    
    // Calculate layout
    val staffHeightPx = with(density) { staffHeight.toPx() }
    val paddingPx = with(density) { horizontalPadding.toPx() }
    val staffSpace = staffHeightPx / 4 // 5 lines = 4 spaces
    val fontSize = staffSpace * 4 // SMuFL font size relative to staff
    
    // Layout the score
    val layout = remember(score) {
        layoutEngine.layoutScore(score, 2000f) // Will recalculate based on content
    }
    
    Box(
        modifier = modifier
            .background(backgroundColor)
            .horizontalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(with(density) { (layout.totalWidth + paddingPx * 2).toDp() })
                .height(staffHeight + 80.dp) // Extra space for ledger lines and articulations
                .pointerInput(onNoteClick) {
                    if (onNoteClick != null) {
                        detectTapGestures { offset ->
                            // Find clicked note
                            findClickedNote(layout, offset, staffSpace)?.let { noteId ->
                                onNoteClick(noteId)
                            }
                        }
                    }
                }
        ) {
            val canvas = drawContext.canvas.nativeCanvas
            val verticalCenter = size.height / 2
            
            // Create paints
            val staffPaint = notationEngine.createStaffPaint(
                lineWidth = staffSpace * 0.13f,
                color = staffColor
            )
            val musicPaint = notationEngine.createMusicPaint(fontSize)
            
            // Draw staff lines
            for (i in 0 until 5) {
                val y = verticalCenter - staffHeightPx / 2 + i * staffSpace
                canvas.drawLine(
                    paddingPx,
                    y,
                    paddingPx + layout.totalWidth,
                    y,
                    staffPaint
                )
            }
            
            // Draw score header (clef, key signature, time signature)
            var headerX = paddingPx
            
            // Draw clef
            val clefGlyph = notationEngine.getClefGlyph(score.clef)
            val clefY = verticalCenter + when (score.clef) {
                ClefType.TREBLE -> staffSpace * 0.5f
                ClefType.BASS -> -staffSpace * 1.5f
                else -> 0f
            }
            canvas.drawText(
                clefGlyph.toString(),
                headerX,
                clefY,
                musicPaint
            )
            headerX += staffSpace * 2.8f
            
            // Draw key signature
            val keyAccidentals = notationEngine.getKeySignatureAccidentals(score.keySignature)
            for ((position, glyph) in keyAccidentals) {
                val accY = verticalCenter - staffHeightPx / 2 + (8 - position) * staffSpace / 2
                canvas.drawText(
                    glyph.toString(),
                    headerX,
                    accY,
                    musicPaint
                )
                headerX += staffSpace * 0.7f
            }
            headerX += staffSpace * 0.5f
            
            // Draw time signature
            if (score.timeSignature.isCommon) {
                val tsY = verticalCenter
                canvas.drawText(
                    SMuFLGlyphs.TimeSignatures.COMMON.toString(),
                    headerX,
                    tsY,
                    musicPaint
                )
            } else {
                // Draw numerator and denominator
                drawTimeSignature(
                    canvas,
                    headerX,
                    verticalCenter,
                    score.timeSignature,
                    musicPaint,
                    staffSpace
                )
            }
            headerX += staffSpace * 2f
            
            // Draw measures
            for (measureLayout in layout.measures) {
                // Draw elements
                for (element in measureLayout.elements) {
                    val state = noteStates[element.id] ?: NoteState.NORMAL
                    val color = notationEngine.getColorForState(state)
                    val elementPaint = notationEngine.createMusicPaint(fontSize, color)
                    
                    when (element) {
                        is ElementLayout.NoteLayout -> {
                            drawNote(
                                canvas,
                                element,
                                verticalCenter,
                                staffHeightPx,
                                staffSpace,
                                elementPaint,
                                staffPaint,
                                notationEngine
                            )
                        }
                        is ElementLayout.ChordLayout -> {
                            for (noteLayout in element.notes) {
                                val noteState = noteStates[noteLayout.id] ?: NoteState.NORMAL
                                val noteColor = notationEngine.getColorForState(noteState)
                                val notePaint = notationEngine.createMusicPaint(fontSize, noteColor)
                                drawNote(
                                    canvas,
                                    noteLayout.copy(stemUp = element.stemUp),
                                    verticalCenter,
                                    staffHeightPx,
                                    staffSpace,
                                    notePaint,
                                    staffPaint,
                                    notationEngine
                                )
                            }
                            // Draw single stem for chord
                            drawChordStem(canvas, element, verticalCenter, staffHeightPx, staffSpace, elementPaint)
                        }
                        is ElementLayout.RestLayout -> {
                            drawRest(canvas, element, verticalCenter, staffSpace, elementPaint, notationEngine)
                        }
                    }
                }
                
                // Draw barline
                drawBarline(
                    canvas,
                    measureLayout.endX,
                    verticalCenter - staffHeightPx / 2,
                    staffHeightPx,
                    measureLayout.barlineType,
                    staffPaint,
                    musicPaint
                )
                
                // Draw measure number
                if (showMeasureNumbers && measureLayout.measureNumber > 0) {
                    val measureNumberPaint = android.graphics.Paint().apply {
                        textSize = staffSpace * 0.8f
                        color = android.graphics.Color.GRAY
                        isAntiAlias = true
                    }
                    canvas.drawText(
                        measureLayout.measureNumber.toString(),
                        measureLayout.startX,
                        verticalCenter - staffHeightPx / 2 - staffSpace,
                        measureNumberPaint
                    )
                }
            }
        }
    }
}

/**
 * Draw a single note
 */
private fun drawNote(
    canvas: android.graphics.Canvas,
    note: ElementLayout.NoteLayout,
    verticalCenter: Float,
    staffHeight: Float,
    staffSpace: Float,
    paint: android.graphics.Paint,
    staffPaint: android.graphics.Paint,
    engine: NotationEngine
) {
    val noteY = verticalCenter - staffHeight / 2 + (8 - note.staffPosition) * staffSpace / 2
    
    // Draw accidental if present
    note.accidental?.let { acc ->
        canvas.drawText(
            acc.glyph.toString(),
            note.x - staffSpace * 1.2f,
            noteY,
            paint
        )
    }
    
    // Draw ledger lines
    if (note.ledgerLineCount > 0) {
        val ledgerWidth = staffSpace * 1.4f
        val startX = note.x - ledgerWidth / 4
        val endX = note.x + ledgerWidth * 0.75f
        
        for (i in 1..note.ledgerLineCount) {
            val ledgerY = if (note.ledgerLinesAbove) {
                verticalCenter - staffHeight / 2 - i * staffSpace
            } else {
                verticalCenter + staffHeight / 2 + i * staffSpace
            }
            canvas.drawLine(startX, ledgerY, endX, ledgerY, staffPaint)
        }
    }
    
    // Draw notehead
    val noteheadGlyph = engine.getNoteheadGlyph(note.durationBeats)
    canvas.drawText(noteheadGlyph.toString(), note.x, noteY, paint)
    
    // Draw stem (for notes shorter than whole note)
    if (note.durationBeats < 4f) {
        val stemLength = staffSpace * 3.5f
        val stemX = if (note.stemUp) note.x + staffSpace * 1.1f else note.x
        val stemStartY = noteY
        val stemEndY = if (note.stemUp) noteY - stemLength else noteY + stemLength
        
        val stemPaint = android.graphics.Paint().apply {
            color = paint.color
            strokeWidth = staffSpace * 0.12f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
        
        // Draw flag (for unbeamed notes shorter than quarter)
        if (note.durationBeats < 1f && note.beamGroup == null) {
            val flagGlyph = engine.getFlagGlyph(note.durationBeats, note.stemUp)
            flagGlyph?.let {
                canvas.drawText(it.toString(), stemX - staffSpace * 0.1f, stemEndY, paint)
            }
        }
    }
    
    // Draw dot
    if (note.dotted || note.doubleDotted) {
        val dotX = note.x + staffSpace * 1.4f
        val dotY = if (note.staffPosition % 2 == 0) noteY - staffSpace / 4 else noteY
        canvas.drawText(SMuFLGlyphs.AugmentationDots.DOT.toString(), dotX, dotY, paint)
        
        if (note.doubleDotted) {
            canvas.drawText(SMuFLGlyphs.AugmentationDots.DOT.toString(), dotX + staffSpace * 0.4f, dotY, paint)
        }
    }
    
    // Draw articulations
    for ((index, articulation) in note.articulations.withIndex()) {
        val artY = if (note.stemUp) {
            noteY + staffSpace * (1.2f + index * 0.6f)
        } else {
            noteY - staffSpace * (1.2f + index * 0.6f)
        }
        canvas.drawText(articulation.glyph.toString(), note.x, artY, paint)
    }
    
    // Draw ornament
    note.ornament?.let { orn ->
        val ornY = noteY - staffSpace * 2f
        canvas.drawText(orn.glyph.toString(), note.x, ornY, paint)
    }
    
    // Draw dynamic
    note.dynamic?.let { dyn ->
        val dynY = verticalCenter + staffHeight / 2 + staffSpace * 2
        canvas.drawText(dyn.glyph.toString(), note.x, dynY, paint)
    }
}

/**
 * Draw chord stem
 */
private fun drawChordStem(
    canvas: android.graphics.Canvas,
    chord: ElementLayout.ChordLayout,
    verticalCenter: Float,
    staffHeight: Float,
    staffSpace: Float,
    paint: android.graphics.Paint
) {
    if (chord.durationBeats >= 4f) return // Whole notes have no stem
    
    val notes = chord.notes.sortedBy { it.staffPosition }
    val lowestNote = notes.first()
    val highestNote = notes.last()
    
    val stemX = if (chord.stemUp) {
        chord.x + staffSpace * 1.1f
    } else {
        chord.x
    }
    
    val startY = verticalCenter - staffHeight / 2 + (8 - if (chord.stemUp) lowestNote.staffPosition else highestNote.staffPosition) * staffSpace / 2
    val endY = verticalCenter - staffHeight / 2 + (8 - if (chord.stemUp) highestNote.staffPosition else lowestNote.staffPosition) * staffSpace / 2 +
        (if (chord.stemUp) -staffSpace * 3.5f else staffSpace * 3.5f)
    
    val stemPaint = android.graphics.Paint().apply {
        color = paint.color
        strokeWidth = staffSpace * 0.12f
        style = android.graphics.Paint.Style.STROKE
        isAntiAlias = true
    }
    canvas.drawLine(stemX, startY, stemX, endY, stemPaint)
}

/**
 * Draw a rest
 */
private fun drawRest(
    canvas: android.graphics.Canvas,
    rest: ElementLayout.RestLayout,
    verticalCenter: Float,
    staffSpace: Float,
    paint: android.graphics.Paint,
    engine: NotationEngine
) {
    val restGlyph = engine.getRestGlyph(rest.durationBeats)
    canvas.drawText(restGlyph.toString(), rest.x, rest.y + verticalCenter, paint)
}

/**
 * Draw time signature
 */
private fun drawTimeSignature(
    canvas: android.graphics.Canvas,
    x: Float,
    centerY: Float,
    timeSignature: TimeSignatureModel,
    paint: android.graphics.Paint,
    staffSpace: Float
) {
    val numString = timeSignature.numerator.toString()
    val denString = timeSignature.denominator.toString()
    
    val numY = centerY - staffSpace
    val denY = centerY + staffSpace
    
    for ((i, digit) in numString.withIndex()) {
        val glyph = SMuFLGlyphs.TimeSignatures.getNumeral(digit.digitToInt())
        canvas.drawText(glyph.toString(), x + i * staffSpace * 0.8f, numY, paint)
    }
    for ((i, digit) in denString.withIndex()) {
        val glyph = SMuFLGlyphs.TimeSignatures.getNumeral(digit.digitToInt())
        canvas.drawText(glyph.toString(), x + i * staffSpace * 0.8f, denY, paint)
    }
}

/**
 * Draw barline
 */
private fun drawBarline(
    canvas: android.graphics.Canvas,
    x: Float,
    topY: Float,
    height: Float,
    type: BarlineType,
    staffPaint: android.graphics.Paint,
    musicPaint: android.graphics.Paint
) {
    when (type) {
        BarlineType.SINGLE -> {
            canvas.drawLine(x, topY, x, topY + height, staffPaint)
        }
        BarlineType.DOUBLE -> {
            canvas.drawLine(x - 4, topY, x - 4, topY + height, staffPaint)
            canvas.drawLine(x, topY, x, topY + height, staffPaint)
        }
        BarlineType.FINAL -> {
            canvas.drawLine(x - 6, topY, x - 6, topY + height, staffPaint)
            val thickPaint = android.graphics.Paint(staffPaint).apply {
                strokeWidth = 4f
            }
            canvas.drawLine(x, topY, x, topY + height, thickPaint)
        }
        else -> {
            canvas.drawText(type.glyph.toString(), x, topY + height / 2, musicPaint)
        }
    }
}

/**
 * Find clicked note
 */
private fun findClickedNote(
    layout: LayoutResult,
    offset: Offset,
    staffSpace: Float
): String? {
    val hitRadius = staffSpace * 1.5f
    
    for (measure in layout.measures) {
        for (element in measure.elements) {
            when (element) {
                is ElementLayout.NoteLayout -> {
                    if (kotlin.math.abs(element.x - offset.x) < hitRadius &&
                        kotlin.math.abs(element.y - offset.y) < hitRadius) {
                        return element.id
                    }
                }
                is ElementLayout.ChordLayout -> {
                    for (note in element.notes) {
                        if (kotlin.math.abs(note.x - offset.x) < hitRadius &&
                            kotlin.math.abs(note.y - offset.y) < hitRadius) {
                            return note.id
                        }
                    }
                }
                else -> {}
            }
        }
    }
    return null
}
