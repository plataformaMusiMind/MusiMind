package com.musimind.presentation.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun KeyShooterScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: KeyShooterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) { viewModel.loadLevels(userId) }
    
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626))))) {
        when (state.gamePhase) {
            KeyShooterPhase.LEVEL_SELECT -> KeyLevelSelect(state, viewModel, userId, onBack)
            KeyShooterPhase.PLAYING -> KeyPlayScreen(state, viewModel)
            KeyShooterPhase.PAUSED -> PauseScreen({ viewModel.resumeGame() }, { viewModel.restartLevel(userId) }, { viewModel.backToLevelSelect() })
            KeyShooterPhase.RESULT -> ResultScreen(state.score, state.stars, state.correctCount, state.wrongCount, state.maxCombo, state.xpEarned, state.coinsEarned, { viewModel.restartLevel(userId) }, {}, { viewModel.backToLevelSelect() })
        }
    }
}

@Composable
private fun KeyLevelSelect(state: KeyShooterState, viewModel: KeyShooterViewModel, userId: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("🎯 Tiro às Armaduras", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                Text("${state.totalStars}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
        } else {
            LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.levels) { level ->
                    val stars = state.highScores[level.id]?.bestStars ?: 0
                    Card(Modifier.fillMaxWidth().aspectRatio(1f).clickable { viewModel.startLevel(userId, level) }, colors = CardDefaults.cardColors(containerColor = Color(0xFFDC2626)), shape = RoundedCornerShape(16.dp)) {
                        Box(Modifier.fillMaxSize().padding(12.dp)) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                                Text("${level.levelNumber}", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(level.title, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                                Row { repeat(3) { i -> Icon(if (i < stars) Icons.Default.Star else Icons.Default.StarBorder, null, tint = if (i < stars) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp)) } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyPlayScreen(state: KeyShooterState, viewModel: KeyShooterViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("PONTOS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Text("${state.score}", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("${state.currentRound}/${state.totalRounds}", color = Color.White)
            if (state.combo > 1) Text("🔥 x${state.combo}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.pauseGame() }) { Icon(Icons.Default.Pause, null, tint = Color.White) }
        }
        
        // Barra de tempo
        LinearProgressIndicator(
            progress = { state.timeRemaining / state.timePerTarget },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = when {
                state.timeRemaining > state.timePerTarget * 0.5f -> Color(0xFF4CAF50)
                state.timeRemaining > state.timePerTarget * 0.25f -> Color(0xFFFF9800)
                else -> Color(0xFFE91E63)
            },
            trackColor = Color.Black.copy(alpha = 0.3f)
        )
        
        // Área de jogo - Alvo
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Alvo com armadura
            state.currentTarget?.let { target ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(1f)
                        .offset(x = (state.targetPosition.x * 100).dp, y = (state.targetPosition.y * 300).dp)
                        .align(Alignment.TopCenter)
                ) {
                    // Desenho do alvo
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2
                        
                        // Círculos concêntricos
                        listOf(1f, 0.75f, 0.5f, 0.25f).forEachIndexed { index, scale ->
                            drawCircle(
                                color = if (index % 2 == 0) Color.White else Color.Red,
                                radius = radius * scale,
                                center = center
                            )
                        }
                    }
                    
                    // Info da armadura
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Mostrar quantidade de acidentes
                        val accidentalSymbol = if (target.isSharps) "♯" else "♭"
                        val accidentalsText = if (target.accidentalCount == 0) "Sem acidentes" else "${target.accidentalCount}$accidentalSymbol"
                        
                        Text(accidentalsText, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
            
            // Feedback
            state.roundResult?.let { result ->
                val (text, color) = when (result) {
                    RoundResult.SUCCESS -> "🎯 ACERTOU!" to Color(0xFF4CAF50)
                    RoundResult.FAIL -> "❌ ERROU!" to Color(0xFFE91E63)
                }
                Text(text, style = MaterialTheme.typography.headlineLarge, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }
        }
        
        // Opções de resposta (4 botões)
        Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(12.dp)) {
            state.answerOptions.chunked(2).forEach { rowOptions ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { option ->
                        Button(
                            onClick = { viewModel.shoot(option) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(option, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
