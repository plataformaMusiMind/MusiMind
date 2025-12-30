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
     * Get user achievements from Supabase
     */
    fun getUserAchievements(): Flow<List<Achievement>> = flow {
        val userId = currentUserId
        
        try {
            // Fetch all achievements
            val allAchievements = postgrest.from("achievements")
                .select()
                .decodeList<AchievementEntity>()
            
            // Fetch user's unlocked achievements
            val userUnlocked = if (userId != null) {
                try {
                    postgrest.from("user_achievements")
                        .select {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<UserAchievementEntity>()
                        .map { it.achievementId }
                        .toSet()
                } catch (e: Exception) {
                    emptySet()
                }
            } else {
                emptySet()
            }
            
            // Map to domain model with unlock status
            val achievements = allAchievements.map { entity ->
                entity.toDomainModel(isUnlocked = userUnlocked.contains(entity.id))
            }
            
            emit(achievements)
            
        } catch (e: Exception) {
            // Fallback to hardcoded achievements
            emit(Achievements.ALL.map { it.copy(isUnlocked = false) })
        }
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
     * Get daily challenges from Supabase
     */
    suspend fun getDailyChallenges(): List<DailyChallenge> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        return try {
            // Try to fetch from Supabase
            val challenges = postgrest.from("daily_challenges")
                .select {
                    filter { eq("date", today) }
                }
                .decodeList<DailyChallengeEntity>()
            
            if (challenges.isNotEmpty()) {
                challenges.map { it.toDomainModel() }
            } else {
                // Return default challenges if none exist for today
                getDefaultDailyChallenges(today)
            }
        } catch (e: Exception) {
            // Fallback to default challenges on error
            getDefaultDailyChallenges(today)
        }
    }
    
    /**
     * Default daily challenges when Supabase data is unavailable
     */
    private fun getDefaultDailyChallenges(date: String): List<DailyChallenge> {
        return listOf(
            DailyChallenge(
                id = "daily_1",
                date = date,
                type = ChallengeType.COMPLETE_EXERCISES,
                title = "Praticante",
                description = "Complete 3 exercícios",
                requirement = 3,
                xpReward = 50,
                coinsReward = 10
            ),
            DailyChallenge(
                id = "daily_2",
                date = date,
                type = ChallengeType.EARN_XP,
                title = "Coletor de XP",
                description = "Ganhe 100 XP",
                requirement = 100,
                xpReward = 25,
                coinsReward = 5
            ),
            DailyChallenge(
                id = "daily_3",
                date = date,
                type = ChallengeType.PERFECT_EXERCISES,
                title = "Perfeccionista",
                description = "Acerte 100% em 1 exercício",
                requirement = 1,
                xpReward = 75,
                coinsReward = 15
            )
        )
    }
    
    /**
     * Get user statistics for profile
     */
    suspend fun getUserStatistics(userId: String): UserStatistics {
        return try {
            val stats = postgrest.from("user_stats")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<UserStatsEntity>()
            
            stats?.toUserStatistics() ?: UserStatistics()
        } catch (e: Exception) {
            UserStatistics()
        }
    }
    
    /**
     * Get recent unlocked achievements
     */
    suspend fun getRecentAchievements(userId: String, limit: Int): List<AchievementInfo> {
        return try {
            val userAchievements = postgrest.from("user_achievements")
                .select {
                    filter { eq("user_id", userId) }
                    order("unlocked_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<UserAchievementEntity>()
            
            userAchievements.mapNotNull { ua ->
                try {
                    val achievement = postgrest.from("achievements")
                        .select { filter { eq("id", ua.achievementId) } }
                        .decodeSingleOrNull<AchievementEntity>()
                    
                    achievement?.let {
                        AchievementInfo(
                            id = it.id,
                            name = it.name,
                            displayName = it.displayName ?: it.name,
                            description = it.description ?: "",
                            icon = it.icon ?: "emoji_events"
                        )
                    }
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * User statistics data class
 */
data class UserStatistics(
    val totalStudyTimeMinutes: Int = 0,
    val exercisesCompleted: Int = 0,
    val accuracyRate: Float = 0f,
    val duelsWon: Int = 0,
    val duelsPlayed: Int = 0,
    val longestStreak: Int = 0
)

/**
 * Achievement info for display
 */
data class AchievementInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val icon: String
)

/**
 * Entity for user_stats table
 */
@kotlinx.serialization.Serializable
data class UserStatsEntity(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String = "",
    @kotlinx.serialization.SerialName("total_study_time_minutes")
    val totalStudyTimeMinutes: Int = 0,
    @kotlinx.serialization.SerialName("exercises_completed")
    val exercisesCompleted: Int = 0,
    @kotlinx.serialization.SerialName("accuracy_rate")
    val accuracyRate: Float = 0f,
    @kotlinx.serialization.SerialName("duels_won")
    val duelsWon: Int = 0,
    @kotlinx.serialization.SerialName("duels_played")
    val duelsPlayed: Int = 0,
    @kotlinx.serialization.SerialName("longest_streak")
    val longestStreak: Int = 0
) {
    fun toUserStatistics() = UserStatistics(
        totalStudyTimeMinutes = totalStudyTimeMinutes,
        exercisesCompleted = exercisesCompleted,
        accuracyRate = accuracyRate,
        duelsWon = duelsWon,
        duelsPlayed = duelsPlayed,
        longestStreak = longestStreak
    )
}

/**
 * Entity class for deserializing daily challenges from Supabase
 */
@kotlinx.serialization.Serializable
private data class DailyChallengeEntity(
    val id: String,
    val date: String,
    @kotlinx.serialization.SerialName("exercise_id")
    val exerciseId: String? = null,
    val title: String,
    val description: String,
    @kotlinx.serialization.SerialName("xp_bonus")
    val xpBonus: Int,
    @kotlinx.serialization.SerialName("coins_bonus")
    val coinsBonus: Int
) {
    fun toDomainModel(): DailyChallenge {
        // Parse requirement from description (e.g., "Complete 3 exercícios" -> 3)
        val requirement = description.filter { it.isDigit() }.toIntOrNull() ?: 1
        
        // Infer challenge type from title
        val type = when {
            title.contains("XP", ignoreCase = true) -> ChallengeType.EARN_XP
            title.contains("Perfec", ignoreCase = true) -> ChallengeType.PERFECT_EXERCISES
            title.contains("Duel", ignoreCase = true) -> ChallengeType.WIN_DUEL
            else -> ChallengeType.COMPLETE_EXERCISES
        }
        
        return DailyChallenge(
            id = id,
            date = date,
            type = type,
            title = title,
            description = description,
            requirement = requirement,
            xpReward = xpBonus,
            coinsReward = coinsBonus
        )
    }
}

/**
 * Entity class for deserializing achievements from Supabase
 */
@kotlinx.serialization.Serializable
private data class AchievementEntity(
    val id: String,
    val name: String,
    @kotlinx.serialization.SerialName("display_name")
    val displayName: String,
    val description: String,
    val icon: String,
    val category: String,
    @kotlinx.serialization.SerialName("requirement_type")
    val requirementType: String,
    @kotlinx.serialization.SerialName("requirement_value")
    val requirementValue: Int,
    @kotlinx.serialization.SerialName("xp_reward")
    val xpReward: Int,
    @kotlinx.serialization.SerialName("coins_reward")
    val coinsReward: Int = 0,
    @kotlinx.serialization.SerialName("is_hidden")
    val isHidden: Boolean = false,
    @kotlinx.serialization.SerialName("sort_order")
    val sortOrder: Int = 0
) {
    fun toDomainModel(isUnlocked: Boolean): Achievement {
        val achievementCategory = when (category.lowercase()) {
            "learning" -> AchievementCategory.LEARNING
            "practice" -> AchievementCategory.PRACTICE
            "streak" -> AchievementCategory.STREAK
            "performance" -> AchievementCategory.PERFORMANCE
            "social" -> AchievementCategory.SOCIAL
            "xp", "level" -> AchievementCategory.EXPLORATION
            "mastery" -> AchievementCategory.MASTERY
            else -> AchievementCategory.EXPLORATION
        }
        
        val tier = when {
            xpReward >= 1000 -> AchievementTier.DIAMOND
            xpReward >= 500 -> AchievementTier.GOLD
            xpReward >= 200 -> AchievementTier.SILVER
            else -> AchievementTier.BRONZE
        }
        
        val reqType = when (requirementType.lowercase()) {
            "exercises" -> RequirementType.COMPLETE_EXERCISES
            "lessons" -> RequirementType.COMPLETE_LESSONS
            "streak" -> RequirementType.STREAK_DAYS
            "xp" -> RequirementType.TOTAL_XP
            "perfect" -> RequirementType.PERFECT_SCORES
            "friends" -> RequirementType.FRIENDS_COUNT
            "duels" -> RequirementType.WIN_DUELS
            "level" -> RequirementType.TOTAL_XP // Map level to XP-based
            else -> RequirementType.FIRST_ACTION
        }
        
        return Achievement(
            id = name, // Use name as ID to match original logic
            title = displayName,
            description = description,
            iconName = icon,
            category = achievementCategory,
            tier = tier,
            xpReward = xpReward,
            requirement = AchievementRequirement(
                type = reqType,
                targetValue = requirementValue
            ),
            isUnlocked = isUnlocked
        )
    }
}

/**
 * Entity for user-achievement relationship
 */
@kotlinx.serialization.Serializable
private data class UserAchievementEntity(
    val id: String,
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("achievement_id")
    val achievementId: String,
    @kotlinx.serialization.SerialName("unlocked_at")
    val unlockedAt: String? = null
)
