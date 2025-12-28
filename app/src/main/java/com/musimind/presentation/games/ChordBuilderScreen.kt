package com.musimind.presentation.games

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ChordBuilderScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: ChordBuilderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) { viewModel.loadLevels(userId) }
    
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5))))) {
        when (state.gamePhase) {
            ChordBuilderPhase.LEVEL_SELECT -> ChordBuilderLevelSelect(state, viewModel, userId, onBack)
            ChordBuilderPhase.PLAYING -> ChordBuilderPlayScreen(state, viewModel)
            ChordBuilderPhase.PAUSED -> PauseScreen({ viewModel.resumeGame() }, { viewModel.restartLevel(userId) }, { viewModel.backToLevelSelect() })
            ChordBuilderPhase.RESULT -> ResultScreen(state.score, state.stars, state.correctCount, state.wrongCount, state.maxCombo, state.xpEarned, state.coinsEarned, { viewModel.restartLevel(userId) }, {}, { viewModel.backToLevelSelect() })
        }
    }
}

@Composable
private fun ChordBuilderLevelSelect(state: ChordBuilderState, viewModel: ChordBuilderViewModel, userId: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("🏗️ Construtor de Acordes", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
                    Card(Modifier.fillMaxWidth().aspectRatio(1f).clickable { viewModel.startLevel(userId, level) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF4F46E5)), shape = RoundedCornerShape(16.dp)) {
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
private fun ChordBuilderPlayScreen(state: ChordBuilderState, viewModel: ChordBuilderViewModel) {
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
        
        // Nome do acorde
        Text("Construa:", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(state.currentChordName, style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        
        Spacer(Modifier.height(16.dp))
        
        // Botão de ouvir o acorde
        OutlinedButton(onClick = { viewModel.playChord() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(if (state.isPlayingChord) Icons.Default.VolumeUp else Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.isPlayingChord) "Tocando..." else "Ouvir Acorde", color = Color.White)
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Feedback
        state.roundResult?.let { result ->
            val (text, color) = when (result) {
                RoundResult.SUCCESS -> "✅ Correto!" to Color(0xFF4CAF50)
                RoundResult.FAIL -> "❌ Incorreto" to Color(0xFFE91E63)
            }
            Text(text, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        
        Spacer(Modifier.weight(1f))
        
        // Teclado virtual (12 notas)
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (0..11).forEach { noteIndex ->
                val isSelected = viewModel.isNoteSelected(noteIndex)
                val isCorrect = viewModel.isNoteCorrect(noteIndex)
                val showResult = state.roundResult != null
                
                val bgColor = when {
                    showResult && isCorrect && isSelected -> Color(0xFF4CAF50)
                    showResult && isCorrect && !isSelected -> Color(0xFFFFD700)
                    showResult && !isCorrect && isSelected -> Color(0xFFE91E63)
                    isSelected -> Color.White
                    else -> Color(0xFF4F46E5)
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.6f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(bgColor)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable(enabled = state.roundResult == null) { viewModel.toggleNote(noteIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        viewModel.getNoteDisplayName(noteIndex),
                        color = if (isSelected && !showResult) Color(0xFF4F46E5) else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Botão confirmar
        if (state.roundResult == null) {
            Button(
                onClick = { viewModel.submitAnswer() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = state.selectedNotes.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(16.dp))
    }
}
