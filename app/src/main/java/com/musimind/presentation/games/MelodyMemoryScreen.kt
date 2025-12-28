package com.musimind.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
 * Tela principal do jogo Melody Memory
 */
@Composable
fun MelodyMemoryScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: MelodyMemoryViewModel = hiltViewModel()
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
                        Color(0xFF06B6D4),
                        Color(0xFF0891B2)
                    )
                )
            )
    ) {
        when (state.gamePhase) {
            MelodyGamePhase.LEVEL_SELECT -> {
                MelodyLevelSelectScreen(
                    levels = state.levels,
                    highScores = state.highScores,
                    totalStars = state.totalStars,
                    isLoading = state.isLoading,
                    onLevelSelect = { level -> viewModel.startLevel(userId, level) },
                    onBack = onBack
                )
            }
            MelodyGamePhase.PLAYING -> {
                MelodyPlayScreen(
                    state = state,
                    onNotePressed = { viewModel.onNotePressed(it) },
                    onRepeat = { viewModel.repeatMelody() },
                    onPause = { viewModel.pauseGame() },
                    viewModel = viewModel
                )
            }
            MelodyGamePhase.PAUSED -> {
                PauseScreen(
                    onResume = { viewModel.resumeGame() },
                    onRestart = { viewModel.restartLevel(userId) },
                    onQuit = { viewModel.backToLevelSelect() }
                )
            }
            MelodyGamePhase.RESULT -> {
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
 * Tela de seleção de níveis
 */
@Composable
private fun MelodyLevelSelectScreen(
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Voltar", tint = Color.White)
            }
            
            Text(
                text = "🧠 Memória Melódica",
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
                    
                    MelodyLevelCard(
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
private fun MelodyLevelCard(
    level: GameLevel,
    stars: Int,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (level.difficulty) {
        1 -> Color(0xFF06B6D4)
        2 -> Color(0xFF0EA5E9)
        3 -> Color(0xFF3B82F6)
        4 -> Color(0xFF6366F1)
        else -> Color(0xFF8B5CF6)
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
 * Tela de gameplay
 */
@Composable
private fun MelodyPlayScreen(
    state: MelodyMemoryState,
    onNotePressed: (String) -> Unit,
    onRepeat: () -> Unit,
    onPause: () -> Unit,
    viewModel: MelodyMemoryViewModel
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
            // Pontuação
            Column {
                Text("PONTOS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Text("${state.score}", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            // Rodada
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Nível ${state.currentMelody.size}/${state.notesCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                // Progresso visual
                Row {
                    repeat(state.notesCount) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .padding(1.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < state.currentMelody.size) Color(0xFF4CAF50) 
                                    else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
            
            // Vidas
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(state.maxLives) { index ->
                    Icon(
                        imageVector = if (index < state.livesRemaining) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (index < state.livesRemaining) Color(0xFFE91E63) else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            IconButton(onClick = onPause) {
                Icon(Icons.Default.Pause, "Pausar", tint = Color.White)
            }
        }
        
        // Área de instruções e feedback
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Visualização da melodia
                MelodyVisualization(
                    melody = state.currentMelody,
                    playerInput = state.playerInput,
                    highlightedNote = state.highlightedNote,
                    phase = state.roundPhase,
                    viewModel = viewModel
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Instrução ou Feedback
                val text = when {
                    state.feedbackMessage != null -> state.feedbackMessage
                    state.roundPhase == MelodyRoundPhase.LISTENING -> "👂 Ouça a melodia..."
                    state.roundPhase == MelodyRoundPhase.YOUR_TURN -> "🎹 Sua vez! Toque as notas"
                    else -> ""
                }
                
                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Combo
                if (state.combo > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔥 Combo x${state.combo}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Botão de repetir (disponível durante YOUR_TURN)
        if (state.roundPhase == MelodyRoundPhase.YOUR_TURN) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onRepeat,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Replay, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Repetir")
                }
            }
        }
        
        // Teclado virtual
        VirtualPiano(
            keys = viewModel.getKeyboardKeys(),
            highlightedNote = state.highlightedNote,
            lastNoteCorrect = state.lastNoteCorrect,
            isEnabled = state.roundPhase == MelodyRoundPhase.YOUR_TURN,
            onNotePressed = { note ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNotePressed(note)
            },
            viewModel = viewModel
        )
    }
}

/**
 * Visualização da melodia (bolinhas)
 */
@Composable
private fun MelodyVisualization(
    melody: List<String>,
    playerInput: List<String>,
    highlightedNote: String?,
    phase: MelodyRoundPhase,
    viewModel: MelodyMemoryViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        melody.forEachIndexed { index, note ->
            val isHighlighted = note == highlightedNote && phase == MelodyRoundPhase.LISTENING
            val isPlayed = index < playerInput.size
            val isCorrect = isPlayed && playerInput[index] == note
            
            val color = when {
                isHighlighted -> Color(0xFFFFD700)
                isPlayed && isCorrect -> Color(0xFF4CAF50)
                isPlayed && !isCorrect -> Color(0xFFE91E63)
                else -> Color.White.copy(alpha = 0.3f)
            }
            
            val scale by animateFloatAsState(
                targetValue = if (isHighlighted) 1.3f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "noteScale"
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(color)
                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
                
                if (phase == MelodyRoundPhase.YOUR_TURN && isPlayed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getNoteDisplayName(playerInput[index]),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE91E63)
                    )
                }
            }
        }
    }
}

/**
 * Teclado virtual de piano
 */
@Composable
private fun VirtualPiano(
    keys: List<String>,
    highlightedNote: String?,
    lastNoteCorrect: Boolean?,
    isEnabled: Boolean,
    onNotePressed: (String) -> Unit,
    viewModel: MelodyMemoryViewModel
) {
    val noteColors = listOf(
        Color(0xFFE91E63), // Dó - Rosa
        Color(0xFFFF9800), // Ré - Laranja
        Color(0xFFFFEB3B), // Mi - Amarelo
        Color(0xFF4CAF50), // Fá - Verde
        Color(0xFF2196F3), // Sol - Azul
        Color(0xFF3F51B5), // Lá - Índigo
        Color(0xFF9C27B0)  // Si - Roxo
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        keys.forEachIndexed { index, note ->
            val isHighlighted = note == highlightedNote
            val baseColor = noteColors.getOrElse(index) { Color.Gray }
            
            val scale by animateFloatAsState(
                targetValue = if (isHighlighted) 1.1f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                label = "keyScale"
            )
            
            Button(
                onClick = { onNotePressed(note) },
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHighlighted) Color(0xFFFFD700) else baseColor,
                    disabledContainerColor = baseColor.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .padding(horizontal = 2.dp)
                    .scale(scale)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = viewModel.getNoteDisplayName(note),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
