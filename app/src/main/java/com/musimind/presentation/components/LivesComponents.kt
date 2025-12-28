package com.musimind.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.musimind.domain.gamification.LivesState
import kotlinx.coroutines.delay
import java.time.Duration

/**
 * Lives (Hearts) UI Components
 * 
 * Displays and manages user lives visually:
 * - Heart indicator in app bar
 * - Full hearts display
 * - Out of lives dialog
 * - Refill options
 */

// ============================================
// Hearts Indicator (for App Bar)
// ============================================

@Composable
fun HeartsIndicator(
    livesState: LivesState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (livesState) {
        is LivesState.Loading -> {
            // Placeholder
            Box(
                modifier = modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        
        is LivesState.Loaded -> {
            val hasUnlimited = livesState.hasUnlimitedHearts
            val lives = livesState.currentLives
            val maxLives = livesState.maxLives
            
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (lives > 0 || hasUnlimited) 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else 
                            Color(0xFFF44336).copy(alpha = 0.2f)
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Heart icon
                Icon(
                    imageVector = if (lives > 0 || hasUnlimited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Vidas",
                    tint = if (lives > 0 || hasUnlimited) Color(0xFFE91E63) else Color(0xFFBDBDBD),
                    modifier = Modifier.size(20.dp)
                )
                
                // Lives count or infinite symbol
                Text(
                    text = if (hasUnlimited) "∞" else "$lives",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (lives > 0 || hasUnlimited) Color(0xFFE91E63) else Color(0xFFBDBDBD)
                )
                
                // Timer for regeneration
                if (!hasUnlimited && lives < maxLives && livesState.timeUntilNextLife != null) {
                    Text(
                        text = formatDuration(livesState.timeUntilNextLife),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        is LivesState.Error -> {
            // Error state
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Erro",
                tint = Color(0xFFF44336),
                modifier = modifier
                    .size(24.dp)
                    .clickable(onClick = onClick)
            )
        }
    }
}

// ============================================
// Full Hearts Display
// ============================================

@Composable
fun FullHeartsDisplay(
    currentLives: Int,
    maxLives: Int,
    hasUnlimited: Boolean,
    timeUntilNextLife: Duration?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hearts row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasUnlimited) {
                // Unlimited hearts display
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE91E63),
                                    Color(0xFFC2185B)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "∞",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Text(
                    text = "Vidas Ilimitadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE91E63)
                )
            } else {
                repeat(maxLives) { index ->
                    val isFilled = index < currentLives
                    AnimatedHeart(isFilled = isFilled)
                }
            }
        }
        
        // Timer for next life
        if (!hasUnlimited && currentLives < maxLives && timeUntilNextLife != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Próxima vida em ${formatDuration(timeUntilNextLife)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnimatedHeart(isFilled: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isFilled) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heartScale"
    )
    
    val color by animateColorAsState(
        targetValue = if (isFilled) Color(0xFFE91E63) else Color(0xFFBDBDBD),
        label = "heartColor"
    )
    
    Icon(
        imageVector = if (isFilled) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .size(32.dp)
            .scale(scale)
    )
}

// ============================================
// Out of Lives Dialog
// ============================================

@Composable
fun OutOfLivesDialog(
    onWatchAd: () -> Unit,
    onBuyLives: () -> Unit,
    onWait: () -> Unit,
    onDismiss: () -> Unit,
    timeUntilNextLife: Duration? = null
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
                // Broken heart animation
                BrokenHeartAnimation()
                
                Text(
                    text = "Você ficou sem vidas!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Não se preocupe, você tem algumas opções:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                // Options
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Watch ad option
                    OutlinedCard(
                        onClick = onWatchAd,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Assistir vídeo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "+1 vida grátis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "Grátis",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    
                    // Buy lives option
                    Card(
                        onClick = onBuyLives,
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Row {
                                    repeat(5) {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recarregar vidas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "5 vidas cheias",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "💎 50",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Wait option
                    TextButton(
                        onClick = onWait,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (timeUntilNextLife != null) 
                                "Esperar (${formatDuration(timeUntilNextLife)})"
                            else 
                                "Esperar 30 minutos"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrokenHeartAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "brokenHeart")
    
    val shake by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )
    
    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Left half
        Icon(
            Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier
                .size(64.dp)
                .offset(x = shake.dp)
        )
    }
}

// ============================================
// Life Lost Animation
// ============================================

@Composable
fun LifeLostAnimation(
    remainingLives: Int,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(2000)
        isVisible = false
        delay(300)
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Breaking heart icon
                Icon(
                    Icons.Default.HeartBroken,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(80.dp)
                )
                
                Text(
                    text = "-1 Vida",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                
                // Remaining lives
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < remainingLives) 
                                Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (index < remainingLives) 
                                Color(0xFFE91E63) else Color(0xFFBDBDBD),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Text(
                    text = if (remainingLives > 0) 
                        "Restam $remainingLives vidas" 
                    else 
                        "Você ficou sem vidas!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }
    }
}

// ============================================
// Accuracy Warning (Below 75%)
// ============================================

@Composable
fun AccuracyWarning(
    accuracy: Float,
    threshold: Float = 75f,
    modifier: Modifier = Modifier
) {
    if (accuracy < threshold) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cuidado!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        text = "Precisão abaixo de ${threshold.toInt()}% resulta em perda de vida",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100).copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = "${accuracy.toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

// ============================================
// Helper Functions
// ============================================

private fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes()
    val seconds = duration.seconds % 60
    
    return if (minutes > 0) {
        "${minutes}min ${seconds}s"
    } else {
        "${seconds}s"
    }
}
