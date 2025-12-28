package com.musimind.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Exercise Category
 */
@Serializable
data class ExerciseCategory(
    val id: String = "",
    val name: String = "",
    @SerialName("display_name")
    val displayName: String = "",
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0
)

/**
 * Exercise
 */
@Serializable
data class Exercise(
    val id: String = "",
    @SerialName("category_id")
    val categoryId: String = "",
    val title: String = "",
    val description: String? = null,
    val difficulty: Int = 1,
    @SerialName("xp_reward")
    val xpReward: Int = 10,
    @SerialName("coins_reward")
    val coinsReward: Int = 5,
    @SerialName("estimated_time_seconds")
    val estimatedTimeSeconds: Int = 60,
    @SerialName("is_premium")
    val isPremium: Boolean = false,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("min_user_level")
    val minUserLevel: Int? = null,
    // Transient field for UI usage (not serialized from DB)
    val category: MusicCategory? = null
)

/**
 * Solfege Note - represents a single note in a solfege exercise
 * Uses MIDI pitch numbers for universal representation
 */
@Serializable
data class SolfegeNote(
    val id: String = "",
    @SerialName("exercise_id")
    val exerciseId: String = "",
    @SerialName("sequence_order")
    val sequenceOrder: Int = 0,
    val pitch: Int = 60, // MIDI note number (60 = C4, 72 = C5)
    @SerialName("duration_beats")
    val durationBeats: Float = 1.0f,
    val tempo: Int = 60,
    val clef: String = "treble",
    @SerialName("time_signature_num")
    val timeSignatureNum: Int = 4,
    @SerialName("time_signature_den")
    val timeSignatureDen: Int = 4,
    val accidental: String? = null // "sharp", "flat", "natural"
) {
    /**
     * Convert MIDI pitch to solfege syllable
     */
    fun toSolfege(): String {
        val noteNames = listOf("Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá", "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si")
        return noteNames[pitch % 12]
    }
    
    /**
     * Convert MIDI pitch to scientific notation (e.g., "C4", "D5")
     */
    fun toScientificNotation(): String {
        val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val octave = (pitch / 12) - 1
        return "${noteNames[pitch % 12]}$octave"
    }
    
    /**
     * Get staff position for treble clef
     * 0 = E4 (bottom line), 8 = F5 (top line)
     */
    fun staffPosition(clefType: String = "treble"): Int {
        return when (clefType) {
            "treble" -> {
                // E4 = 64 is position 0 (bottom line)
                val e4Midi = 64
                midiToDiatonicSteps(pitch) - midiToDiatonicSteps(e4Midi)
            }
            "bass" -> {
                // G2 = 43 is position 0 for bass clef
                val g2Midi = 43
                midiToDiatonicSteps(pitch) - midiToDiatonicSteps(g2Midi)
            }
            else -> 0
        }
    }
    
    private fun midiToDiatonicSteps(midi: Int): Int {
        val octave = (midi / 12) - 1
        val noteInOctave = midi % 12
        val diatonicInOctave = when (noteInOctave) {
            0 -> 0  // C
            1 -> 0  // C#
            2 -> 1  // D
            3 -> 1  // D#
            4 -> 2  // E
            5 -> 3  // F
            6 -> 3  // F#
            7 -> 4  // G
            8 -> 4  // G#
            9 -> 5  // A
            10 -> 5 // A#
            11 -> 6 // B
            else -> 0
        }
        return (octave * 7) + diatonicInOctave
    }
}

/**
 * Rhythm Pattern - for rhythm exercises
 */
@Serializable
data class RhythmPattern(
    val id: String = "",
    @SerialName("exercise_id")
    val exerciseId: String = "",
    @SerialName("sequence_order")
    val sequenceOrder: Int = 0,
    @SerialName("duration_beats")
    val durationBeats: Float = 1.0f,
    @SerialName("is_rest")
    val isRest: Boolean = false,
    val tempo: Int = 80,
    @SerialName("time_signature_num")
    val timeSignatureNum: Int = 4,
    @SerialName("time_signature_den")
    val timeSignatureDen: Int = 4
)

/**
 * Interval Question - for interval recognition exercises
 */
@Serializable
data class IntervalQuestion(
    val id: String = "",
    @SerialName("exercise_id")
    val exerciseId: String = "",
    @SerialName("sequence_order")
    val sequenceOrder: Int = 0,
    @SerialName("lower_pitch")
    val lowerPitch: Int = 60,
    @SerialName("upper_pitch")
    val upperPitch: Int = 64,
    @SerialName("interval_name")
    val intervalName: String = "",
    @SerialName("interval_semitones")
    val intervalSemitones: Int = 4,
    @SerialName("is_ascending")
    val isAscending: Boolean = true
)

/**
 * User Exercise Progress
 */
@Serializable
data class ExerciseProgress(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("exercise_id")
    val exerciseId: String = "",
    @SerialName("completed_at")
    val completedAt: String? = null,
    val score: Int = 0,
    @SerialName("total_questions")
    val totalQuestions: Int = 0,
    @SerialName("correct_answers")
    val correctAnswers: Int = 0,
    val accuracy: Float = 0f,
    @SerialName("time_spent_seconds")
    val timeSpentSeconds: Int = 0,
    @SerialName("xp_earned")
    val xpEarned: Int = 0,
    @SerialName("coins_earned")
    val coinsEarned: Int = 0
)

/**
 * User Achievement - link between user and achievement
 */
@Serializable
data class UserAchievement(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("achievement_id")
    val achievementId: String = "",
    @SerialName("unlocked_at")
    val unlockedAt: String? = null
)

/**
 * Type alias for backward compatibility
 */
typealias UserProgress = ExerciseProgress
