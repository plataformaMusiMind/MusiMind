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
fun SolfegeSingScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: SolfegeSingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) { viewModel.loadLevels(userId) }
    
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF14B8A6), Color(0xFF0D9488))))) {
        when (state.gamePhase) {
            SolfegeSingPhase.LEVEL_SELECT -> SolfegeLevelSelect(state, viewModel, userId, onBack)
            SolfegeSingPhase.PLAYING -> SolfegePlayScreen(state, viewModel)
            SolfegeSingPhase.PAUSED -> PauseScreen({ viewModel.resumeGame() }, { viewModel.restartLevel(userId) }, { viewModel.backToLevelSelect() })
            SolfegeSingPhase.RESULT -> ResultScreen(state.score, state.stars, state.correctCount, state.wrongCount, state.maxCombo, state.xpEarned, state.coinsEarned, { viewModel.restartLevel(userId) }, {}, { viewModel.backToLevelSelect() })
        }
    }
}

@Composable
private fun SolfegeLevelSelect(state: SolfegeSingState, viewModel: SolfegeSingViewModel, userId: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("🎤 Cante o Solfejo", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
                    Card(Modifier.fillMaxWidth().aspectRatio(1f).clickable { viewModel.startLevel(userId, level) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9488)), shape = RoundedCornerShape(16.dp)) {
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
private fun SolfegePlayScreen(state: SolfegeSingState, viewModel: SolfegeSingViewModel) {
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
        
        Spacer(Modifier.weight(0.5f))
        
        // Notas do solfejo
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center) {
            state.currentSolfege.forEachIndexed { index, solfege ->
                val isCurrentNote = index == state.currentNoteIndex
                val detectedNote = state.detectedNotes.getOrNull(index)
                val expectedDegree = state.currentMelody.getOrNull(index)
                val isCorrect = detectedNote == expectedDegree
                
                val color = when {
                    state.melodyPhase == MelodySingPhase.RESULT && detectedNote != null -> if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE91E63)
                    isCurrentNote -> Color(0xFFFFD700)
                    else -> Color.White
                }
                
                val scale by animateFloatAsState(if (isCurrentNote) 1.3f else 1f, label = "noteScale")
                
                Box(
                    modifier = Modifier.padding(horizontal = 4.dp).size(48.dp).scale(scale).clip(CircleShape)
                        .background(color.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(solfege, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Instrução / Status
        val statusText = when (state.melodyPhase) {
            MelodySingPhase.LISTENING -> if (state.isPlayingMelody) "🔊 Ouça a melodia..." else "Preparando..."
            MelodySingPhase.SINGING -> if (state.isRecording) "🎤 Cantando..." else "🎤 Sua vez! Cante!"
            MelodySingPhase.RESULT -> {
                val percent = (state.accuracy * 100).toInt()
                "${state.medal ?: ""} $percent% de precisão"
            }
        }
        Text(statusText, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        
        // Indicador de gravação
        if (state.isRecording) {
            Spacer(Modifier.height(24.dp))
            val pulseAnim = rememberInfiniteTransition(label = "recording")
            val pulse by pulseAnim.animateFloat(1f, 1.3f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "recordPulse")
            
            Box(Modifier.size(80.dp).scale(pulse).clip(CircleShape).background(Color(0xFFE91E63)).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        // Botões de ação
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (state.melodyPhase == MelodySingPhase.LISTENING && !state.isPlayingMelody) {
                OutlinedButton(onClick = { viewModel.replayMelody() }) { 
                    Icon(Icons.Default.Replay, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Repetir", color = Color.White)
                }
            }
            
            if (state.melodyPhase == MelodySingPhase.SINGING && !state.isRecording) {
                Button(onClick = { viewModel.startRecording() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))) {
                    Icon(Icons.Default.Mic, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gravar")
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}
