package com.musimind.presentation.exercise

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.music.notation.model.*
import com.musimind.music.notation.smufl.SMuFLGlyphs
import com.musimind.music.notation.ui.TieSlurRenderer

/**
 * Melodic Perception Exercise Screen - Complete Refactor
 * 
 * Clean architecture based on SolfegeExerciseScreen patterns.
 * Uses Canvas + Bravura font for all musical glyph rendering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelodicPerceptionScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onComplete: (score: Int, total: Int) -> Unit,
    viewModel: MelodicPerceptionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // Force landscape
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadExercise(exerciseId)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            state.isComplete -> {
                CompleteScreen(
                    correctCount = state.correctCount,
                    totalCount = state.targetNotes.size,
                    onContinue = { onComplete(state.correctCount, state.targetNotes.size) }
                )
            }
            else -> {
                MainContent(
                    state = state,
                    onBack = onBack,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    state: MelodicPerceptionState,
    onBack: () -> Unit,
    viewModel: MelodicPerceptionViewModel
) {
    val context = LocalContext.current
    val bravuraTypeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // === HEADER ===
        Header(
            title = state.exerciseTitle,
            onBack = onBack,
            onPlayMelody = { viewModel.playMelody() },
            onPlayUserNotes = { viewModel.playUserNotes() },
            onUndo = { viewModel.undoLastNote() },
            onVerify = { viewModel.verify() }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // === SCORE VIEW with Horizontal Scroll ===
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        
        // Auto-scroll during playback
        LaunchedEffect(state.playbackNoteIndex) {
            if (state.isPlaying && state.playbackNoteIndex >= 0) {
                // Calculate approximate scroll position based on note index
                val noteSpacing = 80.dp.value * density.density
                val targetScroll = (state.playbackNoteIndex * noteSpacing).toInt()
                scrollState.animateScrollTo(maxOf(0, targetScroll - 200))
            }
        }
        
        Box(
            modifier = Modifier
                .weight(0.42f) // 42% for score
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp)
            ) {
                ScoreCanvas(
                    notes = state.userNotes,
                    currentNoteIndex = if (state.isPlaying) state.playbackNoteIndex else state.currentNoteIndex,
                    feedbackResults = state.feedbackResults,
                    showFeedback = state.showFeedback,
                    isPlaying = state.isPlaying,
                    clef = state.clef,
                    typeface = bravuraTypeface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // === INPUT PANEL ===
        InputPanel(
            state = state,
            typeface = bravuraTypeface,
            onNoteSelected = { viewModel.selectNote(it) },
            onOctaveChange = { viewModel.changeOctave(it) },
            onDurationSelected = { viewModel.selectDuration(it) },
            onAccidentalSelected = { viewModel.selectAccidental(it) },
            onDotTypeSelected = { viewModel.selectDotType(it) },
            onToggleTied = { viewModel.toggleTieOnCurrentNote() }, // Toggle tie on current note
            onToggleMetronome = { viewModel.toggleMetronome() },
            onAddNote = { viewModel.addNote() },
            onAddRest = { viewModel.addRest() },
            onNavigatePrevious = { viewModel.previousNote() },
            onNavigateNext = { viewModel.nextNote() },
            onMoveNoteUp = { viewModel.moveNoteUp() },
            onMoveNoteDown = { viewModel.moveNoteDown() }
        )
    }
    
    // === FEEDBACK MODAL ===
    if (state.showFeedbackModal && state.feedbackMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFeedbackModal() },
            title = {
                Text(
                    text = if (state.allCorrect) "✓ Correto!" else "Aviso",
                    fontWeight = FontWeight.Bold,
                    color = if (state.allCorrect) Color(0xFF22C55E) else Color(0xFFEF4444)
                )
            },
            text = {
                Text(state.feedbackMessage ?: "")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissFeedbackModal() }) {
                    Text("OK")
                }
            },
            containerColor = Color.White
        )
    }
}

// ============================================
// HEADER
// ============================================
@Composable
private fun Header(
    title: String,
    onBack: () -> Unit,
    onPlayMelody: () -> Unit,
    onPlayUserNotes: () -> Unit,
    onUndo: () -> Unit,
    onVerify: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Voltar", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, "Desfazer", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = onPlayMelody) {
                Icon(Icons.Default.PlayArrow, "Ouvir Exercício", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onPlayUserNotes) {
                Icon(Icons.Default.Hearing, "Ouvir Resposta", tint = MaterialTheme.colorScheme.secondary)
            }
            
            // Verify Button
            Button(
                onClick = onVerify,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Verificar",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Verificar", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

// ============================================
// SCORE CANVAS (with ledger lines, fixed stems, and BEAMING)
// ============================================
@Composable
private fun ScoreCanvas(
    notes: List<MusicElement>,
    currentNoteIndex: Int,
    feedbackResults: Map<Int, Boolean>,
    showFeedback: Boolean,
    isPlaying: Boolean,
    clef: ClefType,
    typeface: Typeface
) {
    // Calculate dynamic width based on TOTAL BEATS (not just number of notes)
    // This ensures proper spacing regardless of note durations
    val density = LocalDensity.current
    val totalBeats = notes.sumOf { it.durationBeats.toDouble() }.toFloat()
    val beatWidth = 80.dp // Width per beat
    val headerWidth = 150.dp // Space for clef + time signature
    val barlineSpace = 50.dp // Extra space for final barlines
    val minBeats = 8f // Minimum 2 measures worth of space
    
    // Width = header + (beats * beatWidth) + barline space
    val dynamicWidth = headerWidth + (beatWidth * maxOf(totalBeats, minBeats)) + barlineSpace
    
    Canvas(
        modifier = Modifier
            .widthIn(min = dynamicWidth)
            .width(dynamicWidth)
            .fillMaxHeight()
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        
        val staffHeight = size.height * 0.5f
        val staffSpace = staffHeight / 4f
        val startY = (size.height - staffHeight) / 2f
        val staffBottom = startY + staffHeight
        
        // Colors
        val lineColor = android.graphics.Color.BLACK
        val highlightColor = android.graphics.Color.rgb(59, 130, 246)
        val correctColor = android.graphics.Color.rgb(34, 197, 94)
        val incorrectColor = android.graphics.Color.rgb(239, 68, 68)
        
        // Paints
        val linePaint = android.graphics.Paint().apply {
            color = lineColor
            strokeWidth = staffSpace * 0.08f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        
        val ledgerPaint = android.graphics.Paint().apply {
            color = lineColor
            strokeWidth = staffSpace * 0.08f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }
        
        val musicPaint = android.graphics.Paint().apply {
            this.typeface = typeface
            textSize = staffSpace * 4f
            color = lineColor
            isAntiAlias = true
        }
        
        val beamPaint = android.graphics.Paint().apply {
            color = lineColor
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Draw staff lines
        val headerWidth = staffSpace * 6f
        for (i in 0 until 5) {
            val y = startY + i * staffSpace
            canvas.drawLine(0f, y, size.width, y, linePaint)
        }
        
        // Draw clef
        val clefGlyph = when (clef) {
            ClefType.TREBLE -> SMuFLGlyphs.Clefs.TREBLE
            ClefType.BASS -> SMuFLGlyphs.Clefs.BASS
            else -> SMuFLGlyphs.Clefs.TREBLE
        }
        val clefY = when (clef) {
            ClefType.TREBLE -> startY + staffSpace * 3f
            ClefType.BASS -> startY + staffSpace * 1f
            else -> startY + staffSpace * 2f
        }
        canvas.drawText(clefGlyph.toString(), staffSpace * 0.3f, clefY, musicPaint)
        
        // Draw time signature
        canvas.drawText(SMuFLGlyphs.TimeSignatures.COMMON.toString(), staffSpace * 4f, startY + staffSpace * 2f, musicPaint)
        
        if (notes.isEmpty()) {
            // Draw barlines and return
            val barlineX = size.width - staffSpace * 1.5f
            canvas.drawLine(barlineX, startY, barlineX, staffBottom, linePaint)
            val thickPaint = android.graphics.Paint(linePaint).apply { strokeWidth = staffSpace * 0.35f }
            canvas.drawLine(barlineX + staffSpace * 0.5f, startY, barlineX + staffSpace * 0.5f, staffBottom, thickPaint)
            return@Canvas
        }
        
        // Pre-calculate positions for all elements
        val noteheadWidth = staffSpace * 1.3f
        val totalBeats = notes.sumOf { it.durationBeats.toDouble() }.toFloat().coerceAtLeast(4f)
        val beatWidth = (size.width - headerWidth - staffSpace * 3) / totalBeats
        
        data class NoteRenderInfo(
            val index: Int,
            val x: Float,
            val y: Float,
            val staffPosition: Int,
            val duration: Float,
            val isNote: Boolean,
            val note: Note?
        )
        
        val renderInfos = mutableListOf<NoteRenderInfo>()
        var accumulatedBeats = 0f
        
        notes.forEachIndexed { index, element ->
            val elementX = headerWidth + accumulatedBeats * beatWidth + beatWidth * 0.5f
            
            when (element) {
                is Note -> {
                    val staffPosition = element.pitch.staffPosition(clef)
                    // E4 (staffPosition 2) should be at staffBottom (first line from bottom)
                    // Each staff position moves half a staffSpace up
                    // staffBottom corresponds to staffPosition 2 (E4 in treble clef)
                    val noteY = staffBottom - ((staffPosition - 2) * staffSpace / 2f)
                    renderInfos.add(NoteRenderInfo(index, elementX, noteY, staffPosition, element.durationBeats, true, element))
                }
                is Rest -> {
                    val restY = startY + staffSpace * 2f
                    renderInfos.add(NoteRenderInfo(index, elementX, restY, 4, element.durationBeats, false, null))
                }
                else -> {}
            }
            accumulatedBeats += element.durationBeats
        }
        
        // === BEAM GROUPING ===
        // Group consecutive beamable notes by BEAT
        // Each beam group fills exactly 1 beat, then a new group starts
        // This creates visually correct grouping where each beam = 1 beat
        val beamGroups = mutableMapOf<Int, MutableList<NoteRenderInfo>>()
        var currentGroupId = 0
        var lastBeamableIndex = -2 // Tracks continuity of beamable notes
        var currentBeatAccumulator = 0f // Tracks how much of current beat is filled
        
        for (info in renderInfos) {
            if (info.isNote && info.duration < 1f) {
                // This note can be beamed (duration < 1 beat = eighth and shorter)
                
                // Check if this is consecutive to the last beamable note
                val isConsecutive = info.index == lastBeamableIndex + 1
                
                if (!isConsecutive) {
                    // Non-consecutive: start fresh group
                    if (beamGroups[currentGroupId]?.isNotEmpty() == true) {
                        currentGroupId++
                    }
                    currentBeatAccumulator = 0f
                }
                
                // Add note to current group
                beamGroups.getOrPut(currentGroupId) { mutableListOf() }.add(info)
                currentBeatAccumulator += info.duration
                lastBeamableIndex = info.index
                
                // Check if we've completed a beat (or very close to it due to float precision)
                if (currentBeatAccumulator >= 0.999f) {
                    // Beat is complete, start new group for next notes
                    currentGroupId++
                    currentBeatAccumulator = 0f
                }
            } else {
                // Non-beamable element (rest or quarter note or longer)
                // Reset accumulator and potentially start new group
                if (beamGroups[currentGroupId]?.isNotEmpty() == true) {
                    currentGroupId++
                }
                currentBeatAccumulator = 0f
            }
        }
        
        // Filter out single-note groups (beams need ≥2 notes)
        val validBeamGroups = beamGroups.filterValues { it.size >= 2 }
        val beamedNoteIndices = validBeamGroups.values.flatten().map { it.index }.toSet()
        
        // Playback highlight color (bright blue)
        val playbackColor = android.graphics.Color.rgb(59, 130, 246)
        
        // === RENDER NOTES ===
        for (info in renderInfos) {
            val color = when {
                // Feedback colors take priority
                showFeedback && feedbackResults[info.index] == true -> correctColor
                showFeedback && feedbackResults[info.index] == false -> incorrectColor
                // Playback highlight (bright blue pulsing effect)
                isPlaying && info.index == currentNoteIndex -> playbackColor
                // Normal cursor highlight
                !isPlaying && info.index == currentNoteIndex -> highlightColor
                else -> lineColor
            }
            
            val elementPaint = android.graphics.Paint(musicPaint).apply { this.color = color }
            
            if (info.isNote && info.note != null) {
                val note = info.note
                val noteY = info.y
                val elementX = info.x
                val staffPosition = info.staffPosition
                
                // Get the actual notehead glyph for accurate width measurement
                val noteheadGlyph = SMuFLGlyphs.getNoteheadForDuration(note.durationBeats)
                // Measure the actual glyph width for precise ledger line alignment
                val actualNoteheadWidth = elementPaint.measureText(noteheadGlyph.toString())
                
                // === LEDGER LINES ===
                // Ledger lines are drawn on even staff positions outside the staff
                // D4 (position 1) does NOT need a ledger line
                // Only positions 0, -2, -4... below and 12, 14, 16... above need ledger lines
                val ledgerWidth = actualNoteheadWidth * 1.5f
                // Center the ledger line on the notehead using measured width
                val ledgerCenterX = elementX + actualNoteheadWidth / 2f
                val ledgerStartX = ledgerCenterX - ledgerWidth / 2f
                val ledgerEndX = ledgerCenterX + ledgerWidth / 2f
                
                if (staffPosition <= 0) {
                    // Below the staff - draw ledger lines at positions 0, -2, -4, etc.
                    var pos = 0
                    while (pos >= staffPosition) {
                        // Use same formula as noteY
                        val ledgerY = staffBottom - ((pos - 2) * staffSpace / 2f)
                        canvas.drawLine(ledgerStartX, ledgerY, ledgerEndX, ledgerY, ledgerPaint)
                        pos -= 2
                    }
                }
                if (staffPosition >= 12) {
                    // Above the staff - draw ledger lines at positions 12, 14, 16, etc.
                    var pos = 12
                    while (pos <= staffPosition) {
                        val ledgerY = staffBottom - ((pos - 2) * staffSpace / 2f)
                        canvas.drawLine(ledgerStartX, ledgerY, ledgerEndX, ledgerY, ledgerPaint)
                        pos += 2
                    }
                }
                
                // === NOTEHEAD ===
                canvas.drawText(noteheadGlyph.toString(), elementX, noteY, elementPaint)
                
                // === AUGMENTATION DOTS ===
                if (note.dotted || note.doubleDotted) {
                    val dotGlyph = SMuFLGlyphs.AugmentationDots.DOT
                    // Position dots to the right of the notehead
                    // Adjust Y position: if on a line, move dot to adjacent space
                    val isOnLine = staffPosition % 2 == 0
                    val dotY = if (isOnLine) noteY - staffSpace / 2f else noteY
                    
                    val dotSpacing = staffSpace * 0.4f
                    val firstDotX = elementX + actualNoteheadWidth + dotSpacing
                    
                    // Single dot
                    canvas.drawText(dotGlyph.toString(), firstDotX, dotY, elementPaint)
                    
                    // Double dot
                    if (note.doubleDotted) {
                        val secondDotX = firstDotX + dotSpacing
                        canvas.drawText(dotGlyph.toString(), secondDotX, dotY, elementPaint)
                    }
                }
                
                // === STEM ===
                if (note.durationBeats < 4f) {
                    val stemUp = staffPosition < 6
                    val stemLength = staffSpace * 3.5f
                    val stemWidth = staffSpace * 0.12f
                    val stemPaint = android.graphics.Paint().apply {
                        this.color = color
                        strokeWidth = stemWidth
                        isAntiAlias = true
                    }
                    
                    // Notehead height approximation for proper stem connection
                    val noteheadHeight = staffSpace * 0.75f
                    
                    if (stemUp) {
                        // Stem on right side of notehead going up
                        // Use actualNoteheadWidth for precise positioning at right edge
                        val stemX = elementX + actualNoteheadWidth - stemWidth * 0.3f
                        // Start stem exactly at notehead center (vertically)
                        val stemStartY = noteY  
                        val stemEndY = noteY - stemLength
                        canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
                        
                        // Draw flag ONLY if not in a beam group
                        if (note.durationBeats < 1f && info.index !in beamedNoteIndices) {
                            val flagGlyph = SMuFLGlyphs.getFlagForDuration(note.durationBeats, true)
                            flagGlyph?.let { canvas.drawText(it.toString(), stemX, stemEndY, elementPaint) }
                        }
                    } else {
                        // Stem on left side of notehead going down
                        val stemX = elementX + stemWidth / 2f
                        // Start stem exactly at notehead center (vertically)
                        val stemStartY = noteY
                        val stemEndY = noteY + stemLength
                        canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
                        
                        if (note.durationBeats < 1f && info.index !in beamedNoteIndices) {
                            val flagGlyph = SMuFLGlyphs.getFlagForDuration(note.durationBeats, false)
                            flagGlyph?.let { canvas.drawText(it.toString(), stemX, stemEndY, elementPaint) }
                        }
                    }
                }
                
                // === ACCIDENTAL ===
                note.accidental?.let { acc ->
                    val accGlyph = acc.glyph
                    canvas.drawText(accGlyph.toString(), elementX - staffSpace * 1.2f, noteY, elementPaint)
                }
                
                // === TIE (Ligadura de Prolongamento) ===
                // Draw tie curve ONLY if this specific note has tied=true
                if (note.tied) {
                    val nextInfo = renderInfos.getOrNull(info.index + 1)
                    if (nextInfo != null && nextInfo.isNote) {
                        val staffPositionVal = staffPosition
                        val stemUp = staffPositionVal < 6
                        val curveUp = !stemUp  // Curve opposite to stem direction
                        
                        // Calculate tie endpoints - start from right edge of notehead, end at left edge of next
                        val tieStartX = elementX + actualNoteheadWidth * 0.9f
                        val tieEndX = nextInfo.x + actualNoteheadWidth * 0.1f
                        val noteheadOffset = staffSpace * 0.3f
                        val tieStartY = if (curveUp) noteY - noteheadOffset else noteY + noteheadOffset
                        val tieEndY = if (curveUp) nextInfo.y - noteheadOffset else nextInfo.y + noteheadOffset
                        
                        // Calculate curve control points for natural-looking tie
                        val distance = tieEndX - tieStartX
                        val curveHeight = minOf(distance * 0.25f, staffSpace * 1.5f) // Proportional height
                        
                        // Draw thick, tapered tie using filled path (professional look)
                        val tiePaint = android.graphics.Paint().apply {
                            this.color = lineColor
                            style = android.graphics.Paint.Style.FILL
                            isAntiAlias = true
                        }
                        
                        // Create tapered tie shape (thicker in middle, thinner at ends)
                        val tieThickness = staffSpace * 0.15f
                        val midThickness = staffSpace * 0.25f
                        
                        val tiePath = android.graphics.Path().apply {
                            // Start point (thin)
                            moveTo(tieStartX, tieStartY)
                            
                            // Control points for outer curve
                            val outerControlY = if (curveUp) tieStartY - curveHeight else tieStartY + curveHeight
                            val midX = (tieStartX + tieEndX) / 2f
                            
                            // Outer curve (top of tie)
                            cubicTo(
                                tieStartX + distance * 0.2f, outerControlY,
                                tieEndX - distance * 0.2f, if (curveUp) tieEndY - curveHeight else tieEndY + curveHeight,
                                tieEndX, tieEndY
                            )
                            
                            // Inner curve (bottom of tie) - creates thickness
                            val innerOffset = if (curveUp) midThickness else -midThickness
                            val innerControlY = if (curveUp) tieStartY - curveHeight + innerOffset else tieStartY + curveHeight + innerOffset
                            
                            cubicTo(
                                tieEndX - distance * 0.2f, if (curveUp) tieEndY - curveHeight + innerOffset else tieEndY + curveHeight + innerOffset,
                                tieStartX + distance * 0.2f, innerControlY,
                                tieStartX, tieStartY
                            )
                            
                            close()
                        }
                        canvas.drawPath(tiePath, tiePaint)
                    }
                }
            } else {
                // REST
                val restGlyph = SMuFLGlyphs.getRestForDuration(info.duration)
                canvas.drawText(restGlyph.toString(), info.x, info.y, elementPaint)
            }
        }
        
        // === DRAW BEAMS ===
        val beamThickness = staffSpace * 0.5f
        
        for ((_, group) in validBeamGroups) {
            if (group.size < 2) continue
            
            val first = group.first()
            val last = group.last()
            
            // Determine stem direction based on average staff position
            val avgPosition = group.map { it.staffPosition }.average()
            val stemUp = avgPosition < 6
            
            val stemLength = staffSpace * 3.5f
            val stemWidth = staffSpace * 0.12f
            
            // Use measured notehead width for beam group (use first note's glyph as reference)
            val firstNoteGlyph = SMuFLGlyphs.getNoteheadForDuration(first.duration)
            val beamNoteheadWidth = musicPaint.measureText(firstNoteGlyph.toString())
            
            // Calculate beam endpoints
            val firstStemX: Float
            val lastStemX: Float
            val beamY: Float
            
            if (stemUp) {
                firstStemX = first.x + beamNoteheadWidth - stemWidth * 0.3f
                lastStemX = last.x + beamNoteheadWidth - stemWidth * 0.3f
                beamY = minOf(first.y, last.y) - stemLength // Top of stems
            } else {
                firstStemX = first.x + stemWidth / 2f
                lastStemX = last.x + stemWidth / 2f
                beamY = maxOf(first.y, last.y) + stemLength // Bottom of stems
            }
            
            // Draw primary beam (rectangle)
            beamPaint.color = lineColor
            if (stemUp) {
                canvas.drawRect(firstStemX, beamY, lastStemX + stemWidth, beamY + beamThickness, beamPaint)
            } else {
                canvas.drawRect(firstStemX, beamY - beamThickness, lastStemX + stemWidth, beamY, beamPaint)
            }
            
            // Extend stems to beam for all notes in group
            for (info in group) {
                val stemX = if (stemUp) info.x + beamNoteheadWidth - stemWidth * 0.3f else info.x + stemWidth / 2f
                // Start stem at notehead center
                val stemStartY = info.y
                // End stem exactly at beam edge (not center of beam)
                val stemEndY = if (stemUp) beamY + beamThickness else beamY - beamThickness
                
                val stemPaint = android.graphics.Paint().apply {
                    color = lineColor
                    strokeWidth = stemWidth
                    isAntiAlias = true
                }
                canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
            }
            
            // Draw secondary beams for 16th notes, etc.
            // Secondary beams connect notes that need extra beam levels
            val beamGap = staffSpace * 0.25f
            
            for (i in group.indices) {
                val info = group[i]
                
                // Calculate how many beams this note needs (based on duration)
                // Eighth = 1 beam, 16th = 2 beams, 32nd = 3 beams, 64th = 4 beams
                val noteBeamCount = when {
                    info.duration >= 0.5f -> 1   // Eighth note = 1 beam
                    info.duration >= 0.25f -> 2  // 16th = 2 beams
                    info.duration >= 0.125f -> 3 // 32nd = 3 beams
                    else -> 4                     // 64th = 4 beams
                }
                
                // Primary beam is always drawn (level 1), so we handle levels 2+
                for (level in 2..noteBeamCount) {
                    val levelOffset = (beamThickness + beamGap) * (level - 1)
                    val secondaryY = if (stemUp) beamY + levelOffset else beamY - levelOffset
                    
                    val currStemX = if (stemUp) info.x + beamNoteheadWidth - stemWidth * 0.3f else info.x + stemWidth / 2f
                    
                    // Check if next note also needs this beam level
                    val nextInfo = group.getOrNull(i + 1)
                    val nextNoteBeamCount = nextInfo?.let { n ->
                        when {
                            n.duration >= 0.5f -> 1
                            n.duration >= 0.25f -> 2
                            n.duration >= 0.125f -> 3
                            else -> 4
                        }
                    } ?: 0
                    
                    if (nextInfo != null && nextNoteBeamCount >= level) {
                        // Both this note and next need this beam level - draw connecting beam
                        val nextStemX = if (stemUp) nextInfo.x + beamNoteheadWidth - stemWidth * 0.3f else nextInfo.x + stemWidth / 2f
                        
                        if (stemUp) {
                            canvas.drawRect(currStemX, secondaryY, nextStemX + stemWidth, secondaryY + beamThickness, beamPaint)
                        } else {
                            canvas.drawRect(currStemX, secondaryY - beamThickness, nextStemX + stemWidth, secondaryY, beamPaint)
                        }
                    } else {
                        // Only this note needs this beam level - draw fractional (stub) beam
                        val fractionalLength = beamNoteheadWidth * 0.8f
                        
                        // Check previous note to determine stub direction
                        val prevInfo = group.getOrNull(i - 1)
                        val prevNoteBeamCount = prevInfo?.let { n ->
                            when {
                                n.duration >= 0.5f -> 1
                                n.duration >= 0.25f -> 2
                                n.duration >= 0.125f -> 3
                                else -> 4
                            }
                        } ?: 0
                        
                        // If previous note had this level (we already drew connecting beam), skip stub
                        if (prevInfo != null && prevNoteBeamCount >= level) {
                            continue // Already connected by previous beam
                        }
                        
                        // Draw stub pointing towards the nearest note with more beams, or right by default
                        val stubToLeft = i > 0 && (nextInfo == null || prevNoteBeamCount > nextNoteBeamCount)
                        
                        if (stubToLeft) {
                            if (stemUp) {
                                canvas.drawRect(currStemX - fractionalLength, secondaryY, currStemX + stemWidth, secondaryY + beamThickness, beamPaint)
                            } else {
                                canvas.drawRect(currStemX - fractionalLength, secondaryY - beamThickness, currStemX + stemWidth, secondaryY, beamPaint)
                            }
                        } else {
                            if (stemUp) {
                                canvas.drawRect(currStemX, secondaryY, currStemX + fractionalLength + stemWidth, secondaryY + beamThickness, beamPaint)
                            } else {
                                canvas.drawRect(currStemX, secondaryY - beamThickness, currStemX + fractionalLength + stemWidth, secondaryY, beamPaint)
                            }
                        }
                    }
                }
            }
        }
        
        // === DRAW MEASURE BARLINES ===
        // Calculate and draw barlines based on accumulated beats
        val beatsPerMeasure = 4f // 4/4 time signature
        var accumulatedBeatsForBarlines = 0f
        
        for (i in renderInfos.indices) {
            accumulatedBeatsForBarlines += renderInfos[i].duration
            
            // Check if we completed a measure (4 beats)
            if ((accumulatedBeatsForBarlines % beatsPerMeasure) < 0.001f && i < renderInfos.lastIndex) {
                // Draw barline after this element
                val currentX = renderInfos[i].x
                val nextX = renderInfos[i + 1].x
                val barlineX = (currentX + nextX) / 2f + noteheadWidth / 2f
                canvas.drawLine(barlineX, startY, barlineX, staffBottom, linePaint)
            }
        }
        
        // Final barlines - position after last note, not at edge
        val lastNoteX = if (renderInfos.isNotEmpty()) {
            renderInfos.last().x + noteheadWidth
        } else {
            headerWidth
        }
        
        // Add spacing after last note for the barlines
        val finalBarlineX = lastNoteX + staffSpace * 2f
        canvas.drawLine(finalBarlineX, startY, finalBarlineX, staffBottom, linePaint)
        val thickPaint = android.graphics.Paint(linePaint).apply { strokeWidth = staffSpace * 0.35f }
        canvas.drawLine(finalBarlineX + staffSpace * 0.5f, startY, finalBarlineX + staffSpace * 0.5f, staffBottom, thickPaint)
    }
}

// ============================================
// INPUT PANEL - SVG-based with proper alignment
// ============================================
@Composable
private fun InputPanel(
    state: MelodicPerceptionState,
    typeface: Typeface,
    onNoteSelected: (NoteName) -> Unit,
    onOctaveChange: (Int) -> Unit,
    onDurationSelected: (Float) -> Unit,
    onAccidentalSelected: (AccidentalType?) -> Unit,
    onDotTypeSelected: (DotType) -> Unit,
    onToggleTied: () -> Unit,
    onToggleMetronome: () -> Unit,
    onAddNote: () -> Unit,
    onAddRest: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    onMoveNoteUp: () -> Unit,
    onMoveNoteDown: () -> Unit
) {
    val primaryColor = Color(0xFF5B4B8A)
    val secondaryColor = Color(0xFF2DD4BF)
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F6FB)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Note names + Mover + Oitava + Nav + Acidentes + Ações
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Note names + Mover (together)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Natural notes
                    val naturalNotes = listOf(
                        "C" to NoteName.C,
                        "D" to NoteName.D,
                        "E" to NoteName.E,
                        "F" to NoteName.F,
                        "G" to NoteName.G,
                        "A" to NoteName.A,
                        "B" to NoteName.B
                    )
                    naturalNotes.forEach { (label, noteName) ->
                        SelectableButton(
                            text = label,
                            isSelected = state.selectedNote == noteName,
                            onClick = { onNoteSelected(noteName) }
                        )
                    }
                    
                    // Mover - next to note selection
                    Spacer(modifier = Modifier.width(8.dp))
                    SmallIconButton(Icons.Default.KeyboardArrowDown) { onMoveNoteDown() }
                    SmallIconButton(Icons.Default.KeyboardArrowUp) { onMoveNoteUp() }
                }
                
                // Oitava
                LabeledColumn("Oitava") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallIconButton(Icons.Default.Remove) { onOctaveChange(-1) }
                        Text("${state.selectedOctave}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryColor, modifier = Modifier.padding(horizontal = 4.dp))
                        SmallIconButton(Icons.Default.Add) { onOctaveChange(1) }
                    }
                }
                
                // Nota navigation
                LabeledColumn("Nota ${state.currentNoteIndex + 1}/${maxOf(state.userNotes.size, 1)}") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallIconButton(Icons.Default.ChevronLeft) { onNavigatePrevious() }
                        SmallIconButton(Icons.Default.ChevronRight) { onNavigateNext() }
                    }
                }
                
                // Accidentals
                LabeledColumn("Acidente") {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        AccidentalButton("♭", state.selectedAccidental == AccidentalType.FLAT) { 
                            onAccidentalSelected(if (state.selectedAccidental == AccidentalType.FLAT) null else AccidentalType.FLAT) 
                        }
                        AccidentalButton("♯", state.selectedAccidental == AccidentalType.SHARP) { 
                            onAccidentalSelected(if (state.selectedAccidental == AccidentalType.SHARP) null else AccidentalType.SHARP) 
                        }
                    }
                }
                
                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActionBtn("+ Nota", primaryColor, onAddNote)
                    ActionBtn("+ Pausa", secondaryColor, onAddRest)
                }
            }
            
            // Row 2: Durations + Rests
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Note durations + separator + Rest durations
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Note durations with SVG
                    SvgDurationButton("whole", 4f, state.selectedDuration, onDurationSelected, context)
                    SvgDurationButton("half", 2f, state.selectedDuration, onDurationSelected, context)
                    SvgDurationButton("quarter", 1f, state.selectedDuration, onDurationSelected, context)
                    SvgDurationButton("eighth", 0.5f, state.selectedDuration, onDurationSelected, context)
                    SvgDurationButton("sixteenth", 0.25f, state.selectedDuration, onDurationSelected, context)
                    SvgDurationButton("thirtysecond", 0.125f, state.selectedDuration, onDurationSelected, context)
                    SvgDurationButton("sixtyfourth", 0.0625f, state.selectedDuration, onDurationSelected, context)
                    
                    // Separator
                    Text("|", color = Color.Gray, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    
                    // Rest durations with SVG
                    SvgRestButton("whole", 4f, state.selectedDuration, onDurationSelected, context)
                    SvgRestButton("half", 2f, state.selectedDuration, onDurationSelected, context)
                    SvgRestButton("quarter", 1f, state.selectedDuration, onDurationSelected, context)
                    SvgRestButton("eighth", 0.5f, state.selectedDuration, onDurationSelected, context)
                    SvgRestButton("sixteenth", 0.25f, state.selectedDuration, onDurationSelected, context)
                    SvgRestButton("thirtysecond", 0.125f, state.selectedDuration, onDurationSelected, context)
                    SvgRestButton("sixtyfourth", 0.0625f, state.selectedDuration, onDurationSelected, context)
                }
            }
            
            // Row 3: Augmentation dots and Tie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metronome, Dot buttons, and Tie
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Metronome toggle button
                    MetronomeButton(
                        isEnabled = state.metronomeEnabled,
                        onClick = onToggleMetronome
                    )
                    
                    // Separator
                    Text("|", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    
                    // Single dot button
                    DotButton(
                        symbol = "•",
                        label = "Ponto",
                        isSelected = state.selectedDotType == DotType.SINGLE,
                        onClick = { 
                            onDotTypeSelected(if (state.selectedDotType == DotType.SINGLE) DotType.NONE else DotType.SINGLE) 
                        }
                    )
                    
                    // Double dot button
                    DotButton(
                        symbol = "••",
                        label = "Duplo Ponto",
                        isSelected = state.selectedDotType == DotType.DOUBLE,
                        onClick = { 
                            onDotTypeSelected(if (state.selectedDotType == DotType.DOUBLE) DotType.NONE else DotType.DOUBLE) 
                        }
                    )
                    
                    // Separator
                    Text("|", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    
                    // Tie button - check if current note is tied
                    val currentNoteTied = if (state.currentNoteIndex >= 0 && state.currentNoteIndex < state.userNotes.size) {
                        val currentElement = state.userNotes[state.currentNoteIndex]
                        (currentElement as? Note)?.tied ?: false
                    } else false
                    
                    TieButton(
                        isSelected = currentNoteTied,
                        onClick = onToggleTied
                    )
                }
            }
        }
    }
}

// Dot button for augmentation dots
@Composable
private fun DotButton(
    symbol: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = Color(0xFFF97316)
    val buttonColor = Color(0xFFE8E4EF)
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) selectedColor else buttonColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            text = "$symbol $label",
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}

// Metronome button
@Composable
private fun MetronomeButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val enabledColor = Color(0xFF5B4B8A)
    val disabledColor = Color(0xFFE8E4EF)
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEnabled) enabledColor else disabledColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(
            Icons.Default.Timer,
            contentDescription = "Metrônomo",
            modifier = Modifier.size(16.dp),
            tint = if (isEnabled) Color.White else Color.Gray
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isEnabled) "Metrônomo ON" else "Metrônomo",
            fontSize = 11.sp,
            color = if (isEnabled) Color.White else Color.Black
        )
    }
}

// Tie button for slurs/ties
@Composable
private fun TieButton(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = Color(0xFFF97316)
    val buttonColor = Color(0xFFE8E4EF)
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) selectedColor else buttonColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            text = "⌒ Ligadura",
            fontSize = 12.sp,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}

// ============================================
// SVG-BASED DURATION BUTTONS
// ============================================

@Composable
private fun SvgDurationButton(
    noteName: String, // "whole", "half", "quarter", etc.
    duration: Float,
    selectedDuration: Float,
    onSelect: (Float) -> Unit,
    context: android.content.Context
) {
    val isSelected = selectedDuration == duration
    val selectedColor = Color(0xFFF97316)
    val buttonColor = Color(0xFFE8E4EF)
    
    // Map to asset filename
    val fileName = when (noteName) {
        "sixtyfourth" -> "Sixtyfourth-note.svg"
        else -> "Music-${noteName}note.svg"
    }
    
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) selectedColor else buttonColor)
            .clickable { onSelect(duration) },
        contentAlignment = Alignment.Center
    ) {
        coil.compose.AsyncImage(
            model = coil.request.ImageRequest.Builder(context)
                .data("file:///android_asset/rhythmic_figures/$fileName")
                .decoderFactory(coil.decode.SvgDecoder.Factory())
                .build(),
            contentDescription = "$noteName note",
            modifier = Modifier.size(36.dp),
            colorFilter = if (isSelected) 
                androidx.compose.ui.graphics.ColorFilter.tint(Color.White) 
            else 
                androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF5B4B8A))
        )
    }
}

@Composable
private fun SvgRestButton(
    restName: String, // "whole", "half", "quarter", etc.
    duration: Float,
    selectedDuration: Float,
    onSelect: (Float) -> Unit,
    context: android.content.Context
) {
    val isSelected = selectedDuration == duration
    val selectedColor = Color(0xFFF97316)
    val buttonColor = Color(0xFFE8E4EF)
    
    val fileName = "Music-${restName}rest.svg"
    
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) selectedColor else buttonColor)
            .clickable { onSelect(duration) },
        contentAlignment = Alignment.Center
    ) {
        coil.compose.AsyncImage(
            model = coil.request.ImageRequest.Builder(context)
                .data("file:///android_asset/rhythmic_figures/$fileName")
                .decoderFactory(coil.decode.SvgDecoder.Factory())
                .build(),
            contentDescription = "$restName rest",
            modifier = Modifier.size(36.dp),
            colorFilter = if (isSelected) 
                androidx.compose.ui.graphics.ColorFilter.tint(Color.White) 
            else 
                androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF5B4B8A))
        )
    }
}

// ============================================
// HELPER COMPONENTS (with horizontal alignment)
// ============================================

@Composable
private fun LabeledRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
private fun LabeledColumn(label: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun SelectableButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFFF97316) else Color(0xFFE8E4EF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSelected) Color.White else Color(0xFF5B4B8A))
    }
}

@Composable
private fun AccidentalButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFFF97316) else Color(0xFFE8E4EF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isSelected) Color.White else Color(0xFF5B4B8A))
    }
}

@Composable
private fun SmallIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color(0xFF5B4B8A), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ActionBtn(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ============================================
// COMPLETE SCREEN
// ============================================

@Composable
private fun CompleteScreen(
    correctCount: Int,
    totalCount: Int,
    onContinue: () -> Unit
) {
    val percentage = if (totalCount > 0) (correctCount.toFloat() / totalCount * 100).toInt() else 0
    val grade = when {
        percentage >= 90 -> "Excelente! 🎉"
        percentage >= 70 -> "Muito Bom! 🌟"
        percentage >= 50 -> "Bom trabalho! 👍"
        else -> "Continue praticando! 💪"
    }
    
    val isVictory = percentage >= 50
    
    // Confetti animation state
    val confettiColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFF6B6B), // Red
        Color(0xFF4ECDC4), // Teal
        Color(0xFF95E1D3), // Mint
        Color(0xFFF38181), // Pink
        Color(0xFFAA96DA), // Purple
        Color(0xFFFCBF49), // Orange
        Color(0xFF2DD4BF)  // Cyan
    )
    
    // Animate confetti
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val confettiProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )
    
    // Generate random confetti positions
    val confettiParticles = remember {
        List(50) {
            ConfettiParticle(
                startX = kotlin.random.Random.nextFloat(),
                startY = kotlin.random.Random.nextFloat() * -0.3f - 0.1f,
                endY = 1.2f,
                size = kotlin.random.Random.nextFloat() * 12f + 6f,
                colorIndex = kotlin.random.Random.nextInt(confettiColors.size),
                speed = kotlin.random.Random.nextFloat() * 0.5f + 0.5f,
                rotation = kotlin.random.Random.nextFloat() * 360f
            )
        }
    }
    
    // Scale animation for star
    val starScale by animateFloatAsState(
        targetValue = if (isVictory) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 300f),
        label = "star_scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5B4B8A), // Purple
                        Color(0xFF3B82F6)  // Blue
                    )
                )
            )
    ) {
        // Confetti canvas
        if (isVictory) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                confettiParticles.forEach { particle ->
                    val progress = (confettiProgress + particle.speed) % 1f
                    val x = particle.startX * canvasWidth + 
                            kotlin.math.sin(progress * 6.28f + particle.startX * 10) * 30f
                    val y = particle.startY * canvasHeight + 
                            progress * (particle.endY - particle.startY) * canvasHeight
                    
                    if (y > -50 && y < canvasHeight + 50) {
                        rotate(
                            degrees = particle.rotation + progress * 360f,
                            pivot = androidx.compose.ui.geometry.Offset(x, y)
                        ) {
                            drawRect(
                                color = confettiColors[particle.colorIndex],
                                topLeft = androidx.compose.ui.geometry.Offset(x - particle.size / 2, y - particle.size / 2),
                                size = androidx.compose.ui.geometry.Size(particle.size, particle.size * 0.6f)
                            )
                        }
                    }
                }
            }
        }
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Star badge
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(starScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isVictory) 
                                listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
                            else 
                                listOf(Color(0xFF9CA3AF), Color(0xFF6B7280))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isVictory) Icons.Default.Star else Icons.Default.Refresh, 
                    contentDescription = null, 
                    tint = Color.White, 
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Grade text
            Text(
                text = grade, 
                style = MaterialTheme.typography.headlineLarge, 
                fontWeight = FontWeight.Bold, 
                color = Color.White
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Score card
            Card(
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$percentage%", 
                        style = MaterialTheme.typography.displayLarge, 
                        fontWeight = FontWeight.Bold, 
                        color = if (isVictory) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "$correctCount de $totalCount corretas", 
                        style = MaterialTheme.typography.titleLarge, 
                        color = Color.White
                    )
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            // Continue button
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVictory) Color(0xFF22C55E) else Color(0xFFF59E0B)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Continuar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

// Data class for confetti particles
private data class ConfettiParticle(
    val startX: Float,
    val startY: Float,
    val endY: Float,
    val size: Float,
    val colorIndex: Int,
    val speed: Float,
    val rotation: Float
)
