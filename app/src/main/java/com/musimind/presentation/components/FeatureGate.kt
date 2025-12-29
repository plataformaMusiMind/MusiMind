package com.musimind.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.domain.model.FeatureAccessResult
import com.musimind.domain.model.SubscriptionTier
import com.musimind.presentation.subscription.SubscriptionViewModel
import com.musimind.ui.theme.Primary
import com.musimind.ui.theme.PrimaryVariant
import com.musimind.ui.theme.XpGold
import kotlinx.coroutines.launch

/**
 * Feature Gate Component
 * 
 * Wraps content with subscription-based access control.
 * Philosophy: Show everything, but with limits.
 * 
 * Usage:
 * ```
 * FeatureGate(featureKey = FeatureKey.SOLFEGE_SING) {
 *     SolfegeSingContent()
 * }
 * ```
 */
@Composable
fun FeatureGate(
    featureKey: String,
    showLimitIndicator: Boolean = true,
    onUpgradeClick: () -> Unit = {},
    viewModel: SubscriptionViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    var accessResult by remember { mutableStateOf<FeatureAccessResult?>(null) }
    var isChecking by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(featureKey) {
        isChecking = true
        accessResult = viewModel.checkFeatureAccess(featureKey)
        isChecking = false
    }
    
    Box {
        // Always show content
        content()
        
        // Loading overlay
        if (isChecking) {
            // Just show content while checking
        }
        
        // Limit reached overlay
        accessResult?.let { access ->
            if (!access.allowed && access.isLimitReached) {
                LimitReachedOverlay(
                    accessResult = access,
                    onUpgradeClick = onUpgradeClick
                )
            } else if (showLimitIndicator && access.dailyRemaining != null && access.dailyRemaining <= 3) {
                // Show remaining indicator
                LimitIndicator(
                    remaining = access.dailyRemaining,
                    total = access.dailyLimit ?: 0,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

/**
 * Overlay shown when daily/monthly limit is reached
 */
@Composable
fun LimitReachedOverlay(
    accessResult: FeatureAccessResult,
    onUpgradeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Primary, PrimaryVariant))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Limite Atingido!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (accessResult.reason == "daily_limit_reached") {
                        "Você usou ${accessResult.dailyUsed}/${accessResult.dailyLimit} vezes hoje.\nVolte amanhã ou faça upgrade!"
                    } else {
                        "Você atingiu o limite mensal.\nFaça upgrade para continuar!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Upgrade button
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = XpGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (accessResult.upgradeTo) {
                            "spalla" -> "Upgrade para Spalla"
                            "maestro" -> "Upgrade para Maestro"
                            else -> "Ver Planos"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Show benefits
                Spacer(modifier = Modifier.height(16.dp))
                
                accessResult.upgradeTarget?.let { tier ->
                    Text(
                        text = when (tier) {
                            SubscriptionTier.SPALLA -> "• Até 70% de todas as funcionalidades\n• 15+ exercícios por dia\n• Sem anúncios"
                            SubscriptionTier.MAESTRO -> "• 100% de acesso ilimitado\n• Modo offline\n• Recursos de professor"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Indicator showing remaining uses
 */
@Composable
fun LimitIndicator(
    remaining: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) remaining.toFloat() / total else 1f
    val color = when {
        remaining > total / 2 -> Color(0xFF4CAF50)
        remaining > 2 -> Color(0xFFFFC107)
        else -> Color(0xFFE91E63)
    }
    
    Card(
        modifier = modifier
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$remaining restantes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Premium badge for features
 */
@Composable
fun PremiumBadge(
    tier: SubscriptionTier,
    modifier: Modifier = Modifier
) {
    val (color, text, icon) = when (tier) {
        SubscriptionTier.SPALLA -> Triple(
            Color(0xFF6366F1), // Indigo
            "Spalla",
            Icons.Default.MusicNote
        )
        SubscriptionTier.MAESTRO -> Triple(
            XpGold,
            "Maestro",
            Icons.Default.Star
        )
        else -> return
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Unlock percentage indicator
 */
@Composable
fun UnlockProgressIndicator(
    unlockedPercent: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Conteúdo desbloqueado",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$unlockedPercent%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { unlockedPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        if (unlockedPercent < 100) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Faça upgrade para desbloquear mais!",
                style = MaterialTheme.typography.labelSmall,
                color = XpGold
            )
        }
    }
}

/**
 * Compact upgrade prompt
 */
@Composable
fun CompactUpgradePrompt(
    tier: SubscriptionTier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = XpGold
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Desbloqueie mais com ${tier.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${tier.percentage}% de acesso",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Primary
            )
        }
    }
}

/**
 * Teacher/School limit warning
 */
@Composable
fun TeacherLimitWarning(
    currentCount: Int,
    maxCount: Int,
    entity: String, // "alunos" ou "turmas"
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentCount >= maxCount) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Limite atingido",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Você tem $currentCount/$maxCount $entity",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                TextButton(onClick = onUpgradeClick) {
                    Text("Upgrade")
                }
            }
        }
    } else if (currentCount >= maxCount - 1) {
        // Almost at limit
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Primary.copy(alpha = 0.05f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Você tem $currentCount/$maxCount $entity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
