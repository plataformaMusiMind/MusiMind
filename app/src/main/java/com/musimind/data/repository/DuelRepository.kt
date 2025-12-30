package com.musimind.data.repository

import com.musimind.domain.model.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for duels and multiplayer games
 * Uses Supabase Postgrest and Realtime
 */
@Singleton
class DuelRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val realtime: Realtime
) {
    private val currentUserId: String? get() = auth.currentSessionOrNull()?.user?.id
    
    /**
     * Create a new duel challenge
     */
    suspend fun createChallenge(
        opponentId: String,
        category: MusicCategory
    ): String {
        val userId = currentUserId ?: throw IllegalStateException("Not logged in")
        
        // Get current user name
        val user = postgrest.from("users")
            .select {
                filter { eq("auth_id", userId) }
            }
            .decodeSingleOrNull<User>()
        
        val userName = user?.fullName ?: "Anônimo"
        val challengeId = UUID.randomUUID().toString()
        
        val challenge = mapOf(
            "id" to challengeId,
            "from_user_id" to userId,
            "from_user_name" to userName,
            "to_user_id" to opponentId,
            "category" to category.name,
            "status" to ChallengeStatus.PENDING.name,
            "created_at" to System.currentTimeMillis()
        )
        
        postgrest.from("challenges").insert(challenge)
        
        return challengeId
    }
    
    /**
     * Accept a challenge and create the duel
     */
    suspend fun acceptChallenge(challengeId: String): String {
        val userId = currentUserId ?: throw IllegalStateException("Not logged in")
        
        val challenge = postgrest.from("challenges")
            .select {
                filter { eq("id", challengeId) }
            }
            .decodeSingleOrNull<DuelChallenge>() ?: throw Exception("Challenge not found")
        
        if (challenge.toUserId != userId) {
            throw Exception("This challenge is not for you")
        }
        
        // Get opponent name
        val user = postgrest.from("users")
            .select {
                filter { eq("auth_id", userId) }
            }
            .decodeSingleOrNull<User>()
        
        val userName = user?.fullName ?: "Anônimo"
        
        // Create the duel
        val duelId = UUID.randomUUID().toString()
        val questions = when (challenge.category) {
            MusicCategory.INTERVAL_PERCEPTION -> DuelQuestions.generateIntervalQuestions()
            MusicCategory.SOLFEGE -> DuelQuestions.generateNoteQuestions()
            else -> DuelQuestions.generateMixedQuestions()
        }
        
        val duel = Duel(
            id = duelId,
            challengerId = challenge.fromUserId,
            challengerName = challenge.fromUserName,
            opponentId = userId,
            opponentName = userName,
            category = challenge.category,
            questions = questions,
            status = DuelStatus.ACCEPTED
        )
        
        postgrest.from("duels").insert(duel)
        
        // Update challenge status
        postgrest.from("challenges").update(
            mapOf("status" to ChallengeStatus.ACCEPTED.name)
        ) {
            filter { eq("id", challengeId) }
        }
        
        return duelId
    }
    
    /**
     * Decline a challenge
     */
    suspend fun declineChallenge(challengeId: String) {
        postgrest.from("challenges").update(
            mapOf("status" to ChallengeStatus.DECLINED.name)
        ) {
            filter { eq("id", challengeId) }
        }
    }
    
    /**
     * Get pending challenges for current user
     */
    fun getPendingChallenges(): Flow<List<DuelChallenge>> = flow {
        val userId = currentUserId ?: run {
            emit(emptyList())
            return@flow
        }
        
        try {
            val challenges = postgrest.from("challenges")
                .select {
                    filter { 
                        eq("to_user_id", userId)
                        eq("status", ChallengeStatus.PENDING.name)
                    }
                }
                .decodeList<DuelChallenge>()
            
            emit(challenges)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    /**
     * Observe a duel in real-time using Supabase Realtime
     * Falls back to polling if Realtime connection fails
     */
    fun observeDuel(duelId: String): Flow<Duel?> = channelFlow {
        // Initial fetch
        var currentDuel = fetchDuel(duelId)
        send(currentDuel)
        
        try {
            // Create realtime channel
            val realtimeChannel = realtime.channel("duel_$duelId")
            
            // Subscribe to duel updates using correct v3.0 syntax
            val duelChanges = realtimeChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "duels"
                // Note: filter syntax for row-level is: filter("column", FilterOperator.EQ, value)
                // For now, we filter client-side since we're watching all changes to the duels table
            }
            
            // Start listening for changes
            launch {
                duelChanges.onEach { action ->
                    when (action) {
                        is PostgresAction.Update, is PostgresAction.Insert -> {
                            // Fetch updated duel and check if it's the one we're watching
                            val updatedDuel = fetchDuel(duelId)
                            if (updatedDuel != currentDuel) {
                                currentDuel = updatedDuel
                                send(updatedDuel)
                            }
                            
                            // Stop if duel is complete
                            if (currentDuel?.status == DuelStatus.COMPLETED) {
                                realtimeChannel.unsubscribe()
                            }
                        }
                        else -> { }
                    }
                }.launchIn(this)
            }
            
            // Subscribe to channel
            realtimeChannel.subscribe()
            
            // Keep flow alive
            awaitCancellation()
            
        } catch (e: Exception) {
            android.util.Log.w("DuelRepository", "Realtime failed, falling back to polling: ${e.message}")
            
            // Fallback to polling
            while (true) {
                kotlinx.coroutines.delay(2000)
                
                val updatedDuel = fetchDuel(duelId)
                if (updatedDuel != currentDuel) {
                    currentDuel = updatedDuel
                    send(updatedDuel)
                }
                
                if (currentDuel?.status == DuelStatus.COMPLETED) {
                    break
                }
            }
        }
    }.catch { e ->
        android.util.Log.e("DuelRepository", "Error observing duel: ${e.message}")
    }
    
    /**
     * Fetch duel from database
     */
    private suspend fun fetchDuel(duelId: String): Duel? {
        return try {
            postgrest.from("duels")
                .select {
                    filter { eq("id", duelId) }
                }
                .decodeSingleOrNull<Duel>()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Submit answer for current question
     */
    suspend fun submitAnswer(
        duelId: String,
        questionId: String,
        answerIndex: Int,
        timeMs: Long
    ) {
        val userId = currentUserId ?: return
        
        val duel = postgrest.from("duels")
            .select {
                filter { eq("id", duelId) }
            }
            .decodeSingleOrNull<Duel>() ?: return
        
        val question = duel.questions.find { it.id == questionId } ?: return
        val isCorrect = answerIndex == question.correctAnswerIndex
        
        val answer = DuelAnswer(
            questionId = questionId,
            answerIndex = answerIndex,
            isCorrect = isCorrect,
            timeMs = timeMs
        )
        
        // Determine if user is challenger or opponent
        val isChallenger = userId == duel.challengerId
        val currentAnswers = if (isChallenger) duel.challengerAnswers else duel.opponentAnswers
        val newAnswers = currentAnswers + answer
        
        val updates = if (isChallenger) {
            mapOf(
                "challenger_answers" to newAnswers,
                "challenger_score" to if (isCorrect) duel.challengerScore + 1 else duel.challengerScore
            )
        } else {
            mapOf(
                "opponent_answers" to newAnswers,
                "opponent_score" to if (isCorrect) duel.opponentScore + 1 else duel.opponentScore
            )
        }
        
        postgrest.from("duels").update(updates) {
            filter { eq("id", duelId) }
        }
        
        // Check if both players answered all questions
        checkDuelCompletion(duelId)
    }
    
    private suspend fun checkDuelCompletion(duelId: String) {
        val duel = postgrest.from("duels")
            .select {
                filter { eq("id", duelId) }
            }
            .decodeSingleOrNull<Duel>() ?: return
        
        val totalQuestions = duel.questions.size
        val challengerComplete = duel.challengerAnswers.size >= totalQuestions
        val opponentComplete = duel.opponentAnswers.size >= totalQuestions
        
        if (challengerComplete && opponentComplete) {
            // Determine winner
            val winnerId = when {
                duel.challengerScore > duel.opponentScore -> duel.challengerId
                duel.opponentScore > duel.challengerScore -> duel.opponentId
                else -> null // Tie
            }
            
            val updates = mapOf(
                "status" to DuelStatus.COMPLETED.name,
                "winner_id" to winnerId,
                "ended_at" to System.currentTimeMillis()
            )
            
            postgrest.from("duels").update(updates) {
                filter { eq("id", duelId) }
            }
            
            // Award XP to winner
            winnerId?.let { id ->
                val user = postgrest.from("users")
                    .select {
                        filter { eq("auth_id", id) }
                    }
                    .decodeSingleOrNull<User>()
                
                if (user != null) {
                    postgrest.from("users").update(
                        mapOf("xp" to user.xp + duel.xpReward)
                    ) {
                        filter { eq("auth_id", id) }
                    }
                }
            }
        }
    }
    
    /**
     * Get user's duel history
     */
    suspend fun getDuelHistory(limit: Int = 20): List<Duel> {
        val userId = currentUserId ?: return emptyList()
        
        return try {
            // Get duels where user is challenger or opponent
            val asChallenger = postgrest.from("duels")
                .select {
                    filter { eq("challenger_id", userId) }
                    limit(limit.toLong())
                }
                .decodeList<Duel>()
            
            val asOpponent = postgrest.from("duels")
                .select {
                    filter { eq("opponent_id", userId) }
                    limit(limit.toLong())
                }
                .decodeList<Duel>()
            
            (asChallenger + asOpponent)
                .filter { it.status == DuelStatus.COMPLETED }
                .sortedByDescending { it.endedAt }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get active duels for current user
     */
    fun getActiveDuels(): Flow<List<Duel>> = flow {
        val userId = currentUserId ?: run {
            emit(emptyList())
            return@flow
        }
        
        try {
            val asChallenger = postgrest.from("duels")
                .select {
                    filter { eq("challenger_id", userId) }
                }
                .decodeList<Duel>()
            
            val asOpponent = postgrest.from("duels")
                .select {
                    filter { eq("opponent_id", userId) }
                }
                .decodeList<Duel>()
            
            val activeDuels = (asChallenger + asOpponent)
                .filter { it.status in listOf(DuelStatus.ACCEPTED, DuelStatus.IN_PROGRESS) }
            
            emit(activeDuels)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}
