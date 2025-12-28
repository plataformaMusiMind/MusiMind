package com.musimind.music.notation.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.musimind.music.notation.model.*
import com.musimind.music.notation.smufl.SMuFLGlyphs
import android.graphics.Typeface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interactive Score Editor Component
 * 
 * Provides:
 * - Note selection via tap
 * - Note dragging to change pitch
 * - Visual feedback for selected notes
 * - Undo/Redo support via command pattern
 * 
 * Designed for melodic perception exercises and basic score editing.
 */

// ============================================
// Edit Commands for Undo/Redo
// ============================================

sealed class EditCommand {
    abstract fun execute(): Boolean
    abstract fun undo(): Boolean
    
    data class AddNote(
        val note: Note,
        val notesList: MutableList<Note>,
        val position: Int = -1
    ) : EditCommand() {
        override fun execute(): Boolean {
            return if (position >= 0 && position <= notesList.size) {
                notesList.add(position, note)
                true
            } else {
                notesList.add(note)
                true
            }
        }
        
        override fun undo(): Boolean {
            return notesList.remove(note)
        }
    }
    
    data class RemoveNote(
        val note: Note,
        val notesList: MutableList<Note>
    ) : EditCommand() {
        private var originalIndex: Int = -1
        
        override fun execute(): Boolean {
            originalIndex = notesList.indexOf(note)
            return notesList.remove(note)
        }
        
        override fun undo(): Boolean {
            return if (originalIndex >= 0) {
                notesList.add(originalIndex, note)
                true
            } else false
        }
    }
    
    data class ModifyNote(
        val originalNote: Note,
        val modifiedNote: Note,
        val notesList: MutableList<Note>
    ) : EditCommand() {
        override fun execute(): Boolean {
            val index = notesList.indexOf(originalNote)
            return if (index >= 0) {
                notesList[index] = modifiedNote
                true
            } else false
        }
        
        override fun undo(): Boolean {
            val index = notesList.indexOf(modifiedNote)
            return if (index >= 0) {
                notesList[index] = originalNote
                true
            } else false
        }
    }
    
    data class ChangePitch(
        val note: Note,
        val newPitch: Pitch,
        val notesList: MutableList<Note>
    ) : EditCommand() {
        private val oldPitch = note.pitch
        
        override fun execute(): Boolean {
            val index = notesList.indexOf(note)
            return if (index >= 0) {
                notesList[index] = note.copy(pitch = newPitch)
                true
            } else false
        }
        
        override fun undo(): Boolean {
            val modifiedNote = notesList.find { it.id == note.id }
            val index = notesList.indexOf(modifiedNote)
            return if (index >= 0 && modifiedNote != null) {
                notesList[index] = modifiedNote.copy(pitch = oldPitch)
                true
            } else false
        }
    }
    
    data class ChangeDuration(
        val note: Note,
        val newDuration: Float,
        val notesList: MutableList<Note>
    ) : EditCommand() {
        private val oldDuration = note.durationBeats
        
        override fun execute(): Boolean {
            val index = notesList.indexOf(note)
            return if (index >= 0) {
                notesList[index] = note.copy(durationBeats = newDuration)
                true
            } else false
        }
        
        override fun undo(): Boolean {
            val modifiedNote = notesList.find { it.id == note.id }
            val index = notesList.indexOf(modifiedNote)
            return if (index >= 0 && modifiedNote != null) {
                notesList[index] = modifiedNote.copy(durationBeats = oldDuration)
                true
            } else false
        }
    }
}

// ============================================
// Command Manager for Undo/Redo
// ============================================

class CommandManager(private val maxHistorySize: Int = 50) {
    private val undoStack = mutableListOf<EditCommand>()
    private val redoStack = mutableListOf<EditCommand>()
    
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    
    fun executeCommand(command: EditCommand): Boolean {
        val result = command.execute()
        if (result) {
            undoStack.add(command)
            if (undoStack.size > maxHistorySize) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
            updateState()
        }
        return result
    }
    
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        
        val command = undoStack.removeLast()
        val result = command.undo()
        if (result) {
            redoStack.add(command)
            updateState()
        }
        return result
    }
    
    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        
        val command = redoStack.removeLast()
        val result = command.execute()
        if (result) {
            undoStack.add(command)
            updateState()
        }
        return result
    }
    
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateState()
    }
    
    private fun updateState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}

// ============================================
// Score Editor State
// ============================================

data class ScoreEditorState(
    val notes: List<Note> = emptyList(),
    val selectedNoteId: String? = null,
    val isDragging: Boolean = false,
    val dragOffset: Offset = Offset.Zero,
    val isEditable: Boolean = true,
    val showGrid: Boolean = true,
    val snapToGrid: Boolean = true
)

// ============================================
// Score Editor ViewModel-like Manager
// ============================================

class ScoreEditorManager(
    initialNotes: List<Note> = emptyList()
) {
    private val notesList = initialNotes.toMutableList()
    val commandManager = CommandManager()
    
    private val _state = MutableStateFlow(ScoreEditorState(notes = notesList))
    val state: StateFlow<ScoreEditorState> = _state.asStateFlow()
    
    fun selectNote(noteId: String?) {
        _state.value = _state.value.copy(selectedNoteId = noteId)
    }
    
    fun addNote(note: Note, position: Int = -1): Boolean {
        val command = EditCommand.AddNote(note, notesList, position)
        val result = commandManager.executeCommand(command)
        if (result) {
            _state.value = _state.value.copy(notes = notesList.toList())
        }
        return result
    }
    
    fun removeNote(note: Note): Boolean {
        val command = EditCommand.RemoveNote(note, notesList)
        val result = commandManager.executeCommand(command)
        if (result) {
            _state.value = _state.value.copy(
                notes = notesList.toList(),
                selectedNoteId = if (_state.value.selectedNoteId == note.id) null 
                                 else _state.value.selectedNoteId
            )
        }
        return result
    }
    
    fun removeSelectedNote(): Boolean {
        val selectedId = _state.value.selectedNoteId ?: return false
        val note = notesList.find { it.id == selectedId } ?: return false
        return removeNote(note)
    }
    
    fun changePitch(noteId: String, newPitch: Pitch): Boolean {
        val note = notesList.find { it.id == noteId } ?: return false
        val command = EditCommand.ChangePitch(note, newPitch, notesList)
        val result = commandManager.executeCommand(command)
        if (result) {
            _state.value = _state.value.copy(notes = notesList.toList())
        }
        return result
    }
    
    fun changeDuration(noteId: String, newDuration: Float): Boolean {
        val note = notesList.find { it.id == noteId } ?: return false
        val command = EditCommand.ChangeDuration(note, newDuration, notesList)
        val result = commandManager.executeCommand(command)
        if (result) {
            _state.value = _state.value.copy(notes = notesList.toList())
        }
        return result
    }
    
    fun undo(): Boolean {
        val result = commandManager.undo()
        if (result) {
            _state.value = _state.value.copy(notes = notesList.toList())
        }
        return result
    }
    
    fun redo(): Boolean {
        val result = commandManager.redo()
        if (result) {
            _state.value = _state.value.copy(notes = notesList.toList())
        }
        return result
    }
    
    fun setNotes(notes: List<Note>) {
        notesList.clear()
        notesList.addAll(notes)
        commandManager.clear()
        _state.value = _state.value.copy(notes = notesList.toList(), selectedNoteId = null)
    }
    
    fun getNotes(): List<Note> = notesList.toList()
    
    fun getSelectedNote(): Note? {
        return _state.value.selectedNoteId?.let { id ->
            notesList.find { it.id == id }
        }
    }
    
    fun toggleEditable() {
        _state.value = _state.value.copy(isEditable = !_state.value.isEditable)
    }
    
    fun setShowGrid(show: Boolean) {
        _state.value = _state.value.copy(showGrid = show)
    }
    
    fun setSnapToGrid(snap: Boolean) {
        _state.value = _state.value.copy(snapToGrid = snap)
    }
}

// ============================================
// Score Editor Composable
// ============================================

@Composable
fun ScoreEditor(
    manager: ScoreEditorManager,
    modifier: Modifier = Modifier,
    staffHeight: Float = 120f,
    staffSpacing: Float = 8f,
    noteWidth: Float = 24f,
    onNoteSelected: ((Note?) -> Unit)? = null,
    onNoteChanged: ((Note) -> Unit)? = null
) {
    val state by manager.state.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // Load Bravura font
    val bravuraTypeface = remember {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Bravura.otf")
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
    
    // Calculate note positions
    val notePositions = remember(state.notes) {
        calculateNotePositions(state.notes, noteWidth, staffSpacing)
    }
    
    // Selection highlight color
    val selectionColor = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { staffHeight.toDp() })
            .background(Color.White)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.isEditable) {
                    if (state.isEditable) {
                        detectTapGestures { offset ->
                            // Find tapped note
                            val tappedNote = findNoteAtPosition(
                                offset,
                                state.notes,
                                notePositions,
                                noteWidth,
                                staffSpacing
                            )
                            manager.selectNote(tappedNote?.id)
                            onNoteSelected?.invoke(tappedNote)
                        }
                    }
                }
                .pointerInput(state.isEditable, state.selectedNoteId) {
                    if (state.isEditable && state.selectedNoteId != null) {
                        detectDragGestures(
                            onDragStart = { /* Start dragging */ },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Calculate new pitch based on drag
                                val selectedNote = state.notes.find { it.id == state.selectedNoteId }
                                if (selectedNote != null) {
                                    val position = notePositions[selectedNote.id] ?: return@detectDragGestures
                                    val newY = position.y + dragAmount.y
                                    val newPitch = yPositionToPitch(newY, staffSpacing, staffHeight)
                                    if (newPitch != selectedNote.pitch) {
                                        manager.changePitch(selectedNote.id, newPitch)
                                        val updatedNote = state.notes.find { it.id == state.selectedNoteId }
                                        updatedNote?.let { onNoteChanged?.invoke(it) }
                                    }
                                }
                            },
                            onDragEnd = { /* End dragging */ }
                        )
                    }
                }
        ) {
            // Draw staff lines
            drawStaffLines(staffHeight, staffSpacing)
            
            // Draw grid if enabled
            if (state.showGrid) {
                drawBeatGrid(state.notes.size, noteWidth, staffHeight)
            }
            
            // Draw notes
            state.notes.forEachIndexed { index, note ->
                val position = notePositions[note.id] ?: return@forEachIndexed
                val isSelected = note.id == state.selectedNoteId
                
                drawNote(
                    note = note,
                    x = position.x,
                    y = position.y,
                    staffSpacing = staffSpacing,
                    typeface = bravuraTypeface,
                    isSelected = isSelected,
                    selectionColor = selectionColor
                )
            }
        }
    }
}

// ============================================
// Helper Functions
// ============================================

private fun calculateNotePositions(
    notes: List<Note>,
    noteWidth: Float,
    staffSpacing: Float
): Map<String, Offset> {
    val positions = mutableMapOf<String, Offset>()
    val startX = 60f // After clef
    
    notes.forEachIndexed { index, note ->
        val x = startX + index * noteWidth * 1.5f
        val y = pitchToYPosition(note.pitch, staffSpacing, 120f) // Assuming 120 staff height
        positions[note.id] = Offset(x, y)
    }
    
    return positions
}

private fun pitchToYPosition(pitch: Pitch, staffSpacing: Float, staffHeight: Float): Float {
    // Middle line (B4 in treble clef) is at center
    val middleY = staffHeight / 2f
    val staffPosition = pitch.staffPosition(ClefType.TREBLE)
    
    // Each staff position is half a staff space
    return middleY - (staffPosition - 4) * (staffSpacing / 2f)
}

private fun yPositionToPitch(y: Float, staffSpacing: Float, staffHeight: Float): Pitch {
    val middleY = staffHeight / 2f
    val staffPosition = 4 - ((y - middleY) / (staffSpacing / 2f)).toInt()
    
    // Convert staff position to pitch
    // Staff position 0 = E4, 2 = F4, 4 = G4 (middle line), 6 = A4, 8 = B4
    val octaveOffset = staffPosition / 7
    val noteIndex = ((staffPosition % 7) + 7) % 7
    
    val noteName = when (noteIndex) {
        0 -> NoteName.E
        1 -> NoteName.F
        2 -> NoteName.G
        3 -> NoteName.A
        4 -> NoteName.B
        5 -> NoteName.C
        6 -> NoteName.D
        else -> NoteName.C
    }
    
    val octave = 4 + octaveOffset
    
    return Pitch(noteName, octave)
}

private fun findNoteAtPosition(
    offset: Offset,
    notes: List<Note>,
    positions: Map<String, Offset>,
    noteWidth: Float,
    staffSpacing: Float
): Note? {
    val hitRadius = noteWidth * 0.75f
    
    return notes.find { note ->
        val notePos = positions[note.id] ?: return@find false
        val dx = offset.x - notePos.x
        val dy = offset.y - notePos.y
        (dx * dx + dy * dy) <= hitRadius * hitRadius
    }
}

private fun DrawScope.drawStaffLines(staffHeight: Float, staffSpacing: Float) {
    val centerY = staffHeight / 2f
    val lineColor = Color(0xFF1A1A1A)
    
    for (i in -2..2) {
        val y = centerY + i * staffSpacing
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawBeatGrid(noteCount: Int, noteWidth: Float, staffHeight: Float) {
    val gridColor = Color(0xFFE0E0E0)
    val startX = 60f
    
    for (i in 0 until maxOf(noteCount + 4, 8)) {
        val x = startX + i * noteWidth * 1.5f
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, staffHeight),
            strokeWidth = 0.5f
        )
    }
}

private fun DrawScope.drawNote(
    note: Note,
    x: Float,
    y: Float,
    staffSpacing: Float,
    typeface: Typeface,
    isSelected: Boolean,
    selectionColor: Color
) {
    val paint = android.graphics.Paint().apply {
        this.typeface = typeface
        textSize = staffSpacing * 4
        isAntiAlias = true
        color = if (isSelected) selectionColor.toArgb() else android.graphics.Color.BLACK
    }
    
    // Selection highlight
    if (isSelected) {
        drawCircle(
            color = selectionColor.copy(alpha = 0.2f),
            radius = staffSpacing * 1.2f,
            center = Offset(x, y)
        )
    }
    
    // Draw notehead
    val notehead = SMuFLGlyphs.getNoteheadForDuration(note.durationBeats)
    drawContext.canvas.nativeCanvas.drawText(
        notehead.toString(),
        x - staffSpacing * 0.5f,
        y + staffSpacing * 0.5f,
        paint
    )
    
    // Draw stem if needed
    if (note.durationBeats < 4f) {
        val stemUp = y > staffSpacing * 5 / 2 // Below middle line
        val stemLength = staffSpacing * 3.5f
        val stemX = if (stemUp) x + staffSpacing * 0.5f else x - staffSpacing * 0.15f
        
        paint.strokeWidth = 2f
        paint.style = android.graphics.Paint.Style.STROKE
        
        val stemStartY = y
        val stemEndY = if (stemUp) y - stemLength else y + stemLength
        
        drawContext.canvas.nativeCanvas.drawLine(
            stemX, stemStartY, stemX, stemEndY, paint
        )
    }
    
    // Draw ledger lines if needed
    drawLedgerLines(x, y, staffSpacing)
}

private fun DrawScope.drawLedgerLines(x: Float, y: Float, staffSpacing: Float) {
    val centerY = size.height / 2f
    val topStaffLine = centerY - 2 * staffSpacing
    val bottomStaffLine = centerY + 2 * staffSpacing
    
    val lineColor = Color(0xFF1A1A1A)
    val lineExtension = staffSpacing * 0.75f
    
    // Ledger lines above staff
    if (y < topStaffLine - staffSpacing / 2) {
        var ledgerY = topStaffLine - staffSpacing
        while (ledgerY > y - staffSpacing / 2) {
            drawLine(
                color = lineColor,
                start = Offset(x - lineExtension, ledgerY),
                end = Offset(x + lineExtension, ledgerY),
                strokeWidth = 1f
            )
            ledgerY -= staffSpacing
        }
    }
    
    // Ledger lines below staff
    if (y > bottomStaffLine + staffSpacing / 2) {
        var ledgerY = bottomStaffLine + staffSpacing
        while (ledgerY < y + staffSpacing / 2) {
            drawLine(
                color = lineColor,
                start = Offset(x - lineExtension, ledgerY),
                end = Offset(x + lineExtension, ledgerY),
                strokeWidth = 1f
            )
            ledgerY += staffSpacing
        }
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
