package com.musimind.presentation.exercise

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.music.audio.ui.*
import com.musimind.music.notation.model.*
import com.musimind.music.notation.ui.*

/**
 * Solfege exercise screen - users sing notes displayed on staff
 * 
 * Flow:
 * 1. Show note on staff
 * 2. Play the note (optional)
 * 3. User sings the note
 * 4. Detect pitch and provide feedback
 * 5. Show result and move to next note
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
    
    // Request permission on launch
    LaunchedEffect(Unit) {
        viewModel.loadExercise(exerciseId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Solfejo",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Sound toggle
                    IconButton(onClick = { viewModel.togglePlayback() }) {
                        Icon(
                            imageVector = if (state.playbackEnabled) 
                                Icons.Default.VolumeUp 
                            else 
                                Icons.Default.VolumeOff,
                            contentDescription = "Som"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                        onStartListening = { viewModel.startListening() },
                        onStopListening = { viewModel.stopListening() },
                        onPlayNote = { viewModel.playCurrentNote() },
                        onNext = { viewModel.nextNote() },
                        onSkip = { viewModel.skipNote() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SolfegeExerciseContent(
    state: SolfegeState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onPlayNote: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar
        ProgressHeader(
            currentNote = state.currentNoteIndex + 1,
            totalNotes = state.totalNotes,
            correctCount = state.correctCount,
            lives = state.lives
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Note display on staff
        state.currentNote?.let { note ->
            NoteDisplayCard(
                note = note,
                state = state.currentNoteState,
                onPlayNote = onPlayNote
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Pitch indicator
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
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Feedback message
        AnimatedVisibility(
            visible = state.feedbackMessage != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FeedbackCard(
                message = state.feedbackMessage ?: "",
                isCorrect = state.lastResult == ExerciseResult.CORRECT,
                onNext = onNext
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control buttons
        ControlButtons(
            isListening = state.isListening,
            canSkip = state.currentNoteState == NoteState.NORMAL,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            onSkip = onSkip
        )
    }
}

@Composable
private fun ProgressHeader(
    currentNote: Int,
    totalNotes: Int,
    correctCount: Int,
    lives: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lives
        Row {
            repeat(3) { index ->
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (index < lives) Color(0xFFEF4444) else Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Progress
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$currentNote / $totalNotes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$correctCount acertos",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF22C55E)
            )
        }
        
        // Score indicator
        LinearProgressIndicator(
            progress = { currentNote.toFloat() / totalNotes },
            modifier = Modifier
                .width(80.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun NoteDisplayCard(
    note: Note,
    state: NoteState,
    onPlayNote: () -> Unit
) {
    val noteColor by animateColorAsState(
        targetValue = when (state) {
            NoteState.CORRECT -> Color(0xFF22C55E)
            NoteState.INCORRECT -> Color(0xFFEF4444)
            NoteState.HIGHLIGHTED -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(300),
        label = "noteColor"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Staff with note
            NoteOnStaff(
                staffPosition = note.pitch.staffPosition(ClefType.TREBLE),
                clef = ClefType.TREBLE,
                duration = note.durationBeats,
                noteColor = noteColor,
                height = 100.dp,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Solfege name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = com.musimind.music.audio.pitch.PitchUtils.pitchToSolfege(note.pitch),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = noteColor
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Play button
                FilledIconButton(
                    onClick = onPlayNote,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Ouvir nota"
                    )
                }
            }
            
            Text(
                text = com.musimind.music.audio.pitch.PitchUtils.pitchToDisplayString(note.pitch),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    message: String,
    isCorrect: Boolean,
    onNext: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) 
                Color(0xFF22C55E).copy(alpha = 0.2f) 
            else 
                Color(0xFFEF4444).copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isCorrect) Color(0xFF22C55E) else Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Button(onClick = onNext) {
                Text("Próxima")
            }
        }
    }
}

@Composable
private fun ControlButtons(
    isListening: Boolean,
    canSkip: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skip button
        OutlinedButton(
            onClick = onSkip,
            enabled = canSkip
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pular")
        }
        
        // Microphone button
        FloatingActionButton(
            onClick = if (isListening) onStopListening else onStartListening,
            containerColor = if (isListening) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "Parar" else "Ouvir",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ExerciseCompleteContent(
    correctCount: Int,
    totalCount: Int,
    onContinue: () -> Unit
) {
    val percentage = (correctCount.toFloat() / totalCount * 100).toInt()
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
        // Trophy or star
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
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // XP earned
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFBBF24).copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+${correctCount * 10} XP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continuar", style = MaterialTheme.typography.titleMedium)
        }
    }
}
