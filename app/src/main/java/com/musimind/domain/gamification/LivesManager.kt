package com.musimind.domain.gamification

import com.musimind.domain.model.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lives (Hearts) Management System
 * 
 * Features:
 * - Track user lives (hearts) like Duolingo
 * - Auto-regeneration every 30 minutes
 * - Lose life on exercise failure (< 75% accuracy)
 * - Multiple ways to refill lives
 * - Unlimited hearts for premium users
 */

@Singleton
class LivesManager @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) {
    companion object {
        const val MAX_LIVES = 5
        const val LIFE_REGEN_MINUTES = 30L
        const val MIN_ACCURACY_TO_PASS = 75f
    }
    
    private val currentUserId: String? get() = auth.currentSessionOrNull()?.user?.id
    
    // Observable lives state
    private val _livesState = MutableStateFlow<LivesState>(LivesState.Loading)
    val livesState: StateFlow<LivesState> = _livesState.asStateFlow()
    
    /**
     * Initialize and fetch current lives status
     */
    suspend fun initialize() {
        refreshLives()
    }
    
    /**
     * Get current lives from Supabase
     */
    suspend fun refreshLives() {
        val userId = currentUserId ?: run {
            _livesState.value = LivesState.Error("Not authenticated")
            return
        }
        
        try {
            // First, trigger regeneration check on server
            postgrest.rpc("regenerate_lives", buildJsonObject { put("p_user_id", userId) })
            
            // Then fetch current state
            val livesData = postgrest.from("user_lives")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeSingleOrNull<UserLivesEntity>()
            
            if (livesData != null) {
                val hasUnlimited = livesData.unlimitedUntil?.let { 
                    Instant.parse(it).isAfter(Instant.now()) 
                } ?: false
                
                val timeUntilNextLife = livesData.nextLifeRegenAt?.let {
                    val regenTime = Instant.parse(it)
                    if (regenTime.isAfter(Instant.now())) {
                        Duration.between(Instant.now(), regenTime)
                    } else null
                }
                
                _livesState.value = LivesState.Loaded(
                    currentLives = livesData.currentLives,
                    maxLives = livesData.maxLives,
                    hasUnlimitedHearts = hasUnlimited,
                    timeUntilNextLife = timeUntilNextLife
                )
            } else {
                // Create lives record for user
                createUserLives(userId)
            }
        } catch (e: Exception) {
            _livesState.value = LivesState.Error(e.message ?: "Failed to load lives")
        }
    }
    
    /**
     * Create initial lives for new user
     */
    private suspend fun createUserLives(userId: String) {
        try {
            postgrest.from("user_lives").insert(
                mapOf(
                    "user_id" to userId,
                    "current_lives" to MAX_LIVES,
                    "max_lives" to MAX_LIVES
                )
            )
            _livesState.value = LivesState.Loaded(
                currentLives = MAX_LIVES,
                maxLives = MAX_LIVES,
                hasUnlimitedHearts = false,
                timeUntilNextLife = null
            )
        } catch (e: Exception) {
            _livesState.value = LivesState.Error("Failed to create lives: ${e.message}")
        }
    }
    
    /**
     * Lose a life when exercise fails (< 75% accuracy)
     * Returns true if user can continue, false if out of lives
     */
    suspend fun loseLife(exerciseId: String? = null, reason: String = "Exercise failed"): LivesLossResult {
        val userId = currentUserId ?: return LivesLossResult.Error("Not authenticated")
        
        val currentState = _livesState.value
        if (currentState is LivesState.Loaded && currentState.hasUnlimitedHearts) {
            return LivesLossResult.Success(currentLives = MAX_LIVES, wasLastLife = false)
        }
        
        // Validate exerciseId is a valid UUID before sending to PostgreSQL
        val validatedExerciseId = exerciseId?.let { id ->
            try {
                // UUID format: 8-4-4-4-12 hex chars
                if (id.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))) {
                    id
                } else {
                    null // Not a valid UUID, pass null to avoid PostgreSQL error
                }
            } catch (e: Exception) {
                null
            }
        }
        
        return try {
            val result = postgrest.rpc(
                "lose_life",
                buildJsonObject {
                    put("p_user_id", userId)
                    put("p_exercise_id", validatedExerciseId)
                    put("p_reason", reason)
                }
            ).decodeSingleOrNull<LoseLifeResult>()
            
            refreshLives()
            
            if (result?.success == true) {
                val lives = result.currentLives ?: 0
                LivesLossResult.Success(
                    currentLives = lives,
                    wasLastLife = lives == 0
                )
            } else {
                LivesLossResult.OutOfLives(result?.message ?: "No lives remaining")
            }
        } catch (e: Exception) {
            LivesLossResult.Error(e.message ?: "Failed to process life loss")
        }
    }
    
    /**
     * Check if user has lives available
     */
    fun hasLives(): Boolean {
        val state = _livesState.value
        return when (state) {
            is LivesState.Loaded -> state.currentLives > 0 || state.hasUnlimitedHearts
            else -> false
        }
    }
    
    /**
     * Get current lives count
     */
    fun getCurrentLives(): Int {
        val state = _livesState.value
        return when (state) {
            is LivesState.Loaded -> if (state.hasUnlimitedHearts) MAX_LIVES else state.currentLives
            else -> 0
        }
    }
    
    /**
     * Refill all lives
     */
    suspend fun refillLives(method: RefillMethod): Boolean {
        val userId = currentUserId ?: return false
        
        return try {
            postgrest.rpc(
                "refill_lives",
                buildJsonObject {
                    put("p_user_id", userId)
                    put("p_method", method.name)
                    put("p_lives_to_add", MAX_LIVES)
                }
            )
            refreshLives()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Watch an ad to recover one life
     */
    suspend fun watchAdForLife(): Boolean {
        val userId = currentUserId ?: return false
        
        return try {
            postgrest.rpc(
                "refill_lives",
                buildJsonObject {
                    put("p_user_id", userId)
                    put("p_method", "AD_WATCH")
                    put("p_lives_to_add", 1)
                }
            )
            refreshLives()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Activate unlimited hearts for a duration
     */
    suspend fun activateUnlimitedHearts(durationHours: Int): Boolean {
        val userId = currentUserId ?: return false
        
        return try {
            val unlimitedUntil = Instant.now().plus(durationHours.toLong(), ChronoUnit.HOURS)
            
            postgrest.from("user_lives").update(
                mapOf(
                    "unlimited_until" to unlimitedUntil.toString(),
                    "updated_at" to Instant.now().toString()
                )
            ) {
                filter { eq("user_id", userId) }
            }
            
            // Log transaction
            postgrest.from("life_transactions").insert(
                mapOf(
                    "user_id" to userId,
                    "transaction_type" to "UNLIMITED_ACTIVATED",
                    "lives_change" to 0,
                    "reason" to "Unlimited hearts for $durationHours hours"
                )
            )
            
            refreshLives()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Process exercise result and determine if life should be lost
     */
    suspend fun processExerciseResult(
        exerciseId: String,
        accuracy: Float,
        wasCompleted: Boolean
    ): ExerciseLifeResult {
        // If user didn't complete or got less than 75%, lose a life
        if (!wasCompleted || accuracy < MIN_ACCURACY_TO_PASS) {
            val lossResult = loseLife(
                exerciseId = exerciseId,
                reason = if (!wasCompleted) "Exercise not completed" 
                         else "Accuracy below ${MIN_ACCURACY_TO_PASS.toInt()}%"
            )
            
            return when (lossResult) {
                is LivesLossResult.Success -> {
                    if (lossResult.wasLastLife) {
                        ExerciseLifeResult.LifeLostAndOutOfLives(accuracy)
                    } else {
                        ExerciseLifeResult.LifeLost(lossResult.currentLives)
                    }
                }
                is LivesLossResult.OutOfLives -> ExerciseLifeResult.AlreadyOutOfLives
                is LivesLossResult.Error -> ExerciseLifeResult.Error(lossResult.message)
            }
        }
        
        return ExerciseLifeResult.Passed(accuracy)
    }
    
    /**
     * Start a timer flow for live regeneration countdown
     */
    fun startRegenCountdown(): Flow<Duration?> = flow {
        while (true) {
            val state = _livesState.value
            if (state is LivesState.Loaded && state.timeUntilNextLife != null) {
                val remaining = state.timeUntilNextLife.minusSeconds(1)
                if (remaining.isNegative) {
                    refreshLives()
                    emit(null)
                } else {
                    emit(remaining)
                }
            } else {
                emit(null)
            }
            delay(1000)
        }
    }
}

// ============================================
// State Classes
// ============================================

sealed class LivesState {
    object Loading : LivesState()
    
    data class Loaded(
        val currentLives: Int,
        val maxLives: Int,
        val hasUnlimitedHearts: Boolean,
        val timeUntilNextLife: Duration?
    ) : LivesState()
    
    data class Error(val message: String) : LivesState()
}

sealed class LivesLossResult {
    data class Success(val currentLives: Int, val wasLastLife: Boolean) : LivesLossResult()
    data class OutOfLives(val message: String) : LivesLossResult()
    data class Error(val message: String) : LivesLossResult()
}

sealed class ExerciseLifeResult {
    data class Passed(val accuracy: Float) : ExerciseLifeResult()
    data class LifeLost(val remainingLives: Int) : ExerciseLifeResult()
    data class LifeLostAndOutOfLives(val accuracy: Float) : ExerciseLifeResult()
    object AlreadyOutOfLives : ExerciseLifeResult()
    data class Error(val message: String) : ExerciseLifeResult()
}

enum class RefillMethod {
    PURCHASE,
    AD_WATCH,
    STREAK_BONUS,
    ACHIEVEMENT
}

// ============================================
// Entity Classes for Supabase
// ============================================

@Serializable
private data class UserLivesEntity(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("current_lives")
    val currentLives: Int,
    @kotlinx.serialization.SerialName("max_lives")
    val maxLives: Int,
    @kotlinx.serialization.SerialName("last_life_lost_at")
    val lastLifeLostAt: String? = null,
    @kotlinx.serialization.SerialName("next_life_regen_at")
    val nextLifeRegenAt: String? = null,
    @kotlinx.serialization.SerialName("unlimited_until")
    val unlimitedUntil: String? = null
)

@Serializable
private data class LoseLifeResult(
    val success: Boolean = false,
    @kotlinx.serialization.SerialName("current_lives")
    val currentLives: Int? = null,
    val message: String? = null
)
