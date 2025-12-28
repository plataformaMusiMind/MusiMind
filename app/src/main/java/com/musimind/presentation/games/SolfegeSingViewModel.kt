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
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * ViewModel para o jogo Solfege Sing (Cante o Solfejo)
 * 
 * O app toca uma melodia e o jogador canta. O microfone detecta
 * a afinação e o ritmo. Aproveita funcionalidade de solfejo existente.
 */
@HiltViewModel
class SolfegeSingViewModel @Inject constructor(
    private val gamesRepository: GamesRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(SolfegeSingState())
    val state: StateFlow<SolfegeSingState> = _state.asStateFlow()
    
    private var sessionId: String? = null
    
    // Melodias simples para cantar (graus da escala)
    private val easyMelodies = listOf(
        listOf(1, 2, 3, 2, 1),           // Dó Ré Mi Ré Dó
        listOf(1, 3, 5, 3, 1),           // Dó Mi Sol Mi Dó
        listOf(5, 4, 3, 2, 1),           // Sol Fá Mi Ré Dó
        listOf(1, 2, 3, 4, 5, 4, 3, 2, 1) // Escala subindo e descendo
    )
    
    private val mediumMelodies = listOf(
        listOf(1, 3, 5, 8, 5, 3, 1),
        listOf(1, 5, 3, 8, 5, 1),
        listOf(8, 7, 6, 5, 4, 3, 2, 1)
    )
    
    private val hardMelodies = listOf(
        listOf(1, 2, 3, 4, 5, 6, 7, 8, 7, 6, 5, 4, 3, 2, 1),
        listOf(1, 3, 2, 4, 3, 5, 4, 6, 5, 7, 6, 8)
    )
    
    private val degreeToSolfege = mapOf(
        1 to "Dó", 2 to "Ré", 3 to "Mi", 4 to "Fá",
        5 to "Sol", 6 to "Lá", 7 to "Si", 8 to "Dó"
    )
    
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "solfege_sing" }
            
            if (gameType != null) {
                val progress = gamesRepository.getGameProgress(userId, gameType.id).getOrNull()
                _state.update { 
                    it.copy(
                        isLoading = false, gameType = gameType,
                        levels = progress?.levels ?: emptyList(),
                        highScores = progress?.highScores ?: emptyMap(),
                        totalStars = progress?.totalStars ?: 0
                    ) 
                }
            }
        }
    }
    
    fun startLevel(userId: String, level: GameLevel) {
        viewModelScope.launch {
            val config = level.config
            val difficulty = config?.get("difficulty")?.jsonPrimitive?.int ?: 1
            val rounds = config?.get("rounds")?.jsonPrimitive?.int ?: 5
            
            val melodies = when (difficulty) {
                1 -> easyMelodies
                2 -> mediumMelodies
                else -> hardMelodies
            }
            
            _state.update { 
                it.copy(
                    currentLevel = level, gamePhase = SolfegeSingPhase.PLAYING,
                    difficulty = difficulty, totalRounds = rounds, currentRound = 0,
                    availableMelodies = melodies, score = 0, combo = 0, maxCombo = 0,
                    correctCount = 0, wrongCount = 0
                )
            }
            
            val session = gamesRepository.startGameSession(
                userId = userId, gameTypeId = _state.value.gameType?.id ?: "", gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
            
            nextMelody()
        }
    }
    
    private fun nextMelody() {
        val currentState = _state.value
        
        if (currentState.currentRound >= currentState.totalRounds) {
            endGame()
            return
        }
        
        val melody = currentState.availableMelodies.random()
        val solfegeNotation = melody.map { degreeToSolfege[it] ?: "?" }
        
        _state.update { 
            it.copy(
                currentRound = it.currentRound + 1,
                currentMelody = melody,
                currentSolfege = solfegeNotation,
                melodyPhase = MelodySingPhase.LISTENING,
                detectedNotes = emptyList(),
                accuracy = 0f
            )
        }
        
        playMelody(melody)
    }
    
    private fun playMelody(melody: List<Int>) {
        viewModelScope.launch {
            _state.update { it.copy(isPlayingMelody = true, currentNoteIndex = -1) }
            
            delay(500)
            
            melody.forEachIndexed { index, _ ->
                _state.update { it.copy(currentNoteIndex = index) }
                // TODO: Tocar a nota via audio engine
                delay(600)
            }
            
            _state.update { 
                it.copy(
                    isPlayingMelody = false, 
                    currentNoteIndex = -1,
                    melodyPhase = MelodySingPhase.SINGING
                )
            }
        }
    }
    
    fun startRecording() {
        viewModelScope.launch {
            _state.update { it.copy(isRecording = true, detectedNotes = emptyList()) }
            
            // TODO: Iniciar gravação via microfone e detectar pitch
            // A detecção real usaria TarsosDSP ou similar
            
            // Simular gravação por tempo baseado no tamanho da melodia
            val recordingTime = _state.value.currentMelody.size * 600L + 1000L
            delay(recordingTime)
            
            stopRecording()
        }
    }
    
    fun stopRecording() {
        val currentState = _state.value
        if (!currentState.isRecording) return
        
        // Simular detecção de notas (em produção, viria do microfone)
        // Por enquanto, vamos simular um resultado baseado em "random" ponderado
        val simulatedAccuracy = (70..100).random() / 100f
        val simulatedDetected = currentState.currentMelody.mapIndexed { index, degree ->
            if (kotlin.random.Random.nextFloat() < simulatedAccuracy) degree else (1..8).random()
        }
        
        _state.update { 
            it.copy(
                isRecording = false,
                detectedNotes = simulatedDetected,
                melodyPhase = MelodySingPhase.RESULT
            )
        }
        
        evaluatePerformance(simulatedDetected)
    }
    
    private fun evaluatePerformance(detected: List<Int>) {
        val expected = _state.value.currentMelody
        
        var correct = 0
        expected.forEachIndexed { index, expectedNote ->
            val detectedNote = detected.getOrNull(index)
            if (detectedNote == expectedNote) correct++
        }
        
        val accuracy = if (expected.isNotEmpty()) correct.toFloat() / expected.size else 0f
        val points = (accuracy * 300).toInt()
        
        val medal = when {
            accuracy >= 0.95f -> "🥇 Ouro"
            accuracy >= 0.85f -> "🥈 Prata"
            accuracy >= 0.75f -> "🥉 Bronze"
            else -> null
        }
        
        val isSuccess = accuracy >= 0.75f
        
        viewModelScope.launch {
            val newCombo = if (isSuccess) _state.value.combo + 1 else 0
            
            _state.update { 
                it.copy(
                    accuracy = accuracy, medal = medal,
                    score = it.score + points, combo = newCombo,
                    maxCombo = maxOf(it.maxCombo, newCombo),
                    correctCount = it.correctCount + if (isSuccess) 1 else 0,
                    wrongCount = it.wrongCount + if (!isSuccess) 1 else 0
                )
            }
            
            delay(2500)
            nextMelody()
        }
    }
    
    fun replayMelody() {
        playMelody(_state.value.currentMelody)
    }
    
    private fun endGame() {
        viewModelScope.launch {
            val currentState = _state.value
            
            val result = sessionId?.let { sid ->
                gamesRepository.completeGameSession(
                    sessionId = sid, score = currentState.score,
                    correctAnswers = currentState.correctCount, wrongAnswers = currentState.wrongCount,
                    maxCombo = currentState.maxCombo
                ).getOrNull()
            }
            
            _state.update { 
                it.copy(
                    gamePhase = SolfegeSingPhase.RESULT,
                    stars = result?.stars ?: 2, xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    fun pauseGame() { _state.update { it.copy(gamePhase = SolfegeSingPhase.PAUSED) } }
    fun resumeGame() { _state.update { it.copy(gamePhase = SolfegeSingPhase.PLAYING) } }
    fun backToLevelSelect() { _state.update { it.copy(gamePhase = SolfegeSingPhase.LEVEL_SELECT) } }
    fun restartLevel(userId: String) { _state.value.currentLevel?.let { startLevel(userId, it) } }
}

data class SolfegeSingState(
    val isLoading: Boolean = false, val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(), val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0, val gamePhase: SolfegeSingPhase = SolfegeSingPhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null, val difficulty: Int = 1,
    val totalRounds: Int = 5, val currentRound: Int = 0,
    val availableMelodies: List<List<Int>> = emptyList(),
    val currentMelody: List<Int> = emptyList(), val currentSolfege: List<String> = emptyList(),
    val melodyPhase: MelodySingPhase = MelodySingPhase.LISTENING,
    val isPlayingMelody: Boolean = false, val currentNoteIndex: Int = -1,
    val isRecording: Boolean = false, val detectedNotes: List<Int> = emptyList(),
    val accuracy: Float = 0f, val medal: String? = null,
    val score: Int = 0, val combo: Int = 0, val maxCombo: Int = 0,
    val correctCount: Int = 0, val wrongCount: Int = 0,
    val stars: Int = 0, val xpEarned: Int = 0, val coinsEarned: Int = 0
)

enum class SolfegeSingPhase { LEVEL_SELECT, PLAYING, PAUSED, RESULT }
enum class MelodySingPhase { LISTENING, SINGING, RESULT }
