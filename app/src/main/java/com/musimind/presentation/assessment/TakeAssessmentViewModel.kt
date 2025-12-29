package com.musimind.presentation.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.AssessmentEntity
import com.musimind.data.repository.AssessmentQuestionEntity
import com.musimind.data.repository.AssessmentRepository
import com.musimind.music.audio.GameAudioManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/**
 * ViewModel for taking assessments
 * 
 * Manages the assessment flow:
 * - Loading assessment and questions
 * - Timer per question
 * - Answer tracking
 * - Score calculation
 * - Results submission
 */
@HiltViewModel
class TakeAssessmentViewModel @Inject constructor(
    private val repository: AssessmentRepository,
    private val audioManager: GameAudioManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(TakeAssessmentState())
    val state: StateFlow<TakeAssessmentState> = _state.asStateFlow()
    
    private var timerJob: Job? = null
    private var attemptId: String? = null
    private val answers = mutableMapOf<String, String>()
    
    /**
     * Load the assessment and its questions
     */
    fun loadAssessment(assessmentId: String) {
        viewModelScope.launch {
            _state.update { it.copy(phase = AssessmentPhase.LOADING) }
            
            try {
                // Load assessment
                val assessmentResult = repository.getPublicAssessments()
                val assessment = assessmentResult.getOrNull()?.find { it.id == assessmentId }
                    ?: run {
                        val myResult = repository.getMyAssessments()
                        myResult.getOrNull()?.find { it.id == assessmentId }
                    }
                
                if (assessment == null) {
                    _state.update { it.copy(error = "Avaliação não encontrada") }
                    return@launch
                }
                
                // Load questions
                val questionsResult = repository.getAssessmentQuestions(assessmentId)
                val questions = questionsResult.getOrNull() ?: emptyList()
                
                val totalPoints = questions.sumOf { it.points }
                
                _state.update {
                    it.copy(
                        phase = AssessmentPhase.INTRO,
                        assessment = assessment,
                        questions = questions,
                        totalPoints = totalPoints
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    /**
     * Start the assessment
     */
    fun startAssessment() {
        viewModelScope.launch {
            val assessmentId = _state.value.assessment?.id ?: return@launch
            
            // Create attempt record
            val attemptResult = repository.startAttempt(assessmentId)
            attemptId = attemptResult.getOrNull()?.id
            
            // Show first question
            showQuestion(0)
        }
    }
    
    private fun showQuestion(index: Int) {
        val questions = _state.value.questions
        if (index >= questions.size) {
            finishAssessment()
            return
        }
        
        val question = questions[index]
        val timeLimit = question.timeLimitSeconds
        
        _state.update {
            it.copy(
                phase = AssessmentPhase.QUESTION,
                currentQuestionIndex = index,
                currentQuestion = question,
                timeRemaining = timeLimit,
                maxTime = timeLimit,
                selectedAnswer = null,
                showFeedback = false,
                isCorrect = null
            )
        }
        
        startTimer()
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val maxTime = _state.value.maxTime
            for (time in maxTime downTo 0) {
                _state.update { it.copy(timeRemaining = time) }
                
                if (time == 0) {
                    // Time's up - auto-select wrong answer
                    if (_state.value.selectedAnswer == null) {
                        selectAnswer(-1) // No answer selected
                    }
                    break
                }
                
                delay(1000)
            }
        }
    }
    
    /**
     * Select an answer for current question
     */
    fun selectAnswer(answerIndex: Int) {
        if (_state.value.selectedAnswer != null) return
        
        timerJob?.cancel()
        
        val question = _state.value.currentQuestion ?: return
        val correctAnswer = question.correctAnswer
        
        val isCorrect = answerIndex.toString() == correctAnswer || 
                        answerIndex == correctAnswer.toIntOrNull()
        
        // Track answer
        answers[question.id] = answerIndex.toString()
        
        // Update score and correct count
        val earnedPoints = if (isCorrect) question.points else 0
        
        _state.update {
            it.copy(
                selectedAnswer = answerIndex,
                showFeedback = true,
                isCorrect = isCorrect,
                score = it.score + earnedPoints,
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount
            )
        }
        
        // Wait and move to next question
        viewModelScope.launch {
            delay(2000) // Show feedback for 2 seconds
            
            val nextIndex = _state.value.currentQuestionIndex + 1
            showQuestion(nextIndex)
        }
    }
    
    /**
     * Play audio for questions with audio
     */
    fun playAudio() {
        val question = _state.value.currentQuestion ?: return
        
        viewModelScope.launch {
            when (question.questionType) {
                "interval_recognition" -> {
                    // Play interval (example notes)
                    audioManager.playNote("C4", 800, 0.85f)
                    delay(1000)
                    audioManager.playNote("E4", 800, 0.85f)
                }
                "chord_recognition" -> {
                    // Play chord (example)
                    audioManager.playNote("C4", 1500, 0.7f)
                    audioManager.playNote("E4", 1500, 0.7f)
                    audioManager.playNote("G4", 1500, 0.7f)
                }
                else -> {
                    // Play single note
                    audioManager.playNote("C4", 1000, 0.85f)
                }
            }
        }
    }
    
    private fun finishAssessment() {
        timerJob?.cancel()
        
        val score = _state.value.score
        val totalPoints = _state.value.totalPoints
        val percentage = if (totalPoints > 0) (score.toFloat() / totalPoints * 100) else 0f
        val passingScore = _state.value.assessment?.passingScore ?: 60
        val passed = percentage >= passingScore
        
        _state.update {
            it.copy(
                phase = AssessmentPhase.RESULT,
                percentage = percentage,
                passed = passed
            )
        }
        
        // Save attempt to database
        attemptId?.let { id ->
            viewModelScope.launch {
                try {
                    val answersJson = buildJsonObject {
                        answers.forEach { (qId, answer) ->
                            put(qId, answer)
                        }
                    }
                    
                    repository.completeAttempt(
                        attemptId = id,
                        score = score,
                        totalPoints = totalPoints,
                        answers = answersJson
                    )
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

// State
data class TakeAssessmentState(
    val phase: AssessmentPhase = AssessmentPhase.LOADING,
    val error: String? = null,
    val assessment: AssessmentEntity? = null,
    val questions: List<AssessmentQuestionEntity> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val currentQuestion: AssessmentQuestionEntity? = null,
    val timeRemaining: Int = 15,
    val maxTime: Int = 15,
    val selectedAnswer: Int? = null,
    val showFeedback: Boolean = false,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val totalPoints: Int = 0,
    val correctCount: Int = 0,
    val percentage: Float = 0f,
    val passed: Boolean = false
)
