package com.musimind.presentation.exercise

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.musimind.music.audio.core.SolfegeFeedbackState
import com.musimind.music.audio.core.SolfegePhase
import com.musimind.music.notation.smufl.SMuFLGlyphs
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.music.audio.ui.*
import com.musimind.music.notation.model.*
import com.musimind.music.notation.ui.*

/**
 * Solfege exercise screen - redesigned based on reference
 * 
 * Features:
 * - Header with title, progress, key, time signature
 * - Continuous score view with all notes on single staff
 * - Bottom panel with octave control, listen/solfege buttons, tempo, toggles
 * - Separate feedback for pitch (notehead) and duration (stem)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolfegeExerciseScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onComplete: (score: Int, total: Int) -> Unit,
    viewModel: SolfegeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val audioFeedback by viewModel.audioFeedbackState.collectAsState()
    val context = LocalContext.current
    
    // Force landscape orientation for better score visualization
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    // Load exercise on launch
    LaunchedEffect(Unit) {
        viewModel.loadExercise(exerciseId)
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.isComplete -> {
                ExerciseCompleteContent(
                    correctCount = state.correctCount,
                    totalCount = state.totalNotes,
                    onContinue = { onComplete(state.correctCount, state.totalNotes) }
                )
            }
            else -> {
                SolfegeExerciseContent(
                    state = state,
                    audioFeedback = audioFeedback,
                    onBack = onBack,
                    onStartListening = { 
                        if (viewModel.hasPermission()) {
                            viewModel.startListening() 
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopListening = { viewModel.stopListening() },
                    onPlayNote = { viewModel.playCurrentNote() },
                    onPlayMelody = { viewModel.playMelody() },
                    onStopPlayback = { viewModel.stopPlayback() },
                    onNext = { viewModel.nextNote() },
                    onPrevious = { viewModel.previousNote() },
                    onOctaveChange = { viewModel.changeOctave(it) },
                    onToggleBeatNumbers = { viewModel.toggleBeatNumbers() },
                    onToggleSolfegeNames = { viewModel.toggleSolfegeNames() }
                )
            }
        }
    }
}

@Composable
private fun SolfegeExerciseContent(
    state: SolfegeState,
    audioFeedback: SolfegeFeedbackState,
    onBack: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onPlayNote: () -> Unit,
    onPlayMelody: () -> Unit,
    onStopPlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOctaveChange: (Int) -> Unit,
    onToggleBeatNumbers: () -> Unit,
    onToggleSolfegeNames: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header
        SolfegeHeader(
            exerciseTitle = state.exerciseTitle,
            currentStep = state.currentNoteIndex + 1,
            totalSteps = state.totalNotes,
            keySignature = state.keySignature,
            timeSignature = state.timeSignature,
            onBack = onBack,
            onPrevious = onPrevious,
            onNext = onNext
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Continuous Score view - single staff with all notes
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            ContinuousScoreView(
                notes = state.notes,
                currentNoteIndex = state.currentNoteIndex,
                showBeatNumbers = state.showBeatNumbers,
                showSolfegeNames = state.showSolfegeNames,
                clef = state.clef,
                timeSignature = state.timeSignature,
                keySignature = state.keySignature
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Pitch indicator when listening
        AnimatedVisibility(
            visible = state.isListening,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            CompactPitchDisplay(
                pitchResult = state.currentPitchResult,
                targetNote = state.currentNote?.pitch?.let {
                    com.musimind.music.audio.pitch.PitchUtils.pitchToDisplayString(it)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bottom control panel
        SolfegeControlPanel(
            isListening = state.isListening,
            isPlaying = state.isPlaying,
            currentOctave = state.currentOctave,
            tempo = state.tempo,
            currentBeat = state.currentBeat,
            phase = audioFeedback.phase,
            showBeatNumbers = state.showBeatNumbers,
            showSolfegeNames = state.showSolfegeNames,
            statusText = state.statusText,
            onOctaveChange = onOctaveChange,
            onPlayMelody = onPlayMelody,
            onStopPlayback = onStopPlayback,
            onStartSolfege = onStartListening,
            onStopSolfege = onStopListening,
            onToggleBeatNumbers = onToggleBeatNumbers,
            onToggleSolfegeNames = onToggleSolfegeNames
        )
    }
}

@Composable
private fun SolfegeHeader(
    exerciseTitle: String,
    currentStep: Int,
    totalSteps: Int,
    keySignature: String,
    timeSignature: String,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Voltar"
            )
        }
        
        // Title and info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Solfejo $currentStep/$totalSteps $exerciseTitle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tom: $keySignature | $timeSignature",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        // Navigation arrows
        Row {
            IconButton(
                onClick = onPrevious,
                enabled = currentStep > 1
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Anterior",
                    tint = if (currentStep > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            IconButton(
                onClick = onNext,
                enabled = currentStep < totalSteps
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Próximo",
                    tint = if (currentStep < totalSteps) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * Continuous score view with single staff for all notes
 */
@Composable
private fun ContinuousScoreView(
    notes: List<Note>,
    currentNoteIndex: Int,
    showBeatNumbers: Boolean,
    showSolfegeNames: Boolean,
    clef: ClefType,
    timeSignature: String,
    keySignature: String
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
        
        val textPaint = android.graphics.Paint().apply {
            textSize = staffSpace * 0.8f
            color = android.graphics.Color.GRAY
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        
        // Draw 5 staff lines continuously
        val clefWidth = staffSpace * 3.5f
        val tsWidth = staffSpace * 2f
        val headerWidth = clefWidth + tsWidth
        
        // Calculate dynamic spacing
        // To ensure equal spacing between all elements including barlines,
        // we essentially divide the remaining width by (notes.size)
        // Note: noteX is center of the note. 
        val availableWidth = size.width - headerWidth - staffSpace // Reserve space at end
        val noteSpacing = availableWidth / maxOf(notes.size, 1)
        
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
        
        // Draw time signature (C for 4/4)
        val tsX = clefWidth
        if (timeSignature == "4/4") {
            val tsGlyph = com.musimind.music.notation.smufl.SMuFLGlyphs.TimeSignatures.COMMON
            canvas.drawText(tsGlyph.toString(), tsX, verticalCenter, musicPaint)
        } else {
            // Draw numerator and denominator
            val parts = timeSignature.split("/")
            if (parts.size == 2) {
                val numPaint = android.graphics.Paint(musicPaint).apply {
                    textSize = staffSpace * 2.5f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.drawText(parts[0], tsX + staffSpace, startY + staffSpace * 1.2f, numPaint)
                canvas.drawText(parts[1], tsX + staffSpace, startY + staffSpace * 3.2f, numPaint)
            }
        }
        
        // Draw notes on continuous staff
        notes.forEachIndexed { index, note ->
            val noteX = headerWidth + index * noteSpacing + noteSpacing / 2
            
            // Calculate Y position
            val staffPosition = note.pitch.staffPosition(clef)
            val noteY = startY + staffHeight - (staffPosition * staffSpace / 2f) + staffSpace / 2f
            
            // Determine colors based on feedback
            val noteheadColor = when (note.pitchFeedback) {
                FeedbackState.CORRECT -> correctColor
                FeedbackState.INCORRECT -> incorrectColor
                else -> if (index == currentNoteIndex) highlightColor else lineColor
            }
            
            val stemColor = when (note.durationFeedback) {
                FeedbackState.CORRECT -> correctColor
                FeedbackState.INCORRECT -> incorrectColor
                else -> if (index == currentNoteIndex) highlightColor else lineColor
            }
            
            val notePaint = android.graphics.Paint(musicPaint).apply {
                color = noteheadColor
            }
            
            // Draw beat number above (aligned to note center)
            if (showBeatNumbers && note.beatNumber != null) {
                val beatText = if (note.beatNumber == note.beatNumber.toInt().toFloat()) {
                    note.beatNumber.toInt().toString()
                } else {
                    String.format("%.1f", note.beatNumber)
                }
                // Center text above note based on notehead width
                val noteheadWidth = staffSpace * 1.18f
                canvas.drawText(beatText, noteX + noteheadWidth / 2, startY - staffSpace * 0.5f, textPaint)
            }
            
            // Draw ledger lines
            if (staffPosition < 0 || staffPosition > 8) {
                val noteheadWidth = staffSpace * 1.18f
                val noteCenterX = noteX + noteheadWidth / 2f
                val ledgerWidth = staffSpace * 2.2f // Wider ledger line
                val ledgerStartX = noteCenterX - ledgerWidth / 2f
                val ledgerEndX = noteCenterX + ledgerWidth / 2f
                
                if (staffPosition < 0) {
                    val ledgerCount = (-staffPosition + 1) / 2
                    for (j in 1..ledgerCount) {
                        val ledgerY = startY + staffHeight + j * staffSpace
                        canvas.drawLine(ledgerStartX, ledgerY, ledgerEndX, ledgerY, linePaint)
                    }
                } else if (staffPosition > 8) {
                    val ledgerCount = (staffPosition - 8 + 1) / 2
                    for (j in 1..ledgerCount) {
                        val ledgerY = startY - j * staffSpace
                        canvas.drawLine(ledgerStartX, ledgerY, ledgerEndX, ledgerY, linePaint)
                    }
                }
            }
            
            // Draw notehead
            val noteheadGlyph = when {
                note.durationBeats >= 4f -> com.musimind.music.notation.smufl.SMuFLGlyphs.Noteheads.WHOLE
                note.durationBeats >= 2f -> com.musimind.music.notation.smufl.SMuFLGlyphs.Noteheads.HALF
                else -> com.musimind.music.notation.smufl.SMuFLGlyphs.Noteheads.BLACK
            }
            canvas.drawText(noteheadGlyph.toString(), noteX, noteY, notePaint)
            
            // Draw stem with proper connection
            if (note.durationBeats < 4f) {
                val stemUp = staffPosition < 4
                val stemLength = staffSpace * 3.5f
                val noteheadWidth = staffSpace * 1.18f
                
                val stemPaint = android.graphics.Paint().apply {
                    color = stemColor
                    strokeWidth = staffSpace * 0.13f
                    style = android.graphics.Paint.Style.STROKE
                    isAntiAlias = true
                }
                
                if (stemUp) {
                    // Stem on right side going up
                    val stemX = noteX + noteheadWidth - (staffSpace * 0.02f)
                    val stemStartY = noteY - staffSpace * 0.15f
                    val stemEndY = stemStartY - stemLength
                    canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
                    
                    // Draw flag if needed
                    if (note.durationBeats < 1f) {
                        val flagGlyph = SMuFLGlyphs.getFlagForDuration(note.durationBeats, true)
                        flagGlyph?.let {
                            val flagPaint = android.graphics.Paint(musicPaint).apply { color = stemColor }
                            canvas.drawText(it.toString(), stemX - staffSpace * 0.08f, stemEndY, flagPaint)
                        }
                    }
                } else {
                    // Stem on left side going down
                    val stemX = noteX + (staffSpace * 0.02f)
                    val stemStartY = noteY + staffSpace * 0.15f
                    val stemEndY = stemStartY + stemLength
                    canvas.drawLine(stemX, stemStartY, stemX, stemEndY, stemPaint)
                    
                    // Draw flag if needed
                    if (note.durationBeats < 1f) {
                        val flagGlyph = SMuFLGlyphs.getFlagForDuration(note.durationBeats, false)
                        flagGlyph?.let {
                            val flagPaint = android.graphics.Paint(musicPaint).apply { color = stemColor }
                            canvas.drawText(it.toString(), stemX - staffSpace * 0.08f, stemEndY, flagPaint)
                        }
                    }
                }
            }
            
            // Draw solfege name below - with EXTRA padding to avoid overlapping stems
            if (showSolfegeNames && note.solfegeName != null) {
                // Fixed position low enough to clear most downward stems
                val textY = startY + staffHeight + staffSpace * 5f
                val noteheadWidth = staffSpace * 1.18f
                canvas.drawText(note.solfegeName, noteX + noteheadWidth / 2, textY, textPaint)
            }
            
            // Draw barlines
            if (index > 0) {
                var totalBeats = 0f
                for (i in 0..index) {
                    totalBeats += notes[i].durationBeats
                }
                // If this note completes a measure, draw barline AFTER it
                if (totalBeats.toInt() % 4 == 0 && index < notes.size - 1) {
                    // Barline halfway between current note and next note
                    val barX = noteX + noteSpacing / 2 + (noteSpacing * 0.18f) // Slight adjustment for optical centering
                    canvas.drawLine(barX, startY, barX, startY + staffHeight, linePaint)
                }
            }
        }
        
        // Draw final barline
        canvas.drawLine(size.width - staffSpace, startY, size.width - staffSpace, startY + staffHeight, linePaint)
        val thickPaint = android.graphics.Paint(linePaint).apply { strokeWidth = staffSpace * 0.4f }
        canvas.drawLine(size.width - staffSpace * 0.4f, startY, size.width - staffSpace * 0.4f, startY + staffHeight, thickPaint)
    }
}

@Composable
private fun SolfegeControlPanel(
    isListening: Boolean,
    isPlaying: Boolean,
    currentOctave: Int,
    tempo: Int,
    currentBeat: Int, // Visual metronome beat (1-4)
    phase: SolfegePhase,
    showBeatNumbers: Boolean,
    showSolfegeNames: Boolean,
    statusText: String,
    onOctaveChange: (Int) -> Unit,
    onPlayMelody: () -> Unit,
    onStopPlayback: () -> Unit,
    onStartSolfege: () -> Unit,
    onStopSolfege: () -> Unit,
    onToggleBeatNumbers: () -> Unit,
    onToggleSolfegeNames: () -> Unit
) {
    // Animated scale for metronome beat pulse
    val beatScale by animateFloatAsState(
        targetValue = if (currentBeat > 0 && phase != SolfegePhase.IDLE) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "beatScale"
    )
    
    // Beat color based on downbeat
    val beatColor by animateColorAsState(
        targetValue = when {
            phase == SolfegePhase.COUNTDOWN -> Color(0xFFFBBF24) // Yellow for countdown
            currentBeat == 1 -> Color(0xFF22C55E) // Green for downbeat
            currentBeat > 0 -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "beatColor"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Octave control
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text(
                    text = "Oitava",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onOctaveChange(-1) },
                        contentPadding = PaddingValues(2.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .width(30.dp)
                    ) {
                        Text("-8", fontSize = 12.sp)
                    }
                    Text(
                        text = currentOctave.toString(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = { onOctaveChange(1) },
                        contentPadding = PaddingValues(2.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .width(30.dp)
                    ) {
                        Text("+8", fontSize = 12.sp)
                    }
                }
            }
            
            // Listen/Stop button
            Button(
                onClick = if (isPlaying) onStopPlayback else onPlayMelody,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) Color(0xFFEF4444) else Color(0xFF3B82F6) // Red or Blue
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .height(40.dp)
                    .padding(end = 12.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPlaying) "Parar" else "Ouvir", fontSize = 14.sp)
            }
            
            // Solfege button -> "Notas" as requested
            Button(
                onClick = if (isListening) onStopSolfege else onStartSolfege,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Color(0xFFEF4444) else Color(0xFF22C55E) // Red or Green
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .height(40.dp)
                    .padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isListening) "Parar" else "Solfejar", fontSize = 14.sp)
            }
            
            // Tempo / Visual Metronome
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = "$tempo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "BPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Visual Metronome Square - Animated
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .scale(beatScale)
                        .clip(RoundedCornerShape(4.dp))
                        .background(beatColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            phase == SolfegePhase.COUNTDOWN && currentBeat > 0 -> currentBeat.toString()
                            phase != SolfegePhase.IDLE && currentBeat > 0 -> currentBeat.toString()
                            else -> "-"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentBeat == 1 || phase == SolfegePhase.COUNTDOWN) 
                            Color.White 
                        else 
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Space filler
            Spacer(modifier = Modifier.weight(1f))
            
            // Toggles
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tempos", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                    Switch(
                        checked = showBeatNumbers,
                        onCheckedChange = { onToggleBeatNumbers() },
                        modifier = Modifier.height(24.dp).scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notas", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp)) // Renamed from Solfejo
                    Switch(
                        checked = showSolfegeNames,
                        onCheckedChange = { onToggleSolfegeNames() },
                        modifier = Modifier.height(24.dp).scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Status indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isListening) Color(0xFF22C55E) else Color(0xFF6B7280))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExerciseCompleteContent(
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
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "$correctCount de $totalCount corretas",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
