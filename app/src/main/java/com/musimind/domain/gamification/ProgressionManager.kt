package com.musimind.domain.gamification

import com.musimind.domain.model.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Progression Manager - Handles content unlocking and user progress
 * 
 * Features:
 * - Progressive content unlocking
 * - Level-based exercise access
 * - Node dependency management
 * - Category-specific levels
 * - Adaptive difficulty based on placement test
 */

@Singleton
class ProgressionManager @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth
) {
    companion object {
        const val MAX_LEVEL = 10
        const val XP_PER_LEVEL_BASE = 100
        const val XP_MULTIPLIER = 1.5f
    }
    
    private val currentUserId: String? get() = auth.currentSessionOrNull()?.user?.id
    
    private val _progressionState = MutableStateFlow<UserProgressionState>(UserProgressionState.Loading)
    val progressionState: StateFlow<UserProgressionState> = _progressionState.asStateFlow()
    
    /**
     * Initialize user progression
     */
    suspend fun initialize() {
        refreshProgression()
    }
    
    /**
     * Fetch current progression from Supabase
     */
    suspend fun refreshProgression() {
        val userId = currentUserId ?: run {
            _progressionState.value = UserProgressionState.Error("Not authenticated")
            return
        }
        
        try {
            val progression = postgrest.from("user_progression")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeSingleOrNull<UserProgressionEntity>()
            
            if (progression != null) {
                _progressionState.value = UserProgressionState.Loaded(
                    placementTestCompleted = progression.placementTestCompleted,
                    placementLevel = progression.placementLevel,
                    categoryLevels = CategoryLevels(
                        solfege = progression.solfegeLevel,
                        rhythm = progression.rhythmLevel,
                        intervals = progression.intervalsLevel,
                        melodic = progression.melodicLevel,
                        theory = progression.theoryLevel
                    ),
                    unlockedNodes = progression.unlockedNodes.toSet(),
                    completedNodes = progression.completedNodes.toSet(),
                    completedExercises = progression.completedExercises.toSet(),
                    totalExercisesCompleted = progression.totalExercisesCompleted,
                    averageAccuracy = progression.averageAccuracy
                )
            } else {
                // Create progression record
                createUserProgression(userId)
            }
        } catch (e: Exception) {
            _progressionState.value = UserProgressionState.Error(e.message ?: "Failed to load progression")
        }
    }
    
    /**
     * Create initial progression for new user
     */
    private suspend fun createUserProgression(userId: String) {
        try {
            postgrest.from("user_progression").insert(
                mapOf("user_id" to userId)
            )
            _progressionState.value = UserProgressionState.Loaded(
                placementTestCompleted = false,
                placementLevel = 1,
                categoryLevels = CategoryLevels(),
                unlockedNodes = emptySet(),
                completedNodes = emptySet(),
                completedExercises = emptySet(),
                totalExercisesCompleted = 0,
                averageAccuracy = 0f
            )
        } catch (e: Exception) {
            _progressionState.value = UserProgressionState.Error("Failed to create progression")
        }
    }
    
    /**
     * Check if user needs to take placement test
     */
    fun needsPlacementTest(): Boolean {
        val state = _progressionState.value
        return state is UserProgressionState.Loaded && !state.placementTestCompleted
    }
    
    /**
     * Check if a node is unlocked for the current user
     */
    suspend fun isNodeUnlocked(nodeId: String): Boolean {
        val userId = currentUserId ?: return false
        
        // First node is always unlocked
        val state = _progressionState.value
        if (state is UserProgressionState.Loaded && state.unlockedNodes.contains(nodeId)) {
            return true
        }
        
        return try {
            val result = postgrest.rpc(
                "is_node_unlocked",
                buildJsonObject {
                    put("p_user_id", userId)
                    put("p_node_id", nodeId)
                }
            ).decodeSingleOrNull<Boolean>()
            result ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if an exercise is unlocked based on user's level
     */
    fun isExerciseUnlocked(exercise: Exercise, category: MusicCategory): Boolean {
        val state = _progressionState.value
        if (state !is UserProgressionState.Loaded) return false
        
        // Check if already completed
        if (state.completedExercises.contains(exercise.id)) return true
        
        // Check level requirement
        val userLevel = state.categoryLevels.getLevel(category)
        val requiredLevel = exercise.minUserLevel ?: 1
        
        return userLevel >= requiredLevel
    }
    
    /**
     * Get required level to unlock an exercise
     */
    fun getExerciseUnlockRequirement(exercise: Exercise, category: MusicCategory): UnlockRequirement {
        val state = _progressionState.value
        if (state !is UserProgressionState.Loaded) {
            return UnlockRequirement.Unknown
        }
        
        val userLevel = state.categoryLevels.getLevel(category)
        val requiredLevel = exercise.minUserLevel ?: 1
        
        return if (userLevel >= requiredLevel) {
            UnlockRequirement.Unlocked
        } else {
            UnlockRequirement.LevelRequired(
                currentLevel = userLevel,
                requiredLevel = requiredLevel,
                category = category
            )
        }
    }
    
    /**
     * Complete an exercise and update progression
     */
    suspend fun completeExercise(
        exerciseId: String,
        category: MusicCategory,
        accuracy: Float,
        xpEarned: Int
    ): ExerciseCompletionResult {
        val userId = currentUserId ?: return ExerciseCompletionResult.Error("Not authenticated")
        
        val state = _progressionState.value
        if (state !is UserProgressionState.Loaded) {
            return ExerciseCompletionResult.Error("Progression not loaded")
        }
        
        return try {
            // Update progression in Supabase
            val updatedCompletedExercises = state.completedExercises + exerciseId
            val newTotal = state.totalExercisesCompleted + 1
            val newAvgAccuracy = (state.averageAccuracy * state.totalExercisesCompleted + accuracy) / newTotal
            
            postgrest.from("user_progression").update(
                mapOf(
                    "completed_exercises" to updatedCompletedExercises.toList(),
                    "total_exercises_completed" to newTotal,
                    "average_accuracy" to newAvgAccuracy,
                    "updated_at" to "now()"
                )
            ) {
                filter { eq("user_id", userId) }
            }
            
            // Check if user should level up in this category
            val levelUpResult = checkAndProcessLevelUp(category, xpEarned)
            
            refreshProgression()
            
            ExerciseCompletionResult.Success(
                newTotal = newTotal,
                leveledUp = levelUpResult?.didLevelUp ?: false,
                newLevel = levelUpResult?.newLevel ?: state.categoryLevels.getLevel(category)
            )
        } catch (e: Exception) {
            ExerciseCompletionResult.Error(e.message ?: "Failed to complete exercise")
        }
    }
    
    /**
     * Complete a learning node
     */
    suspend fun completeNode(nodeId: String): Boolean {
        val userId = currentUserId ?: return false
        
        val state = _progressionState.value
        if (state !is UserProgressionState.Loaded) return false
        
        return try {
            val updatedCompletedNodes = state.completedNodes + nodeId
            
            postgrest.from("user_progression").update(
                mapOf(
                    "completed_nodes" to updatedCompletedNodes.toList(),
                    "updated_at" to "now()"
                )
            ) {
                filter { eq("user_id", userId) }
            }
            
            // Unlock next nodes
            unlockDependentNodes(nodeId)
            
            refreshProgression()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Unlock nodes that depend on a completed node
     */
    private suspend fun unlockDependentNodes(completedNodeId: String) {
        val userId = currentUserId ?: return
        
        try {
            // Find nodes that require this node
            val dependentNodes = postgrest.from("node_requirements")
                .select {
                    filter { 
                        eq("requirement_type", "NODE_COMPLETE")
                        eq("required_node_id", completedNodeId)
                    }
                }
                .decodeList<NodeRequirementEntity>()
            
            val state = _progressionState.value
            if (state !is UserProgressionState.Loaded) return
            
            val newUnlockedNodes = state.unlockedNodes.toMutableSet()
            
            for (req in dependentNodes) {
                // Check if all requirements for this node are met
                if (isNodeUnlocked(req.nodeId)) {
                    newUnlockedNodes.add(req.nodeId)
                }
            }
            
            if (newUnlockedNodes.size > state.unlockedNodes.size) {
                postgrest.from("user_progression").update(
                    mapOf("unlocked_nodes" to newUnlockedNodes.toList())
                ) {
                    filter { eq("user_id", userId) }
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Check if user should level up in a category
     */
    private suspend fun checkAndProcessLevelUp(
        category: MusicCategory,
        xpEarned: Int
    ): LevelUpResult? {
        val userId = currentUserId ?: return null
        val state = _progressionState.value as? UserProgressionState.Loaded ?: return null
        
        val currentLevel = state.categoryLevels.getLevel(category)
        if (currentLevel >= MAX_LEVEL) return null
        
        // Check XP threshold for next level
        val xpForNextLevel = calculateXpForLevel(currentLevel + 1)
        
        // This is simplified - in reality you'd track XP per category
        // For now, we level up after every 5 exercises in a category
        val exercisesInCategory = state.completedExercises.count { 
            // This would need proper category tracking
            true 
        }
        
        if (exercisesInCategory % 5 == 0) {
            val newLevel = (currentLevel + 1).coerceAtMost(MAX_LEVEL)
            
            val columnName = when (category) {
                MusicCategory.SOLFEGE -> "solfege_level"
                MusicCategory.RHYTHM -> "rhythm_level"
                MusicCategory.INTERVALS -> "intervals_level"
                MusicCategory.MELODIC_PERCEPTION -> "melodic_level"
                MusicCategory.THEORY -> "theory_level"
                else -> return null
            }
            
            postgrest.from("user_progression").update(
                mapOf(columnName to newLevel)
            ) {
                filter { eq("user_id", userId) }
            }
            
            return LevelUpResult(didLevelUp = true, newLevel = newLevel)
        }
        
        return LevelUpResult(didLevelUp = false, newLevel = currentLevel)
    }
    
    /**
     * Calculate XP required for a level
     */
    fun calculateXpForLevel(level: Int): Int {
        return (XP_PER_LEVEL_BASE * Math.pow(XP_MULTIPLIER.toDouble(), (level - 1).toDouble())).toInt()
    }
    
    /**
     * Set user level based on placement test
     */
    suspend fun setPlacementTestResult(score: Int, determinedLevel: Int): Boolean {
        val userId = currentUserId ?: return false
        
        return try {
            postgrest.from("user_progression").update(
                mapOf(
                    "placement_test_completed" to true,
                    "placement_test_score" to score,
                    "placement_level" to determinedLevel,
                    "solfege_level" to determinedLevel,
                    "rhythm_level" to determinedLevel,
                    "intervals_level" to determinedLevel,
                    "melodic_level" to determinedLevel,
                    "theory_level" to determinedLevel,
                    "updated_at" to "now()"
                )
            ) {
                filter { eq("user_id", userId) }
            }
            
            refreshProgression()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get exercises filtered by user's level
     */
    suspend fun getAvailableExercises(category: MusicCategory): List<ExerciseWithLockStatus> {
        val state = _progressionState.value
        if (state !is UserProgressionState.Loaded) return emptyList()
        
        val userLevel = state.categoryLevels.getLevel(category)
        
        return try {
            val categoryName = when (category) {
                MusicCategory.SOLFEGE -> "solfege"
                MusicCategory.RHYTHM -> "rhythm"
                MusicCategory.INTERVALS -> "intervals"
                MusicCategory.MELODIC_PERCEPTION -> "melodic"
                MusicCategory.THEORY -> "theory"
                else -> return emptyList()
            }
            
            val exercises = postgrest.from("exercises")
                .select {
                    filter { 
                        eq("category_id", postgrest.from("exercise_categories")
                            .select { filter { eq("name", categoryName) } }
                        )
                        eq("is_active", true)
                    }
                    order("difficulty", Order.ASCENDING)
                    order("sort_order", Order.ASCENDING)
                }
                .decodeList<ExerciseEntity>()
            
            exercises.map { exercise ->
                val isCompleted = state.completedExercises.contains(exercise.id)
                val isUnlocked = userLevel >= (exercise.minUserLevel ?: 1)
                
                ExerciseWithLockStatus(
                    exercise = exercise.toDomainModel(category),
                    isLocked = !isUnlocked && !isCompleted,
                    isCompleted = isCompleted,
                    lockReason = if (!isUnlocked && !isCompleted) {
                        "Necessário nível ${exercise.minUserLevel ?: 1} em ${category.displayName}"
                    } else null
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// ============================================
// State and Result Classes
// ============================================

sealed class UserProgressionState {
    object Loading : UserProgressionState()
    
    data class Loaded(
        val placementTestCompleted: Boolean,
        val placementLevel: Int,
        val categoryLevels: CategoryLevels,
        val unlockedNodes: Set<String>,
        val completedNodes: Set<String>,
        val completedExercises: Set<String>,
        val totalExercisesCompleted: Int,
        val averageAccuracy: Float
    ) : UserProgressionState()
    
    data class Error(val message: String) : UserProgressionState()
}

data class CategoryLevels(
    val solfege: Int = 1,
    val rhythm: Int = 1,
    val intervals: Int = 1,
    val melodic: Int = 1,
    val theory: Int = 1
) {
    fun getLevel(category: MusicCategory): Int {
        return when (category) {
            MusicCategory.SOLFEGE -> solfege
            MusicCategory.RHYTHM -> rhythm
            MusicCategory.INTERVALS -> intervals
            MusicCategory.MELODIC_PERCEPTION -> melodic
            MusicCategory.THEORY -> theory
            else -> 1
        }
    }
}

sealed class UnlockRequirement {
    object Unlocked : UnlockRequirement()
    object Unknown : UnlockRequirement()
    
    data class LevelRequired(
        val currentLevel: Int,
        val requiredLevel: Int,
        val category: MusicCategory
    ) : UnlockRequirement()
    
    data class NodeRequired(
        val requiredNodeId: String,
        val requiredNodeTitle: String
    ) : UnlockRequirement()
    
    data class XpRequired(
        val currentXp: Int,
        val requiredXp: Int
    ) : UnlockRequirement()
}

sealed class ExerciseCompletionResult {
    data class Success(
        val newTotal: Int,
        val leveledUp: Boolean,
        val newLevel: Int
    ) : ExerciseCompletionResult()
    
    data class Error(val message: String) : ExerciseCompletionResult()
}

data class LevelUpResult(
    val didLevelUp: Boolean,
    val newLevel: Int
)

data class ExerciseWithLockStatus(
    val exercise: Exercise,
    val isLocked: Boolean,
    val isCompleted: Boolean,
    val lockReason: String?
)

// ============================================
// Entity Classes
// ============================================

@Serializable
private data class UserProgressionEntity(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("placement_test_completed")
    val placementTestCompleted: Boolean = false,
    @kotlinx.serialization.SerialName("placement_test_score")
    val placementTestScore: Int? = null,
    @kotlinx.serialization.SerialName("placement_level")
    val placementLevel: Int = 1,
    @kotlinx.serialization.SerialName("solfege_level")
    val solfegeLevel: Int = 1,
    @kotlinx.serialization.SerialName("rhythm_level")
    val rhythmLevel: Int = 1,
    @kotlinx.serialization.SerialName("intervals_level")
    val intervalsLevel: Int = 1,
    @kotlinx.serialization.SerialName("melodic_level")
    val melodicLevel: Int = 1,
    @kotlinx.serialization.SerialName("theory_level")
    val theoryLevel: Int = 1,
    @kotlinx.serialization.SerialName("unlocked_nodes")
    val unlockedNodes: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("completed_nodes")
    val completedNodes: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("completed_exercises")
    val completedExercises: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("total_exercises_completed")
    val totalExercisesCompleted: Int = 0,
    @kotlinx.serialization.SerialName("average_accuracy")
    val averageAccuracy: Float = 0f
)

@Serializable
private data class NodeRequirementEntity(
    val id: String,
    @kotlinx.serialization.SerialName("node_id")
    val nodeId: String,
    @kotlinx.serialization.SerialName("requirement_type")
    val requirementType: String,
    @kotlinx.serialization.SerialName("required_node_id")
    val requiredNodeId: String? = null,
    @kotlinx.serialization.SerialName("required_level")
    val requiredLevel: Int? = null
)

@Serializable
private data class ExerciseEntity(
    val id: String,
    val title: String,
    val description: String? = null,
    val difficulty: Int = 1,
    @kotlinx.serialization.SerialName("xp_reward")
    val xpReward: Int = 10,
    @kotlinx.serialization.SerialName("coins_reward")
    val coinsReward: Int = 5,
    @kotlinx.serialization.SerialName("min_user_level")
    val minUserLevel: Int? = 1,
    @kotlinx.serialization.SerialName("is_premium")
    val isPremium: Boolean = false
) {
    fun toDomainModel(category: MusicCategory) = Exercise(
        id = id,
        categoryId = "",
        title = title,
        description = description,
        difficulty = difficulty,
        xpReward = xpReward,
        coinsReward = coinsReward,
        minUserLevel = minUserLevel,
        isPremium = isPremium,
        category = category
    )
}
