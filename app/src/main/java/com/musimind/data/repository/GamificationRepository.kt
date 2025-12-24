package com.musimind.data.repository

import com.musimind.domain.model.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for gamification data (stats, achievements, leaderboards)
 * Uses Supabase Postgrest
 */
@Singleton
class GamificationRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) {
    private val currentUserId: String? get() = auth.currentSessionOrNull()?.user?.id
    
    /**
     * Get user stats
     */
    suspend fun getUserStats(userId: String = currentUserId ?: ""): UserStats? {
        return try {
            val user = postgrest.from("users")
                .select {
                    filter { eq("auth_id", userId) }
                }
                .decodeSingleOrNull<User>()
            
            user?.let { 
                UserStats(
                    totalXp = it.xp,
                    level = it.level,
                    currentStreak = it.streak,
                    longestStreak = it.longestStreak,
                    exercisesCompleted = 0, // TODO: Track separately
                    lessonsCompleted = 0,
                    perfectScores = 0,
                    averageAccuracy = 0f
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Update user stats after completing an exercise
     */
    suspend fun updateStatsAfterExercise(
        category: MusicCategory,
        xpEarned: Int,
        isPerfect: Boolean,
        accuracy: Float
    ) {
        val userId = currentUserId ?: return
        
        try {
            val user = postgrest.from("users")
                .select {
                    filter { eq("auth_id", userId) }
                }
                .decodeSingleOrNull<User>() ?: return
            
            val newXp = user.xp + xpEarned
            val newLevel = LevelSystem.getLevelForXp(newXp)
            
            postgrest.from("users").update(
                mapOf(
                    "xp" to newXp,
                    "level" to newLevel,
                    "last_active_at" to "now()"
                )
            ) {
                filter { eq("auth_id", userId) }
            }
            
            // Update streak
            updateStreak()
            
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Update streak
     */
    suspend fun updateStreak(): Int {
        val userId = currentUserId ?: return 0
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        return try {
            val user = postgrest.from("users")
                .select {
                    filter { eq("auth_id", userId) }
                }
                .decodeSingleOrNull<User>() ?: return 0
            
            val lastStreakDate = user.lastStreakDate ?: ""
            val currentStreak = user.streak
            val longestStreak = user.longestStreak
            
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.time) }
            
            val newStreak = when {
                lastStreakDate == today -> currentStreak // Already practiced today
                lastStreakDate == yesterday -> currentStreak + 1 // Consecutive day
                else -> 1 // Start new streak
            }
            
            postgrest.from("users").update(
                mapOf(
                    "last_streak_date" to today,
                    "streak" to newStreak,
                    "longest_streak" to maxOf(longestStreak, newStreak)
                )
            ) {
                filter { eq("auth_id", userId) }
            }
            
            newStreak
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get user achievements
     */
    fun getUserAchievements(): Flow<List<Achievement>> = flow {
        // TODO: Implement achievements table
        emit(Achievements.ALL.map { it.copy(isUnlocked = false) })
    }
    
    /**
     * Get leaderboard
     */
    suspend fun getLeaderboard(type: LeaderboardType, limit: Int = 50): List<LeaderboardEntry> {
        return try {
            val users = postgrest.from("users")
                .select {
                    order("xp", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<User>()
            
            users.mapIndexed { index, user ->
                LeaderboardEntry(
                    rank = index + 1,
                    userId = user.id,
                    displayName = user.fullName.ifBlank { "Anônimo" },
                    avatarUrl = user.avatarUrl,
                    xp = user.xp,
                    level = user.level,
                    streakDays = user.streak,
                    isCurrentUser = user.authId == currentUserId
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get daily challenges
     */
    suspend fun getDailyChallenges(): List<DailyChallenge> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Generate default daily challenges
        return listOf(
            DailyChallenge(
                id = "daily_1",
                date = today,
                type = ChallengeType.COMPLETE_EXERCISES,
                title = "Praticante",
                description = "Complete 3 exercícios",
                requirement = 3,
                xpReward = 50,
                coinsReward = 10
            ),
            DailyChallenge(
                id = "daily_2",
                date = today,
                type = ChallengeType.EARN_XP,
                title = "Coletor de XP",
                description = "Ganhe 100 XP",
                requirement = 100,
                xpReward = 25,
                coinsReward = 5
            ),
            DailyChallenge(
                id = "daily_3",
                date = today,
                type = ChallengeType.PERFECT_EXERCISES,
                title = "Perfeccionista",
                description = "Acerte 100% em 1 exercício",
                requirement = 1,
                xpReward = 75,
                coinsReward = 15
            )
        )
    }
}
