package com.musimind.presentation.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.domain.games.GameCategory
import com.musimind.domain.games.GameType
import com.musimind.domain.games.GamesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tela principal de jogos (Game Hub)
 * Lista todos os mini-jogos disponíveis organizados por categoria
 */
@Composable
fun GamesHubScreen(
    userId: String,
    onGameSelect: (String) -> Unit, // gameName
    onBack: () -> Unit,
    viewModel: GamesHubViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadGames(userId)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667EEA),
                        Color(0xFF764BA2)
                    )
                )
            )
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
                    text = "🎮 Mini-Jogos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Descrição
            Text(
                text = "Pratique seus conhecimentos musicais de forma divertida!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Agrupar jogos por categoria
                    val groupedGames = state.games.groupBy { it.gameCategory }
                    
                    groupedGames.forEach { (category, games) ->
                        item {
                            CategoryHeader(category = category)
                        }
                        
                        items(games) { game ->
                            val isUnlocked = game.name in state.unlockedGames
                            GameCard(
                                game = game,
                                isUnlocked = isUnlocked,
                                onClick = { 
                                    if (isUnlocked) {
                                        onGameSelect(game.name)
                                    }
                                }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: GameCategory) {
    val (icon, title, color) = when (category) {
        GameCategory.RHYTHM -> Triple("🥁", "Ritmo", Color(0xFFEF4444))
        GameCategory.MELODY -> Triple("🎵", "Melodia", Color(0xFF3B82F6))
        GameCategory.HARMONY -> Triple("🎹", "Harmonia", Color(0xFF10B981))
        GameCategory.THEORY -> Triple("📖", "Teoria", Color(0xFF8B5CF6))
        GameCategory.MIXED -> Triple("🌟", "Especial", Color(0xFFFF6B6B))
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(color.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun GameCard(
    game: GameType,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(game.color))
    } catch (e: Exception) {
        Color(0xFF6366F1)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnlocked) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) backgroundColor else Color.Gray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do jogo
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = getGameIcon(game.name)
                Text(text = icon, fontSize = 28.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info do jogo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                if (game.description != null) {
                    Text(
                        text = game.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                }
            }
            
            // Status
            if (isUnlocked) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Jogar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun getGameIcon(gameName: String): String {
    return when (gameName) {
        "note_catcher" -> "🎵"
        "rhythm_tap" -> "🥁"
        "interval_hero" -> "📏"
        "scale_puzzle" -> "🧩"
        "chord_match" -> "🃏"
        "melody_memory" -> "🧠"
        "key_shooter" -> "🎯"
        "tempo_run" -> "🏃"
        "solfege_sing" -> "🎤"
        "chord_builder" -> "🏗️"
        "progression_quest" -> "🗺️"
        "daily_challenge" -> "📅"
        else -> "🎮"
    }
}

/**
 * ViewModel para o Game Hub
 */
@HiltViewModel
class GamesHubViewModel @Inject constructor(
    private val gamesRepository: GamesRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(GamesHubState())
    val state: StateFlow<GamesHubState> = _state.asStateFlow()
    
    fun loadGames(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Carregar tipos de jogos
            val gamesResult = gamesRepository.loadGameTypes()
            val games = gamesResult.getOrNull() ?: emptyList()
            
            // Verificar quais estão desbloqueados
            val unlockedResult = gamesRepository.getUnlockedGames(userId)
            val unlocked = unlockedResult.getOrNull() ?: emptyList()
            
            _state.update { 
                it.copy(
                    isLoading = false,
                    games = games,
                    unlockedGames = unlocked
                )
            }
        }
    }
}

data class GamesHubState(
    val isLoading: Boolean = false,
    val games: List<GameType> = emptyList(),
    val unlockedGames: List<String> = emptyList(),
    val error: String? = null
)
