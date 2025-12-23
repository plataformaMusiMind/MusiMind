package com.musimind.music.notation.layout

import com.musimind.music.notation.model.*

/**
 * Layout engine for calculating positions of music elements
 * 
 * This engine calculates the exact positions for rendering music notation
 * following professional engraving rules.
 */
class ScoreLayoutEngine {
    
    /**
     * Layout parameters (all values relative to staff space unit)
     */
    data class LayoutParams(
        val staffSpace: Float = 8f,           // Height between staff lines
        val noteWidth: Float = 1.2f,          // Width of a notehead in staff spaces
        val stemLength: Float = 3.5f,         // Standard stem length in staff spaces
        val stemWidth: Float = 0.12f,         // Stem thickness in staff spaces
        val beamSpacing: Float = 0.75f,       // Space between beams
        val beamThickness: Float = 0.5f,      // Beam thickness
        val ledgerLineExtension: Float = 0.4f, // Extension beyond notehead
        val ledgerLineThickness: Float = 0.16f,
        val staffLineThickness: Float = 0.13f,
        val barlineThickness: Float = 0.16f,
        val thinBarlineThickness: Float = 0.16f,
        val thickBarlineThickness: Float = 0.5f,
        val clefWidth: Float = 2.8f,
        val keySignatureGap: Float = 0.5f,
        val timeSignatureGap: Float = 0.8f,
        val accidentalGap: Float = 0.3f,
        val dotGap: Float = 0.25f,
        val measureGap: Float = 1.5f,
        val minimumNoteSpacing: Float = 1.8f
    )
    
    private val params = LayoutParams()
    
    /**
     * Calculate layout for a complete score
     */
    fun layoutScore(score: Score, availableWidth: Float): LayoutResult {
        val layouts = mutableListOf<MeasureLayout>()
        var currentX = 0f
        
        // Calculate initial offset (clef, key signature, time signature)
        val headerWidth = calculateHeaderWidth(score)
        currentX += headerWidth
        
        // Layout each measure
        for (measure in score.measures) {
            val measureLayout = layoutMeasure(measure, score, currentX)
            layouts.add(measureLayout)
            currentX += measureLayout.width
        }
        
        return LayoutResult(
            measures = layouts,
            totalWidth = currentX,
            staffHeight = params.staffSpace * 4, // 5 lines = 4 spaces
            headerWidth = headerWidth
        )
    }
    
    /**
     * Calculate header width (clef, key sig, time sig)
     */
    private fun calculateHeaderWidth(score: Score): Float {
        var width = params.clefWidth
        width += params.keySignatureGap
        width += score.keySignature.accidentals * 0.7f
        width += params.timeSignatureGap
        width += 1.5f // Time signature width
        width += params.measureGap
        return width * params.staffSpace
    }
    
    /**
     * Layout a single measure
     */
    private fun layoutMeasure(
        measure: Measure,
        score: Score,
        startX: Float
    ): MeasureLayout {
        val elementLayouts = mutableListOf<ElementLayout>()
        var currentX = startX + params.measureGap * params.staffSpace
        
        // Sort elements by time position (voice handling)
        val sortedElements = measure.elements.sortedBy { 
            when (it) {
                is Note -> it.positionX ?: 0f
                is Chord -> it.positionX ?: 0f
                is Rest -> it.positionX ?: 0f
            }
        }
        
        // Layout each element
        for (element in sortedElements) {
            val layout = when (element) {
                is Note -> layoutNote(element, score.clef, currentX)
                is Chord -> layoutChord(element, score.clef, currentX)
                is Rest -> layoutRest(element, currentX)
            }
            elementLayouts.add(layout)
            currentX += calculateElementWidth(element)
        }
        
        // Add barline
        val barlineX = currentX + params.measureGap * params.staffSpace
        
        return MeasureLayout(
            measureNumber = measure.number,
            elements = elementLayouts,
            startX = startX,
            endX = barlineX,
            width = barlineX - startX,
            barlineType = measure.barlineType
        )
    }
    
    /**
     * Layout a single note
     */
    private fun layoutNote(note: Note, clef: ClefType, x: Float): ElementLayout.NoteLayout {
        val staffPosition = note.pitch.staffPosition(clef)
        val y = calculateYPosition(staffPosition)
        val stemUp = calculateStemDirection(staffPosition)
        val needsLedgerLines = needsLedgerLines(staffPosition)
        
        return ElementLayout.NoteLayout(
            id = note.id,
            x = x,
            y = y,
            staffPosition = staffPosition,
            durationBeats = note.durationBeats,
            stemUp = stemUp,
            ledgerLineCount = if (needsLedgerLines) getLedgerLineCount(staffPosition) else 0,
            ledgerLinesAbove = staffPosition > 8,
            accidental = note.accidental,
            articulations = note.articulations,
            dynamic = note.dynamic,
            ornament = note.ornament,
            dotted = note.dotted,
            doubleDotted = note.doubleDotted,
            beamGroup = note.beamGroup,
            state = note.state
        )
    }
    
    /**
     * Layout a chord (multiple notes)
     */
    private fun layoutChord(chord: Chord, clef: ClefType, x: Float): ElementLayout.ChordLayout {
        val noteLayouts = chord.notes.map { note ->
            layoutNote(note, clef, x) as ElementLayout.NoteLayout
        }
        
        // Determine stem direction based on average position
        val avgPosition = noteLayouts.map { it.staffPosition }.average()
        val stemUp = avgPosition <= 4
        
        // Adjust noteheads that are a second apart
        val adjustedNotes = adjustSecondsInChord(noteLayouts, stemUp)
        
        return ElementLayout.ChordLayout(
            id = chord.id,
            x = x,
            durationBeats = chord.durationBeats,
            notes = adjustedNotes,
            stemUp = stemUp,
            arpeggio = chord.arpeggio
        )
    }
    
    /**
     * Layout a rest
     */
    private fun layoutRest(rest: Rest, x: Float): ElementLayout.RestLayout {
        val y = calculateYPosition(4) // Center of staff
        return ElementLayout.RestLayout(
            id = rest.id,
            x = x,
            y = y,
            durationBeats = rest.durationBeats,
            isWholeMeasure = rest.isWholeMeasure
        )
    }
    
    /**
     * Calculate Y position from staff position
     */
    private fun calculateYPosition(staffPosition: Int): Float {
        // Position 0 = bottom line, position 8 = top line
        // We measure from top, so invert
        return (8 - staffPosition) * params.staffSpace / 2
    }
    
    /**
     * Calculate element width based on duration
     */
    private fun calculateElementWidth(element: MusicElement): Float {
        val baseWidth = params.minimumNoteSpacing * params.staffSpace
        // Longer notes get more space (logarithmic relationship)
        val durationFactor = kotlin.math.log2(element.durationBeats + 1) / 2 + 0.5f
        return baseWidth * durationFactor
    }
    
    /**
     * Determine stem direction
     */
    private fun calculateStemDirection(staffPosition: Int): Boolean {
        return staffPosition <= 4 // Below or on middle line = stem up
    }
    
    /**
     * Check if ledger lines are needed
     */
    private fun needsLedgerLines(staffPosition: Int): Boolean {
        return staffPosition < 0 || staffPosition > 8
    }
    
    /**
     * Get number of ledger lines
     */
    private fun getLedgerLineCount(staffPosition: Int): Int {
        return when {
            staffPosition < 0 -> (-staffPosition + 1) / 2
            staffPosition > 8 -> (staffPosition - 8 + 1) / 2
            else -> 0
        }
    }
    
    /**
     * Adjust noteheads that are a second apart in a chord
     */
    private fun adjustSecondsInChord(
        notes: List<ElementLayout.NoteLayout>,
        stemUp: Boolean
    ): List<ElementLayout.NoteLayout> {
        val sorted = notes.sortedBy { it.staffPosition }
        val adjusted = mutableListOf<ElementLayout.NoteLayout>()
        var previousOffset = false
        
        for (i in sorted.indices) {
            val note = sorted[i]
            val needsOffset = if (i > 0) {
                val prevPosition = sorted[i - 1].staffPosition
                (note.staffPosition - prevPosition == 1) && !previousOffset
            } else false
            
            if (needsOffset) {
                // Offset notehead to opposite side of stem
                val offsetX = if (stemUp) params.noteWidth * params.staffSpace else -params.noteWidth * params.staffSpace
                adjusted.add(note.copy(x = note.x + offsetX, offsetForSecond = true))
                previousOffset = true
            } else {
                adjusted.add(note.copy(offsetForSecond = false))
                previousOffset = false
            }
        }
        
        return adjusted
    }
}

/**
 * Result of layout calculation
 */
data class LayoutResult(
    val measures: List<MeasureLayout>,
    val totalWidth: Float,
    val staffHeight: Float,
    val headerWidth: Float
)

/**
 * Layout for a single measure
 */
data class MeasureLayout(
    val measureNumber: Int,
    val elements: List<ElementLayout>,
    val startX: Float,
    val endX: Float,
    val width: Float,
    val barlineType: BarlineType
)

/**
 * Sealed class for element layouts
 */
sealed class ElementLayout {
    abstract val id: String
    abstract val x: Float
    abstract val durationBeats: Float
    
    data class NoteLayout(
        override val id: String,
        override val x: Float,
        val y: Float,
        val staffPosition: Int,
        override val durationBeats: Float,
        val stemUp: Boolean,
        val ledgerLineCount: Int,
        val ledgerLinesAbove: Boolean,
        val accidental: AccidentalType?,
        val articulations: List<ArticulationType>,
        val dynamic: DynamicType?,
        val ornament: OrnamentType?,
        val dotted: Boolean,
        val doubleDotted: Boolean,
        val beamGroup: Int?,
        val state: NoteState,
        val offsetForSecond: Boolean = false
    ) : ElementLayout()
    
    data class ChordLayout(
        override val id: String,
        override val x: Float,
        override val durationBeats: Float,
        val notes: List<NoteLayout>,
        val stemUp: Boolean,
        val arpeggio: Boolean
    ) : ElementLayout()
    
    data class RestLayout(
        override val id: String,
        override val x: Float,
        val y: Float,
        override val durationBeats: Float,
        val isWholeMeasure: Boolean
    ) : ElementLayout()
}
