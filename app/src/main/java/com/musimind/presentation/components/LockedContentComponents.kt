package com.musimind.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musimind.domain.gamification.ExerciseWithLockStatus
import com.musimind.domain.gamification.UnlockRequirement

/**
 * Components for Locked Content Display
 * 
 * Displays exercise cards with lock status:
 * - Locked exercises (grayed out with lock icon)
 * - Completed exercises (with checkmark)
 * - Available exercises
 * - Premium exercises (with crown)
 */

// ============================================
// Exercise Card with Lock Status
// ============================================

@Composable
fun ExerciseCard(
    exerciseWithStatus: ExerciseWithLockStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exercise = exerciseWithStatus.exercise
    val isLocked = exerciseWithStatus.isLocked
    val isCompleted = exerciseWithStatus.isCompleted
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isLocked) 0.6f else 1f)
            .clickable(enabled = !isLocked, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isCompleted) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> Color(0xFF4CAF50)
                            isLocked -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isCompleted -> Icon(
                        Icons.Default.Check,
                        contentDescription = "Concluído",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    isLocked -> Icon(
                        Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    exercise.isPremium -> Icon(
                        Icons.Default.Star,
                        contentDescription = "Premium",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    else -> DifficultyIndicator(difficulty = exercise.difficulty)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = exercise.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (exercise.isPremium) {
                        PremiumBadge()
                    }
                }
                
                Text(
                    text = exercise.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Lock reason
                if (isLocked && exerciseWithStatus.lockReason != null) {
                    Text(
                        text = "🔒 ${exerciseWithStatus.lockReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF44336),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Rewards
            if (!isLocked && !isCompleted) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "+${exercise.xpReward}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "XP", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    if (exercise.coinsReward > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🪙", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "+${exercise.coinsReward}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }
            }
            
            // Chevron for available exercises
            if (!isLocked) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DifficultyIndicator(difficulty: Int) {
    val color = when (difficulty) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFF8BC34A)
        3 -> Color(0xFFFFC107)
        4 -> Color(0xFFFF9800)
        5 -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }
    
    Text(
        text = "$difficulty",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
private fun PremiumBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFFD700).copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "PRO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB8860B)
        )
    }
}

// ============================================
// Node Card with Lock Status (Learning Path)
// ============================================

@Composable
fun LearningNodeCard(
    title: String,
    description: String,
    isLocked: Boolean,
    isCompleted: Boolean,
    progress: Float, // 0-1
    totalLessons: Int,
    completedLessons: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isCompleted) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> Color(0xFF4CAF50)
                                isLocked -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isCompleted -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Concluído",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        isLocked -> Icon(
                            Icons.Default.Lock,
                            contentDescription = "Bloqueado",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        else -> Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
                
                if (!isLocked) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Progress bar (only for unlocked, non-completed nodes)
            if (!isLocked && !isCompleted && totalLessons > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    
                    Text(
                        text = "$completedLessons/$totalLessons",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Locked message
            if (isLocked) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Complete os módulos anteriores para desbloquear",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }
    }
}

// ============================================
// Unlock Requirement Display
// ============================================

@Composable
fun UnlockRequirementCard(
    requirement: UnlockRequirement,
    modifier: Modifier = Modifier
) {
    when (requirement) {
        is UnlockRequirement.LevelRequired -> {
            RequirementDisplay(
                icon = Icons.Default.TrendingUp,
                iconColor = MaterialTheme.colorScheme.primary,
                title = "Nível Necessário",
                description = "Atinja o nível ${requirement.requiredLevel} em ${requirement.category.displayName}",
                currentProgress = "${requirement.currentLevel}/${requirement.requiredLevel}",
                progress = requirement.currentLevel.toFloat() / requirement.requiredLevel,
                modifier = modifier
            )
        }
        is UnlockRequirement.NodeRequired -> {
            RequirementDisplay(
                icon = Icons.Default.School,
                iconColor = Color(0xFFFF9800),
                title = "Módulo Necessário",
                description = "Complete: ${requirement.requiredNodeTitle}",
                currentProgress = null,
                progress = 0f,
                modifier = modifier
            )
        }
        is UnlockRequirement.XpRequired -> {
            RequirementDisplay(
                icon = Icons.Default.Star,
                iconColor = Color(0xFFFFD700),
                title = "XP Necessário",
                description = "Acumule ${requirement.requiredXp} XP",
                currentProgress = "${requirement.currentXp}/${requirement.requiredXp}",
                progress = requirement.currentXp.toFloat() / requirement.requiredXp,
                modifier = modifier
            )
        }
        else -> {}
    }
}

@Composable
private fun RequirementDisplay(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    currentProgress: String?,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                if (currentProgress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = iconColor
                        )
                        Text(
                            text = currentProgress,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// Connected Line for Learning Path
// ============================================

@Composable
fun NodeConnector(
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(4.dp)
            .height(40.dp)
            .background(
                if (isCompleted) Color(0xFF4CAF50) 
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
    )
}
