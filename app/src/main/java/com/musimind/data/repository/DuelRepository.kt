package com.musimind.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.musimind.domain.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for duels and multiplayer games
 * Uses Realtime Database for low-latency updates
 */
@Singleton
class DuelRepository @Inject constructor(
    private val realtimeDb: FirebaseDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUserId: String? get() = auth.currentUser?.uid
    
    // Realtime Database references
    private val duelsRef get() = realtimeDb.getReference("duels")
    private val challengesRef get() = realtimeDb.getReference("challenges")
    private val quizRoomsRef get() = realtimeDb.getReference("quiz_rooms")
    
    /**
     * Create a new duel challenge
     */
    suspend fun createChallenge(
        opponentId: String,
        category: MusicCategory
    ): String {
        val userId = currentUserId ?: throw IllegalStateException("Not logged in")
        
        // Get current user name
        val userDoc = firestore.collection("users").document(userId).get().await()
        val userName = userDoc.getString("displayName") ?: "Anônimo"
        
        val challengeId = challengesRef.push().key ?: throw Exception("Could not create challenge")
        
        val challenge = DuelChallenge(
            id = challengeId,
            fromUserId = userId,
            fromUserName = userName,
            toUserId = opponentId,
            category = category
        )
        
        challengesRef.child(challengeId).setValue(challenge).await()
        
        // Also add to opponent's pending challenges
        realtimeDb.getReference("user_challenges/$opponentId/$challengeId")
            .setValue(true).await()
        
        return challengeId
    }
    
    /**
     * Accept a challenge and create the duel
     */
    suspend fun acceptChallenge(challengeId: String): String {
        val userId = currentUserId ?: throw IllegalStateException("Not logged in")
        
        val challengeSnapshot = challengesRef.child(challengeId).get().await()
        val challenge = challengeSnapshot.getValue(DuelChallenge::class.java)
            ?: throw Exception("Challenge not found")
        
        if (challenge.toUserId != userId) {
            throw Exception("This challenge is not for you")
        }
        
        // Get opponent name
        val userDoc = firestore.collection("users").document(userId).get().await()
        val userName = userDoc.getString("displayName") ?: "Anônimo"
        
        // Create the duel
        val duelId = duelsRef.push().key ?: throw Exception("Could not create duel")
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
        
        duelsRef.child(duelId).setValue(duel).await()
        
        // Update challenge status
        challengesRef.child(challengeId).child("status")
            .setValue(ChallengeStatus.ACCEPTED.name).await()
        
        return duelId
    }
    
    /**
     * Decline a challenge
     */
    suspend fun declineChallenge(challengeId: String) {
        challengesRef.child(challengeId).child("status")
            .setValue(ChallengeStatus.DECLINED.name).await()
    }
    
    /**
     * Get pending challenges for current user
     */
    fun getPendingChallenges(): Flow<List<DuelChallenge>> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val query = challengesRef
            .orderByChild("toUserId")
            .equalTo(userId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val challenges = snapshot.children.mapNotNull {
                    it.getValue(DuelChallenge::class.java)
                }.filter { it.status == ChallengeStatus.PENDING }
                trySend(challenges)
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
    
    /**
     * Observe a duel in real-time
     */
    fun observeDuel(duelId: String): Flow<Duel?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val duel = snapshot.getValue(Duel::class.java)
                trySend(duel)
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        
        duelsRef.child(duelId).addValueEventListener(listener)
        awaitClose { duelsRef.child(duelId).removeEventListener(listener) }
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
        val duelSnapshot = duelsRef.child(duelId).get().await()
        val duel = duelSnapshot.getValue(Duel::class.java) ?: return
        
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
        val answersPath = if (isChallenger) "challengerAnswers" else "opponentAnswers"
        val scorePath = if (isChallenger) "challengerScore" else "opponentScore"
        
        // Add answer
        val currentAnswers = if (isChallenger) duel.challengerAnswers else duel.opponentAnswers
        val newAnswers = currentAnswers + answer
        duelsRef.child(duelId).child(answersPath).setValue(newAnswers).await()
        
        // Update score if correct
        if (isCorrect) {
            val currentScore = if (isChallenger) duel.challengerScore else duel.opponentScore
            duelsRef.child(duelId).child(scorePath).setValue(currentScore + 1).await()
        }
        
        // Check if both players answered all questions
        checkDuelCompletion(duelId)
    }
    
    private suspend fun checkDuelCompletion(duelId: String) {
        val duelSnapshot = duelsRef.child(duelId).get().await()
        val duel = duelSnapshot.getValue(Duel::class.java) ?: return
        
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
                "winnerId" to winnerId,
                "endedAt" to System.currentTimeMillis()
            )
            
            duelsRef.child(duelId).updateChildren(updates).await()
            
            // Award XP to winner
            winnerId?.let { id ->
                firestore.runTransaction { transaction ->
                    val userRef = firestore.collection("users").document(id)
                    val user = transaction.get(userRef)
                    val currentXp = user.getLong("totalXp")?.toInt() ?: 0
                    transaction.update(userRef, "totalXp", currentXp + duel.xpReward)
                    transaction.update(userRef, "duelsWon", FieldValue.increment(1))
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
            val asChallenger = duelsRef
                .orderByChild("challengerId")
                .equalTo(userId)
                .limitToLast(limit)
                .get()
                .await()
                .children
                .mapNotNull { it.getValue(Duel::class.java) }
            
            val asOpponent = duelsRef
                .orderByChild("opponentId")
                .equalTo(userId)
                .limitToLast(limit)
                .get()
                .await()
                .children
                .mapNotNull { it.getValue(Duel::class.java) }
            
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
    fun getActiveDuels(): Flow<List<Duel>> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val duels = snapshot.children.mapNotNull { 
                    it.getValue(Duel::class.java) 
                }.filter { duel ->
                    (duel.challengerId == userId || duel.opponentId == userId) &&
                    duel.status in listOf(DuelStatus.ACCEPTED, DuelStatus.IN_PROGRESS)
                }
                trySend(duels)
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        
        duelsRef.addValueEventListener(listener)
        awaitClose { duelsRef.removeEventListener(listener) }
    }
}
