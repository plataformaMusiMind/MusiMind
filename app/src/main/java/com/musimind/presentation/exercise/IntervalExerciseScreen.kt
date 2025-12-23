package com.musimind.presentation.exercise

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.music.notation.model.ClefType
import com.musimind.music.notation.ui.IntervalOnStaff

/**
 * Interval recognition exercise - users identify musical intervals
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalExerciseScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onComplete: (score: Int, total: Int) -> Unit,
    viewModel: IntervalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadExercise(exerciseId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Percepção Intervalar") },
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
                    IntervalCompleteContent(
                        correctCount = state.correctCount,
                        totalCount = state.totalIntervals,
                        onContinue = { onComplete(state.correctCount, state.totalIntervals) }
                    )
                }
                else -> {
                    IntervalExerciseContent(
                        state = state,
                        onPlayInterval = { viewModel.playInterval() },
                        onSelectAnswer = { viewModel.selectAnswer(it) },
                        onNext = { viewModel.nextInterval() }
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalExerciseContent(
    state: IntervalState,
    onPlayInterval: () -> Unit,
    onSelectAnswer: (IntervalType) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress
        IntervalProgressHeader(
            currentInterval = state.currentIntervalIndex + 1,
            totalIntervals = state.totalIntervals,
            correctCount = state.correctCount,
            lives = state.lives
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Interval display
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
                    text = "Qual é este intervalo?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Staff with interval
                state.currentInterval?.let { interval ->
                    IntervalOnStaff(
                        lowerPosition = interval.lowerPosition,
                        upperPosition = interval.upperPosition,
                        clef = ClefType.TREBLE,
                        noteColor = when (state.answerState) {
                            AnswerState.CORRECT -> Color(0xFF22C55E)
                            AnswerState.INCORRECT -> Color(0xFFEF4444)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Play button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilledTonalButton(onClick = onPlayInterval) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Melódico")
                    }
                    
                    FilledTonalButton(onClick = onPlayInterval) {
                        Icon(Icons.Default.Piano, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Harmônico")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Answer options
        Text(
            text = "Selecione o intervalo:",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(state.answerOptions) { option ->
                IntervalOptionCard(
                    interval = option,
                    isSelected = state.selectedAnswer == option,
                    isCorrect = state.answerState == AnswerState.CORRECT && state.selectedAnswer == option,
                    isIncorrect = state.answerState == AnswerState.INCORRECT && state.selectedAnswer == option,
                    showCorrect = state.answerState == AnswerState.INCORRECT && 
                                  option == state.currentInterval?.type,
                    enabled = state.answerState == AnswerState.PENDING,
                    onClick = { onSelectAnswer(option) }
                )
            }
        }
        
        // Feedback
        AnimatedVisibility(
            visible = state.feedbackMessage != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut()
        ) {
            IntervalFeedbackCard(
                message = state.feedbackMessage ?: "",
                isCorrect = state.answerState == AnswerState.CORRECT,
                correctAnswer = state.currentInterval?.type?.displayName ?: "",
                onNext = onNext
            )
        }
    }
}

@Composable
private fun IntervalProgressHeader(
    currentInterval: Int,
    totalIntervals: Int,
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
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$currentInterval / $totalIntervals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$correctCount acertos",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF22C55E)
            )
        }
        
        LinearProgressIndicator(
            progress = { currentInterval.toFloat() / totalIntervals },
            modifier = Modifier
                .width(80.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun IntervalOptionCard(
    interval: IntervalType,
    isSelected: Boolean,
    isCorrect: Boolean,
    isIncorrect: Boolean,
    showCorrect: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCorrect || showCorrect -> Color(0xFF22C55E).copy(alpha = 0.2f)
        isIncorrect -> Color(0xFFEF4444).copy(alpha = 0.2f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isCorrect || showCorrect -> Color(0xFF22C55E)
        isIncorrect -> Color(0xFFEF4444)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = interval.shortName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = interval.displayName,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun IntervalFeedbackCard(
    message: String,
    isCorrect: Boolean,
    correctAnswer: String,
    onNext: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) 
                Color(0xFF22C55E).copy(alpha = 0.2f) 
            else 
                Color(0xFFEF4444).copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Column {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isCorrect) {
                            Text(
                                text = "Resposta: $correctAnswer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF22C55E)
                            )
                        }
                    }
                }
                
                Button(onClick = onNext) {
                    Text("Próximo")
                }
            }
        }
    }
}

@Composable
private fun IntervalCompleteContent(
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
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = Color(0xFFFBBF24),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = when {
                percentage >= 90 -> "Excelente!"
                percentage >= 70 -> "Muito Bom!"
                else -> "Continue praticando!"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "$correctCount de $totalCount",
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
