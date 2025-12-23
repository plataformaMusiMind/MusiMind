package com.musimind.domain.model

import kotlinx.serialization.Serializable

/**
 * Duel game between two players
 */
@Serializable
data class Duel(
    val id: String = "",
    val challengerId: String = "",
    val challengerName: String = "",
    val opponentId: String = "",
    val opponentName: String = "",
    val status: DuelStatus = DuelStatus.PENDING,
    val category: MusicCategory = MusicCategory.SOLFEGE,
    val questions: List<DuelQuestion> = emptyList(),
    val challengerScore: Int = 0,
    val opponentScore: Int = 0,
    val challengerAnswers: List<DuelAnswer> = emptyList(),
    val opponentAnswers: List<DuelAnswer> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val winnerId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val xpReward: Int = 50
) {
    val isComplete: Boolean get() = status == DuelStatus.COMPLETED
    val isTie: Boolean get() = isComplete && challengerScore == opponentScore
    
    fun getWinnerName(): String? = when (winnerId) {
        challengerId -> challengerName
        opponentId -> opponentName
        else -> null
    }
}

@Serializable
enum class DuelStatus {
    PENDING,      // Waiting for opponent to accept
    ACCEPTED,     // Opponent accepted, waiting to start
    IN_PROGRESS,  // Game ongoing
    COMPLETED,    // Game finished
    DECLINED,     // Opponent declined
    EXPIRED,      // No response in time
    CANCELLED     // Challenger cancelled
}

/**
 * Question in a duel
 */
@Serializable
data class DuelQuestion(
    val id: String,
    val type: DuelQuestionType,
    val prompt: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val audioData: String? = null, // Base64 encoded audio or URL
    val imageData: String? = null, // Base64 encoded image or URL
    val timeLimit: Int = 15 // seconds
)

@Serializable
enum class DuelQuestionType {
    NOTE_IDENTIFICATION,     // What note is this?
    INTERVAL_IDENTIFICATION, // What interval is this?
    CHORD_IDENTIFICATION,    // What chord is this?
    RHYTHM_IDENTIFICATION,   // What rhythm pattern is this?
    KEY_SIGNATURE,          // What key is this?
    THEORY_KNOWLEDGE        // Music theory question
}

/**
 * Player answer in duel
 */
@Serializable
data class DuelAnswer(
    val questionId: String,
    val answerIndex: Int,
    val isCorrect: Boolean,
    val timeMs: Long // Time taken to answer
)

/**
 * Duel invitation/challenge
 */
@Serializable
data class DuelChallenge(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val fromUserAvatar: String? = null,
    val toUserId: String,
    val category: MusicCategory,
    val status: ChallengeStatus = ChallengeStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24 hours
)

@Serializable
enum class ChallengeStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED
}

/**
 * Quiz room for multiplayer quiz
 */
@Serializable
data class QuizRoom(
    val id: String,
    val name: String,
    val hostId: String,
    val category: MusicCategory,
    val maxPlayers: Int = 4,
    val players: List<QuizPlayer> = emptyList(),
    val status: QuizRoomStatus = QuizRoomStatus.WAITING,
    val currentQuestionIndex: Int = 0,
    val questions: List<DuelQuestion> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val playerCount: Int get() = players.size
    val isFull: Boolean get() = playerCount >= maxPlayers
    val canStart: Boolean get() = playerCount >= 2 && status == QuizRoomStatus.WAITING
}

@Serializable
enum class QuizRoomStatus {
    WAITING,
    STARTING,
    IN_PROGRESS,
    COMPLETED
}

@Serializable
data class QuizPlayer(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val score: Int = 0,
    val isReady: Boolean = false,
    val answers: List<DuelAnswer> = emptyList()
)

/**
 * Predefined duel questions generator
 */
object DuelQuestions {
    
    fun generateNoteQuestions(count: Int = 5): List<DuelQuestion> {
        val notes = listOf("Dó", "Ré", "Mi", "Fá", "Sol", "Lá", "Si")
        val englishNotes = listOf("C", "D", "E", "F", "G", "A", "B")
        
        return (0 until count).map { i ->
            val correctIndex = (0..6).random()
            val options = notes.shuffled().take(4).toMutableList()
            if (!options.contains(notes[correctIndex])) {
                options[0] = notes[correctIndex]
            }
            options.shuffle()
            
            DuelQuestion(
                id = "note_$i",
                type = DuelQuestionType.NOTE_IDENTIFICATION,
                prompt = "Qual é esta nota? ${englishNotes[correctIndex]}4",
                options = options,
                correctAnswerIndex = options.indexOf(notes[correctIndex]),
                timeLimit = 10
            )
        }
    }
    
    fun generateIntervalQuestions(count: Int = 5): List<DuelQuestion> {
        val intervals = listOf(
            "2ª menor", "2ª maior", "3ª menor", "3ª maior",
            "4ª justa", "5ª justa", "6ª menor", "6ª maior",
            "7ª menor", "7ª maior", "8ª justa"
        )
        
        return (0 until count).map { i ->
            val correctIndex = (0 until intervals.size).random()
            val options = intervals.shuffled().take(4).toMutableList()
            if (!options.contains(intervals[correctIndex])) {
                options[0] = intervals[correctIndex]
            }
            options.shuffle()
            
            DuelQuestion(
                id = "interval_$i",
                type = DuelQuestionType.INTERVAL_IDENTIFICATION,
                prompt = "Identifique o intervalo",
                options = options,
                correctAnswerIndex = options.indexOf(intervals[correctIndex]),
                timeLimit = 12
            )
        }
    }
    
    fun generateTheoryQuestions(count: Int = 5): List<DuelQuestion> {
        val questions = listOf(
            Triple("Quantos semitons há em um tom?", listOf("1", "2", "3", "4"), 1),
            Triple("Qual nota é a sensível de Dó Maior?", listOf("Sol", "Fá", "Si", "Ré"), 2),
            Triple("Qual é a relativa menor de Sol Maior?", listOf("Lá menor", "Mi menor", "Ré menor", "Fá menor"), 1),
            Triple("Quantos bemóis tem Fá Maior?", listOf("0", "1", "2", "3"), 1),
            Triple("Qual acorde é I-IV-V-I?", listOf("Cadência plagal", "Cadência perfeita", "Cadência deceptiva", "Cadência suspensiva"), 1),
            Triple("O que significa 'Adagio'?", listOf("Rápido", "Devagar", "Moderado", "Muito rápido"), 1),
            Triple("Qual figura vale 2 tempos em 4/4?", listOf("Semínima", "Mínima", "Semibreve", "Colcheia"), 1),
            Triple("Quantos sustenidos tem Ré Maior?", listOf("1", "2", "3", "4"), 1)
        )
        
        return questions.shuffled().take(count).mapIndexed { i, (prompt, options, correct) ->
            DuelQuestion(
                id = "theory_$i",
                type = DuelQuestionType.THEORY_KNOWLEDGE,
                prompt = prompt,
                options = options,
                correctAnswerIndex = correct,
                timeLimit = 15
            )
        }
    }
    
    fun generateMixedQuestions(count: Int = 5): List<DuelQuestion> {
        val noteQ = generateNoteQuestions(count / 2)
        val theoryQ = generateTheoryQuestions(count - count / 2)
        return (noteQ + theoryQ).shuffled()
    }
}
