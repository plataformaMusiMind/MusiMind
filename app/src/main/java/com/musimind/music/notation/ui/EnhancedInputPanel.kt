package com.musimind.music.notation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musimind.music.notation.model.AccidentalType
import com.musimind.music.notation.model.NoteName
import com.musimind.music.notation.smufl.SMuFLGlyphs

/**
 * Enhanced Input Panel for Music Score Editing
 * 
 * Features:
 * - Note name selection (Dó, Ré, Mi, Fá, Sol, Lá, Si)
 * - Duration selection with visual indicators
 * - Accidental selection (natural, sharp, flat)
 * - Octave adjustment
 * - Rest insertion
 * - Undo/Redo buttons
 * - Premium design with animations
 */

// ============================================
// Data Classes for Input State
// ============================================

data class NoteInputState(
    val selectedNote: NoteName = NoteName.C,
    val selectedOctave: Int = 4,
    val selectedDuration: Float = 1f,  // Quarter note default
    val selectedAccidental: AccidentalType? = null,
    val isRestMode: Boolean = false
)

// ============================================
// Duration Option
// ============================================

data class DurationOption(
    val beats: Float,
    val label: String,
    val portuguese: String
)

val durationOptions = listOf(
    DurationOption(4f, "𝅝", "Semibreve"),
    DurationOption(2f, "𝅗𝅥", "Mínima"),
    DurationOption(1f, "𝅘𝅥", "Semínima"),
    DurationOption(0.5f, "𝅘𝅥𝅮", "Colcheia"),
    DurationOption(0.25f, "𝅘𝅥𝅯", "Semicolcheia")
)

// ============================================
// Note Names in Portuguese
// ============================================

val noteNames = listOf(
    NoteName.C to "Dó",
    NoteName.D to "Ré",
    NoteName.E to "Mi",
    NoteName.F to "Fá",
    NoteName.G to "Sol",
    NoteName.A to "Lá",
    NoteName.B to "Si"
)

// ============================================
// Enhanced Input Panel
// ============================================

@Composable
fun EnhancedInputPanel(
    state: NoteInputState,
    onNoteSelected: (NoteName) -> Unit,
    onDurationSelected: (Float) -> Unit,
    onOctaveChanged: (Int) -> Unit,
    onAccidentalSelected: (AccidentalType?) -> Unit,
    onRestToggled: (Boolean) -> Unit,
    onAddNote: () -> Unit,
    onAddRest: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean = true,
    canRedo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(surfaceColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with undo/redo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Entrada de Notas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Desfazer",
                        tint = if (canUndo) primaryColor else onSurfaceColor.copy(alpha = 0.38f)
                    )
                }
                
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "Refazer",
                        tint = if (canRedo) primaryColor else onSurfaceColor.copy(alpha = 0.38f)
                    )
                }
            }
        }
        
        Divider(color = onSurfaceColor.copy(alpha = 0.1f))
        
        // Note Selection Row
        Text(
            text = "Nota",
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceColor.copy(alpha = 0.6f)
        )
        
        NoteSelectionRow(
            selectedNote = state.selectedNote,
            onNoteSelected = onNoteSelected,
            isRestMode = state.isRestMode
        )
        
        // Duration & Octave Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Duration section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Duração",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceColor.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                DurationSelectionRow(
                    selectedDuration = state.selectedDuration,
                    onDurationSelected = onDurationSelected
                )
            }
            
            // Octave section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Oitava",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceColor.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                OctaveSelector(
                    octave = state.selectedOctave,
                    onOctaveChanged = onOctaveChanged
                )
            }
        }
        
        // Accidentals Row
        Text(
            text = "Acidentes",
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceColor.copy(alpha = 0.6f)
        )
        
        AccidentalSelectionRow(
            selectedAccidental = state.selectedAccidental,
            onAccidentalSelected = onAccidentalSelected
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Add Rest Button
            OutlinedButton(
                onClick = onAddRest,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PauseCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pausa")
            }
            
            // Add Note Button
            Button(
                onClick = onAddNote,
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar Nota", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================
// Note Selection Row
// ============================================

@Composable
private fun NoteSelectionRow(
    selectedNote: NoteName,
    onNoteSelected: (NoteName) -> Unit,
    isRestMode: Boolean
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(noteNames) { (note, label) ->
            NoteButton(
                label = label,
                isSelected = selectedNote == note && !isRestMode,
                onClick = { onNoteSelected(note) }
            )
        }
    }
}

@Composable
private fun NoteButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(),
        label = "noteScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant,
        label = "noteColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "noteTextColor"
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp
        )
    }
}

// ============================================
// Duration Selection Row
// ============================================

@Composable
private fun DurationSelectionRow(
    selectedDuration: Float,
    onDurationSelected: (Float) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        durationOptions.forEach { option ->
            DurationButton(
                option = option,
                isSelected = selectedDuration == option.beats,
                onClick = { onDurationSelected(option.beats) }
            )
        }
    }
}

@Composable
private fun DurationButton(
    option: DurationOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                      else Color.Transparent,
        label = "durationBg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "durationBorder"
    )
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = option.label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = option.portuguese.take(4),
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ============================================
// Octave Selector
// ============================================

@Composable
private fun OctaveSelector(
    octave: Int,
    onOctaveChanged: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = { if (octave > 2) onOctaveChanged(octave - 1) },
            modifier = Modifier.size(32.dp),
            enabled = octave > 2
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Oitava abaixo"
            )
        }
        
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp, 36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$octave",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        IconButton(
            onClick = { if (octave < 6) onOctaveChanged(octave + 1) },
            modifier = Modifier.size(32.dp),
            enabled = octave < 6
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Oitava acima"
            )
        }
    }
}

// ============================================
// Accidental Selection Row
// ============================================

@Composable
private fun AccidentalSelectionRow(
    selectedAccidental: AccidentalType?,
    onAccidentalSelected: (AccidentalType?) -> Unit
) {
    val accidentals = listOf(
        null to "♮" to "Natural",
        AccidentalType.SHARP to "♯" to "Sustenido",
        AccidentalType.FLAT to "♭" to "Bemol"
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Natural (no accidental)
        AccidentalButton(
            symbol = "♮",
            label = "Natural",
            isSelected = selectedAccidental == null,
            onClick = { onAccidentalSelected(null) }
        )
        
        // Sharp
        AccidentalButton(
            symbol = "♯",
            label = "Sustenido",
            isSelected = selectedAccidental == AccidentalType.SHARP,
            onClick = { onAccidentalSelected(AccidentalType.SHARP) }
        )
        
        // Flat
        AccidentalButton(
            symbol = "♭",
            label = "Bemol",
            isSelected = selectedAccidental == AccidentalType.FLAT,
            onClick = { onAccidentalSelected(AccidentalType.FLAT) }
        )
    }
}

@Composable
private fun AccidentalButton(
    symbol: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                      else Color.Transparent,
        label = "accidentalBg"
    )
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = symbol,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

// ============================================
// Compact Input Panel (for smaller screens)
// ============================================

@Composable
fun CompactInputPanel(
    state: NoteInputState,
    onNoteSelected: (NoteName) -> Unit,
    onDurationSelected: (Float) -> Unit,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Note buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            noteNames.forEach { (note, label) ->
                val isSelected = state.selectedNote == note
                
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onNoteSelected(note) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label.take(2),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Add button
        FloatingActionButton(
            onClick = onAddNote,
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar")
        }
    }
}
