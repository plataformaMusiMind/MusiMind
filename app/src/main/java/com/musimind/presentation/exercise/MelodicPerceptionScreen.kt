package com.musimind.presentation.exercise

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.music.notation.model.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.musimind.music.notation.smufl.SMuFLGlyphs
import kotlin.math.max

/**
 * Melodic Perception exercise screen
 * 
 * Users input notes they hear and get feedback on correctness.
 * Uses unified feedback (entire note is green or red).
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
    
    // Force landscape orientation
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    // Load exercise
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
                MelodicPerceptionCompleteContent(
                    correctCount = state.correctCount,
                    totalCount = state.targetNotes.size,
                    onContinue = { onComplete(state.correctCount, state.targetNotes.size) }
                )
            }
            else -> {
                MelodicPerceptionContent(
                    state = state,
                    onBack = onBack,
                    onNoteSelected = { viewModel.selectNote(it) },
                    onOctaveChange = { viewModel.changeOctave(it) },
                    onDurationSelected = { viewModel.selectDuration(it) },
                    onAccidentalSelected = { viewModel.selectAccidental(it) },
                    onAddNote = { viewModel.addNote() },
                    onAddRest = { viewModel.addRest() },
                    onVerify = { viewModel.verify() },
                    onUndo = { viewModel.undoLastNote() },
                    onPlayMelody = { viewModel.playMelody() },
                    onPlayUserNotes = { viewModel.playUserNotes() },
                    onHelp = { viewModel.showHelp() },
                    onNavigatePrevious = { viewModel.previousNote() },
                    onNavigateNext = { viewModel.nextNote() }
                )
            }
        }
    }
}

@Composable
private fun MelodicPerceptionContent(
    state: MelodicPerceptionState,
    onBack: () -> Unit,
    onNoteSelected: (NoteName) -> Unit,
    onOctaveChange: (Int) -> Unit,
    onDurationSelected: (Float) -> Unit,
    onAccidentalSelected: (AccidentalType?) -> Unit,
    onAddNote: () -> Unit,
    onAddRest: () -> Unit,
    onVerify: () -> Unit,
    onUndo: () -> Unit,
    onPlayMelody: () -> Unit,
    onPlayUserNotes: () -> Unit,
    onHelp: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        MelodicPerceptionHeader(
            title = state.exerciseTitle,
            onBack = onBack,
            onHelp = onHelp,
            onUndo = onUndo,
            onPlayMelody = onPlayMelody,
            onPlayUserNotes = onPlayUserNotes
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Score view area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(16.dp)
        ) {
            UserNotesScoreView(
                notes = state.userNotes,
                currentNoteIndex = state.currentNoteIndex,
                feedbackResults = state.feedbackResults,
                clef = state.clef,
                showFeedback = state.showFeedback
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Feedback message
        AnimatedVisibility(
            visible = state.feedbackMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.allCorrect) 
                        Color(0xFF22C55E).copy(alpha = 0.2f) 
                    else 
                        Color(0xFFEF4444).copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (state.allCorrect) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (state.allCorrect) Color(0xFF22C55E) else Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.feedbackMessage ?: "",
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Input panel
        NoteInputPanel(
            selectedNote = state.selectedNote,
            selectedOctave = state.selectedOctave,
            selectedDuration = state.selectedDuration,
            selectedAccidental = state.selectedAccidental,
            currentNoteNumber = state.currentNoteIndex + 1,
            totalNotes = maxOf(state.userNotes.size, 1),
            onNoteSelected = onNoteSelected,
            onOctaveChange = onOctaveChange,
            onDurationSelected = onDurationSelected,
            onAccidentalSelected = onAccidentalSelected,
            onAddNote = onAddNote,
            onAddRest = onAddRest,
            onVerify = onVerify,
            onNavigatePrevious = onNavigatePrevious,
            onNavigateNext = onNavigateNext
        )
    }
}

@Composable
private fun MelodicPerceptionHeader(
    title: String,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onUndo: () -> Unit,
    onPlayMelody: () -> Unit,
    onPlayUserNotes: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Voltar",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        
        Row {
            IconButton(onClick = onHelp) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = "Ajuda",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onUndo) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Desfazer",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            // Play exercise button
            IconButton(onClick = onPlayMelody) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Ouvir exercício",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // Play user's notes button
            IconButton(onClick = onPlayUserNotes) {
                Icon(
                    imageVector = Icons.Default.Hearing,
                    contentDescription = "Ouvir resposta",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun UserNotesScoreView(
    notes: List<MusicElement>,
    currentNoteIndex: Int,
    feedbackResults: Map<Int, Boolean>,
    clef: ClefType,
    showFeedback: Boolean
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val context = LocalContext.current
    
    val typeface = remember {
        try {
            android.graphics.Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            android.graphics.Typeface.DEFAULT
        }
    }
    
    // Calculate total slots based on notes size, ensuring at least some space
    // If empty, we might want to show some empty measure lines
    
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        
        val staffHeight = size.height * 0.4f
        val staffSpace = staffHeight / 4f
        val verticalCenter = size.height / 2f
        val startY = verticalCenter - staffHeight / 2f
        
        // Colors
        val lineColor = android.graphics.Color.BLACK
        val highlightColor = android.graphics.Color.rgb(59, 130, 246) // Blue
        val correctColor = android.graphics.Color.rgb(34, 197, 94) // Green
        val incorrectColor = android.graphics.Color.rgb(239, 68, 68) // Red
        val neutralColor = android.graphics.Color.BLACK
        
        // Paints
        val linePaint = android.graphics.Paint().apply {
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
        
        // Draw 5 staff lines continuously
        val clefWidth = staffSpace * 3.5f
        val tsWidth = staffSpace * 2f
        val headerWidth = clefWidth + tsWidth
        
        // Dynamic spacing: if few notes, give them some space but don't stretch too much
        // Effectively min 4 slots or actual size
        val effectiveSize = maxOf(notes.size, 8) 
        val availableWidth = size.width - headerWidth - staffSpace * 2
        val noteSpacing = availableWidth / effectiveSize
        
        for (i in 0 until 5) {
            val y = startY + i * staffSpace
            canvas.drawLine(0f, y, size.width, y, linePaint)
        }
        
        // Draw clef
        val clefGlyph = when (clef) {
            ClefType.TREBLE -> com.musimind.music.notation.smufl.SMuFLGlyphs.Clefs.TREBLE
            ClefType.BASS -> com.musimind.music.notation.smufl.SMuFLGlyphs.Clefs.BASS
            else -> com.musimind.music.notation.smufl.SMuFLGlyphs.Clefs.TREBLE
        }
        val clefY = when (clef) {
            ClefType.TREBLE -> startY + staffSpace * 3f
            ClefType.BASS -> startY + staffSpace * 1f
            else -> startY + staffSpace * 2f
        }
        canvas.drawText(clefGlyph.toString(), staffSpace * 0.3f, clefY, musicPaint)
        
        // Draw time signature (4/4 default for now)
        val tsGlyph = com.musimind.music.notation.smufl.SMuFLGlyphs.TimeSignatures.COMMON
        canvas.drawText(tsGlyph.toString(), clefWidth, verticalCenter, musicPaint)
        
        // Draw Notes & Rests
        if (notes.isEmpty()) {
            // Draw dummy barlines to show "empty" score
            val measureWidth = noteSpacing * 4
            for (i in 1..4) {
                 val barX = headerWidth + i * measureWidth
                 if (barX < size.width) {
                     canvas.drawLine(barX, startY, barX, startY + staffHeight, linePaint)
                 }
            }
            
            // Draw final barline
            canvas.drawLine(size.width - staffSpace, startY, size.width - staffSpace, startY + staffHeight, linePaint)
            val thickPaint = android.graphics.Paint(linePaint).apply { strokeWidth = staffSpace * 0.4f }
            canvas.drawLine(size.width - staffSpace * 0.4f, startY, size.width - staffSpace * 0.4f, startY + staffHeight, thickPaint)
            
        } else {
            notes.forEachIndexed { index, element ->
                val elementX = headerWidth + index * noteSpacing + noteSpacing / 2
                
                // Determine color
                val isCorrect = feedbackResults[index]
                val color = when {
                    showFeedback && isCorrect == true -> correctColor
                    showFeedback && isCorrect == false -> incorrectColor
                    index == currentNoteIndex -> highlightColor
                    else -> neutralColor
                }
                
                val elementPaint = android.graphics.Paint(musicPaint).apply {
                    this.color = color
                }
                
                when (element) {
                    is Note -> {
                        // Calculate Y position
                        val staffPosition = element.pitch.staffPosition(clef)
                        val noteY = startY + staffHeight - (staffPosition * staffSpace / 2f) + staffSpace / 2f
                        
                        // Ledger lines
                        if (staffPosition < 0 || staffPosition > 8) {
                            val noteheadWidth = staffSpace * 1.18f
                            val noteCenterX = elementX + noteheadWidth / 2f
                            val ledgerWidth = staffSpace * 2.2f
                            val ledgerStartX = noteCenterX - ledgerWidth / 2f
                            val ledgerEndX = noteCenterX + ledgerWidth / 2f
                            
                            val ledgerPaint = android.graphics.Paint(linePaint).apply { this.color = color }
                            
                            if (staffPosition < 0) {
                                val ledgerCount = (-staffPosition + 1) / 2
                                for (j in 1..ledgerCount) {
                                    val ledgerY = startY + staffHeight + j * staffSpace
                                    canvas.drawLine(ledgerStartX, ledgerY, ledgerEndX, ledgerY, ledgerPaint)
                                }
                            } else if (staffPosition > 8) {
                                val ledgerCount = (staffPosition - 8 + 1) / 2
                                for (j in 1..ledgerCount) {
                                    val ledgerY = startY - j * staffSpace
                                    canvas.drawLine(ledgerStartX, ledgerY, ledgerEndX, ledgerY, ledgerPaint)
                                }
                            }
                        }
                        
                        // Notehead
                        val noteheadGlyph = com.musimind.music.notation.smufl.SMuFLGlyphs.getNoteheadForDuration(element.durationBeats)
                        canvas.drawText(noteheadGlyph.toString(), elementX, noteY, elementPaint)
                        
                        // Stem
                        if (element.durationBeats < 4f) {
                            val stemUp = staffPosition < 4
                            val stemLength = staffSpace * 3.5f
                            val noteheadWidth = staffSpace * 1.18f
                            
                            val stemPaint = android.graphics.Paint().apply {
                                this.color = color
                                strokeWidth = staffSpace * 0.13f
                                style = android.graphics.Paint.Style.STROKE
                                isAntiAlias = true
                            }
                            
                            if (stemUp) {
                                val stemX = elementX + noteheadWidth - (staffSpace * 0.02f)
                                val stemStartY = noteY - staffSpace * 0.15f
                                val stemEndY = stemStartY - stemLength
                                canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
                                
                                // Flag
                                val flagGlyph = SMuFLGlyphs.getFlagForDuration(element.durationBeats, true)
                                flagGlyph?.let {
                                    canvas.drawText(it.toString(), stemX - staffSpace * 0.08f, stemEndY, elementPaint)
                                }
                            } else {
                                val stemX = elementX + (staffSpace * 0.02f)
                                val stemStartY = noteY + staffSpace * 0.15f
                                val stemEndY = stemStartY + stemLength
                                canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
                                
                                // Flag
                                val flagGlyph = SMuFLGlyphs.getFlagForDuration(element.durationBeats, false)
                                flagGlyph?.let {
                                    canvas.drawText(it.toString(), stemX - staffSpace * 0.08f, stemEndY, elementPaint)
                                }
                            }
                        }
                        
                        // Accidental
                        if (element.accidental != null) {
                            val accGlyph = element.accidental.glyph
                            val accSpace = staffSpace * 1.5f
                            canvas.drawText(accGlyph.toString(), elementX - accSpace, noteY, elementPaint)
                        }
                    }
                    is Rest -> {
                        // Rest Y position usually center line (line 2, index 2) -> startY + 2*staffSpace
                        // or shifted manually based on type. WHOLE rest hangs from line 1 (index 1), HALF sits on line 2 (index 2).
                        // SMuFL rests are usually designed to be placed at a specific baseline.
                        // Standard Rests:
                        // Quarter: center.
                        // Eighth: center.
                        // Whole: usually below 2nd line from top?
                        // Let's approximate to center staff for simpler rendering, or use standard positioning.
                        
                        // Center of staff
                        val restY = verticalCenter + (staffSpace * 1f) // Slight adjustment for baseline
                        val restGlyph = com.musimind.music.notation.smufl.SMuFLGlyphs.getRestForDuration(element.durationBeats)
                        
                        canvas.drawText(restGlyph.toString(), elementX, restY, elementPaint)
                    }
                    else -> {}
                }
                
                // Draw barlines
                if (index > 0) {
                    var totalBeats = 0f
                    for (i in 0..index) {
                        val dur = when(val item = notes[i]) {
                            is Note -> item.durationBeats
                            is Rest -> item.durationBeats
                            else -> 0f
                        }
                        totalBeats += dur
                    }
                    if (totalBeats.toInt() % 4 == 0 && index < notes.size - 1) {
                         val barX = elementX + noteSpacing / 2
                         canvas.drawLine(barX, startY, barX, startY + staffHeight, linePaint)
                    }
                }
            }
            
            // Final barline
            canvas.drawLine(size.width - staffSpace, startY, size.width - staffSpace, startY + staffHeight, linePaint)
            val thickPaint = android.graphics.Paint(linePaint).apply { strokeWidth = staffSpace * 0.4f }
            canvas.drawLine(size.width - staffSpace * 0.4f, startY, size.width - staffSpace * 0.4f, startY + staffHeight, thickPaint)
        }
    }
}

@Composable
private fun MelodicPerceptionCompleteContent(
    correctCount: Int,
    totalCount: Int,
    onContinue: () -> Unit
) {
    val percentage = if (totalCount > 0) (correctCount.toFloat() / totalCount * 100).toInt() else 0
    val grade = when {
        percentage >= 90 -> "Excelente!"
        percentage >= 70 -> "Muito Bom!"
        percentage >= 50 -> "Bom trabalho!"
        else -> "Continue praticando!"
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFBBF24),
                            Color(0xFFF59E0B)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = grade,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "$correctCount de $totalCount corretas",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
        
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF22C55E)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF22C55E)
            )
        ) {
            Text("Continuar", style = MaterialTheme.typography.titleMedium)
        }
    }
}
