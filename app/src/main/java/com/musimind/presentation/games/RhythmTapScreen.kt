package com.musimind.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.domain.games.GameLevel

/**
 * Tela principal do jogo Rhythm Tap
 */
@Composable
fun RhythmTapScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: RhythmTapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadLevels(userId)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE91E63),
                        Color(0xFF9C27B0)
                    )
                )
            )
    ) {
        when (state.gamePhase) {
            RhythmGamePhase.LEVEL_SELECT -> {
                RhythmLevelSelectScreen(
                    levels = state.levels,
                    highScores = state.highScores,
                    totalStars = state.totalStars,
                    isLoading = state.isLoading,
                    onLevelSelect = { level -> viewModel.startLevel(userId, level) },
                    onBack = onBack
                )
            }
            RhythmGamePhase.COUNTDOWN -> {
                CountdownScreen(countdownValue = state.countdownValue)
            }
            RhythmGamePhase.PLAYING -> {
                RhythmPlayScreen(
                    state = state,
                    onTap = { viewModel.onTap() },
                    onPause = { viewModel.pauseGame() }
                )
            }
            RhythmGamePhase.PAUSED -> {
                PauseScreen(
                    onResume = { viewModel.resumeGame() },
                    onRestart = { viewModel.restartLevel(userId) },
                    onQuit = { viewModel.backToLevelSelect() }
                )
            }
            RhythmGamePhase.RESULT -> {
                ResultScreen(
                    score = state.score,
                    stars = state.stars,
                    correctCount = state.correctCount,
                    wrongCount = state.wrongCount,
                    maxCombo = state.maxCombo,
                    xpEarned = state.xpEarned,
                    coinsEarned = state.coinsEarned,
                    onPlayAgain = { viewModel.restartLevel(userId) },
                    onNextLevel = { },
                    onBack = { viewModel.backToLevelSelect() }
                )
            }
        }
    }
}

/**
 * Tela de seleção de níveis do Rhythm Tap
 */
@Composable
private fun RhythmLevelSelectScreen(
    levels: List<GameLevel>,
    highScores: Map<String, com.musimind.domain.games.GameHighScore>,
    totalStars: Int,
    isLoading: Boolean,
    onLevelSelect: (GameLevel) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Voltar", tint = Color.White)
            }
            
            Text(
                text = "🥁 Batida Perfeita",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$totalStars", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(levels) { level ->
                    val highScore = highScores[level.id]
                    val stars = highScore?.bestStars ?: 0
                    val isLocked = level.levelNumber > 1 && 
                        highScores[levels.getOrNull(level.levelNumber - 2)?.id]?.bestStars == null
                    
                    RhythmLevelCard(
                        level = level,
                        stars = stars,
                        isLocked = isLocked,
                        onClick = { if (!isLocked) onLevelSelect(level) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RhythmLevelCard(
    level: GameLevel,
    stars: Int,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (level.difficulty) {
        1 -> Color(0xFFE91E63)
        2 -> Color(0xFFFF5722)
        3 -> Color(0xFFFF9800)
        4 -> Color(0xFF9C27B0)
        else -> Color(0xFF673AB7)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(enabled = !isLocked) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) Color.Gray.copy(alpha = 0.3f) else backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${level.levelNumber}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = level.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                
                Row {
                    repeat(3) { index ->
                        Icon(
                            imageVector = if (index < stars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (index < stars) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            if (isLocked) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, "Bloqueado", tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

/**
 * Tela de countdown
 */
@Composable
private fun CountdownScreen(countdownValue: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val scale by animateFloatAsState(
            targetValue = if (countdownValue > 0) 1.5f else 0f,
            animationSpec = tween(300),
            label = "countdownScale"
        )
        
        if (countdownValue > 0) {
            Text(
                text = "$countdownValue",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 120.sp,
                modifier = Modifier.scale(scale)
            )
        } else {
            Text(
                text = "🎵 VAI!",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Tela de gameplay do Rhythm Tap
 */
@Composable
private fun RhythmPlayScreen(
    state: RhythmTapState,
    onTap: () -> Unit,
    onPause: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("PONTOS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Text("${state.score}", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            // Rodada
            Text(
                text = "Rodada ${state.currentRound}/${state.totalRounds}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            
            // Combo
            if (state.combo > 1) {
                Text(
                    text = "🔥 x${state.combo}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(onClick = onPause) {
                Icon(Icons.Default.Pause, "Pausar", tint = Color.White)
            }
        }
        
        // Área principal
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Indicador visual de batidas
                BeatIndicator(
                    expectedBeats = state.expectedBeats.size,
                    currentBeatIndex = state.beatIndicatorIndex,
                    playerBeats = state.playerBeats.size,
                    isListening = state.isListening
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Instrução
                val instruction = when (state.roundPhase) {
                    RoundPhase.LISTENING -> "👂 Ouça o ritmo..."
                    RoundPhase.YOUR_TURN -> "👆 Sua vez! Toque!"
                    RoundPhase.FEEDBACK -> ""
                }
                
                AnimatedVisibility(
                    visible = instruction.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Resultado da rodada
                state.lastRoundResult?.let { result ->
                    val (text, color) = when (result) {
                        RoundResult.SUCCESS -> "✓ Ótimo!" to Color(0xFF4CAF50)
                        RoundResult.FAIL -> "✗ Tente novamente" to Color(0xFFE91E63)
                    }
                    
                    Text(
                        text = text,
                        style = MaterialTheme.typography.headlineMedium,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Área de toque (grande botão)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    if (state.isListening) 
                        Color.White.copy(alpha = 0.2f) 
                    else 
                        Color.Black.copy(alpha = 0.2f)
                )
                .clickable(
                    enabled = state.isListening,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTap()
                },
            contentAlignment = Alignment.Center
        ) {
            val pulseAnim = rememberInfiniteTransition(label = "pulse")
            val pulse by pulseAnim.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            
            Box(
                modifier = Modifier
                    .size(if (state.isListening) 120.dp else 100.dp)
                    .scale(if (state.isListening) pulse else 1f)
                    .clip(CircleShape)
                    .background(
                        if (state.isListening)
                            Color.White.copy(alpha = 0.3f)
                        else
                            Color.White.copy(alpha = 0.1f)
                    )
                    .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.isListening) "TAP!" else "🎵",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Indicador visual das batidas
 */
@Composable
private fun BeatIndicator(
    expectedBeats: Int,
    currentBeatIndex: Int,
    playerBeats: Int,
    isListening: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(expectedBeats) { index ->
            val isActive = index == currentBeatIndex
            val isPlayed = isListening && index < playerBeats
            
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.3f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "beatScale"
            )
            
            val color = when {
                isActive -> Color(0xFFFFD700)
                isPlayed -> Color(0xFF4CAF50)
                else -> Color.White.copy(alpha = 0.3f)
            }
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// Reutiliza PauseScreen e ResultScreen do NoteCatcherScreen
