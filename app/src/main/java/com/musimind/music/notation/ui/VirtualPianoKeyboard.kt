package com.musimind.music.notation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musimind.music.notation.model.NoteName
import com.musimind.music.notation.model.Pitch

/**
 * Virtual MIDI Piano Keyboard Component
 * 
 * Features:
 * - Realistic piano layout with white and black keys
 * - Touch input for note selection
 * - Visual feedback on key press
 * - Configurable octave range
 * - Labels in Portuguese solfege (Dó, Ré, Mi...)
 * - Integration with pitch output
 */

// ============================================
// Key Configuration
// ============================================

data class PianoKey(
    val noteName: NoteName,
    val isBlack: Boolean,
    val solfegeName: String,
    val offsetFraction: Float = 0f // For black keys positioning
)

private val pianoKeys = listOf(
    PianoKey(NoteName.C, false, "Dó"),
    PianoKey(NoteName.C_SHARP, true, "Dó#", 0.75f),
    PianoKey(NoteName.D, false, "Ré"),
    PianoKey(NoteName.D_SHARP, true, "Ré#", 1.75f),
    PianoKey(NoteName.E, false, "Mi"),
    PianoKey(NoteName.F, false, "Fá"),
    PianoKey(NoteName.F_SHARP, true, "Fá#", 3.75f),
    PianoKey(NoteName.G, false, "Sol"),
    PianoKey(NoteName.G_SHARP, true, "Sol#", 4.75f),
    PianoKey(NoteName.A, false, "Lá"),
    PianoKey(NoteName.A_SHARP, true, "Lá#", 5.75f),
    PianoKey(NoteName.B, false, "Si")
)

private val whiteKeys = pianoKeys.filter { !it.isBlack }
private val blackKeys = pianoKeys.filter { it.isBlack }

// ============================================
// Piano Keyboard Composable
// ============================================

@Composable
fun VirtualPianoKeyboard(
    onNotePressed: (Pitch) -> Unit,
    onNoteReleased: (Pitch) -> Unit = {},
    modifier: Modifier = Modifier,
    startOctave: Int = 4,
    numOctaves: Int = 2,
    whiteKeyWidth: Dp = 48.dp,
    whiteKeyHeight: Dp = 180.dp,
    blackKeyWidth: Dp = 32.dp,
    blackKeyHeight: Dp = 110.dp,
    showLabels: Boolean = true,
    highlightedNotes: Set<Pitch> = emptySet()
) {
    var pressedKeys by remember { mutableStateOf(setOf<Pitch>()) }
    
    Box(
        modifier = modifier
            .height(whiteKeyHeight)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF2D2D2D)
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
    ) {
        Row {
            repeat(numOctaves) { octaveIndex ->
                val currentOctave = startOctave + octaveIndex
                
                Box {
                    // White keys
                    Row {
                        whiteKeys.forEach { key ->
                            val pitch = Pitch(key.noteName, currentOctave)
                            val isPressed = pressedKeys.contains(pitch)
                            val isHighlighted = highlightedNotes.contains(pitch)
                            
                            WhiteKey(
                                key = key,
                                pitch = pitch,
                                isPressed = isPressed,
                                isHighlighted = isHighlighted,
                                width = whiteKeyWidth,
                                height = whiteKeyHeight - 8.dp,
                                showLabel = showLabels,
                                onPress = {
                                    pressedKeys = pressedKeys + pitch
                                    onNotePressed(pitch)
                                },
                                onRelease = {
                                    pressedKeys = pressedKeys - pitch
                                    onNoteReleased(pitch)
                                }
                            )
                        }
                    }
                    
                    // Black keys (overlay)
                    blackKeys.forEach { key ->
                        val pitch = Pitch(key.noteName, currentOctave)
                        val isPressed = pressedKeys.contains(pitch)
                        val isHighlighted = highlightedNotes.contains(pitch)
                        
                        BlackKey(
                            key = key,
                            pitch = pitch,
                            isPressed = isPressed,
                            isHighlighted = isHighlighted,
                            width = blackKeyWidth,
                            height = blackKeyHeight,
                            offsetX = whiteKeyWidth * key.offsetFraction,
                            showLabel = showLabels,
                            onPress = {
                                pressedKeys = pressedKeys + pitch
                                onNotePressed(pitch)
                            },
                            onRelease = {
                                pressedKeys = pressedKeys - pitch
                                onNoteReleased(pitch)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// White Key Component
// ============================================

@Composable
private fun WhiteKey(
    key: PianoKey,
    pitch: Pitch,
    isPressed: Boolean,
    isHighlighted: Boolean,
    width: Dp,
    height: Dp,
    showLabel: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(),
        label = "whiteKeyScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color(0xFFE0E0E0)
            isHighlighted -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.White
        },
        label = "whiteKeyBg"
    )
    
    val shadowElevation = if (isPressed) 1.dp else 4.dp
    
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .padding(horizontal = 1.dp)
            .scale(scale)
            .shadow(shadowElevation, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.9f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color(0xFFBDBDBD),
                shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        if (showLabel) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = key.solfegeName,
                    fontSize = 12.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                    color = if (isHighlighted) 
                        MaterialTheme.colorScheme.primary 
                        else Color(0xFF424242)
                )
                
                // Octave indicator for C notes
                if (key.noteName == NoteName.C) {
                    Text(
                        text = "${pitch.octave}",
                        fontSize = 10.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
        }
    }
}

// ============================================
// Black Key Component
// ============================================

@Composable
private fun BlackKey(
    key: PianoKey,
    pitch: Pitch,
    isPressed: Boolean,
    isHighlighted: Boolean,
    width: Dp,
    height: Dp,
    offsetX: Dp,
    showLabel: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(),
        label = "blackKeyScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color(0xFF424242)
            isHighlighted -> MaterialTheme.colorScheme.primary
            else -> Color(0xFF1A1A1A)
        },
        label = "blackKeyBg"
    )
    
    Box(
        modifier = Modifier
            .offset(x = offsetX)
            .width(width)
            .height(height)
            .scale(scale)
            .shadow(if (isPressed) 2.dp else 6.dp, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.95f),
                        backgroundColor
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        if (showLabel) {
            Text(
                text = key.solfegeName,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = if (isHighlighted) Color.White else Color(0xFFBDBDBD),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}

// ============================================
// Compact Piano Keyboard (Single Octave)
// ============================================

@Composable
fun CompactPianoKeyboard(
    onNotePressed: (Pitch) -> Unit,
    modifier: Modifier = Modifier,
    octave: Int = 4,
    highlightedNotes: Set<Pitch> = emptySet()
) {
    VirtualPianoKeyboard(
        onNotePressed = onNotePressed,
        modifier = modifier,
        startOctave = octave,
        numOctaves = 1,
        whiteKeyWidth = 40.dp,
        whiteKeyHeight = 120.dp,
        blackKeyWidth = 26.dp,
        blackKeyHeight = 70.dp,
        showLabels = true,
        highlightedNotes = highlightedNotes
    )
}

// ============================================
// Mini Piano Keyboard (For Note Selection Only)
// ============================================

@Composable
fun MiniPianoKeyboard(
    selectedNote: NoteName?,
    onNoteSelected: (NoteName) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFF212121), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        whiteKeys.forEach { key ->
            val isSelected = selectedNote == key.noteName
            
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                    else Color.White,
                label = "miniKeyBg"
            )
            
            val textColor = if (isSelected) Color.White else Color.Black
            
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(backgroundColor)
                    .clickable { onNoteSelected(key.noteName) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key.solfegeName.take(2),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

// ============================================
// Preview/Demo Component
// ============================================

@Composable
fun PianoKeyboardDemo() {
    var lastPressed by remember { mutableStateOf<Pitch?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Última nota: ${lastPressed?.let { "${it.note.name} ${it.octave}" } ?: "Nenhuma"}",
            style = MaterialTheme.typography.titleMedium
        )
        
        VirtualPianoKeyboard(
            onNotePressed = { lastPressed = it },
            startOctave = 4,
            numOctaves = 2
        )
    }
}
