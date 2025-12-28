package com.musimind.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Premium Animated Components for MusiMind
 * 
 * High-quality animations for:
 * - Achievement unlocks with particles
 * - Success/error feedback
 * - Loading states
 * - XP gain animations
 * - Streak celebrations
 * - Level up effects
 */

// ============================================
// Achievement Unlock Animation
// ============================================

@Composable
fun AchievementUnlockAnimation(
    achievementTitle: String,
    achievementDescription: String,
    xpReward: Int,
    iconName: String = "emoji_events",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    // Animate in
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "achievementScale"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isVisible) 0f else -360f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "achievementRotation"
    )
    
    // Particle animation
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleOffset"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        // Particles background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawParticlesBurst(particleOffset)
        }
        
        // Main achievement card
        Card(
            modifier = Modifier
                .scale(scale)
                .rotate(rotation)
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(min = 280.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Crown icon
                Text(
                    text = "👑",
                    fontSize = 48.sp
                )
                
                Text(
                    text = "Conquista Desbloqueada!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Achievement icon with glow
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = achievementTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = achievementDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // XP reward with animation
                AnimatedXpBadge(xp = xpReward)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continuar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================
// Animated XP Badge
// ============================================

@Composable
fun AnimatedXpBadge(
    xp: Int,
    modifier: Modifier = Modifier
) {
    var displayedXp by remember { mutableStateOf(0) }
    
    LaunchedEffect(xp) {
        val steps = 20
        val stepDelay = 50L
        val stepAmount = xp / steps
        
        repeat(steps) { i ->
            displayedXp = stepAmount * (i + 1)
            delay(stepDelay)
        }
        displayedXp = xp
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "+$displayedXp",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "XP",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

// ============================================
// Success/Error Feedback Animation
// ============================================

@Composable
fun FeedbackAnimation(
    isSuccess: Boolean,
    message: String,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(1500)
        isVisible = false
        delay(300)
        onFinish()
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "feedbackScale"
    )
    
    val iconColor = if (isSuccess) 
        Color(0xFF4CAF50) else Color(0xFFF44336)
    val backgroundColor = if (isSuccess)
        Color(0xFF4CAF50).copy(alpha = 0.1f) 
        else Color(0xFFF44336).copy(alpha = 0.1f)
    
    Box(
        modifier = modifier.scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = iconColor
                )
            }
            
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        }
    }
}

// ============================================
// Loading Animation with Musical Notes
// ============================================

@Composable
fun MusicalLoadingAnimation(
    message: String = "Carregando...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    val noteOffsets = listOf(
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -20f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "note1"
        ),
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -20f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = 100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "note2"
        ),
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -20f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "note3"
        )
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("♪", "♫", "♬").forEachIndexed { index, note ->
                Text(
                    text = note,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.offset(y = noteOffsets[index].value.dp)
                )
            }
        }
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ============================================
// Streak Celebration Animation
// ============================================

@Composable
fun StreakCelebration(
    streakDays: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300)
        showDetails = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (showDetails) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "streakScale"
    )
    
    // Fire animation
    val infiniteTransition = rememberInfiniteTransition(label = "fire")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireScale"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .scale(scale)
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fire emoji with pulse
                Text(
                    text = "🔥",
                    fontSize = 64.sp,
                    modifier = Modifier.scale(fireScale)
                )
                
                Text(
                    text = "$streakDays Dias!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Incrível! Seu streak está pegando fogo!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // Bonus XP
                if (streakDays >= 7) {
                    AnimatedXpBadge(xp = streakDays * 5)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(onClick = onDismiss) {
                    Text("Continuar 🎵")
                }
            }
        }
    }
}

// ============================================
// Level Up Animation
// ============================================

@Composable
fun LevelUpAnimation(
    newLevel: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationPhase by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        delay(200)
        animationPhase = 1
        delay(500)
        animationPhase = 2
    }
    
    val starRotation by animateFloatAsState(
        targetValue = if (animationPhase >= 1) 360f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "starRotation"
    )
    
    val levelScale by animateFloatAsState(
        targetValue = if (animationPhase >= 2) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "levelScale"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Rotating stars
            Box(
                modifier = Modifier
                    .rotate(starRotation)
                    .size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⭐",
                    fontSize = 80.sp
                )
            }
            
            Text(
                text = "LEVEL UP!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.scale(levelScale)
            )
            
            Text(
                text = "Nível $newLevel",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(levelScale)
            )
            
            if (animationPhase >= 2) {
                Button(onClick = onDismiss) {
                    Text("Vamos lá! 🚀")
                }
            }
        }
    }
}

// ============================================
// Particle Burst Drawing
// ============================================

private fun DrawScope.drawParticlesBurst(offset: Float) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val maxRadius = size.minDimension / 2
    
    val particleColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFF6B6B), // Coral
        Color(0xFF4ECDC4), // Teal
        Color(0xFFFFE66D), // Yellow
        Color(0xFF95E1D3), // Mint
        Color(0xFFF38181)  // Pink
    )
    
    repeat(30) { i ->
        val angle = (i * 12f + offset * 360f) * (Math.PI / 180f).toFloat()
        val distance = (offset * maxRadius * 0.8f) + (i % 5) * 20f
        val alpha = 1f - offset.coerceIn(0f, 1f)
        
        val x = centerX + kotlin.math.cos(angle) * distance
        val y = centerY + kotlin.math.sin(angle) * distance
        
        drawCircle(
            color = particleColors[i % particleColors.size].copy(alpha = alpha * 0.7f),
            radius = 4f + (i % 4) * 2f,
            center = Offset(x, y)
        )
    }
}

// ============================================
// Pulsing Dot Indicator
// ============================================

@Composable
fun PulsingDot(
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ============================================
// Animated Progress Ring
// ============================================

@Composable
fun AnimatedProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val diameter = size.minDimension - stroke
        val radius = diameter / 2
        val centerOffset = Offset(size.width / 2, size.height / 2)
        
        // Background ring
        drawCircle(
            color = backgroundColor,
            radius = radius,
            center = centerOffset,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
        )
        
        // Progress arc
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = StrokeCap.Round
            )
        )
    }
}
