package com.musimind.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.AssessmentEntity
import com.musimind.data.repository.AssessmentQuestionEntity
import com.musimind.data.repository.AssessmentRepository
import com.musimind.data.repository.TeacherRepository
import com.musimind.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/**
 * ViewModel for creating and managing assessments
 * 
 * Allows teachers/schools to:
 * - Create assessments with various question types
 * - Add questions with audio, score rendering, etc.
 * - Assign assessments to classes
 * - View student results
 */
@HiltViewModel
class AssessmentViewModel @Inject constructor(
    private val assessmentRepository: AssessmentRepository,
    private val teacherRepository: TeacherRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(AssessmentState())
    val state: StateFlow<AssessmentState> = _state.asStateFlow()
    
    init {
        loadAssessments()
        loadClasses()
    }
    
    /**
     * Load teacher's assessments
     */
    fun loadAssessments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val result = assessmentRepository.getMyAssessments()
            
            result.onSuccess { assessments ->
                _state.update { it.copy(isLoading = false, assessments = assessments) }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
    
    /**
     * Load teacher's classes for assignment
     */
    private fun loadClasses() {
        viewModelScope.launch {
            try {
                val classes = teacherRepository.getTeacherClasses()
                _state.update { it.copy(classes = classes) }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Create a new assessment
     */
    fun createAssessment(
        title: String,
        description: String,
        category: String,
        isPublic: Boolean = false,
        passingScore: Int = 60
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val result = assessmentRepository.createAssessment(
                title = title,
                description = description,
                category = category,
                isPublic = isPublic,
                passingScore = passingScore
            )
            
            result.onSuccess { assessment ->
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        currentAssessment = assessment,
                        assessments = s.assessments + assessment,
                        message = "Avaliação criada com sucesso!"
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
    
    /**
     * Add a question to current assessment
     */
    fun addQuestion(question: AssessmentQuestionInput) {
        val assessmentId = _state.value.currentAssessment?.id ?: return
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val scoreRender = question.scoreData?.let { score ->
                buildJsonObject {
                    put("clef", score.clef)
                    put("key_signature", score.keySignature)
                    put("time_signature", score.timeSignature)
                }
            }
            
            val result = assessmentRepository.addQuestion(
                assessmentId = assessmentId,
                questionType = question.type.name.lowercase(),
                questionText = question.text,
                options = question.options,
                correctAnswer = question.correctAnswer,
                audioUrl = question.audioUrl,
                scoreRender = scoreRender,
                timeLimitSeconds = question.timeLimit,
                points = question.points,
                orderIndex = _state.value.currentQuestions.size
            )
            
            result.onSuccess { questionEntity ->
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        currentQuestions = s.currentQuestions + questionEntity,
                        message = "Pergunta adicionada!"
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
    
    /**
     * Load questions for an assessment
     */
    fun loadQuestions(assessmentId: String) {
        viewModelScope.launch {
            val result = assessmentRepository.getAssessmentQuestions(assessmentId)
            
            result.onSuccess { questions ->
                _state.update { it.copy(currentQuestions = questions) }
            }
        }
    }
    
    /**
     * Select an assessment to edit
     */
    fun selectAssessment(assessment: AssessmentEntity) {
        _state.update { it.copy(currentAssessment = assessment) }
        loadQuestions(assessment.id)
    }
    
    /**
     * Assign assessment to a class
     */
    fun assignToClass(classId: String, dueDate: String? = null) {
        val assessmentId = _state.value.currentAssessment?.id ?: return
        
        viewModelScope.launch {
            val result = assessmentRepository.assignToClass(assessmentId, classId, dueDate)
            
            result.onSuccess {
                _state.update { it.copy(message = "Avaliação atribuída à turma!") }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message) }
            }
        }
    }
    
    /**
     * Delete assessment
     */
    fun deleteAssessment(assessmentId: String) {
        viewModelScope.launch {
            val result = assessmentRepository.deleteAssessment(assessmentId)
            
            result.onSuccess {
                _state.update { s ->
                    s.copy(
                        assessments = s.assessments.filter { it.id != assessmentId },
                        currentAssessment = null,
                        message = "Avaliação excluída!"
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message) }
            }
        }
    }
    
    /**
     * Generate questions automatically
     */
    fun generateQuestions(
        count: Int,
        types: List<QuestionType>,
        difficulty: Int
    ) {
        val generatedQuestions = QuestionGenerator.generateQuizQuestions(
            count = count,
            categories = types,
            difficulty = difficulty
        )
        
        // Convert RichQuestion to AssessmentQuestionInput
        val inputs = generatedQuestions.map { q ->
            AssessmentQuestionInput(
                type = q.type,
                text = q.text,
                options = q.options,
                correctAnswer = q.correctAnswerIndex.toString(),
                audioData = q.audioData,
                scoreData = q.scoreData,
                timeLimit = q.timeLimit,
                points = q.points
            )
        }
        
        // Add each question
        inputs.forEach { input ->
            addQuestion(input)
        }
    }
    
    fun clearMessage() {
        _state.update { it.copy(message = null, error = null) }
    }
    
    fun clearCurrentAssessment() {
        _state.update { it.copy(currentAssessment = null, currentQuestions = emptyList()) }
    }
}

// State
data class AssessmentState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val assessments: List<AssessmentEntity> = emptyList(),
    val currentAssessment: AssessmentEntity? = null,
    val currentQuestions: List<AssessmentQuestionEntity> = emptyList(),
    val classes: List<StudentClass> = emptyList()
)

// Input model for creating questions
data class AssessmentQuestionInput(
    val type: QuestionType,
    val text: String,
    val options: List<QuestionOption> = emptyList(),
    val correctAnswer: String,
    val audioData: AudioQuestionData? = null,
    val scoreData: ScoreQuestionData? = null,
    val audioUrl: String? = null,
    val timeLimit: Int = 15,
    val points: Int = 10
)
