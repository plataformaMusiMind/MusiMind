package com.musimind.presentation.components

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.domain.model.SubscriptionTier
import com.musimind.presentation.subscription.SubscriptionViewModel
import com.musimind.ui.theme.Primary
import com.musimind.ui.theme.PrimaryVariant
import com.musimind.ui.theme.XpGold

/**
 * Subscription status card for profile screen
 */
@Composable
fun SubscriptionCard(
    onUpgradeClick: () -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (state.currentTier) {
                SubscriptionTier.MAESTRO -> XpGold.copy(alpha = 0.1f)
                SubscriptionTier.SPALLA -> Color(0xFF6366F1).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tier icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when (state.currentTier) {
                                SubscriptionTier.MAESTRO -> Brush.linearGradient(
                                    listOf(XpGold, Color(0xFFFFB74D))
                                )
                                SubscriptionTier.SPALLA -> Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF818CF8))
                                )
                                else -> Brush.linearGradient(
                                    listOf(Primary.copy(alpha = 0.5f), PrimaryVariant.copy(alpha = 0.5f))
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (state.currentTier) {
                            SubscriptionTier.MAESTRO -> Icons.Default.Star
                            SubscriptionTier.SPALLA -> Icons.Default.MusicNote
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Plano ${state.currentTier.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = when {
                            state.isInTrial -> "Trial: ${state.trialDaysRemaining ?: 0} dias restantes"
                            state.isActive -> "${state.currentTier.percentage}% de acesso"
                            state.isPastDue -> "Pagamento pendente"
                            else -> "Grátis - ${state.currentTier.percentage}% de acesso"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            state.isPastDue -> MaterialTheme.colorScheme.error
                            state.isInTrial -> XpGold
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                // Status badge
                if (state.isActive && state.currentTier != SubscriptionTier.FREEMIUM) {
                    Badge(
                        containerColor = Color(0xFF4CAF50)
                    ) {
                        Text("Ativo", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar showing access percentage
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Acesso ao conteúdo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.currentTier.percentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (state.currentTier) {
                            SubscriptionTier.MAESTRO -> XpGold
                            SubscriptionTier.SPALLA -> Color(0xFF6366F1)
                            else -> Primary
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LinearProgressIndicator(
                    progress = { state.currentTier.percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when (state.currentTier) {
                        SubscriptionTier.MAESTRO -> XpGold
                        SubscriptionTier.SPALLA -> Color(0xFF6366F1)
                        else -> Primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.currentTier != SubscriptionTier.MAESTRO) {
                    Button(
                        onClick = onUpgradeClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (state.currentTier) {
                                SubscriptionTier.SPALLA -> XpGold
                                else -> Primary
                            }
                        )
                    ) {
                        Icon(
                            Icons.Default.Star,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (state.currentTier == SubscriptionTier.FREEMIUM) 
                                "Fazer Upgrade" 
                            else 
                                "Ir para Maestro",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                
                if (state.isActive && state.currentTier != SubscriptionTier.FREEMIUM) {
                    OutlinedButton(
                        onClick = onManageClick,
                        modifier = if (state.currentTier == SubscriptionTier.MAESTRO) 
                            Modifier.fillMaxWidth() 
                        else 
                            Modifier,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Gerenciar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            
            // Features hint
            if (state.currentTier != SubscriptionTier.MAESTRO) {
                Spacer(modifier = Modifier.height(12.dp))
                
                val nextFeatures = when (state.currentTier) {
                    SubscriptionTier.FREEMIUM -> listOf(
                        "15+ exercícios/dia",
                        "Sem anúncios",
                        "Pitch detection"
                    )
                    SubscriptionTier.SPALLA -> listOf(
                        "100% ilimitado",
                        "Modo Professor",
                        "Modo Offline"
                    )
                    else -> emptyList()
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    nextFeatures.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact upgrade banner
 */
@Composable
fun UpgradeBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Primary, PrimaryVariant)
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = XpGold,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Desbloqueie 100%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "7 dias grátis • Cancele quando quiser",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}
