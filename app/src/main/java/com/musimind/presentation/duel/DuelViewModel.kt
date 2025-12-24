package com.musimind.presentation.duel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.DuelRepository
import com.musimind.domain.model.Duel
import com.musimind.domain.model.DuelQuestion
import com.musimind.domain.model.DuelStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DuelViewModel @Inject constructor(
    private val repository: DuelRepository,
    private val auth: Auth
) : ViewModel() {
    
    private val _state = MutableStateFlow(DuelState())
    val state: StateFlow<DuelState> = _state.asStateFlow()
    
    private var timerJob: Job? = null
    private var answerStartTime: Long = 0
    
    fun loadDuel(duelId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, duelId = duelId) }
            
            repository.observeDuel(duelId).collect { duel ->
                if (duel == null) {
                    _state.update { it.copy(isLoading = false, error = "Duelo não encontrado") }
                    return@collect
                }
                
                val currentUserId = auth.currentSessionOrNull()?.user?.id
                val isChallenger = duel.challengerId == currentUserId
                val currentAnswers = if (isChallenger) duel.challengerAnswers else duel.opponentAnswers
                val questionIndex = currentAnswers.size
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        duel = duel,
                        isChallenger = isChallenger,
                        questionIndex = questionIndex,
                        totalQuestions = duel.questions.size,
                        currentQuestion = duel.questions.getOrNull(questionIndex),
                        hasAnswered = false,
                        selectedAnswer = null,
                        showResult = false
                    )
                }
                
                // Start timer for current question
                if (duel.status == DuelStatus.IN_PROGRESS || duel.status == DuelStatus.ACCEPTED) {
                    duel.questions.getOrNull(questionIndex)?.let { question ->
                        startTimer(question.timeLimit)
                    }
                }
            }
        }
    }
    
    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        answerStartTime = System.currentTimeMillis()
        
        _state.update { it.copy(timeRemaining = seconds) }
        
        timerJob = viewModelScope.launch {
            for (remaining in seconds downTo 0) {
                _state.update { it.copy(timeRemaining = remaining) }
                
                if (remaining == 0) {
                    // Time's up - auto submit with wrong answer
                    if (!_state.value.hasAnswered) {
                        submitAnswer(-1)
                    }
                    break
                }
                
                delay(1000)
            }
        }
    }
    
    fun selectAnswer(index: Int) {
        if (_state.value.hasAnswered) return
        
        _state.update { 
            it.copy(
                selectedAnswer = index,
                hasAnswered = true,
                showResult = true
            )
        }
        
        timerJob?.cancel()
        
        submitAnswer(index)
    }
    
    private fun submitAnswer(answerIndex: Int) {
        viewModelScope.launch {
            val state = _state.value
            val question = state.currentQuestion ?: return@launch
            val duelId = state.duelId
            val timeMs = System.currentTimeMillis() - answerStartTime
            
            try {
                repository.submitAnswer(
                    duelId = duelId,
                    questionId = question.id,
                    answerIndex = answerIndex,
                    timeMs = timeMs
                )
                
                // Show result for a moment before loading next question
                delay(1500)
                
                // The observeDuel flow will automatically update with next question
                
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

data class DuelState(
    val isLoading: Boolean = true,
    val duelId: String = "",
    val duel: Duel? = null,
    val isChallenger: Boolean = false,
    val questionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val currentQuestion: DuelQuestion? = null,
    val timeRemaining: Int = 0,
    val selectedAnswer: Int? = null,
    val hasAnswered: Boolean = false,
    val showResult: Boolean = false,
    val error: String? = null
)
