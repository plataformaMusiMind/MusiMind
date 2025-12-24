package com.musimind.music.audio.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musimind.music.audio.pitch.PitchResult
import com.musimind.music.audio.pitch.PitchUtils
import com.musimind.ui.theme.*
import kotlin.math.abs

/**
 * Pitch indicator component for solfege exercises
 */
@Composable
fun PitchIndicator(
    pitchResult: PitchResult?,
    targetNote: String?,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val detectedNote = (pitchResult as? PitchResult.Detected)?.note ?: ""
    val cents = (pitchResult as? PitchResult.Detected)?.cents ?: 0
    val frequency = (pitchResult as? PitchResult.Detected)?.frequency ?: 0f
    
    // Determine match quality
    val matchQuality = if (pitchResult is PitchResult.Detected && targetNote != null) {
        when {
            pitchResult.matchesPitch(targetNote, 10) -> MatchQuality.EXCELLENT
            pitchResult.matchesPitch(targetNote, 25) -> MatchQuality.GOOD
            pitchResult.matchesPitch(targetNote, 50) -> MatchQuality.FAIR
            else -> MatchQuality.NONE
        }
    } else {
        MatchQuality.NONE
    }
    
    val indicatorColor = when {
        !isListening -> Color.Gray
        matchQuality == MatchQuality.EXCELLENT -> GamificationGreen
        matchQuality == MatchQuality.GOOD -> GamificationYellow
        matchQuality == MatchQuality.FAIR -> GamificationOrange
        else -> WarningRed
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
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
            // Note display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = detectedNote.ifEmpty { "--" },
                    style = MaterialTheme.typography.displayMedium,
                    color = indicatorColor
                )
                
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = null,
                    tint = indicatorColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cents deviation meter
            CentsDeviationMeter(
                cents = cents,
                matchQuality = matchQuality,
                isListening = isListening
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Frequency display
            Text(
                text = if (frequency > 0) String.format("%.1f Hz", frequency) else "-- Hz",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Target note (if any)
            if (targetNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Alvo: ${PitchUtils.frequencyToNoteName(PitchUtils.noteNameToFrequency(targetNote))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Cents deviation meter
 */
@Composable
private fun CentsDeviationMeter(
    cents: Int,
    matchQuality: MatchQuality,
    isListening: Boolean
) {
    val indicatorColor = when {
        !isListening -> Color.Gray
        matchQuality == MatchQuality.EXCELLENT -> GamificationGreen
        matchQuality == MatchQuality.GOOD -> GamificationYellow
        matchQuality == MatchQuality.FAIR -> GamificationOrange
        else -> WarningRed
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Cents deviation bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            // Background bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // Center line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .align(Alignment.Center)
                    .background(MaterialTheme.colorScheme.primary)
            )
            
            // Indicator
            if (isListening && cents != 0) {
                val normalizedOffset = (cents.coerceIn(-50, 50) / 50f) * 0.4f
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center)
                        .offset(x = (normalizedOffset * 150).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(indicatorColor)
                )
            }
        }
        
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("♭", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${abs(cents)} cents", style = MaterialTheme.typography.bodySmall, color = indicatorColor)
            Text("♯", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Match quality levels
 */
enum class MatchQuality {
    NONE,
    FAIR,
    GOOD,
    EXCELLENT
}

/**
 * Compact pitch display for exercise screens
 */
@Composable
fun CompactPitchDisplay(
    pitchResult: PitchResult?,
    targetNote: String?,
    modifier: Modifier = Modifier
) {
    val detected = pitchResult as? PitchResult.Detected
    val isMatching = detected?.matchesPitch(targetNote ?: "", 25) == true
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isMatching) GamificationGreen.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = if (isMatching) GamificationGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = detected?.note ?: "--",
            style = MaterialTheme.typography.titleMedium,
            color = if (isMatching) GamificationGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Note visualization with frequency wave
 */
@Composable
fun NoteVisualization(
    targetNote: Int?, // MIDI note number
    modifier: Modifier = Modifier
) {
    if (targetNote == null) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        // Draw wave
        val frequency = PitchUtils.midiToFrequency(targetNote)
        val normalizedFreq = (frequency / 440f).coerceIn(0.5f, 2f)
        
        var previousPoint: Offset? = null
        for (x in 0..width.toInt() step 2) {
            val normalizedX = x / width * 4 * Math.PI.toFloat()
            val y = centerY + (height / 3) * kotlin.math.sin(normalizedX * normalizedFreq + phase)
            
            previousPoint?.let { prev ->
                drawLine(
                    color = Color(0xFF7B2FF7),
                    start = prev,
                    end = Offset(x.toFloat(), y),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
            previousPoint = Offset(x.toFloat(), y)
        }
    }
}
