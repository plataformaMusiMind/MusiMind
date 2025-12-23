package com.musimind.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.domain.model.NodeStatus
import com.musimind.ui.theme.NodeAvailable
import com.musimind.ui.theme.NodeCompleted
import com.musimind.ui.theme.NodeLocked
import com.musimind.ui.theme.Primary
import com.musimind.ui.theme.PrimaryVariant
import com.musimind.ui.theme.StreakOrange
import com.musimind.ui.theme.XpGold

/**
 * Home Screen - Main learning path (Duolingo-style)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNodeClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar with Stats
        TopAppBar(
            title = {
                Text(
                    text = "MusiMind",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                // Streak indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = "Streak",
                        modifier = Modifier.size(24.dp),
                        tint = StreakOrange
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${uiState.streak}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StreakOrange
                    )
                }
                
                // XP indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "XP",
                        modifier = Modifier.size(24.dp),
                        tint = XpGold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${uiState.xp}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = XpGold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Level Progress
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nível ${uiState.level}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${uiState.xp} / ${uiState.xpToNextLevel} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { uiState.levelProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        
        // Learning Path
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(uiState.nodes) { index, node ->
                // Draw connecting line to next node
                if (index < uiState.nodes.size - 1) {
                    LearningPathConnector(
                        fromCompleted = node.status == NodeStatus.COMPLETED,
                        modifier = Modifier.height(40.dp)
                    )
                }
                
                // Node item
                LearningPathNode(
                    title = node.title,
                    status = node.status,
                    xpReward = node.xpReward,
                    offsetX = if (index % 2 == 0) (-40).dp else 40.dp,
                    onClick = { 
                        if (node.status != NodeStatus.LOCKED) {
                            onNodeClick(node.id)
                        }
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LearningPathNode(
    title: String,
    status: NodeStatus,
    xpReward: Int,
    offsetX: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val nodeColor = when (status) {
        NodeStatus.COMPLETED -> NodeCompleted
        NodeStatus.AVAILABLE, NodeStatus.IN_PROGRESS -> NodeAvailable
        NodeStatus.LOCKED -> NodeLocked
    }
    
    val scale by animateFloatAsState(
        targetValue = if (status == NodeStatus.AVAILABLE) 1.1f else 1f,
        animationSpec = tween(500),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(x = offsetX)
    ) {
        Box(
            modifier = Modifier
                .size((80 * scale).dp)
                .shadow(
                    elevation = if (status == NodeStatus.AVAILABLE) 8.dp else 2.dp,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(
                    if (status == NodeStatus.LOCKED) {
                        Brush.linearGradient(listOf(nodeColor, nodeColor))
                    } else {
                        Brush.linearGradient(
                            listOf(nodeColor, nodeColor.copy(alpha = 0.8f))
                        )
                    }
                )
                .clickable(enabled = status != NodeStatus.LOCKED, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (status) {
                    NodeStatus.COMPLETED -> Icons.Filled.Check
                    NodeStatus.LOCKED -> Icons.Filled.Lock
                    else -> Icons.Filled.PlayArrow
                },
                contentDescription = null,
                modifier = Modifier.size((32 * scale).dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (status == NodeStatus.AVAILABLE) FontWeight.Bold else FontWeight.Normal,
            color = if (status == NodeStatus.LOCKED) 
                MaterialTheme.colorScheme.onSurfaceVariant 
            else 
                MaterialTheme.colorScheme.onBackground
        )
        
        if (status != NodeStatus.LOCKED) {
            Text(
                text = "+$xpReward XP",
                style = MaterialTheme.typography.labelSmall,
                color = XpGold
            )
        }
    }
}

@Composable
private fun LearningPathConnector(
    fromCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (fromCompleted) NodeCompleted else NodeLocked
    
    Canvas(modifier = modifier.width(4.dp)) {
        val pathEffect = if (!fromCompleted) {
            PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        } else null
        
        drawLine(
            color = lineColor,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = 4f,
            pathEffect = pathEffect
        )
    }
}
