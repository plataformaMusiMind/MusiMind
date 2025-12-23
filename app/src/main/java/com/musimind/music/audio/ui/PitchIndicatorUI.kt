package com.musimind.music.audio.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musimind.music.audio.pitch.PitchDetectionState
import com.musimind.music.audio.pitch.PitchResult
import com.musimind.music.audio.pitch.PitchUtils
import com.musimind.music.notation.model.Pitch

/**
 * Real-time pitch indicator showing detected note and tuning
 */
@Composable
fun PitchIndicator(
    state: PitchDetectionState,
    modifier: Modifier = Modifier,
    showFrequency: Boolean = false,
    showCents: Boolean = true
) {
    val animatedMatchQuality by animateFloatAsState(
        targetValue = state.matchQuality,
        animationSpec = tween(200),
        label = "matchQuality"
    )
    
    val indicatorColor by animateColorAsState(
        targetValue = when {
            !state.isListening -> Color.Gray
            state.matchQuality >= 90f -> Color(0xFF22C55E) // Green
            state.matchQuality >= 60f -> Color(0xFFFBBF24) // Yellow
            state.matchQuality > 0f -> Color(0xFFEF4444)   // Red
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(200),
        label = "indicatorColor"
    )
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pitch display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Solfege name
                Text(
                    text = state.displaySolfege,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = indicatorColor
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Note name
                Text(
                    text = state.displayNote,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Cents deviation meter
            if (showCents && state.currentPitch.isValid) {
                CentsDeviationMeter(
                    centsDeviation = state.currentPitch.centsDeviation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = state.displayCents,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Frequency display
            if (showFrequency) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.displayFrequency,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            // Match quality indicator (when there's a target)
            if (state.targetPitch != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Alvo: ${PitchUtils.pitchToSolfege(state.targetPitch)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    LinearProgressIndicator(
                        progress = { animatedMatchQuality / 100f },
                        modifier = Modifier
                            .width(100.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = indicatorColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "${animatedMatchQuality.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = indicatorColor
                    )
                }
            }
        }
    }
}

/**
 * Cents deviation meter (tuner style)
 */
@Composable
fun CentsDeviationMeter(
    centsDeviation: Float,
    modifier: Modifier = Modifier,
    range: Float = 50f
) {
    val normalizedPosition = (centsDeviation / range).coerceIn(-1f, 1f)
    
    val indicatorColor = when {
        kotlin.math.abs(centsDeviation) <= 10f -> Color(0xFF22C55E) // Green - in tune
        kotlin.math.abs(centsDeviation) <= 25f -> Color(0xFFFBBF24) // Yellow - close
        else -> Color(0xFFEF4444) // Red - out of tune
    }
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        // Draw track
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        
        // Draw center marker
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 2f
        )
        
        // Draw indicator
        val indicatorX = centerX + (normalizedPosition * size.width / 2)
        drawCircle(
            color = indicatorColor,
            radius = 12f,
            center = Offset(indicatorX, centerY)
        )
    }
}

/**
 * Circular pitch indicator with animated ring
 */
@Composable
fun CircularPitchIndicator(
    state: PitchDetectionState,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (state.isMatching) state.matchQuality / 100f else 0f,
        animationSpec = tween(300),
        label = "progress"
    )
    
    val ringColor by animateColorAsState(
        targetValue = when {
            state.matchQuality >= 90f -> Color(0xFF22C55E)
            state.matchQuality >= 60f -> Color(0xFFFBBF24)
            state.matchQuality > 0f -> Color(0xFFEF4444)
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "ringColor"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Background ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.Gray.copy(alpha = 0.1f),
                style = Stroke(width = 16f)
            )
        }
        
        // Progress ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )
        }
        
        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.displaySolfege,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
            
            Text(
                text = state.displayNote,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Target note display for exercises
 */
@Composable
fun TargetPitchDisplay(
    targetPitch: Pitch,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Cante esta nota:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .size((80 * if (isListening) pulseScale else 1f).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = PitchUtils.pitchToSolfege(targetPitch),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = PitchUtils.pitchToDisplayString(targetPitch),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Text(
                text = "${PitchUtils.pitchToFrequency(targetPitch).toInt()} Hz",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Microphone button with animation
 */
@Composable
fun MicrophoneButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val pulseAnimation = rememberInfiniteTransition(label = "micPulse")
    val pulseAlpha by pulseAnimation.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Pulsing background when listening
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha)
                    )
            )
        }
        
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            containerColor = if (isListening) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "Parar" else "Iniciar",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
