package com.musimind.presentation.duel

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.domain.model.Duel
import com.musimind.domain.model.DuelQuestion
import com.musimind.domain.model.DuelStatus

/**
 * Duel game screen - real-time quiz battle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelScreen(
    duelId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: DuelViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(duelId) {
        viewModel.loadDuel(duelId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duelo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Sair")
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
                state.duel?.status == DuelStatus.COMPLETED -> {
                    DuelResultContent(
                        duel = state.duel!!,
                        isCurrentUserChallenger = state.isChallenger,
                        onContinue = onComplete
                    )
                }
                state.currentQuestion != null -> {
                    DuelQuestionContent(
                        state = state,
                        onSelectAnswer = { viewModel.selectAnswer(it) }
                    )
                }
                else -> {
                    WaitingForOpponent(
                        duel = state.duel,
                        onCancel = onBack
                    )
                }
            }
        }
    }
}

@Composable
private fun DuelQuestionContent(
    state: DuelState,
    onSelectAnswer: (Int) -> Unit
) {
    val question = state.currentQuestion ?: return
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score header
        ScoreHeader(
            challengerName = state.duel?.challengerName ?: "",
            challengerScore = state.duel?.challengerScore ?: 0,
            opponentName = state.duel?.opponentName ?: "",
            opponentScore = state.duel?.opponentScore ?: 0,
            isChallenger = state.isChallenger
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Timer
        TimerIndicator(
            timeRemaining = state.timeRemaining,
            totalTime = question.timeLimit
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Question progress
        Text(
            text = "Questão ${state.questionIndex + 1} de ${state.totalQuestions}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Question
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Answer options
        question.options.forEachIndexed { index, option ->
            AnswerOption(
                text = option,
                index = index,
                isSelected = state.selectedAnswer == index,
                isCorrect = state.showResult && index == question.correctAnswerIndex,
                isIncorrect = state.showResult && state.selectedAnswer == index && 
                              index != question.correctAnswerIndex,
                enabled = !state.hasAnswered,
                onClick = { onSelectAnswer(index) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ScoreHeader(
    challengerName: String,
    challengerScore: Int,
    opponentName: String,
    opponentScore: Int,
    isChallenger: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Challenger (or current user if challenger)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isChallenger) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
                    .border(
                        2.dp,
                        if (isChallenger) Color(0xFF22C55E) else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = challengerName.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = challengerName.take(8),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
        
        // Score
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$challengerScore",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = " × ",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "$opponentScore",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        // Opponent
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (!isChallenger) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
                    .border(
                        2.dp,
                        if (!isChallenger) Color(0xFF22C55E) else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opponentName.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = opponentName.take(8),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TimerIndicator(
    timeRemaining: Int,
    totalTime: Int
) {
    val progress = timeRemaining.toFloat() / totalTime
    val color = when {
        progress > 0.5f -> Color(0xFF22C55E)
        progress > 0.25f -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }
    
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(64.dp),
            color = color,
            strokeWidth = 6.dp
        )
        
        Text(
            text = "$timeRemaining",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun AnswerOption(
    text: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    isIncorrect: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCorrect -> Color(0xFF22C55E).copy(alpha = 0.2f)
        isIncorrect -> Color(0xFFEF4444).copy(alpha = 0.2f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isCorrect -> Color(0xFF22C55E)
        isIncorrect -> Color(0xFFEF4444)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(),
        label = "optionScale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = enabled, onClick = onClick)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option letter
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(borderColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ('A' + index).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            // Result icon
            if (isCorrect) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Correto",
                    tint = Color(0xFF22C55E)
                )
            } else if (isIncorrect) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Incorreto",
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
private fun WaitingForOpponent(
    duel: Duel?,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Aguardando oponente...",
            style = MaterialTheme.typography.titleLarge
        )
        
        duel?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "vs ${it.opponentName}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(onClick = onCancel) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun DuelResultContent(
    duel: Duel,
    isCurrentUserChallenger: Boolean,
    onContinue: () -> Unit
) {
    val currentUserScore = if (isCurrentUserChallenger) duel.challengerScore else duel.opponentScore
    val opponentScore = if (isCurrentUserChallenger) duel.opponentScore else duel.challengerScore
    val currentUserId = if (isCurrentUserChallenger) duel.challengerId else duel.opponentId
    
    val isWinner = duel.winnerId == currentUserId
    val isTie = duel.isTie
    
    val infiniteTransition = rememberInfiniteTransition(label = "result")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "resultScale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Result icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(if (isWinner) scale else 1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = when {
                            isWinner -> listOf(Color(0xFFFFD700), Color(0xFFF59E0B))
                            isTie -> listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                            else -> listOf(Color(0xFFF87171), Color(0xFFEF4444))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isWinner -> Icons.Default.EmojiEvents
                    isTie -> Icons.Default.Handshake
                    else -> Icons.Default.SentimentDissatisfied
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = when {
                isWinner -> "Vitória! 🎉"
                isTie -> "Empate!"
                else -> "Derrota"
            },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Score
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$currentUserScore",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = " × ",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "$opponentScore",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // XP reward
        if (isWinner) {
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
                        text = "+${duel.xpReward} XP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continuar")
        }
    }
}
