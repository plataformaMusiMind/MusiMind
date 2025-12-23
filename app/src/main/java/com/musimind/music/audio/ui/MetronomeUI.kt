package com.musimind.music.audio.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musimind.music.audio.metronome.MetronomeState

/**
 * Complete metronome control panel
 */
@Composable
fun MetronomePanel(
    state: MetronomeState,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onSoundToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            // BPM Display
            Text(
                text = "${state.bpm}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "BPM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // BPM Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Decrease by 10
                FilledTonalIconButton(
                    onClick = { onBpmChange(state.bpm - 10) }
                ) {
                    Text("-10", style = MaterialTheme.typography.labelSmall)
                }
                
                // Decrease by 1
                FilledTonalIconButton(
                    onClick = { onBpmChange(state.bpm - 1) }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "-1")
                }
                
                // Play/Stop
                FloatingActionButton(
                    onClick = if (state.isPlaying) onStop else onPlay,
                    containerColor = if (state.isPlaying) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Parar" else "Iniciar"
                    )
                }
                
                // Increase by 1
                FilledTonalIconButton(
                    onClick = { onBpmChange(state.bpm + 1) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "+1")
                }
                
                // Increase by 10
                FilledTonalIconButton(
                    onClick = { onBpmChange(state.bpm + 10) }
                ) {
                    Text("+10", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Slider
            Slider(
                value = state.bpm.toFloat(),
                onValueChange = { onBpmChange(it.toInt()) },
                valueRange = 40f..240f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Beat indicators and sound toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Beat indicators
                BeatIndicator(
                    currentBeat = state.currentBeat,
                    totalBeats = state.beatsPerMeasure
                )
                
                // Sound toggle
                IconButton(onClick = onSoundToggle) {
                    Icon(
                        imageVector = if (state.soundEnabled) 
                            Icons.Default.VolumeUp 
                        else 
                            Icons.Default.VolumeOff,
                        contentDescription = "Som",
                        tint = if (state.soundEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

/**
 * Visual beat indicator
 */
@Composable
fun BeatIndicator(
    currentBeat: Int,
    totalBeats: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 1..totalBeats) {
            val isActive = i == currentBeat
            val isAccent = i == 1
            
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.2f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                label = "beatScale"
            )
            
            val color by animateColorAsState(
                targetValue = when {
                    isActive && isAccent -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                animationSpec = tween(100),
                label = "beatColor"
            )
            
            Box(
                modifier = Modifier
                    .size(if (isAccent) 20.dp else 16.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Compact metronome widget
 */
@Composable
fun MetronomeCompact(
    state: MetronomeState,
    onToggle: () -> Unit,
    onBpmIncrease: () -> Unit,
    onBpmDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // BPM decrease
        IconButton(
            onClick = onBpmDecrease,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Diminuir BPM")
        }
        
        // BPM display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${state.bpm}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "BPM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        
        // BPM increase
        IconButton(
            onClick = onBpmIncrease,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aumentar BPM")
        }
        
        Divider(
            modifier = Modifier
                .height(32.dp)
                .width(1.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        
        // Play/Stop button
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (state.isPlaying) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

/**
 * BPM preset buttons
 */
@Composable
fun BpmPresets(
    currentBpm: Int,
    onBpmSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        60 to "Largo",
        80 to "Andante",
        100 to "Moderato",
        120 to "Allegro",
        140 to "Vivace",
        160 to "Presto"
    )
    
    Column(modifier = modifier) {
        Text(
            text = "Andamentos",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (bpm, name) ->
                val isSelected = currentBpm == bpm
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onBpmSelect(bpm) },
                    label = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$bpm",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )
            }
        }
    }
}
