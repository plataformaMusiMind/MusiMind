package com.musimind.presentation.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musimind.music.notation.model.*
import com.musimind.music.notation.smufl.SMuFLGlyphs

/**
 * Reusable Note Input Panel component for melodic perception exercises
 * 
 * Features:
 * - Note selection (C-D-E-F-G-A-B)
 * - Octave control with arrows
 * - Duration selector (whole to sixteenth)
 * - Rest selector (matching durations)
 * - Accidental buttons (flat, sharp)
 * - Action buttons (add note, add rest, verify)
 */
@Composable
fun NoteInputPanel(
    selectedNote: NoteName?,
    selectedOctave: Int,
    selectedDuration: Float,
    selectedAccidental: AccidentalType?,
    currentNoteNumber: Int,
    totalNotes: Int,
    onNoteSelected: (NoteName) -> Unit,
    onOctaveChange: (Int) -> Unit,
    onDurationSelected: (Float) -> Unit,
    onAccidentalSelected: (AccidentalType?) -> Unit,
    onAddNote: () -> Unit,
    onAddRest: () -> Unit,
    onVerify: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Note Selection (Center) + Accidentals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoteButtonsRow(
                    selectedNote = selectedNote,
                    onNoteSelected = onNoteSelected,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                AccidentalButtons(
                    selectedAccidental = selectedAccidental,
                    onAccidentalSelected = onAccidentalSelected
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OctaveControl(
                    octave = selectedOctave,
                    onOctaveChange = onOctaveChange
                )
            }
            
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            
            // Row 2: Durations (Notes & Rests)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    DurationSelector(
                        selectedDuration = selectedDuration,
                        onDurationSelected = onDurationSelected,
                        isRest = false
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    DurationSelector(
                        selectedDuration = selectedDuration,
                        onDurationSelected = onDurationSelected,
                        isRest = true
                    )
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            
            // Row 3: Navigation + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoteNavigator(
                    currentNote = currentNoteNumber,
                    totalNotes = totalNotes,
                    onPrevious = onNavigatePrevious,
                    onNext = onNavigateNext
                )
                
                ActionButtons(
                    onAddNote = onAddNote,
                    onAddRest = onAddRest,
                    onVerify = onVerify
                )
            }
        }
    }
}

@Composable
private fun NoteButtonsRow(
    selectedNote: NoteName?,
    onNoteSelected: (NoteName) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly // Distribute evenly
    ) {
        val noteLabels = listOf("C", "D", "E", "F", "G", "A", "B")
        val noteNames = NoteName.entries
        
        noteLabels.forEachIndexed { index, label ->
            val noteName = noteNames[index]
            val isSelected = selectedNote == noteName
            
            Box(
                modifier = Modifier
                    .size(40.dp) // Slightly larger touch target
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onNoteSelected(noteName) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun OctaveControl(
    octave: Int,
    onOctaveChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(4.dp)
    ) {
        IconButton(
            onClick = { onOctaveChange(-1) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Octave down",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = octave.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        IconButton(
            onClick = { onOctaveChange(1) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Octave up",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NoteNavigator(
    currentNote: Int,
    totalNotes: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(4.dp)
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(24.dp),
            enabled = currentNote > 1
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous",
                tint = if (currentNote > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        
        Text(
            text = "$currentNote/$totalNotes",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(24.dp),
            enabled = currentNote < totalNotes
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next",
                tint = if (currentNote < totalNotes) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun AccidentalButtons(
    selectedAccidental: AccidentalType?,
    onAccidentalSelected: (AccidentalType?) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Flat button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selectedAccidental == AccidentalType.FLAT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable { 
                    onAccidentalSelected(
                        if (selectedAccidental == AccidentalType.FLAT) null else AccidentalType.FLAT
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♭",
                color = if (selectedAccidental == AccidentalType.FLAT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )
        }
        
        // Sharp button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selectedAccidental == AccidentalType.SHARP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable { 
                    onAccidentalSelected(
                        if (selectedAccidental == AccidentalType.SHARP) null else AccidentalType.SHARP
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♯",
                color = if (selectedAccidental == AccidentalType.SHARP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onAddNote: () -> Unit,
    onAddRest: () -> Unit,
    onVerify: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add Note
        Button(
            onClick = onAddNote,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Nota")
        }
        
        // Add Rest
        Button(
            onClick = onAddRest,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer, // Changed to Timer as Pause might be ambiguous or unavailable in specific set
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pausa")
        }
        
        // Verify
        Button(
            onClick = onVerify,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Verificar")
        }
    }
}

@Composable
private fun DurationSelector(
    selectedDuration: Float,
    onDurationSelected: (Float) -> Unit,
    isRest: Boolean
) {
    val durations = listOf(
        4f to if (isRest) "𝄻" else "𝅝",   // Whole
        2f to if (isRest) "𝄼" else "𝅗𝅥",   // Half
        1f to if (isRest) "𝄽" else "♩",   // Quarter
        0.5f to if (isRest) "𝄾" else "♪", // Eighth
        0.25f to if (isRest) "𝄿" else "𝅘𝅥𝅯" // Sixteenth
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (isRest) "Pausas" else "Duração",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            durations.forEach { (duration, symbol) ->
                val isSelected = selectedDuration == duration
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable { onDurationSelected(duration) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbol,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}
