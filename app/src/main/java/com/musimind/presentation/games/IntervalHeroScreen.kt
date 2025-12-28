package com.musimind.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun IntervalHeroScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: IntervalHeroViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) { viewModel.loadLevels(userId) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))))
    ) {
        when (state.gamePhase) {
            IntervalGamePhase.LEVEL_SELECT -> IntervalLevelSelect(state, viewModel, userId, onBack)
            IntervalGamePhase.PLAYING -> IntervalPlayScreen(state, viewModel)
            IntervalGamePhase.PAUSED -> PauseScreen(
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.restartLevel(userId) },
                onQuit = { viewModel.backToLevelSelect() }
            )
            IntervalGamePhase.RESULT -> ResultScreen(
                score = state.score, stars = state.stars,
                correctCount = state.correctCount, wrongCount = state.wrongCount,
                maxCombo = state.maxCombo, xpEarned = state.xpEarned, coinsEarned = state.coinsEarned,
                onPlayAgain = { viewModel.restartLevel(userId) },
                onNextLevel = { }, onBack = { viewModel.backToLevelSelect() }
            )
        }
    }
}

@Composable
private fun IntervalLevelSelect(state: IntervalHeroState, viewModel: IntervalHeroViewModel, userId: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("⚔️ Herói dos Intervalos", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("${state.totalStars}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.levels) { level ->
                    val stars = state.highScores[level.id]?.bestStars ?: 0
                    Card(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { viewModel.startLevel(userId, level) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
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
private fun IntervalPlayScreen(state: IntervalHeroState, viewModel: IntervalHeroViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("PONTOS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Text("${state.score}", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("Rodada ${state.currentRound}/${state.totalRounds}", color = Color.White)
            if (state.combo > 1) Text("🔥 x${state.combo}", style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.pauseGame() }) { Icon(Icons.Default.Pause, null, tint = Color.White) }
        }
        
        // Barras de vida
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("❤️ Você", color = Color.White, fontSize = 12.sp)
                LinearProgressIndicator(progress = { state.playerHealth / 100f }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), color = Color(0xFF4CAF50), trackColor = Color.Gray)
            }
            Spacer(Modifier.width(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${state.currentInterval?.monsterEmoji ?: "👹"} Monstro", color = Color.White, fontSize = 12.sp)
                LinearProgressIndicator(progress = { state.monsterHealth / 100f }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), color = Color(0xFFE91E63), trackColor = Color.Gray)
            }
        }
        
        // Área central
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Monstro
                val pulseAnim = rememberInfiniteTransition(label = "monster")
                val scale by pulseAnim.animateFloat(1f, 1.1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "monsterScale")
                Text(state.currentInterval?.monsterEmoji ?: "👹", fontSize = 80.sp, modifier = Modifier.scale(if (state.isPlaying) scale else 1f))
                
                Spacer(Modifier.height(16.dp))
                
                // Indicação
                if (state.isPlaying) {
                    Text("🔊 Ouvindo intervalo...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text("Qual é o intervalo?", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                
                // Feedback
                state.roundResult?.let { result ->
                    val (text, color) = when (result) {
                        RoundResult.SUCCESS -> "⚔️ ACERTOU!" to Color(0xFF4CAF50)
                        RoundResult.FAIL -> "💔 ERROU!" to Color(0xFFE91E63)
                    }
                    Text(text, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
                }
                
                // Dica
                if (state.showingHint) {
                    Text("💡 ${state.hintText}", color = Color(0xFFFFD700), modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        
        // Botões de ação
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = { viewModel.replayInterval() }) { 
                Icon(Icons.Default.Replay, null)
                Spacer(Modifier.width(4.dp))
                Text("Repetir", color = Color.White)
            }
            OutlinedButton(onClick = { viewModel.showHint() }) {
                Icon(Icons.Default.Lightbulb, null)
                Spacer(Modifier.width(4.dp))
                Text("Dica", color = Color.White)
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Opções de resposta
        Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(12.dp)) {
            state.answerOptions.chunked(2).forEach { rowOptions ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { option ->
                        Button(
                            onClick = { viewModel.selectAnswer(option) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(viewModel.getIntervalDisplayName(option), fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
