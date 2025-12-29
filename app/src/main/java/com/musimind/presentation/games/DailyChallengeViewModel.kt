package com.musimind.presentation.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.domain.games.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.random.Random

/**
 * ViewModel para o Desafio Diário
 * 
 * Um desafio especial que muda todos os dias, combinando elementos
 * de diferentes tipos de jogos. Oferece recompensas especiais.
 */
@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val gamesRepository: GamesRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(DailyChallengeState())
    val state: StateFlow<DailyChallengeState> = _state.asStateFlow()
    
    private var sessionId: String? = null
    
    // Tipos de desafios do dia
    private val challengeTypes = listOf(
        ChallengeType.INTERVAL_BLITZ,
        ChallengeType.CHORD_RUSH,
        ChallengeType.RHYTHM_MARATHON,
        ChallengeType.MELODY_CHAIN,
        ChallengeType.MIXED_CHALLENGE
    )
    
    fun loadDailyChallenge(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Determinar desafio do dia baseado na data
            val today = LocalDate.now()
            val seed = today.toEpochDay().toInt()
            val random = Random(seed)
            
            val challengeType = challengeTypes[random.nextInt(challengeTypes.size)]
            val difficulty = (random.nextInt(3) + 1) // 1-3
            
            val (title, description, icon) = when (challengeType) {
                ChallengeType.INTERVAL_BLITZ -> Triple(
                    "Blitz de Intervalos",
                    "Identifique ${10 + difficulty * 5} intervalos em ${30 + difficulty * 10} segundos!",
                    "⚡"
                )
                ChallengeType.CHORD_RUSH -> Triple(
                    "Corrida de Acordes",
                    "Monte ${5 + difficulty * 2} acordes corretamente!",
                    "🏃"
                )
                ChallengeType.RHYTHM_MARATHON -> Triple(
                    "Maratona Rítmica",
                    "Reproduza ${8 + difficulty * 4} padrões rítmicos!",
                    "🥁"
                )
                ChallengeType.MELODY_CHAIN -> Triple(
                    "Corrente Melódica",
                    "Memorize e reproduza uma melodia de ${4 + difficulty * 2} notas!",
                    "🎵"
                )
                ChallengeType.MIXED_CHALLENGE -> Triple(
                    "Desafio Misto",
                    "Complete ${12 + difficulty * 3} questões variadas!",
                    "🎯"
                )
            }
            
            // Verificar se já completou hoje buscando do servidor
            val alreadyCompleted = checkIfCompletedToday(userId, today.toString())
            
            _state.update { 
                it.copy(
                    isLoading = false,
                    challengeType = challengeType,
                    title = title,
                    description = description,
                    icon = icon,
                    difficulty = difficulty,
                    dateFormatted = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    xpReward = 100 + (difficulty * 50),
                    coinsReward = 50 + (difficulty * 25),
                    alreadyCompleted = alreadyCompleted
                )
            }
        }
    }
    
    private suspend fun checkIfCompletedToday(userId: String, date: String): Boolean {
        return try {
            val result = gamesRepository.checkDailyChallengeCompleted(userId, date)
            result.isSuccess && result.getOrNull() == true
        } catch (e: Exception) {
            false
        }
    }
    
    fun startChallenge(userId: String) {
        viewModelScope.launch {
            val state = _state.value
            
            _state.update { 
                it.copy(
                    gamePhase = DailyChallengePhase.PLAYING,
                    currentQuestion = 0,
                    totalQuestions = 10 + state.difficulty * 5,
                    score = 0,
                    combo = 0,
                    maxCombo = 0,
                    correctCount = 0,
                    wrongCount = 0,
                    timeRemaining = 60 + state.difficulty * 30 // Tempo total
                )
            }
            
            val session = gamesRepository.startGameSession(
                userId = userId,
                gameTypeId = "daily_challenge", // ID especial
                gameLevelId = null
            ).getOrNull()
            sessionId = session?.id
            
            nextQuestion()
            startTimer()
        }
    }
    
    private fun startTimer() {
        viewModelScope.launch {
            while (_state.value.timeRemaining > 0 && _state.value.gamePhase == DailyChallengePhase.PLAYING) {
                delay(1000)
                _state.update { it.copy(timeRemaining = it.timeRemaining - 1) }
            }
            
            if (_state.value.gamePhase == DailyChallengePhase.PLAYING) {
                endChallenge()
            }
        }
    }
    
    private fun nextQuestion() {
        val currentState = _state.value
        
        if (currentState.currentQuestion >= currentState.totalQuestions) {
            endChallenge()
            return
        }
        
        // Gerar pergunta baseada no tipo de desafio
        val question = generateQuestion(currentState.challengeType, currentState.difficulty)
        
        _state.update { 
            it.copy(
                currentQuestion = it.currentQuestion + 1,
                currentQuestionData = question,
                roundResult = null
            )
        }
    }
    
    private fun generateQuestion(type: ChallengeType, difficulty: Int): ChallengeQuestion {
        return when (type) {
            ChallengeType.INTERVAL_BLITZ -> {
                val intervals = listOf("2m", "2M", "3m", "3M", "4J", "5J", "6m", "6M", "7m", "7M")
                val correct = intervals.random()
                val options = (listOf(correct) + intervals.filter { it != correct }.shuffled().take(3)).shuffled()
                ChallengeQuestion("Qual intervalo?", "🎵 → 🎵", correct, options, QuestionCategory.INTERVAL)
            }
            ChallengeType.CHORD_RUSH -> {
                val chords = listOf("C", "Cm", "G", "Am", "F", "Dm", "E", "Em")
                val correct = chords.random()
                val options = (listOf(correct) + chords.filter { it != correct }.shuffled().take(3)).shuffled()
                ChallengeQuestion("Monte o acorde: $correct", "🎹", correct, options, QuestionCategory.CHORD)
            }
            ChallengeType.RHYTHM_MARATHON -> {
                val patterns = listOf("♩ ♩ ♩ ♩", "♩ ♩ ♫ ♩", "♫ ♫ ♩ ♩", "♩ ♫ ♩ ♫")
                val correct = patterns.random()
                val options = patterns.shuffled()
                ChallengeQuestion("Qual padrão você ouviu?", "🥁", correct, options, QuestionCategory.RHYTHM)
            }
            ChallengeType.MELODY_CHAIN, ChallengeType.MIXED_CHALLENGE -> {
                // Pergunta mista
                val categories = listOf(QuestionCategory.INTERVAL, QuestionCategory.CHORD, QuestionCategory.RHYTHM, QuestionCategory.THEORY)
                val category = categories.random()
                
                when (category) {
                    QuestionCategory.THEORY -> {
                        val questions = listOf(
                            Triple("Quantos semitons em uma 5ª justa?", "7", listOf("5", "6", "7", "8")),
                            Triple("Qual a relativa menor de Dó Maior?", "Lá menor", listOf("Mi menor", "Ré menor", "Lá menor", "Si menor")),
                            Triple("Qual nota é o 5º grau de Sol Maior?", "Ré", listOf("Dó", "Ré", "Mi", "Si"))
                        )
                        val (q, a, opts) = questions.random()
                        ChallengeQuestion(q, "📚", a, opts.shuffled(), QuestionCategory.THEORY)
                    }
                    else -> generateQuestion(ChallengeType.INTERVAL_BLITZ, difficulty)
                }
            }
        }
    }
    
    fun submitAnswer(answer: String) {
        val currentState = _state.value
        val question = currentState.currentQuestionData ?: return
        val isCorrect = answer == question.correctAnswer
        
        if (isCorrect) {
            val newCombo = currentState.combo + 1
            val timeBonus = (currentState.timeRemaining / 10)
            val points = 50 + (newCombo * 10) + timeBonus
            
            _state.update { 
                it.copy(
                    score = it.score + points, combo = newCombo,
                    maxCombo = maxOf(it.maxCombo, newCombo), correctCount = it.correctCount + 1,
                    roundResult = RoundResult.SUCCESS
                )
            }
        } else {
            _state.update { 
                it.copy(combo = 0, wrongCount = it.wrongCount + 1, roundResult = RoundResult.FAIL)
            }
        }
        
        viewModelScope.launch {
            delay(800)
            nextQuestion()
        }
    }
    
    private fun endChallenge() {
        viewModelScope.launch {
            val currentState = _state.value
            val success = currentState.correctCount >= currentState.totalQuestions * 0.6
            
            val result = sessionId?.let { sid ->
                gamesRepository.completeGameSession(
                    sessionId = sid, score = currentState.score,
                    correctAnswers = currentState.correctCount, wrongAnswers = currentState.wrongCount,
                    maxCombo = currentState.maxCombo
                ).getOrNull()
            }
            
            _state.update { 
                it.copy(
                    gamePhase = DailyChallengePhase.RESULT,
                    success = success,
                    finalXp = if (success) it.xpReward else it.xpReward / 2,
                    finalCoins = if (success) it.coinsReward else it.coinsReward / 2
                )
            }
        }
    }
    
    fun backToMenu() {
        _state.update { it.copy(gamePhase = DailyChallengePhase.MENU) }
    }
}

enum class ChallengeType {
    INTERVAL_BLITZ, CHORD_RUSH, RHYTHM_MARATHON, MELODY_CHAIN, MIXED_CHALLENGE
}

enum class QuestionCategory {
    INTERVAL, CHORD, RHYTHM, MELODY, THEORY
}

data class ChallengeQuestion(
    val question: String,
    val visual: String,
    val correctAnswer: String,
    val options: List<String>,
    val category: QuestionCategory
)

data class DailyChallengeState(
    val isLoading: Boolean = false,
    val gamePhase: DailyChallengePhase = DailyChallengePhase.MENU,
    
    // Info do desafio
    val challengeType: ChallengeType = ChallengeType.MIXED_CHALLENGE,
    val title: String = "",
    val description: String = "",
    val icon: String = "📅",
    val difficulty: Int = 1,
    val dateFormatted: String = "",
    val xpReward: Int = 100,
    val coinsReward: Int = 50,
    val alreadyCompleted: Boolean = false,
    
    // Gameplay
    val currentQuestion: Int = 0,
    val totalQuestions: Int = 10,
    val currentQuestionData: ChallengeQuestion? = null,
    val timeRemaining: Int = 90,
    val roundResult: RoundResult? = null,
    
    // Score
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    
    // Result
    val success: Boolean = false,
    val finalXp: Int = 0,
    val finalCoins: Int = 0
)

enum class DailyChallengePhase { MENU, PLAYING, RESULT }
