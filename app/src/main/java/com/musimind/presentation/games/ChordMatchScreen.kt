package com.musimind.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ChordMatchScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: ChordMatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) { viewModel.loadLevels(userId) }
    
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))))) {
        when (state.gamePhase) {
            ChordMatchPhase.LEVEL_SELECT -> ChordLevelSelect(state, viewModel, userId, onBack)
            ChordMatchPhase.PLAYING -> ChordPlayScreen(state, viewModel)
            ChordMatchPhase.PAUSED -> PauseScreen({ viewModel.resumeGame() }, { viewModel.restartLevel(userId) }, { viewModel.backToLevelSelect() })
            ChordMatchPhase.RESULT -> ResultScreen(state.score, state.stars, state.correctCount, state.wrongCount, state.maxCombo, state.xpEarned, state.coinsEarned, { viewModel.restartLevel(userId) }, {}, { viewModel.backToLevelSelect() })
        }
    }
}

@Composable
private fun ChordLevelSelect(state: ChordMatchState, viewModel: ChordMatchViewModel, userId: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("🃏 Match de Acordes", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
                    Card(Modifier.fillMaxWidth().aspectRatio(1f).clickable { viewModel.startLevel(userId, level) }, colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706)), shape = RoundedCornerShape(16.dp)) {
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
private fun ChordPlayScreen(state: ChordMatchState, viewModel: ChordMatchViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("PONTOS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Text("${state.score}", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("Pares: ${state.matchedPairs}/${state.totalPairs}", color = Color.White)
            Text("Jogadas: ${state.moves}", color = Color.White.copy(alpha = 0.7f))
            IconButton(onClick = { viewModel.pauseGame() }) { Icon(Icons.Default.Pause, null, tint = Color.White) }
        }
        
        // Grid de cartas
        val columns = if (state.cards.size <= 8) 2 else if (state.cards.size <= 12) 3 else 4
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.cards) { card ->
                MemoryCard(
                    card = card,
                    onClick = { viewModel.flipCard(card.id) }
                )
            }
        }
    }
}

@Composable
private fun MemoryCard(card: MatchCard, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(300),
        label = "cardRotation"
    )
    
    val cardColor = when {
        card.isMatched -> Color(0xFF4CAF50).copy(alpha = 0.8f)
        card.isFlipped -> Color.White
        else -> Color(0xFFD97706)
    }
    
    Card(
        modifier = Modifier
            .aspectRatio(0.75f)
            .graphicsLayer { rotationY = rotation }
            .clickable(enabled = !card.isFlipped && !card.isMatched) { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (rotation > 90f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (card.type == CardType.SOUND) {
                        Icon(Icons.Default.VolumeUp, null, tint = Color(0xFFD97706), modifier = Modifier.size(32.dp))
                    }
                    Text(
                        card.displayText,
                        color = if (card.isMatched) Color.White else Color(0xFFD97706),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (card.displayText.length > 6) 14.sp else 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { rotationY = 180f }
                    )
                }
            } else {
                Text("🎵", fontSize = 32.sp)
            }
        }
    }
}
