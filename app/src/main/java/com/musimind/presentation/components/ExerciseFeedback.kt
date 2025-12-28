package com.musimind.presentation.components

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Exercise Feedback Components
 * 
 * Visual feedback during music exercises:
 * - Real-time accuracy display
 * - Pitch matching indicator
 * - Note comparison view
 * - Result celebration
 * - Streak counter
 */

// ============================================
// Pitch Accuracy Meter
// ============================================

@Composable
fun PitchAccuracyMeter(
    currentPitch: Float, // 0-100 cents deviation
    targetPitch: String, // e.g., "Dó"
    isListening: Boolean = true,
    modifier: Modifier = Modifier
) {
    val deviation = currentPitch.coerceIn(-50f, 50f)
    
    val animatedDeviation by animateFloatAsState(
        targetValue = deviation,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "pitchDeviation"
    )
    
    val accuracyColor = when {
        kotlin.math.abs(deviation) < 5 -> Color(0xFF4CAF50)  // Green - excellent
        kotlin.math.abs(deviation) < 15 -> Color(0xFFFFC107) // Yellow - good
        kotlin.math.abs(deviation) < 30 -> Color(0xFFFF9800) // Orange - fair
        else -> Color(0xFFF44336)                            // Red - poor
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Target note display
        Text(
            text = targetPitch,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pitch meter bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Center line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline)
                    .align(Alignment.Center)
            )
            
            // Pitch indicator
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = (animatedDeviation * 2.5f).dp)
                        .clip(CircleShape)
                        .background(accuracyColor)
                        .align(Alignment.Center)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Baixo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Afinado",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accuracyColor
            )
            Text(
                text = "Alto",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Accuracy percentage
        if (isListening) {
            val accuracy = (100 - kotlin.math.abs(deviation) * 2).coerceIn(0f, 100f)
            Text(
                text = "${accuracy.toInt()}% afinado",
                style = MaterialTheme.typography.bodyMedium,
                color = accuracyColor,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// ============================================
// Circular Accuracy Display
// ============================================

@Composable
fun CircularAccuracyDisplay(
    accuracy: Float, // 0-100
    label: String = "Precisão",
    modifier: Modifier = Modifier
) {
    val animatedAccuracy by animateFloatAsState(
        targetValue = accuracy,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "accuracy"
    )
    
    val color = when {
        accuracy >= 90 -> Color(0xFF4CAF50)
        accuracy >= 70 -> Color(0xFFFFC107)
        accuracy >= 50 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val centerOffset = Offset(size.width / 2, size.height / 2)
            
            // Background arc
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = radius,
                center = centerOffset,
                style = Stroke(strokeWidth)
            )
            
            // Progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (animatedAccuracy / 100f),
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedAccuracy.toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================
// Note Comparison Card
// ============================================

@Composable
fun NoteComparisonCard(
    expectedNote: String,
    playedNote: String?,
    isCorrect: Boolean?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (isCorrect) {
                true -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                false -> Color(0xFFF44336).copy(alpha = 0.1f)
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expected note
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Esperado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = expectedNote,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Arrow or result icon
            AnimatedContent(
                targetState = isCorrect,
                label = "resultIcon"
            ) { correct ->
                when (correct) {
                    true -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Correto",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    false -> Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Incorreto",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(32.dp)
                    )
                    null -> Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Played note
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tocado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = playedNote ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (isCorrect) {
                        true -> Color(0xFF4CAF50)
                        false -> Color(0xFFF44336)
                        null -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

// ============================================
// Streak Counter  
// ============================================

@Composable
fun StreakCounter(
    currentStreak: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streakPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (currentStreak >= 5) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streakScale"
    )
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (currentStreak >= 5) {
                        listOf(Color(0xFFFF6B35), Color(0xFFFF9800))
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (currentStreak >= 5) "🔥" else "⚡",
            fontSize = 20.sp,
            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
        )
        
        Text(
            text = "$currentStreak",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (currentStreak >= 5) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Text(
            text = "acertos",
            style = MaterialTheme.typography.labelMedium,
            color = if (currentStreak >= 5) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

// ============================================
// Countdown Timer (for timed exercises)
// ============================================

@Composable
fun CountdownTimer(
    totalSeconds: Int,
    remainingSeconds: Int,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remainingSeconds.toFloat() / totalSeconds.toFloat()
    
    val progressColor = when {
        progress > 0.5f -> MaterialTheme.colorScheme.primary
        progress > 0.25f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "timerProgress"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = animatedProgress,
                color = progressColor,
                strokeWidth = 4.dp,
                modifier = Modifier.fillMaxSize()
            )
            
            Icon(
                imageVector = if (isRunning) Icons.Default.Timer else Icons.Default.Pause,
                contentDescription = null,
                tint = progressColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column {
            Text(
                text = formatTime(remainingSeconds),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
            Text(
                text = "restantes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) {
        "$minutes:${secs.toString().padStart(2, '0')}"
    } else {
        "${secs}s"
    }
}

// ============================================
// Exercise Result Summary
// ============================================

@Composable
fun ExerciseResultSummary(
    correctCount: Int,
    totalCount: Int,
    accuracy: Float,
    timeTaken: Int, // seconds
    xpEarned: Int,
    coinsEarned: Int,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPerfect = accuracy >= 100f
    val isGood = accuracy >= 80f
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Result emoji
        Text(
            text = when {
                isPerfect -> "🏆"
                isGood -> "⭐"
                accuracy >= 60 -> "👍"
                else -> "💪"
            },
            fontSize = 64.sp
        )
        
        // Title
        Text(
            text = when {
                isPerfect -> "Perfeito!"
                isGood -> "Muito bem!"
                accuracy >= 60 -> "Bom trabalho!"
                else -> "Continue praticando!"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = "$correctCount/$totalCount", label = "Acertos")
            StatItem(value = "${accuracy.toInt()}%", label = "Precisão")
            StatItem(value = formatTime(timeTaken), label = "Tempo")
        }
        
        // Rewards
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RewardBadge(value = "+$xpEarned", label = "XP", color = MaterialTheme.colorScheme.primary)
            RewardBadge(value = "+$coinsEarned", label = "Moedas", color = Color(0xFFFFD700))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Repetir")
            }
            
            Button(
                onClick = onContinue,
                modifier = Modifier.weight(1f)
            ) {
                Text("Continuar")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RewardBadge(value: String, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

// ============================================
// Listening Indicator (microphone animation)
// ============================================

@Composable
fun ListeningIndicator(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "listening")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            // Pulse rings
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size((50 + index * 15).dp)
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.1f - index * 0.03f
                            )
                        )
                )
            }
        }
        
        // Microphone icon
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = if (isListening) "Ouvindo..." else "Microfone",
            tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .size(32.dp)
                .rotate(if (isListening) rotation else 0f)
        )
    }
}
