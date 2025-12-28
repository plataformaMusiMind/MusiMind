package com.musimind.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.domain.games.FallingNote
import com.musimind.domain.games.GameLevel

/**
 * Tela principal do jogo Note Catcher
 */
@Composable
fun NoteCatcherScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: NoteCatcherViewModel = hiltViewModel()
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
                        Color(0xFF1E3A5F),
                        Color(0xFF0D1B2A)
                    )
                )
            )
    ) {
        when (state.gamePhase) {
            GamePhase.LEVEL_SELECT -> {
                LevelSelectScreen(
                    levels = state.levels,
                    highScores = state.highScores,
                    totalStars = state.totalStars,
                    isLoading = state.isLoading,
                    onLevelSelect = { level -> viewModel.startLevel(userId, level) },
                    onBack = onBack
                )
            }
            GamePhase.PLAYING -> {
                GamePlayScreen(
                    state = state,
                    onSelectAnswer = { viewModel.selectAnswer(it) },
                    onPause = { viewModel.pauseGame() },
                    viewModel = viewModel
                )
            }
            GamePhase.PAUSED -> {
                PauseScreen(
                    onResume = { viewModel.resumeGame() },
                    onRestart = { viewModel.restartLevel(userId) },
                    onQuit = { viewModel.backToLevelSelect() }
                )
            }
            GamePhase.RESULT -> {
                ResultScreen(
                    score = state.score,
                    stars = state.stars,
                    correctCount = state.correctCount,
                    wrongCount = state.wrongCount,
                    maxCombo = state.maxCombo,
                    xpEarned = state.xpEarned,
                    coinsEarned = state.coinsEarned,
                    onPlayAgain = { viewModel.restartLevel(userId) },
                    onNextLevel = { /* TODO: próximo nível */ },
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
private fun LevelSelectScreen(
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
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White
                )
            }
            
            Text(
                text = "🎵 Caça-Notas",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            // Total de estrelas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$totalStars",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
                    
                    LevelCard(
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

/**
 * Card de nível
 */
@Composable
private fun LevelCard(
    level: GameLevel,
    stars: Int,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (level.difficulty) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFF2196F3)
        3 -> Color(0xFF9C27B0)
        4 -> Color(0xFFFF9800)
        else -> Color(0xFFE91E63)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Número do nível
                Text(
                    text = "${level.levelNumber}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                // Título
                Text(
                    text = level.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                
                // Estrelas
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
            
            // Lock overlay
            if (isLocked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tela de gameplay
 */
@Composable
private fun GamePlayScreen(
    state: NoteCatcherState,
    onSelectAnswer: (String) -> Unit,
    onPause: () -> Unit,
    viewModel: NoteCatcherViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header com pontuação e tempo
        GameHeader(
            score = state.score,
            combo = state.combo,
            timeRemaining = state.timeRemaining,
            onPause = onPause
        )
        
        // Área de jogo com pauta
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Desenhar pauta musical
            StaffCanvas(
                fallingNotes = state.fallingNotes,
                modifier = Modifier.fillMaxSize()
            )
            
            // Feedback de acerto/erro
            state.lastFeedback?.let { feedback ->
                FeedbackOverlay(feedback = feedback)
            }
        }
        
        // Botões de resposta
        AnswerButtons(
            options = viewModel.getAnswerOptions(),
            onSelect = { displayName ->
                val noteLetter = viewModel.displayNameToNote(displayName)
                onSelectAnswer(noteLetter)
            }
        )
    }
}

/**
 * Header do jogo
 */
@Composable
private fun GameHeader(
    score: Int,
    combo: Int,
    timeRemaining: Int,
    onPause: () -> Unit
) {
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
            Text(
                text = "PONTOS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Combo
        if (combo > 1) {
            val infiniteTransition = rememberInfiniteTransition(label = "combo")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "comboScale"
            )
            
            Text(
                text = "🔥 x$combo",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFFF6B6B),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
            )
        }
        
        // Tempo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = if (timeRemaining <= 10) Color.Red else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatTime(timeRemaining),
                style = MaterialTheme.typography.titleMedium,
                color = if (timeRemaining <= 10) Color.Red else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Botão de pausa
        IconButton(onClick = onPause) {
            Icon(
                Icons.Default.Pause,
                contentDescription = "Pausar",
                tint = Color.White
            )
        }
    }
}

/**
 * Canvas com pauta musical e notas caindo
 */
@Composable
private fun StaffCanvas(
    fallingNotes: List<FallingNote>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val staffTop = height * 0.2f
        val staffHeight = height * 0.3f
        val lineSpacing = staffHeight / 4
        
        // Desenhar linhas da pauta
        for (i in 0..4) {
            val y = staffTop + (i * lineSpacing)
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(40f, y),
                end = Offset(width - 40f, y),
                strokeWidth = 2f
            )
        }
        
        // Desenhar clave de sol (simplificada)
        val clefPath = Path().apply {
            moveTo(60f, staffTop + staffHeight * 0.8f)
            cubicTo(
                80f, staffTop + staffHeight * 0.3f,
                100f, staffTop,
                80f, staffTop - 20f
            )
        }
        drawPath(
            path = clefPath,
            color = Color.White.copy(alpha = 0.8f),
            style = Stroke(width = 3f)
        )
        
        // Desenhar notas caindo
        for (note in fallingNotes) {
            val noteY = note.yPosition
            val noteX = width / 2
            
            // Calcular posição Y baseada no MIDI
            val midiOffset = (note.midiNumber - 60) * (lineSpacing / 2)
            val adjustedY = staffTop + staffHeight / 2 - midiOffset
            
            // Cor da nota baseada no feedback
            val noteColor = when (note.isCorrect) {
                true -> Color(0xFF4CAF50)
                false -> Color(0xFFE91E63)
                null -> Color.White
            }
            
            // Desenhar nota (oval)
            drawOval(
                color = noteColor,
                topLeft = Offset(noteX - 15f, adjustedY + noteY - 10f),
                size = androidx.compose.ui.geometry.Size(30f, 20f)
            )
            
            // Haste da nota
            drawLine(
                color = noteColor,
                start = Offset(noteX + 14f, adjustedY + noteY),
                end = Offset(noteX + 14f, adjustedY + noteY - 50f),
                strokeWidth = 2f
            )
        }
    }
}

/**
 * Overlay de feedback
 */
@Composable
private fun FeedbackOverlay(feedback: FeedbackType) {
    val color = when (feedback) {
        FeedbackType.CORRECT -> Color(0xFF4CAF50)
        FeedbackType.WRONG -> Color(0xFFE91E63)
    }
    
    val text = when (feedback) {
        FeedbackType.CORRECT -> "✓"
        FeedbackType.WRONG -> "✗"
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 120.sp,
            color = color.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Botões de resposta
 */
@Composable
private fun AnswerButtons(
    options: List<String>,
    onSelect: (String) -> Unit
) {
    val colors = listOf(
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
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEachIndexed { index, option ->
            Button(
                onClick = { onSelect(option) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.getOrElse(index) { Color.Gray }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = option,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
