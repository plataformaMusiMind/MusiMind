package com.musimind.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.musimind.domain.gamification.*
import kotlinx.coroutines.delay

/**
 * Reward Display Components
 * 
 * Visual feedback for rewards:
 * - XP gain popup
 * - Level up celebration
 * - Daily bonus claim
 * - Streak milestone
 * - Coins earned
 */

// ============================================
// XP Reward Popup
// ============================================

@Composable
fun XpRewardPopup(
    xpAmount: Int,
    bonuses: List<XpBonus>,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(3000)
        isVisible = false
        delay(500)
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // XP Icon with animation
                    AnimatedXpIcon()
                    
                    // XP Amount
                    Text(
                        text = "+$xpAmount XP",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Bonuses breakdown
                    if (bonuses.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            bonuses.forEach { bonus ->
                                BonusItem(bonus)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedXpIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "xpIcon")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xpScale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xpRotation"
    )
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .graphicsLayer { rotationZ = rotation }
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "XP",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
private fun BonusItem(bonus: XpBonus) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = bonus.name,
            style = MaterialTheme.typography.bodyMedium
        )
        if (bonus.multiplier > 1f) {
            Text(
                text = "×${String.format("%.1f", bonus.multiplier)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
        }
        if (bonus.flatBonus > 0) {
            Text(
                text = "+${bonus.flatBonus}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ============================================
// Level Up Celebration
// ============================================

@Composable
fun LevelUpCelebration(
    newLevel: Int,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(4000)
        isVisible = false
        delay(500)
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Confetti animation
                    LevelUpConfetti()
                    
                    Text(
                        text = "LEVEL UP!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Level number with ring
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Animated ring
                        CircularProgressIndicator(
                            progress = 1f,
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 8.dp
                        )
                        
                        Text(
                            text = "$newLevel",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    Text(
                        text = "Parabéns! Você chegou ao nível $newLevel!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    
                    // Unlocks info
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Novos exercícios desbloqueados!",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continuar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelUpConfetti() {
    // Simple animated confetti effect
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("🎉", "⭐", "🎊", "✨", "🏆").forEachIndexed { index, emoji ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 100),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "confettiY$index"
            )
            
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier.offset(y = offsetY.dp)
            )
        }
    }
}

// ============================================
// Daily Bonus Dialog
// ============================================

@Composable
fun DailyBonusDialog(
    reward: DailyBonusReward,
    onClaim: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Calendar icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFFFA000)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎁",
                        fontSize = 40.sp
                    )
                }
                
                Text(
                    text = "Bônus Diário!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (reward.day > 1) {
                    Text(
                        text = "Dia ${reward.day} de streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                // Rewards
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    RewardItem(
                        icon = "⚡",
                        value = "+${reward.xp}",
                        label = "XP"
                    )
                    RewardItem(
                        icon = "🪙",
                        value = "+${reward.coins}",
                        label = "Moedas"
                    )
                }
                
                Button(
                    onClick = {
                        onClaim()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Resgatar!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardItem(
    icon: String,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 32.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ============================================
// Streak Milestone Celebration
// ============================================

@Composable
fun StreakMilestoneCelebration(
    days: Int,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(3500)
        isVisible = false
        delay(500)
        onDismiss()
    }
    
    val milestoneEmoji = when {
        days >= 365 -> "👑"
        days >= 100 -> "🏆"
        days >= 50 -> "🌟"
        days >= 30 -> "🔥"
        days >= 14 -> "⚡"
        days >= 7 -> "✨"
        else -> "🎯"
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn() + fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = milestoneEmoji,
                        fontSize = 64.sp
                    )
                    
                    Text(
                        text = "$days DIAS!",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF6B35)
                    )
                    
                    Text(
                        text = "Incrível! Você manteve seu streak por $days dias seguidos!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    
                    // Fire animation
                    Row {
                        repeat(5) {
                            Text("🔥", fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// Coins Earned Animation
// ============================================

@Composable
fun CoinsEarnedPopup(
    amount: Int,
    reason: String,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(2500)
        isVisible = false
        delay(300)
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFD700).copy(alpha = 0.95f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "🪙", fontSize = 28.sp)
                    
                    Column {
                        Text(
                            text = "+$amount",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF5D4037)
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D4037).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// Reward Listener Composable
// ============================================

@Composable
fun RewardEventListener(
    pendingReward: PendingReward?,
    onDismiss: () -> Unit
) {
    when (val reward = pendingReward) {
        is PendingReward.XpGained -> {
            XpRewardPopup(
                xpAmount = reward.amount,
                bonuses = reward.bonuses,
                onDismiss = onDismiss
            )
        }
        is PendingReward.LevelUp -> {
            LevelUpCelebration(
                newLevel = reward.newLevel,
                onDismiss = onDismiss
            )
        }
        is PendingReward.DailyBonusAvailable -> {
            DailyBonusDialog(
                reward = reward.reward,
                onClaim = { /* handled by ViewModel */ },
                onDismiss = onDismiss
            )
        }
        is PendingReward.StreakMilestone -> {
            StreakMilestoneCelebration(
                days = reward.days,
                onDismiss = onDismiss
            )
        }
        is PendingReward.CoinsGained -> {
            CoinsEarnedPopup(
                amount = reward.amount,
                reason = reward.reason,
                onDismiss = onDismiss
            )
        }
        is PendingReward.AchievementUnlocked -> {
            // Achievement animation handled by AnimatedComponents
        }
        else -> {}
    }
}
