package com.musimind.domain.gamification

import com.musimind.domain.model.*
import com.musimind.domain.notification.MusiMindNotificationManager
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central Rewards Manager
 * 
 * Integrates all gamification systems:
 * - XP and Leveling
 * - Coins (Currency)
 * - Streaks
 * - Achievements
 * - Daily Bonuses
 * - Multipliers
 */

@Singleton
class RewardsManager @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val livesManager: LivesManager,
    private val progressionManager: ProgressionManager,
    private val notificationManager: MusiMindNotificationManager
) {
    companion object {
        // XP Multipliers
        const val STREAK_MULTIPLIER_PER_DAY = 0.05f // 5% per streak day
        const val MAX_STREAK_MULTIPLIER = 2.0f // Cap at 2x
        const val PERFECT_SCORE_BONUS = 1.5f
        const val FIRST_EXERCISE_OF_DAY_BONUS = 25
        
        // Streak requirements
        const val MIN_XP_FOR_STREAK = 10 // Minimum XP to count for streak
        
        // Level thresholds
        val LEVEL_XP_THRESHOLDS = listOf(
            0, 100, 250, 500, 1000, 2000, 4000, 7500, 12500, 20000,
            30000, 45000, 65000, 90000, 125000, 170000, 230000, 310000, 420000, 560000
        )
    }
    
    private val currentUserId: String? get() = auth.currentSessionOrNull()?.user?.id
    
    // Observable rewards state
    private val _rewardsState = MutableStateFlow<RewardsState>(RewardsState.Loading)
    val rewardsState: StateFlow<RewardsState> = _rewardsState.asStateFlow()
    
    // Pending rewards queue (for animations)
    private val _pendingRewards = MutableSharedFlow<PendingReward>()
    val pendingRewards: SharedFlow<PendingReward> = _pendingRewards.asSharedFlow()
    
    /**
     * Initialize rewards system
     */
    suspend fun initialize() {
        refreshRewardsState()
        checkDailyBonus()
    }
    
    /**
     * Refresh current rewards state from Supabase
     */
    suspend fun refreshRewardsState() {
        val userId = currentUserId ?: run {
            _rewardsState.value = RewardsState.Error("Not authenticated")
            return
        }
        
        try {
            val userData = postgrest.from("users")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserRewardsEntity>()
            
            if (userData != null) {
                val level = calculateLevel(userData.xp)
                val xpForCurrentLevel = if (level > 0 && level < LEVEL_XP_THRESHOLDS.size) 
                    LEVEL_XP_THRESHOLDS[level - 1] else 0
                val xpForNextLevel = if (level < LEVEL_XP_THRESHOLDS.size) 
                    LEVEL_XP_THRESHOLDS[level] else LEVEL_XP_THRESHOLDS.last()
                
                val streakMultiplier = calculateStreakMultiplier(userData.currentStreak)
                
                _rewardsState.value = RewardsState.Loaded(
                    xp = userData.xp,
                    level = level,
                    xpForCurrentLevel = xpForCurrentLevel,
                    xpForNextLevel = xpForNextLevel,
                    coins = userData.coins,
                    gems = userData.gems,
                    currentStreak = userData.currentStreak,
                    longestStreak = userData.longestStreak,
                    streakMultiplier = streakMultiplier,
                    lastActivityDate = userData.lastActivityDate?.let { 
                        LocalDate.parse(it.take(10)) 
                    }
                )
            }
        } catch (e: Exception) {
            _rewardsState.value = RewardsState.Error(e.message ?: "Failed to load rewards")
        }
    }
    
    /**
     * Award XP for completing an exercise
     */
    suspend fun awardExerciseXp(
        baseXp: Int,
        accuracy: Float,
        isFirstOfDay: Boolean,
        isPerfect: Boolean = false,
        bonusReasons: List<String> = emptyList()
    ): XpAwardResult {
        val userId = currentUserId ?: return XpAwardResult.Error("Not authenticated")
        
        val state = _rewardsState.value
        if (state !is RewardsState.Loaded) return XpAwardResult.Error("State not loaded")
        
        // Calculate multipliers
        var totalMultiplier = 1.0f
        val appliedBonuses = mutableListOf<XpBonus>()
        
        // Streak multiplier
        if (state.streakMultiplier > 1.0f) {
            totalMultiplier *= state.streakMultiplier
            appliedBonuses.add(XpBonus(
                name = "Streak ${state.currentStreak} dias",
                multiplier = state.streakMultiplier
            ))
        }
        
        // Perfect score bonus
        if (isPerfect || accuracy >= 100f) {
            totalMultiplier *= PERFECT_SCORE_BONUS
            appliedBonuses.add(XpBonus(
                name = "Pontuação Perfeita!",
                multiplier = PERFECT_SCORE_BONUS
            ))
        }
        
        // Calculate final XP
        var finalXp = (baseXp * totalMultiplier).toInt()
        
        // First exercise of day bonus
        if (isFirstOfDay) {
            finalXp += FIRST_EXERCISE_OF_DAY_BONUS
            appliedBonuses.add(XpBonus(
                name = "Primeiro do dia!",
                flatBonus = FIRST_EXERCISE_OF_DAY_BONUS
            ))
        }
        
        return try {
            val newXp = state.xp + finalXp
            val oldLevel = state.level
            val newLevel = calculateLevel(newXp)
            val didLevelUp = newLevel > oldLevel
            
            // Update in Supabase
            postgrest.from("users").update(
                mapOf(
                    "xp" to newXp,
                    "level" to newLevel,
                    "last_activity_date" to Instant.now().toString()
                )
            ) {
                filter { eq("id", userId) }
            }
            
            // Check and update streak
            updateStreak()
            
            // Emit pending reward for animation
            _pendingRewards.emit(PendingReward.XpGained(
                amount = finalXp,
                bonuses = appliedBonuses
            ))
            
            // Level up notification
            if (didLevelUp) {
                _pendingRewards.emit(PendingReward.LevelUp(newLevel))
            }
            
            // Check achievements
            checkXpAchievements(newXp)
            
            refreshRewardsState()
            
            XpAwardResult.Success(
                xpAwarded = finalXp,
                totalXp = newXp,
                leveledUp = didLevelUp,
                newLevel = newLevel,
                bonuses = appliedBonuses
            )
        } catch (e: Exception) {
            XpAwardResult.Error(e.message ?: "Failed to award XP")
        }
    }
    
    /**
     * Award coins
     */
    suspend fun awardCoins(amount: Int, reason: String): Boolean {
        val userId = currentUserId ?: return false
        val state = _rewardsState.value as? RewardsState.Loaded ?: return false
        
        return try {
            val newCoins = state.coins + amount
            
            postgrest.from("users").update(
                mapOf("coins" to newCoins)
            ) {
                filter { eq("id", userId) }
            }
            
            _pendingRewards.emit(PendingReward.CoinsGained(amount, reason))
            
            refreshRewardsState()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Spend coins
     */
    suspend fun spendCoins(amount: Int, reason: String): Boolean {
        val userId = currentUserId ?: return false
        val state = _rewardsState.value as? RewardsState.Loaded ?: return false
        
        if (state.coins < amount) return false
        
        return try {
            val newCoins = state.coins - amount
            
            postgrest.from("users").update(
                mapOf("coins" to newCoins)
            ) {
                filter { eq("id", userId) }
            }
            
            // Log transaction
            postgrest.from("coin_transactions").insert(
                mapOf(
                    "user_id" to userId,
                    "amount" to -amount,
                    "reason" to reason,
                    "balance_after" to newCoins
                )
            )
            
            refreshRewardsState()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Update streak based on activity
     */
    private suspend fun updateStreak() {
        val userId = currentUserId ?: return
        val state = _rewardsState.value as? RewardsState.Loaded ?: return
        
        val today = LocalDate.now(ZoneId.systemDefault())
        val lastActivity = state.lastActivityDate
        
        try {
            when {
                // First activity ever or same day
                lastActivity == null || lastActivity == today -> {
                    // Do nothing to streak
                }
                
                // Consecutive day - increment streak
                lastActivity == today.minusDays(1) -> {
                    val newStreak = state.currentStreak + 1
                    val newLongest = maxOf(newStreak, state.longestStreak)
                    
                    postgrest.from("users").update(
                        mapOf(
                            "current_streak" to newStreak,
                            "longest_streak" to newLongest
                        )
                    ) {
                        filter { eq("id", userId) }
                    }
                    
                    // Streak milestone notifications
                    if (newStreak in listOf(3, 7, 14, 30, 50, 100, 365)) {
                        _pendingRewards.emit(PendingReward.StreakMilestone(newStreak))
                        checkStreakAchievements(newStreak)
                    }
                }
                
                // Streak broken
                else -> {
                    postgrest.from("users").update(
                        mapOf("current_streak" to 1)
                    ) {
                        filter { eq("id", userId) }
                    }
                    
                    if (state.currentStreak > 0) {
                        _pendingRewards.emit(PendingReward.StreakLost(state.currentStreak))
                    }
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Check for daily bonus
     */
    private suspend fun checkDailyBonus() {
        val userId = currentUserId ?: return
        
        try {
            val today = LocalDate.now()
            val lastBonus = postgrest.from("daily_bonuses")
                .select {
                    filter { 
                        eq("user_id", userId)
                        eq("claim_date", today.toString())
                    }
                }
                .decodeSingleOrNull<DailyBonusEntity>()
            
            if (lastBonus == null) {
                // Bonus available!
                _pendingRewards.emit(PendingReward.DailyBonusAvailable(determineDailyBonusReward()))
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Claim daily bonus
     */
    suspend fun claimDailyBonus(): DailyBonusReward? {
        val userId = currentUserId ?: return null
        
        val today = LocalDate.now()
        val reward = determineDailyBonusReward()
        
        return try {
            // Record claim
            postgrest.from("daily_bonuses").insert(
                mapOf(
                    "user_id" to userId,
                    "claim_date" to today.toString(),
                    "xp_reward" to reward.xp,
                    "coins_reward" to reward.coins
                )
            )
            
            // Award rewards
            if (reward.xp > 0) {
                awardExerciseXp(reward.xp, 100f, false)
            }
            if (reward.coins > 0) {
                awardCoins(reward.coins, "Bônus Diário")
            }
            
            _pendingRewards.emit(PendingReward.DailyBonusClaimed(reward))
            
            reward
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Determine daily bonus based on streak
     */
    private fun determineDailyBonusReward(): DailyBonusReward {
        val state = _rewardsState.value as? RewardsState.Loaded
        val streak = state?.currentStreak ?: 0
        
        return when {
            streak >= 30 -> DailyBonusReward(xp = 50, coins = 25, day = streak)
            streak >= 14 -> DailyBonusReward(xp = 35, coins = 15, day = streak)
            streak >= 7 -> DailyBonusReward(xp = 25, coins = 10, day = streak)
            streak >= 3 -> DailyBonusReward(xp = 15, coins = 5, day = streak)
            else -> DailyBonusReward(xp = 10, coins = 3, day = streak)
        }
    }
    
    /**
     * Check and unlock XP-related achievements
     */
    private suspend fun checkXpAchievements(totalXp: Int) {
        val achievementThresholds = listOf(
            100 to "first_100_xp",
            500 to "xp_500",
            1000 to "xp_1000",
            5000 to "xp_5000",
            10000 to "xp_10000",
            25000 to "xp_25000",
            50000 to "xp_50000",
            100000 to "xp_100000"
        )
        
        for ((threshold, achievementId) in achievementThresholds) {
            if (totalXp >= threshold) {
                unlockAchievement(achievementId)
            }
        }
    }
    
    /**
     * Check and unlock streak achievements
     */
    private suspend fun checkStreakAchievements(streak: Int) {
        val achievementThresholds = listOf(
            3 to "streak_3",
            7 to "streak_7",
            14 to "streak_14",
            30 to "streak_30",
            50 to "streak_50",
            100 to "streak_100",
            365 to "streak_365"
        )
        
        for ((threshold, achievementId) in achievementThresholds) {
            if (streak >= threshold) {
                unlockAchievement(achievementId)
            }
        }
    }
    
    /**
     * Unlock an achievement
     */
    private suspend fun unlockAchievement(achievementId: String) {
        val userId = currentUserId ?: return
        
        try {
            // Check if already unlocked
            val existing = postgrest.from("user_achievements")
                .select {
                    filter { 
                        eq("user_id", userId)
                        eq("achievement_id", achievementId)
                    }
                }
                .decodeSingleOrNull<Any>()
            
            if (existing != null) return
            
            // Unlock
            postgrest.from("user_achievements").insert(
                mapOf(
                    "user_id" to userId,
                    "achievement_id" to achievementId,
                    "unlocked_at" to Instant.now().toString()
                )
            )
            
            // Get achievement details for notification
            val achievement = postgrest.from("achievements")
                .select {
                    filter { eq("id", achievementId) }
                }
                .decodeSingleOrNull<AchievementEntity>()
            
            if (achievement != null) {
                _pendingRewards.emit(PendingReward.AchievementUnlocked(
                    id = achievement.id,
                    title = achievement.title,
                    xpReward = achievement.xpReward
                ))
                
                // Award XP from achievement
                if (achievement.xpReward > 0) {
                    awardExerciseXp(achievement.xpReward, 100f, false)
                }
                
                // Push notification
                notificationManager.showAchievementUnlocked(
                    achievement.title,
                    achievement.xpReward
                )
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Calculate level from XP
     */
    private fun calculateLevel(xp: Int): Int {
        for (i in LEVEL_XP_THRESHOLDS.indices.reversed()) {
            if (xp >= LEVEL_XP_THRESHOLDS[i]) {
                return i + 1
            }
        }
        return 1
    }
    
    /**
     * Calculate streak multiplier
     */
    private fun calculateStreakMultiplier(streakDays: Int): Float {
        val multiplier = 1.0f + (streakDays * STREAK_MULTIPLIER_PER_DAY)
        return multiplier.coerceAtMost(MAX_STREAK_MULTIPLIER)
    }
}

// ============================================
// State Classes
// ============================================

sealed class RewardsState {
    object Loading : RewardsState()
    
    data class Loaded(
        val xp: Int,
        val level: Int,
        val xpForCurrentLevel: Int,
        val xpForNextLevel: Int,
        val coins: Int,
        val gems: Int,
        val currentStreak: Int,
        val longestStreak: Int,
        val streakMultiplier: Float,
        val lastActivityDate: LocalDate?
    ) : RewardsState() {
        val xpProgress: Float
            get() {
                val needed = xpForNextLevel - xpForCurrentLevel
                val current = xp - xpForCurrentLevel
                return (current.toFloat() / needed).coerceIn(0f, 1f)
            }
    }
    
    data class Error(val message: String) : RewardsState()
}

sealed class XpAwardResult {
    data class Success(
        val xpAwarded: Int,
        val totalXp: Int,
        val leveledUp: Boolean,
        val newLevel: Int,
        val bonuses: List<XpBonus>
    ) : XpAwardResult()
    
    data class Error(val message: String) : XpAwardResult()
}

data class XpBonus(
    val name: String,
    val multiplier: Float = 1f,
    val flatBonus: Int = 0
)

data class DailyBonusReward(
    val xp: Int,
    val coins: Int,
    val day: Int
)

sealed class PendingReward {
    data class XpGained(val amount: Int, val bonuses: List<XpBonus>) : PendingReward()
    data class CoinsGained(val amount: Int, val reason: String) : PendingReward()
    data class LevelUp(val newLevel: Int) : PendingReward()
    data class StreakMilestone(val days: Int) : PendingReward()
    data class StreakLost(val previousStreak: Int) : PendingReward()
    data class AchievementUnlocked(val id: String, val title: String, val xpReward: Int) : PendingReward()
    data class DailyBonusAvailable(val reward: DailyBonusReward) : PendingReward()
    data class DailyBonusClaimed(val reward: DailyBonusReward) : PendingReward()
}

// ============================================
// Entity Classes
// ============================================

@Serializable
private data class UserRewardsEntity(
    val id: String,
    val xp: Int = 0,
    val level: Int = 1,
    val coins: Int = 0,
    val gems: Int = 0,
    @kotlinx.serialization.SerialName("current_streak")
    val currentStreak: Int = 0,
    @kotlinx.serialization.SerialName("longest_streak")
    val longestStreak: Int = 0,
    @kotlinx.serialization.SerialName("last_activity_date")
    val lastActivityDate: String? = null
)

@Serializable
private data class DailyBonusEntity(
    val id: String,
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("claim_date")
    val claimDate: String
)

@Serializable
private data class AchievementEntity(
    val id: String,
    val title: String,
    @kotlinx.serialization.SerialName("xp_reward")
    val xpReward: Int = 0
)
