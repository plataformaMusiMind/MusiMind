package com.musimind.domain.model

import kotlinx.serialization.Serializable

/**
 * Daily challenge system
 */
@Serializable
data class DailyChallenge(
    val id: String,
    val date: String, // yyyy-MM-dd format
    val type: ChallengeType,
    val title: String,
    val description: String,
    val requirement: Int,
    val currentProgress: Int = 0,
    val xpReward: Int,
    val coinsReward: Int = 0,
    val isCompleted: Boolean = false
) {
    val progressPercentage: Float get() = 
        (currentProgress.toFloat() / requirement).coerceIn(0f, 1f)
}

@Serializable
enum class ChallengeType {
    COMPLETE_EXERCISES,
    PERFECT_EXERCISES,
    EARN_XP,
    MAINTAIN_STREAK,
    WIN_DUEL,
    PRACTICE_CATEGORY
}

/**
 * Leaderboard entry
 */
@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val xp: Int,
    val level: Int,
    val streakDays: Int,
    val isCurrentUser: Boolean = false
)

/**
 * Leaderboard types
 */
enum class LeaderboardType {
    WEEKLY,
    MONTHLY,
    ALL_TIME,
    FRIENDS
}

/**
 * User statistics for profile and leaderboards
 */
@Serializable
data class UserStats(
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val exercisesCompleted: Int = 0,
    val lessonsCompleted: Int = 0,
    val perfectScores: Int = 0,
    val totalPlayTimeMinutes: Int = 0,
    val averageAccuracy: Float = 0f,
    val duelsWon: Int = 0,
    val duelsLost: Int = 0,
    val achievementsUnlocked: Int = 0,
    val friendsCount: Int = 0,
    
    // Category-specific stats
    val solfegeExercises: Int = 0,
    val rhythmExercises: Int = 0,
    val intervalExercises: Int = 0,
    val melodyExercises: Int = 0,
    val harmonyExercises: Int = 0
) {
    val duelWinRate: Float get() = if (duelsWon + duelsLost > 0) {
        duelsWon.toFloat() / (duelsWon + duelsLost)
    } else 0f
}

/**
 * Virtual currency system
 */
@Serializable
data class Wallet(
    val coins: Int = 0,
    val gems: Int = 0
)

/**
 * Shop item
 */
@Serializable
data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val type: ShopItemType,
    val priceCoins: Int = 0,
    val priceGems: Int = 0,
    val isPurchased: Boolean = false
)

@Serializable
enum class ShopItemType {
    AVATAR,
    THEME,
    POWER_UP,
    STREAK_FREEZE
}

/**
 * Power-ups for exercises
 */
@Serializable
data class PowerUp(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val type: PowerUpType,
    val quantity: Int = 0
)

@Serializable
enum class PowerUpType {
    DOUBLE_XP,          // 2x XP for 1 exercise
    EXTRA_LIFE,         // +1 life in exercise
    SKIP_QUESTION,      // Skip without penalty
    STREAK_FREEZE,      // Protect streak for 1 day
    HINT               // Show hint
}

/**
 * Reward for completing actions
 */
@Serializable
data class Reward(
    val type: RewardType,
    val amount: Int,
    val description: String = ""
)

@Serializable
enum class RewardType {
    XP,
    COINS,
    GEMS,
    POWER_UP,
    ACHIEVEMENT
}
