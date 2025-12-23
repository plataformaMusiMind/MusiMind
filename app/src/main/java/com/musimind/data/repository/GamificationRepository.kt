package com.musimind.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.musimind.domain.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for gamification data (stats, achievements, leaderboards)
 */
@Singleton
class GamificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUserId: String? get() = auth.currentUser?.uid
    
    // Collections
    private val usersCollection get() = firestore.collection("users")
    private val achievementsCollection get() = firestore.collection("user_achievements")
    private val dailyChallengesCollection get() = firestore.collection("daily_challenges")
    private val leaderboardCollection get() = firestore.collection("leaderboard")
    
    /**
     * Get user stats
     */
    suspend fun getUserStats(userId: String = currentUserId ?: ""): UserStats? {
        return try {
            val doc = usersCollection.document(userId).get().await()
            doc.toObject(UserStats::class.java)
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
        
        firestore.runTransaction { transaction ->
            val userRef = usersCollection.document(userId)
            val user = transaction.get(userRef).toObject(UserStats::class.java) ?: UserStats()
            
            val updatedStats = user.copy(
                totalXp = user.totalXp + xpEarned,
                exercisesCompleted = user.exercisesCompleted + 1,
                perfectScores = if (isPerfect) user.perfectScores + 1 else user.perfectScores,
                averageAccuracy = calculateNewAverage(
                    user.averageAccuracy, 
                    accuracy, 
                    user.exercisesCompleted
                ),
                solfegeExercises = if (category == MusicCategory.SOLFEGE) 
                    user.solfegeExercises + 1 else user.solfegeExercises,
                rhythmExercises = if (category == MusicCategory.RHYTHMIC_PERCEPTION) 
                    user.rhythmExercises + 1 else user.rhythmExercises,
                intervalExercises = if (category == MusicCategory.INTERVAL_PERCEPTION) 
                    user.intervalExercises + 1 else user.intervalExercises,
                level = LevelSystem.getLevelForXp(user.totalXp + xpEarned)
            )
            
            transaction.set(userRef, updatedStats, com.google.firebase.firestore.SetOptions.merge())
        }.await()
        
        // Check achievements
        checkAchievements()
    }
    
    private fun calculateNewAverage(
        currentAverage: Float, 
        newValue: Float, 
        previousCount: Int
    ): Float {
        return (currentAverage * previousCount + newValue) / (previousCount + 1)
    }
    
    /**
     * Update streak
     */
    suspend fun updateStreak(): Int {
        val userId = currentUserId ?: return 0
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        return firestore.runTransaction { transaction ->
            val userRef = usersCollection.document(userId)
            val snapshot = transaction.get(userRef)
            
            val lastPracticeDate = snapshot.getString("lastPracticeDate") ?: ""
            val currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0
            val longestStreak = snapshot.getLong("longestStreak")?.toInt() ?: 0
            
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.time) }
            
            val newStreak = when {
                lastPracticeDate == today -> currentStreak // Already practiced today
                lastPracticeDate == yesterday -> currentStreak + 1 // Consecutive day
                else -> 1 // Start new streak
            }
            
            transaction.update(userRef, mapOf(
                "lastPracticeDate" to today,
                "currentStreak" to newStreak,
                "longestStreak" to maxOf(longestStreak, newStreak)
            ))
            
            newStreak
        }.await()
    }
    
    /**
     * Get user achievements
     */
    fun getUserAchievements(): Flow<List<Achievement>> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = achievementsCollection
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Achievements.ALL.map { it.copy(isUnlocked = false) })
                    return@addSnapshotListener
                }
                
                val unlockedIds = snapshot?.get("unlockedIds") as? List<*> ?: emptyList<String>()
                val achievements = Achievements.ALL.map { achievement ->
                    achievement.copy(
                        isUnlocked = unlockedIds.contains(achievement.id)
                    )
                }
                trySend(achievements)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Check and unlock achievements based on current stats
     */
    private suspend fun checkAchievements() {
        val userId = currentUserId ?: return
        val stats = getUserStats(userId) ?: return
        
        val currentlyUnlocked = try {
            val doc = achievementsCollection.document(userId).get().await()
            doc.get("unlockedIds") as? List<*> ?: emptyList<String>()
        } catch (e: Exception) {
            emptyList<String>()
        }
        
        val newlyUnlocked = mutableListOf<String>()
        
        for (achievement in Achievements.ALL) {
            if (currentlyUnlocked.contains(achievement.id)) continue
            
            val shouldUnlock = when (achievement.requirement.type) {
                RequirementType.COMPLETE_EXERCISES -> 
                    stats.exercisesCompleted >= achievement.requirement.targetValue
                RequirementType.COMPLETE_LESSONS -> 
                    stats.lessonsCompleted >= achievement.requirement.targetValue
                RequirementType.STREAK_DAYS -> 
                    stats.currentStreak >= achievement.requirement.targetValue
                RequirementType.TOTAL_XP -> 
                    stats.totalXp >= achievement.requirement.targetValue
                RequirementType.PERFECT_SCORES -> 
                    stats.perfectScores >= achievement.requirement.targetValue
                RequirementType.ACCURACY_PERCENTAGE -> 
                    stats.averageAccuracy * 100 >= achievement.requirement.targetValue
                RequirementType.FRIENDS_COUNT -> 
                    stats.friendsCount >= achievement.requirement.targetValue
                RequirementType.WIN_DUELS -> 
                    stats.duelsWon >= achievement.requirement.targetValue
                else -> false
            }
            
            if (shouldUnlock) {
                newlyUnlocked.add(achievement.id)
            }
        }
        
        if (newlyUnlocked.isNotEmpty()) {
            val allUnlocked = currentlyUnlocked.map { it.toString() } + newlyUnlocked
            achievementsCollection.document(userId).set(
                mapOf(
                    "unlockedIds" to allUnlocked,
                    "lastUpdated" to System.currentTimeMillis()
                )
            ).await()
        }
    }
    
    /**
     * Get leaderboard
     */
    suspend fun getLeaderboard(type: LeaderboardType, limit: Int = 50): List<LeaderboardEntry> {
        val query = when (type) {
            LeaderboardType.WEEKLY -> leaderboardCollection
                .orderBy("weeklyXp", Query.Direction.DESCENDING)
            LeaderboardType.MONTHLY -> leaderboardCollection
                .orderBy("monthlyXp", Query.Direction.DESCENDING)
            LeaderboardType.ALL_TIME -> usersCollection
                .orderBy("totalXp", Query.Direction.DESCENDING)
            LeaderboardType.FRIENDS -> {
                // Get friends first, then filter
                usersCollection.orderBy("totalXp", Query.Direction.DESCENDING)
            }
        }.limit(limit.toLong())
        
        return try {
            val docs = query.get().await()
            docs.documents.mapIndexed { index, doc ->
                LeaderboardEntry(
                    rank = index + 1,
                    userId = doc.id,
                    displayName = doc.getString("displayName") ?: "Anônimo",
                    avatarUrl = doc.getString("avatarUrl"),
                    xp = doc.getLong("totalXp")?.toInt() ?: 0,
                    level = doc.getLong("level")?.toInt() ?: 1,
                    streakDays = doc.getLong("currentStreak")?.toInt() ?: 0,
                    isCurrentUser = doc.id == currentUserId
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
        val userId = currentUserId ?: return emptyList()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        return try {
            val doc = dailyChallengesCollection
                .document("$userId-$today")
                .get()
                .await()
            
            if (doc.exists()) {
                doc.toObject(DailyChallengesDoc::class.java)?.challenges ?: generateDailyChallenges()
            } else {
                val challenges = generateDailyChallenges()
                dailyChallengesCollection.document("$userId-$today").set(
                    DailyChallengesDoc(challenges)
                ).await()
                challenges
            }
        } catch (e: Exception) {
            generateDailyChallenges()
        }
    }
    
    private fun generateDailyChallenges(): List<DailyChallenge> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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

@kotlinx.serialization.Serializable
private data class DailyChallengesDoc(
    val challenges: List<DailyChallenge> = emptyList()
)
