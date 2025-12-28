package com.musimind.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.domain.gamification.ProgressionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * ViewModel for Placement Test
 * 
 * Implements adaptive testing algorithm:
 * 1. Start with medium difficulty questions
 * 2. Increase difficulty on correct answers
 * 3. Decrease difficulty on wrong answers
 * 4. Converge to user's actual level
 */

@HiltViewModel
class PlacementTestViewModel @Inject constructor(
    private val postgrest: Postgrest,
    private val progressionManager: ProgressionManager
) : ViewModel() {
    
    companion object {
        const val QUESTIONS_PER_TEST = 12
        const val TIME_PER_QUESTION = 30 // seconds
        const val INITIAL_DIFFICULTY = 3
    }
    
    private val _state = MutableStateFlow<PlacementTestState>(PlacementTestState.Intro)
    val state: StateFlow<PlacementTestState> = _state.asStateFlow()
    
    private var allQuestions: List<PlacementQuestionEntity> = emptyList()
    private var currentQuestionIndex = 0
    private var currentDifficulty = INITIAL_DIFFICULTY
    private var answers = mutableListOf<AnswerResult>()
    private var timerJob: Job? = null
    private var currentTimeRemaining = TIME_PER_QUESTION
    
    init {
        loadQuestions()
    }
    
    /**
     * Load questions from Supabase
     */
    private fun loadQuestions() {
        viewModelScope.launch {
            try {
                allQuestions = postgrest.from("placement_test_questions")
                    .select {
                        filter { eq("is_active", true) }
                    }
                    .decodeList<PlacementQuestionEntity>()
            } catch (e: Exception) {
                // Use fallback questions if fetch fails
                allQuestions = getFallbackQuestions()
            }
        }
    }
    
    /**
     * Start the placement test
     */
    fun startTest() {
        currentQuestionIndex = 0
        currentDifficulty = INITIAL_DIFFICULTY
        answers.clear()
        
        showNextQuestion()
    }
    
    /**
     * Show next question based on adaptive algorithm
     */
    private fun showNextQuestion() {
        if (currentQuestionIndex >= QUESTIONS_PER_TEST) {
            calculateResult()
            return
        }
        
        // Find question at current difficulty level
        val question = selectQuestion()
        
        if (question != null) {
            currentTimeRemaining = TIME_PER_QUESTION
            startTimer()
            
            _state.value = PlacementTestState.Question(
                question = question.toDomain(),
                currentIndex = currentQuestionIndex,
                totalQuestions = QUESTIONS_PER_TEST,
                timeRemaining = currentTimeRemaining
            )
        } else {
            // No more questions at this difficulty, adjust difficulty
            if (currentDifficulty > 1) {
                currentDifficulty--
                showNextQuestion()
            } else {
                // End test early
                calculateResult()
            }
        }
    }
    
    /**
     * Select a question at the current difficulty level
     */
    private fun selectQuestion(): PlacementQuestionEntity? {
        // Try exact difficulty
        var candidates = allQuestions.filter { 
            it.difficulty == currentDifficulty &&
            !answers.any { a -> a.questionId == it.id }
        }
        
        // If no exact match, try nearby difficulties
        if (candidates.isEmpty()) {
            candidates = allQuestions.filter { 
                it.difficulty in (currentDifficulty - 1)..(currentDifficulty + 1) &&
                !answers.any { a -> a.questionId == it.id }
            }
        }
        
        return candidates.randomOrNull()
    }
    
    /**
     * Submit answer and update adaptive algorithm
     */
    fun submitAnswer(answer: String) {
        timerJob?.cancel()
        
        val currentState = _state.value
        if (currentState !is PlacementTestState.Question) return
        
        val isCorrect = answer == currentState.question.correctAnswer
        
        answers.add(AnswerResult(
            questionId = currentState.question.id,
            answer = answer,
            isCorrect = isCorrect,
            difficulty = currentState.question.difficulty,
            timeTaken = TIME_PER_QUESTION - currentTimeRemaining
        ))
        
        // Adaptive algorithm: adjust difficulty
        if (isCorrect) {
            // Correct answer: increase difficulty (max 10)
            currentDifficulty = (currentDifficulty + 1).coerceAtMost(10)
        } else {
            // Wrong answer: decrease difficulty (min 1)
            currentDifficulty = (currentDifficulty - 1).coerceAtLeast(1)
        }
        
        currentQuestionIndex++
        
        viewModelScope.launch {
            delay(1500) // Show correct/incorrect feedback
            showNextQuestion()
        }
    }
    
    /**
     * Start countdown timer for question
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (currentTimeRemaining > 0) {
                delay(1000)
                currentTimeRemaining--
                
                val currentState = _state.value
                if (currentState is PlacementTestState.Question) {
                    _state.value = currentState.copy(timeRemaining = currentTimeRemaining)
                }
            }
            
            // Time's up - submit empty answer
            submitAnswer("")
        }
    }
    
    /**
     * Calculate final result using Item Response Theory (simplified)
     */
    private fun calculateResult() {
        _state.value = PlacementTestState.Calculating
        
        viewModelScope.launch {
            delay(2500) // Show calculating animation
            
            val totalScore = answers.sumOf { answer ->
                if (answer.isCorrect) {
                    answer.difficulty * 10 // Weight by difficulty
                } else {
                    0
                }
            }
            
            val correctCount = answers.count { it.isCorrect }
            val maxPossibleScore = QUESTIONS_PER_TEST * 10 * 10 // 12 questions @ max difficulty 10
            
            // Calculate level (1-10) based on performance
            val level = calculateLevel(correctCount, answers)
            
            // Save result to Supabase
            progressionManager.setPlacementTestResult(totalScore, level)
            
            _state.value = PlacementTestState.Result(
                level = level,
                score = totalScore,
                correctAnswers = correctCount,
                totalQuestions = answers.size
            )
        }
    }
    
    /**
     * Calculate user level based on answers
     */
    private fun calculateLevel(correctCount: Int, answers: List<AnswerResult>): Int {
        // If very few correct, beginner level
        if (correctCount <= 2) return 1
        
        // Calculate weighted average of correctly answered difficulty levels
        val correctAnswers = answers.filter { it.isCorrect }
        if (correctAnswers.isEmpty()) return 1
        
        val averageCorrectDifficulty = correctAnswers.map { it.difficulty }.average()
        
        // Factor in accuracy
        val accuracy = correctCount.toFloat() / answers.size
        
        // Combined formula
        val estimatedLevel = (averageCorrectDifficulty * accuracy).toInt()
        
        return estimatedLevel.coerceIn(1, 10)
    }
    
    /**
     * Fallback questions if Supabase fetch fails
     */
    private fun getFallbackQuestions(): List<PlacementQuestionEntity> {
        return listOf(
            // Level 1-2 (Basic)
            PlacementQuestionEntity(
                id = "1", questionType = "NOTE_NAME", difficulty = 1,
                questionText = "Qual é o nome da primeira nota da escala de Dó maior?",
                correctAnswer = "Dó", answerOptions = listOf("Dó", "Ré", "Mi", "Fá"),
                points = 10, category = "theory"
            ),
            PlacementQuestionEntity(
                id = "2", questionType = "NOTE_NAME", difficulty = 1,
                questionText = "Qual nota vem depois de Mi?",
                correctAnswer = "Fá", answerOptions = listOf("Ré", "Sol", "Fá", "Si"),
                points = 10, category = "theory"
            ),
            PlacementQuestionEntity(
                id = "3", questionType = "INTERVAL_ID", difficulty = 2,
                questionText = "Dó para Ré é um intervalo de...?",
                correctAnswer = "Segunda", answerOptions = listOf("Uníssono", "Segunda", "Terça", "Quarta"),
                points = 15, category = "intervals"
            ),
            
            // Level 3-4 (Intermediate)
            PlacementQuestionEntity(
                id = "4", questionType = "INTERVAL_ID", difficulty = 3,
                questionText = "Dó para Mi é um intervalo de...?",
                correctAnswer = "Terça Maior", answerOptions = listOf("Segunda", "Terça Menor", "Terça Maior", "Quarta"),
                points = 20, category = "intervals"
            ),
            PlacementQuestionEntity(
                id = "5", questionType = "SCALE_ID", difficulty = 4,
                questionText = "A escala com todas as teclas brancas do piano é...?",
                correctAnswer = "Dó Maior", answerOptions = listOf("Dó Maior", "Lá menor", "Sol Maior", "Fá Maior"),
                points = 25, category = "theory"
            ),
            PlacementQuestionEntity(
                id = "6", questionType = "RHYTHM_TAP", difficulty = 4,
                questionText = "Quantas colcheias cabem em uma semínima?",
                correctAnswer = "2", answerOptions = listOf("1", "2", "4", "8"),
                points = 25, category = "rhythm"
            ),
            
            // Level 5-6 (Upper Intermediate)
            PlacementQuestionEntity(
                id = "7", questionType = "CHORD_ID", difficulty = 5,
                questionText = "Um acorde maior é formado por...?",
                correctAnswer = "Tônica + 3ª Maior + 5ª Justa",
                answerOptions = listOf(
                    "Tônica + 3ª Menor + 5ª Justa",
                    "Tônica + 3ª Maior + 5ª Justa",
                    "Tônica + 4ª Justa + 5ª Justa",
                    "Tônica + 3ª Maior + 6ª Maior"
                ),
                points = 30, category = "theory"
            ),
            PlacementQuestionEntity(
                id = "8", questionType = "INTERVAL_ID", difficulty = 6,
                questionText = "Dó para Si é um intervalo de...?",
                correctAnswer = "Sétima Maior", answerOptions = listOf("Sexta", "Sétima Menor", "Sétima Maior", "Oitava"),
                points = 35, category = "intervals"
            ),
            
            // Level 7-8 (Advanced)
            PlacementQuestionEntity(
                id = "9", questionType = "SCALE_ID", difficulty = 7,
                questionText = "A escala menor harmônica difere da natural em qual grau?",
                correctAnswer = "7º grau", answerOptions = listOf("3º grau", "5º grau", "6º grau", "7º grau"),
                points = 40, category = "theory"
            ),
            PlacementQuestionEntity(
                id = "10", questionType = "CHORD_ID", difficulty = 8,
                questionText = "Qual é o acorde de dominante de Sol Maior?",
                correctAnswer = "Ré Maior", answerOptions = listOf("Dó Maior", "Mi menor", "Ré Maior", "Lá menor"),
                points = 45, category = "theory"
            ),
            
            // Level 9-10 (Expert)
            PlacementQuestionEntity(
                id = "11", questionType = "THEORY_KNOWLEDGE", difficulty = 9,
                questionText = "Qual modo começa no 2º grau da escala maior?",
                correctAnswer = "Dórico", answerOptions = listOf("Frígio", "Dórico", "Lídio", "Mixolídio"),
                points = 50, category = "theory"
            ),
            PlacementQuestionEntity(
                id = "12", questionType = "CHORD_ID", difficulty = 10,
                questionText = "Um acorde semidiminuto contém qual tipo de sétima?",
                correctAnswer = "Sétima menor", answerOptions = listOf("Sétima maior", "Sétima menor", "Sétima diminuta", "Sem sétima"),
                points = 55, category = "theory"
            )
        )
    }
}

// ============================================
// Data Classes
// ============================================

data class AnswerResult(
    val questionId: String,
    val answer: String,
    val isCorrect: Boolean,
    val difficulty: Int,
    val timeTaken: Int
)

@Serializable
data class PlacementQuestionEntity(
    val id: String,
    @kotlinx.serialization.SerialName("question_type")
    val questionType: String,
    val difficulty: Int,
    @kotlinx.serialization.SerialName("question_text")
    val questionText: String,
    @kotlinx.serialization.SerialName("correct_answer")
    val correctAnswer: String,
    @kotlinx.serialization.SerialName("answer_options")
    val answerOptions: List<String>,
    val points: Int,
    val category: String
) {
    fun toDomain() = PlacementQuestion(
        id = id,
        text = questionText,
        options = answerOptions,
        correctAnswer = correctAnswer,
        difficulty = difficulty,
        category = category,
        hasAudio = questionType in listOf("INTERVAL_ID", "CHORD_ID", "MELODY_REPEAT"),
        audioUrl = null
    )
}
