package com.musimind.data.repository

import com.musimind.domain.model.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for exercises and learning content
 * All data comes from Supabase (source of truth)
 */
@Singleton
class ExerciseRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) {
    private val currentUserId: String? get() = auth.currentSessionOrNull()?.user?.id
    
    // ========== CATEGORIES ==========
    
    /**
     * Get all exercise categories
     */
    suspend fun getCategories(): List<ExerciseCategory> {
        return try {
            postgrest.from("exercise_categories")
                .select {
                    filter { eq("is_active", true) }
                    order("sort_order", Order.ASCENDING)
                }
                .decodeList<ExerciseCategory>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get category by ID
     */
    suspend fun getCategoryById(categoryId: String): ExerciseCategory? {
        return try {
            postgrest.from("exercise_categories")
                .select {
                    filter { eq("id", categoryId) }
                }
                .decodeSingleOrNull<ExerciseCategory>()
        } catch (e: Exception) {
            null
        }
    }
    
    // ========== EXERCISES ==========
    
    /**
     * Get exercises by category
     */
    suspend fun getExercisesByCategory(categoryId: String): List<Exercise> {
        return try {
            postgrest.from("exercises")
                .select {
                    filter { 
                        eq("category_id", categoryId)
                        eq("is_active", true)
                    }
                    order("sort_order", Order.ASCENDING)
                }
                .decodeList<Exercise>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get exercise by ID
     */
    suspend fun getExerciseById(exerciseId: String): Exercise? {
        return try {
            postgrest.from("exercises")
                .select {
                    filter { eq("id", exerciseId) }
                }
                .decodeSingleOrNull<Exercise>()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get all active exercises
     */
    suspend fun getAllExercises(): List<Exercise> {
        return try {
            postgrest.from("exercises")
                .select {
                    filter { eq("is_active", true) }
                    order("sort_order", Order.ASCENDING)
                }
                .decodeList<Exercise>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get solfege notes for an exercise
     */
    suspend fun getSolfegeNotes(exerciseId: String): List<SolfegeNote> {
        return try {
            postgrest.from("solfege_notes")
                .select {
                    filter { eq("exercise_id", exerciseId) }
                    order("sequence_order", Order.ASCENDING)
                }
                .decodeList<SolfegeNote>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get melodic perception notes for an exercise
     * Used by MelodicPerceptionViewModel
     */
    suspend fun getMelodicNotes(exerciseId: String): List<SolfegeNote> {
        return try {
            postgrest.from("melodic_notes")
                .select {
                    filter { eq("exercise_id", exerciseId) }
                    order("sequence_order", Order.ASCENDING)
                }
                .decodeList<SolfegeNote>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // ========== RHYTHM PATTERNS ==========
    
    /**
     * Get rhythm patterns for an exercise
     */
    suspend fun getRhythmPatterns(exerciseId: String): List<RhythmPattern> {
        return try {
            postgrest.from("rhythm_patterns")
                .select {
                    filter { eq("exercise_id", exerciseId) }
                    order("sequence_order", Order.ASCENDING)
                }
                .decodeList<RhythmPattern>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // ========== INTERVAL QUESTIONS ==========
    
    /**
     * Get interval questions for an exercise
     */
    suspend fun getIntervalQuestions(exerciseId: String): List<IntervalQuestion> {
        return try {
            postgrest.from("interval_questions")
                .select {
                    filter { eq("exercise_id", exerciseId) }
                    order("sequence_order", Order.ASCENDING)
                }
                .decodeList<IntervalQuestion>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // ========== LEARNING PATHS ==========
    
    /**
     * Get learning path for user type
     */
    suspend fun getLearningPath(userType: String = "student"): LearningPath? {
        return try {
            postgrest.from("learning_paths")
                .select {
                    filter { 
                        eq("user_type", userType)
                        eq("is_active", true)
                    }
                }
                .decodeSingleOrNull<LearningPath>()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get learning nodes for a path
     */
    suspend fun getLearningNodes(pathId: String): List<LearningNode> {
        return try {
            postgrest.from("learning_nodes")
                .select {
                    filter { 
                        eq("path_id", pathId)
                        eq("is_active", true)
                    }
                    order("sort_order", Order.ASCENDING)
                }
                .decodeList<LearningNode>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // ========== USER PROGRESS ==========
    
    /**
     * Save exercise progress
     */
    suspend fun saveProgress(
        exerciseId: String,
        score: Int,
        totalQuestions: Int,
        correctAnswers: Int,
        timeSpentSeconds: Int,
        xpEarned: Int,
        coinsEarned: Int
    ): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            val accuracy = if (totalQuestions > 0) correctAnswers.toFloat() / totalQuestions else 0f
            
            val progress = mapOf(
                "user_id" to userId,
                "exercise_id" to exerciseId,
                "score" to score,
                "total_questions" to totalQuestions,
                "correct_answers" to correctAnswers,
                "accuracy" to accuracy,
                "time_spent_seconds" to timeSpentSeconds,
                "xp_earned" to xpEarned,
                "coins_earned" to coinsEarned
            )
            
            postgrest.from("user_exercise_progress").insert(progress)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user's progress history
     */
    suspend fun getUserProgress(): List<ExerciseProgress> {
        val userId = currentUserId ?: return emptyList()
        
        return try {
            postgrest.from("user_exercise_progress")
                .select {
                    filter { eq("user_id", userId) }
                    order("completed_at", Order.DESCENDING)
                    limit(100)
                }
                .decodeList<ExerciseProgress>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Check if user has completed an exercise
     */
    suspend fun hasCompletedExercise(exerciseId: String): Boolean {
        val userId = currentUserId ?: return false
        
        return try {
            val result = postgrest.from("user_exercise_progress")
                .select {
                    filter { 
                        eq("user_id", userId)
                        eq("exercise_id", exerciseId)
                    }
                    limit(1)
                }
                .decodeList<ExerciseProgress>()
            
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    // ========== DAILY CHALLENGES ==========
    
    /**
     * Get today's daily challenge
     */
    suspend fun getTodayChallenge(): DailyChallenge? {
        val today = java.time.LocalDate.now().toString()
        
        return try {
            postgrest.from("daily_challenges")
                .select {
                    filter { eq("date", today) }
                }
                .decodeSingleOrNull<DailyChallenge>()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if user completed today's challenge
     */
    suspend fun hasCompletedTodayChallenge(): Boolean {
        val userId = currentUserId ?: return false
        val challenge = getTodayChallenge() ?: return false
        
        return try {
            val result = postgrest.from("user_daily_completions")
                .select {
                    filter { 
                        eq("user_id", userId)
                        eq("challenge_id", challenge.id)
                    }
                    limit(1)
                }
                .decodeList<Map<String, String>>()
            
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    // ========== ACHIEVEMENTS ==========
    
    /**
     * Get all achievements
     */
    suspend fun getAllAchievements(): List<Achievement> {
        return try {
            postgrest.from("achievements")
                .select {
                    filter { eq("is_hidden", false) }
                    order("sort_order", Order.ASCENDING)
                }
                .decodeList<Achievement>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get user's unlocked achievements
     */
    suspend fun getUserAchievements(): List<UserAchievement> {
        val userId = currentUserId ?: return emptyList()
        
        return try {
            postgrest.from("user_achievements")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<UserAchievement>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Unlock an achievement for the current user
     */
    suspend fun unlockAchievement(achievementId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            postgrest.from("user_achievements").upsert(
                mapOf(
                    "user_id" to userId,
                    "achievement_id" to achievementId
                )
            ) {
                onConflict = "user_id,achievement_id"
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
