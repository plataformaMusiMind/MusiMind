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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProgressionQuestScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: ProgressionQuestViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) { viewModel.loadLevels(userId) }
    
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))))) {
        when (state.gamePhase) {
            ProgressionQuestPhase.LEVEL_SELECT -> QuestLevelSelect(state, viewModel, userId, onBack)
            ProgressionQuestPhase.PLAYING -> QuestPlayScreen(state, viewModel)
            ProgressionQuestPhase.PAUSED -> PauseScreen({ viewModel.resumeGame() }, { viewModel.restartLevel(userId) }, { viewModel.backToLevelSelect() })
            ProgressionQuestPhase.RESULT -> ResultScreen(state.score, state.stars, state.correctCount, state.wrongCount, state.maxCombo, state.xpEarned, state.coinsEarned, { viewModel.restartLevel(userId) }, {}, { viewModel.backToLevelSelect() })
        }
    }
}

@Composable
private fun QuestLevelSelect(state: ProgressionQuestState, viewModel: ProgressionQuestViewModel, userId: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("🗺️ Missão Harmônica", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
                    Card(Modifier.fillMaxWidth().aspectRatio(1f).clickable { viewModel.startLevel(userId, level) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF5B21B6)), shape = RoundedCornerShape(16.dp)) {
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
private fun QuestPlayScreen(state: ProgressionQuestState, viewModel: ProgressionQuestViewModel) {
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
        
        // Barras de vida
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("🧙 Herói", color = Color.White, fontSize = 12.sp)
                LinearProgressIndicator(progress = { state.playerHealth / 100f }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), color = Color(0xFF4CAF50), trackColor = Color.Gray)
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Text("👹 Boss", color = Color.White, fontSize = 12.sp)
                LinearProgressIndicator(progress = { state.bossHealth / 100f }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), color = Color(0xFFE91E63), trackColor = Color.Gray)
            }
        }
        
        // Progressão visual
        state.currentProgression?.let { progression ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${progression.emoji} ${progression.style}", color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        progression.degrees.forEachIndexed { index, degree ->
                            val isTarget = index == state.targetChordIndex
                            val isPlaying = index == state.currentChordPlaying
                            
                            val scale by animateFloatAsState(if (isPlaying) 1.2f else 1f, label = "chordScale")
                            
                            Box(
                                modifier = Modifier
                                    .size(if (isTarget) 56.dp else 48.dp)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .background(when {
                                        isPlaying -> Color(0xFFFFD700)
                                        isTarget -> Color(0xFFE91E63)
                                        else -> Color.White.copy(alpha = 0.2f)
                                    })
                                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isTarget) "?" else degree, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (isTarget) 24.sp else 14.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Qual é o acorde marcado?", color = Color.White)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Feedback
        state.roundResult?.let { result ->
            val (text, color) = when (result) {
                RoundResult.SUCCESS -> "⚔️ CRÍTICO!" to Color(0xFF4CAF50)
                RoundResult.FAIL -> "💔 DANO!" to Color(0xFFE91E63)
            }
            Text(text, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        
        Spacer(Modifier.weight(1f))
        
        // Botão repetir
        OutlinedButton(onClick = { viewModel.replayProgression() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.Replay, null)
            Spacer(Modifier.width(4.dp))
            Text("Repetir", color = Color.White)
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Opções de resposta
        Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(12.dp)) {
            state.answerOptions.chunked(2).forEach { rowOptions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { option ->
                        Button(
                            onClick = { viewModel.selectAnswer(option) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B21B6)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = state.roundResult == null
                        ) {
                            Text(option, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
