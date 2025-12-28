package com.musimind.presentation.games

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TempoRunScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: TempoRunViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) { viewModel.loadLevels(userId) }
    
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))))) {
        when (state.gamePhase) {
            TempoRunPhase.LEVEL_SELECT -> TempoLevelSelect(state, viewModel, userId, onBack)
            TempoRunPhase.PLAYING -> TempoPlayScreen(state, viewModel)
            TempoRunPhase.PAUSED -> PauseScreen({ viewModel.resumeGame() }, { viewModel.restartLevel(userId) }, { viewModel.backToLevelSelect() })
            TempoRunPhase.RESULT -> ResultScreen(state.score, state.stars, state.correctCount, state.wrongCount, state.maxCombo, state.xpEarned, state.coinsEarned, { viewModel.restartLevel(userId) }, {}, { viewModel.backToLevelSelect() })
        }
    }
}

@Composable
private fun TempoLevelSelect(state: TempoRunState, viewModel: TempoRunViewModel, userId: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("🏃 Corrida do Andamento", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
                    Card(Modifier.fillMaxWidth().aspectRatio(1f).clickable { viewModel.startLevel(userId, level) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1D4ED8)), shape = RoundedCornerShape(16.dp)) {
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
private fun TempoPlayScreen(state: TempoRunState, viewModel: TempoRunViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("PONTOS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Text("${state.score}", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("${state.currentRound}/${state.totalRounds}", color = Color.White)
            Row {
                repeat(state.maxLives) { i ->
                    Icon(if (i < state.lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (i < state.lives) Color(0xFFE91E63) else Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = { viewModel.pauseGame() }) { Icon(Icons.Default.Pause, null, tint = Color.White) }
        }
        
        // Timer
        LinearProgressIndicator(progress = { state.timeToAnswer / 5f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Color(0xFF4CAF50), trackColor = Color.Black.copy(alpha = 0.3f))
        
        // Área do jogo com faixas
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Faixas de andamento
            Row(modifier = Modifier.fillMaxSize()) {
                state.activeTempos.forEachIndexed { index, tempo ->
                    val isPlayerHere = index == state.playerLane
                    val isCorrect = index == state.correctLane
                    val showCorrect = state.roundResult != null && isCorrect
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                when {
                                    showCorrect && state.roundResult == RoundResult.SUCCESS -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                                    showCorrect && state.roundResult == RoundResult.FAIL -> Color(0xFFE91E63).copy(alpha = 0.3f)
                                    isPlayerHere -> Color.White.copy(alpha = 0.2f)
                                    else -> Color.Transparent
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tempo.emoji, fontSize = 32.sp)
                            Text(tempo.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${tempo.minBpm}-${tempo.maxBpm}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            
                            if (isPlayerHere) {
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFFD700)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎵", fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
            }
            
            // Indicador de BPM atual
            if (state.isPlaying) {
                val infiniteTransition = rememberInfiniteTransition(label = "beat")
                val scale by infiniteTransition.animateFloat(1f, 1.2f, infiniteRepeatable(tween(60000 / state.currentBpm), RepeatMode.Reverse), label = "beatScale")
                
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)) {
                    Text("♪ ${state.currentBpm} BPM", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Controles
        Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { viewModel.moveLane(-1) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))) {
                Icon(Icons.Default.KeyboardArrowLeft, null, modifier = Modifier.size(32.dp))
            }
            
            Button(onClick = { viewModel.confirmAnswer() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), modifier = Modifier.size(64.dp), shape = CircleShape) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(32.dp))
            }
            
            Button(onClick = { viewModel.moveLane(1) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))) {
                Icon(Icons.Default.KeyboardArrowRight, null, modifier = Modifier.size(32.dp))
            }
        }
    }
}
