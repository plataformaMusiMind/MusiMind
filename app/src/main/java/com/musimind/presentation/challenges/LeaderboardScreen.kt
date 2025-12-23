package com.musimind.presentation.challenges

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.domain.model.LeaderboardEntry
import com.musimind.domain.model.LeaderboardType
import com.musimind.presentation.gamification.LeaderboardRow

/**
 * Leaderboard screen showing rankings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit = {},
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadLeaderboard()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab selector
            TabRow(
                selectedTabIndex = state.selectedType.ordinal,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                LeaderboardType.entries.forEach { type ->
                    Tab(
                        selected = state.selectedType == type,
                        onClick = { viewModel.selectType(type) },
                        text = {
                            Text(
                                when (type) {
                                    LeaderboardType.WEEKLY -> "Semanal"
                                    LeaderboardType.MONTHLY -> "Mensal"
                                    LeaderboardType.ALL_TIME -> "Geral"
                                    LeaderboardType.FRIENDS -> "Amigos"
                                }
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.entries.isEmpty() -> {
                    EmptyLeaderboard()
                }
                else -> {
                    // Top 3 podium
                    if (state.entries.size >= 3) {
                        PodiumDisplay(
                            first = state.entries[0],
                            second = state.entries[1],
                            third = state.entries[2],
                            onUserClick = onUserClick
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Rest of leaderboard
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.entries.drop(3),
                            key = { it.userId }
                        ) { entry ->
                            LeaderboardRow(
                                entry = entry,
                                onClick = { onUserClick(entry.userId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumDisplay(
    first: LeaderboardEntry,
    second: LeaderboardEntry,
    third: LeaderboardEntry,
    onUserClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Second place
        PodiumPosition(
            entry = second,
            height = 100.dp,
            color = Color(0xFFC0C0C0),
            onClick = { onUserClick(second.userId) }
        )
        
        // First place
        PodiumPosition(
            entry = first,
            height = 140.dp,
            color = Color(0xFFFFD700),
            onClick = { onUserClick(first.userId) }
        )
        
        // Third place
        PodiumPosition(
            entry = third,
            height = 80.dp,
            color = Color(0xFFCD7F32),
            onClick = { onUserClick(third.userId) }
        )
    }
}

@Composable
private fun PodiumPosition(
    entry: LeaderboardEntry,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(color, color.copy(alpha = 0.6f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.displayName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = entry.displayName.take(10),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
        
        Text(
            text = "${entry.xp} XP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Podium base
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(color, color.copy(alpha = 0.7f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EmptyLeaderboard() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Leaderboard,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Nenhum ranking disponível",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Complete exercícios para aparecer no ranking!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
