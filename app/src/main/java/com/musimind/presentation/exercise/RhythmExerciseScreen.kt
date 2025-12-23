package com.musimind.presentation.exercise

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.music.audio.metronome.MetronomeState
import com.musimind.music.audio.ui.BeatIndicator
import com.musimind.music.audio.ui.MetronomeCompact
import com.musimind.music.notation.model.Duration
import com.musimind.music.notation.ui.RestSymbol
import com.musimind.music.notation.ui.NoteSymbol

/**
 * Rhythm exercise screen - users tap rhythmic patterns
 * 
 * The user sees a rhythmic pattern and must tap it correctly in time with the metronome.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RhythmExerciseScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onComplete: (score: Int, total: Int) -> Unit,
    viewModel: RhythmViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val metronomeState by viewModel.metronomeState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadExercise(exerciseId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Percepção Rítmica") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.isComplete -> {
                    RhythmCompleteContent(
                        correctCount = state.correctCount,
                        totalCount = state.totalPatterns,
                        onContinue = { onComplete(state.correctCount, state.totalPatterns) }
                    )
                }
                else -> {
                    RhythmExerciseContent(
                        state = state,
                        metronomeState = metronomeState,
                        onStartMetronome = { viewModel.startMetronome() },
                        onStopMetronome = { viewModel.stopMetronome() },
                        onTap = { viewModel.onTap() },
                        onPlayPattern = { viewModel.playPattern() },
                        onNext = { viewModel.nextPattern() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RhythmExerciseContent(
    state: RhythmState,
    metronomeState: MetronomeState,
    onStartMetronome: () -> Unit,
    onStopMetronome: () -> Unit,
    onTap: () -> Unit,
    onPlayPattern: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress
        RhythmProgressHeader(
            currentPattern = state.currentPatternIndex + 1,
            totalPatterns = state.totalPatterns,
            correctCount = state.correctCount
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Metronome
        MetronomeCompact(
            state = metronomeState,
            onToggle = { if (metronomeState.isPlaying) onStopMetronome() else onStartMetronome() },
            onBpmIncrease = { },
            onBpmDecrease = { }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Rhythm pattern display
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
                Text(
                    text = "Reproduza este ritmo:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pattern visualization
                RhythmPatternDisplay(
                    pattern = state.currentPattern,
                    currentBeat = if (state.isRecording) state.recordingBeat else -1
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Play button
                FilledTonalButton(onClick = onPlayPattern) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvir Padrão")
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // User taps visualization
        if (state.isRecording) {
            UserTapsDisplay(
                expectedTaps = state.currentPattern.size,
                userTaps = state.userTaps
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Feedback
        AnimatedVisibility(
            visible = state.feedbackMessage != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut()
        ) {
            RhythmFeedbackCard(
                message = state.feedbackMessage ?: "",
                isCorrect = state.lastResult == ExerciseResult.CORRECT,
                onNext = onNext
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tap button
        TapButton(
            isRecording = state.isRecording,
            onTap = onTap,
            enabled = metronomeState.isPlaying
        )
    }
}

@Composable
private fun RhythmProgressHeader(
    currentPattern: Int,
    totalPatterns: Int,
    correctCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$currentPattern / $totalPatterns",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "$correctCount acertos",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF22C55E)
        )
        
        LinearProgressIndicator(
            progress = { currentPattern.toFloat() / totalPatterns },
            modifier = Modifier
                .width(100.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun RhythmPatternDisplay(
    pattern: List<Float>,
    currentBeat: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        pattern.forEachIndexed { index, duration ->
            val isHighlighted = index == currentBeat
            val color = if (isHighlighted) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurface
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isHighlighted) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (duration > 0) {
                    NoteSymbol(
                        duration = duration,
                        size = 32.dp,
                        color = color
                    )
                } else {
                    RestSymbol(
                        duration = kotlin.math.abs(duration),
                        size = 32.dp,
                        color = color.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserTapsDisplay(
    expectedTaps: Int,
    userTaps: List<Long>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Seus toques: ",
            style = MaterialTheme.typography.bodyMedium
        )
        
        repeat(expectedTaps) { index ->
            val hasTapped = index < userTaps.size
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasTapped) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun TapButton(
    isRecording: Boolean,
    onTap: () -> Unit,
    enabled: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "tapScale"
    )
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (enabled) listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    ) else listOf(
                        Color.Gray,
                        Color.Gray.copy(alpha = 0.5f)
                    )
                )
            )
            .clickable(enabled = enabled, onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = "Tocar",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (isRecording) "TAP!" else "Iniciar",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun RhythmFeedbackCard(
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
                    tint = if (isCorrect) Color(0xFF22C55E) else Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = message, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onNext) { Text("Próximo") }
        }
    }
}

@Composable
private fun RhythmCompleteContent(
    correctCount: Int,
    totalCount: Int,
    onContinue: () -> Unit
) {
    val percentage = (correctCount.toFloat() / totalCount * 100).toInt()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Exercício Completo!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "$correctCount de $totalCount corretas",
            style = MaterialTheme.typography.titleLarge
        )
        
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Continuar")
        }
    }
}
