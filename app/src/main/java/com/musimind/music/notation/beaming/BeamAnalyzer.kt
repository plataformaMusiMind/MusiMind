package com.musimind.music.notation.beaming

import com.musimind.music.notation.model.*

/**
 * Analyzes groups of notes and determines beam geometry and structure.
 * 
 * This is the core engine for professional music notation beaming.
 * Implements rules from "Behind Bars" by Elaine Gould and SMuFL specifications.
 * 
 * Key beaming rules:
 * 1. Beams group notes < 1 beat (eighth notes and shorter)
 * 2. Beams should not obscure beat boundaries
 * 3. Simple time (2/4, 3/4, 4/4): Binary subdivision, group by beat
 * 4. Compound time (6/8, 9/8, 12/8): Ternary subdivision, group by dotted beat
 * 5. Secondary beams may break at subdivisions for rhythmic clarity
 */
class BeamAnalyzer(
    /** Staff space in pixels (vertical distance between staff lines) */
    private val staffSpace: Float,
    
    /** Width of a notehead in pixels */
    private val noteheadWidth: Float
) {
    
    // SMuFL-standard constants (in staff spaces)
    private val beamThickness = 0.5f * staffSpace    // Per SMuFL
    private val beamGap = 0.25f * staffSpace         // Per SMuFL
    private val stemThickness = 0.12f * staffSpace   // Per SMuFL
    private val standardStemLength = 3.5f * staffSpace // Standard stem length
    
    /**
     * Assign beam groups to a list of notes based on time signature and position.
     * 
     * CRITICAL: Beams must NOT cross bar lines!
     * 
     * @param notes List of music elements to analyze
     * @param timeSignature Time signature for beaming rules
     * @return Map of note index to beam group ID (null if not beamed)
     */
    fun assignBeamGroups(
        notes: List<MusicElement>,
        timeSignature: TimeSignatureModel = TimeSignatureModel(4, 4)
    ): Map<Int, Int> {
        val beamGroups = mutableMapOf<Int, Int>()
        var currentGroupId = 0
        var groupStartIndex: Int? = null
        var currentBeatPosition = 0f
        
        val beatsPerMeasure = timeSignature.numerator.toFloat() * (4f / timeSignature.denominator)
        val beatsPerGroup = getBeatsPerBeamGroup(timeSignature)
        
        notes.forEachIndexed { index, element ->
            val duration = element.durationBeats
            val shouldBeam = canBeBeamed(duration) && element is Note
            
            // Calculate which measure this note starts in
            val measureNumberBefore = (currentBeatPosition / beatsPerMeasure).toInt()
            val measureNumberAfter = ((currentBeatPosition + duration) / beatsPerMeasure).toInt()
            
            // Check if note crosses bar line
            val crossesBarLine = measureNumberAfter > measureNumberBefore && 
                                 (currentBeatPosition + duration) % beatsPerMeasure > 0.001f
            
            // Calculate beat position within current measure
            val positionInMeasure = currentBeatPosition % beatsPerMeasure
            
            // Check if we're crossing a beat group boundary (e.g., beats 1-2 vs 3-4)
            val beatGroupBefore = (positionInMeasure / beatsPerGroup).toInt()
            val beatGroupAfter = ((positionInMeasure + duration) / beatsPerGroup).toInt()
            val crossesBeatGroup = beatGroupAfter > beatGroupBefore
            
            if (shouldBeam && !crossesBarLine) {
                // Start new group if:
                // 1. No active group
                // 2. Crossing beat group boundary within measure
                // 3. At start of new measure
                val atMeasureStart = positionInMeasure < 0.001f
                
                if (groupStartIndex == null || crossesBeatGroup || atMeasureStart) {
                    if (groupStartIndex != null) {
                        currentGroupId++
                    }
                    groupStartIndex = index
                }
                
                beamGroups[index] = currentGroupId
            } else {
                // This note breaks the beam group (rest, too long, or crosses bar)
                if (groupStartIndex != null) {
                    currentGroupId++
                    groupStartIndex = null
                }
            }
            
            currentBeatPosition += duration
        }
        
        // Filter out single-note "groups" - beams need at least 2 notes
        val groupCounts = beamGroups.values.groupingBy { it }.eachCount()
        return beamGroups.filterValues { groupCounts[it]!! >= 2 }
    }
    
    /**
     * Create an AdvancedBeamGroup with full geometry from a set of notes.
     * 
     * @param notes Notes in the beam group
     * @param noteXPositions Map of note to its X position in pixels
     * @param noteStaffPositions Map of note to its staff position (0 = middle line)
     * @param noteYPositions Map of note to its Y position in pixels
     */
    fun analyzeAdvancedBeamGroup(
        notes: List<Note>,
        noteXPositions: Map<Note, Float>,
        noteStaffPositions: Map<Note, Int>,
        noteYPositions: Map<Note, Float>
    ): AdvancedBeamGroup {
        require(notes.isNotEmpty()) { "Beam group cannot be empty" }
        
        val group = AdvancedBeamGroup(notes = notes)
        
        // Step 1: Determine stem direction based on furthest note from center
        group.stemDirection = calculateStemDirection(notes, noteStaffPositions)
        
        // Step 2: Calculate X positions (left and right)
        calculateXPositions(group, noteXPositions)
        
        // Step 3: Calculate beam geometry (Y positions with slope)
        calculateBeamGeometry(group, noteStaffPositions, noteYPositions)
        
        // Step 4: Analyze all beam levels and create segments
        analyzeBeamSegments(group)
        
        return group
    }
    
    /**
     * Determine stem direction based on the note furthest from the center line.
     * 
     * Per standard engraving practice:
     * - Notes below center line: stems UP
     * - Notes above center line: stems DOWN
     * - At center: stems DOWN (convention)
     */
    private fun calculateStemDirection(
        notes: List<Note>,
        noteStaffPositions: Map<Note, Int>
    ): StemDirection {
        if (noteStaffPositions.isEmpty()) return StemDirection.UP
        
        // Staff position 0 = center line (B4 in treble clef)
        val centerLine = 0
        
        // Find note furthest from center
        var farthestNote: Note? = null
        var maxDistance = 0
        
        for (note in notes) {
            val position = noteStaffPositions[note] ?: continue
            val distance = kotlin.math.abs(position - centerLine)
            if (distance > maxDistance) {
                maxDistance = distance
                farthestNote = note
            }
        }
        
        if (farthestNote == null) return StemDirection.UP
        
        val farthestPos = noteStaffPositions[farthestNote]!!
        
        // Above center (positive) = stems down, Below center (negative) = stems up
        return if (farthestPos >= centerLine) StemDirection.DOWN else StemDirection.UP
    }
    
    /**
     * Calculate X positions based on stem attachment points.
     * 
     * Per SMuFL:
     * - Stem up: attaches at stemUpSE (south-east of notehead)
     * - Stem down: attaches at stemDownNW (north-west of notehead)
     */
    private fun calculateXPositions(
        group: AdvancedBeamGroup,
        noteXPositions: Map<Note, Float>
    ) {
        if (noteXPositions.isEmpty()) {
            // Fallback: estimate positions
            group.leftX = 0f
            group.rightX = (group.notes.size - 1) * noteheadWidth * 2
            return
        }
        
        val firstNote = group.notes.first()
        val lastNote = group.notes.last()
        
        val firstNoteX = noteXPositions[firstNote] ?: 0f
        val lastNoteX = noteXPositions[lastNote] ?: 0f
        

        val stemUpXOffset = noteheadWidth * 0.92f
        val stemDownXOffset = noteheadWidth * 0.08f
        
        val xOffset = if (group.stemDirection == StemDirection.UP) {
            stemUpXOffset
        } else {
            stemDownXOffset
        }
        
        group.leftX = firstNoteX + xOffset
        group.rightX = lastNoteX + xOffset
    }
    
    /**
     * Calculate beam geometry (Y positions) including slope.
     * 
     * Per "Behind Bars":
     * - Beam should follow melodic contour
     * - Maximum slope is limited for readability
     * - Beam should be at least 2.5 staff spaces from the note
     */
    private fun calculateBeamGeometry(
        group: AdvancedBeamGroup,
        noteStaffPositions: Map<Note, Int>,
        noteYPositions: Map<Note, Float>
    ) {
        val firstNote = group.notes.first()
        val lastNote = group.notes.last()
        
        val firstNoteY = noteYPositions[firstNote] ?: 0f
        val lastNoteY = noteYPositions[lastNote] ?: 0f
        
        // Calculate stem length based on beam levels needed
        val maxBeams = group.notes.maxOf { 
            DurationType.fromBeatValue(it.durationBeats).beamCount 
        }
        
        // Additional stem length for multiple beam levels
        val additionalLength = if (maxBeams > 1) {
            (maxBeams - 1) * (beamThickness + beamGap)
        } else 0f
        
        val stemLength = standardStemLength + additionalLength
        
        // Calculate beam Y at each end
        if (group.stemDirection == StemDirection.UP) {
            // Stems up: beam is above notes
            val avgY = (firstNoteY + lastNoteY) / 2
            val beamBaseY = avgY - stemLength
            
            // Calculate slope from melodic contour
            val slopeAngle = calculateBeamSlope(noteStaffPositions, group.stemDirection)
            val slopePixels = slopeAngle * staffSpace
            
            val halfWidth = (group.rightX - group.leftX) / 2
            group.leftY = beamBaseY + slopePixels * (-halfWidth / (group.rightX - group.leftX + 1))
            group.rightY = beamBaseY + slopePixels * (halfWidth / (group.rightX - group.leftX + 1))
        } else {
            // Stems down: beam is below notes
            val avgY = (firstNoteY + lastNoteY) / 2
            val beamBaseY = avgY + stemLength
            
            val slopeAngle = calculateBeamSlope(noteStaffPositions, group.stemDirection)
            val slopePixels = slopeAngle * staffSpace
            
            val halfWidth = (group.rightX - group.leftX) / 2
            group.leftY = beamBaseY - slopePixels * (-halfWidth / (group.rightX - group.leftX + 1))
            group.rightY = beamBaseY - slopePixels * (halfWidth / (group.rightX - group.leftX + 1))
        }
    }
    
    /**
     * Calculate beam slope based on melodic contour.
     * Returns slope in staff spaces per beam width.
     */
    private fun calculateBeamSlope(
        noteStaffPositions: Map<Note, Int>,
        stemDirection: StemDirection
    ): Float {
        if (noteStaffPositions.size < 2) return 0f
        
        val positions = noteStaffPositions.values.toList()
        val first = positions.first()
        val last = positions.last()
        
        // Calculate raw slope
        var slope = (last - first).toFloat() / positions.size
        
        // Limit maximum slope (per Behind Bars guidelines)
        val maxSlope = 1.0f // Maximum 1 staff space per beat
        slope = slope.coerceIn(-maxSlope, maxSlope)
        
        // Invert for stems down
        return if (stemDirection == StemDirection.DOWN) -slope else slope
    }
    
    /**
     * Analyze and create all beam segments for the group.
     */
    private fun analyzeBeamSegments(group: AdvancedBeamGroup) {
        // Primary beam (level 1): always spans all notes
        group.beamSegments.add(BeamSegment(
            level = 1,
            startNoteIndex = 0,
            endNoteIndex = group.notes.size - 1,
            isFractional = false
        ))
        
        // Find maximum beam level needed
        val maxLevel = group.notes.maxOf { 
            DurationType.fromBeatValue(it.durationBeats).beamCount 
        }
        
        // Analyze each secondary beam level
        for (level in 2..maxLevel) {
            analyzeBeamLevel(group, level)
        }
    }
    
    /**
     * Analyze a specific beam level and create appropriate segments.
     */
    private fun analyzeBeamLevel(group: AdvancedBeamGroup, level: Int) {
        var segmentStart: Int? = null
        
        for (i in group.notes.indices) {
            val note = group.notes[i]
            val noteBeams = DurationType.fromBeatValue(note.durationBeats).beamCount
            
            if (noteBeams >= level) {
                // This note needs this beam level
                if (segmentStart == null) {
                    segmentStart = i
                }
            } else {
                // This note doesn't need this level - close segment
                if (segmentStart != null) {
                    if (segmentStart == i - 1) {
                        // Single note: create fractional beam
                        group.beamSegments.add(createFractionalBeam(group, segmentStart, i, level))
                    } else {
                        // Multiple notes: create full segment
                        group.beamSegments.add(BeamSegment(
                            level = level,
                            startNoteIndex = segmentStart,
                            endNoteIndex = i - 1,
                            isFractional = false
                        ))
                    }
                    segmentStart = null
                }
            }
        }
        
        // Close final segment
        if (segmentStart != null) {
            if (segmentStart == group.notes.size - 1) {
                // Last note alone: fractional beam pointing left
                group.beamSegments.add(createFractionalBeam(
                    group, segmentStart, group.notes.size, level
                ))
            } else {
                group.beamSegments.add(BeamSegment(
                    level = level,
                    startNoteIndex = segmentStart,
                    endNoteIndex = group.notes.size - 1,
                    isFractional = false
                ))
            }
        }
    }
    
    /**
     * Create a fractional (broken/stub) beam.
     */
    private fun createFractionalBeam(
        group: AdvancedBeamGroup,
        noteIndex: Int,
        nextNoteIndex: Int,
        level: Int
    ): BeamSegment {
        // Determine direction based on context
        val side = when {
            noteIndex == 0 -> FractionalBeamSide.RIGHT
            nextNoteIndex >= group.notes.size -> FractionalBeamSide.LEFT
            else -> {
                // In the middle: check rhythm context (dotted rhythms point right)
                val note = group.notes[noteIndex]
                val prevNote = group.notes[noteIndex - 1]
                if (note.durationBeats < prevNote.durationBeats) {
                    FractionalBeamSide.RIGHT
                } else {
                    FractionalBeamSide.LEFT
                }
            }
        }
        
        return BeamSegment(
            level = level,
            startNoteIndex = noteIndex,
            endNoteIndex = noteIndex,
            isFractional = true,
            fractionalSide = side,
            fractionalLength = noteheadWidth
        )
    }
    
    // ===========================================
    // Helper methods
    // ===========================================
    
    /**
     * Get beats per beam group based on time signature.
     * 
     * Simple time (2, 3, 4 numerator): Group by beat
     * Compound time (6, 9, 12 numerator): Group by dotted beat
     */
    private fun getBeatsPerBeamGroup(timeSignature: TimeSignatureModel): Float {
        return when {
            // Compound time signatures (numerator divisible by 3, denominator 8)
            timeSignature.numerator in listOf(6, 9, 12) && timeSignature.denominator == 8 -> {
                // Group by dotted quarter (1.5 beats in quarter-note terms)
                1.5f
            }
            // Simple time signatures
            else -> {
                // Group by quarter note (1 beat)
                1f
            }
        }
    }
    
    /**
     * Check if a duration can be beamed (eighth note or shorter).
     */
    private fun canBeBeamed(beats: Float): Boolean = beats < 1f
}
