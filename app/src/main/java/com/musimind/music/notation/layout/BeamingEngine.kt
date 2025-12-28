package com.musimind.music.notation.layout

import com.musimind.music.notation.model.*

/**
 * Engine for grouping notes with beams according to music notation rules.
 * 
 * Beaming Rules:
 * - Beams group notes < 1 beat (eighth, sixteenth, 32nd, 64th)
 * - Beams should not obscure beat boundaries
 * - Simple time (2/4, 3/4, 4/4): Binary subdivision
 * - Compound time (6/8, 9/8, 12/8): Ternary subdivision
 */
object BeamingEngine {
    
    /**
     * Data class representing a beam group
     */
    data class BeamGroup(
        val groupId: Int,
        val noteIndices: List<Int>,  // Indices into the original notes list
        val beamLevel: Int           // 1 = eighth, 2 = sixteenth, 3 = 32nd, 4 = 64th
    )
    
    /**
     * Assign beam groups to a list of notes based on time signature and position.
     * 
     * @param notes List of music elements (notes/rests)
     * @param timeSignature The time signature for beaming rules
     * @return Map of note index to beam group ID (or null if not beamed)
     */
    fun assignBeamGroups(
        notes: List<MusicElement>,
        timeSignature: TimeSignatureModel = TimeSignatureModel(4, 4)
    ): Map<Int, Int> {
        val beamGroups = mutableMapOf<Int, Int>()
        var currentGroupId = 0
        var groupStartIndex: Int? = null
        var currentBeat = 0f
        
        val beatsPerGroup = getBeatsPerBeamGroup(timeSignature)
        
        notes.forEachIndexed { index, element ->
            val duration = element.durationBeats
            val shouldBeam = Duration.shouldBeam(duration) && element is Note
            
            if (shouldBeam) {
                // Check if we need to start a new group (crossing beat boundary)
                val beatInGroup = currentBeat % beatsPerGroup
                val wouldCrossBoundary = beatInGroup + duration > beatsPerGroup
                
                if (groupStartIndex == null || wouldCrossBoundary) {
                    // Start new group
                    if (groupStartIndex != null) {
                        currentGroupId++
                    }
                    groupStartIndex = index
                }
                
                beamGroups[index] = currentGroupId
            } else {
                // This note breaks the beam group
                if (groupStartIndex != null) {
                    currentGroupId++
                    groupStartIndex = null
                }
            }
            
            currentBeat += duration
        }
        
        // Filter out single-note "groups" - beams need at least 2 notes
        val groupCounts = beamGroups.values.groupingBy { it }.eachCount()
        return beamGroups.filterValues { groupCounts[it]!! >= 2 }
    }
    
    /**
     * Get the number of beats per beam group based on time signature.
     * 
     * Simple time (2, 3, 4 numerator): Group by beat (1 quarter = 2 eighths)
     * Compound time (6, 9, 12 numerator): Group by dotted beat (3 eighths)
     */
    private fun getBeatsPerBeamGroup(timeSignature: TimeSignatureModel): Float {
        return when {
            // Compound time signatures
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
     * Calculate beam positions for rendering.
     * Returns a list of beam groups with their rendering info.
     */
    fun calculateBeamPositions(
        notes: List<MusicElement>,
        beamGroups: Map<Int, Int>,
        notePositions: List<Float>  // X positions of each note
    ): List<BeamRenderInfo> {
        val result = mutableListOf<BeamRenderInfo>()
        
        // Group notes by beam group ID
        val groupedNotes = beamGroups.entries.groupBy({ it.value }, { it.key })
        
        groupedNotes.forEach { (groupId, indices) ->
            if (indices.size >= 2) {
                val sortedIndices = indices.sorted()
                val startIndex = sortedIndices.first()
                val endIndex = sortedIndices.last()
                
                // Find minimum beam level (most flags) in the group
                val minDuration = sortedIndices.mapNotNull { 
                    (notes.getOrNull(it) as? Note)?.durationBeats 
                }.minOrNull() ?: 1f
                
                val beamLevel = Duration.getNumberOfFlags(minDuration)
                
                // Determine stem direction (majority rule or based on average pitch)
                val avgPosition = sortedIndices.mapNotNull { i ->
                    val note = notes.getOrNull(i) as? Note
                    note?.pitch?.staffPosition(ClefType.TREBLE)
                }.average()
                
                val stemUp = avgPosition < 4  // Middle line of treble clef
                
                result.add(
                    BeamRenderInfo(
                        groupId = groupId,
                        startNoteIndex = startIndex,
                        endNoteIndex = endIndex,
                        noteIndices = sortedIndices,
                        beamLevel = beamLevel,
                        stemUp = stemUp,
                        startX = notePositions.getOrNull(startIndex) ?: 0f,
                        endX = notePositions.getOrNull(endIndex) ?: 0f
                    )
                )
            }
        }
        
        return result
    }
    
    /**
     * Information needed to render a beam
     */
    data class BeamRenderInfo(
        val groupId: Int,
        val startNoteIndex: Int,
        val endNoteIndex: Int,
        val noteIndices: List<Int>,
        val beamLevel: Int,          // Number of beam lines (1-4)
        val stemUp: Boolean,
        val startX: Float,
        val endX: Float
    )
}
