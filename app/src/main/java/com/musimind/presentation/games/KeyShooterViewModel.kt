package com.musimind.presentation.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.domain.games.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import kotlin.random.Random

/**
 * ViewModel para o jogo Key Signature Shooter (Tiro às Armaduras)
 * 
 * Armaduras de clave aparecem como "alvos" e o jogador deve identificar
 * a tonalidade correta antes do tempo acabar.
 */
@HiltViewModel
class KeyShooterViewModel @Inject constructor(
    private val gamesRepository: GamesRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(KeyShooterState())
    val state: StateFlow<KeyShooterState> = _state.asStateFlow()
    
    private var sessionId: String? = null
    private var gameLoop: Job? = null
    
    // Armaduras de clave
    private val keySignatures = listOf(
        KeySignatureInfo("C", 0, false, "Dó Maior / Lá menor"),
        KeySignatureInfo("G", 1, true, "Sol Maior / Mi menor"),
        KeySignatureInfo("D", 2, true, "Ré Maior / Si menor"),
        KeySignatureInfo("A", 3, true, "Lá Maior / Fá# menor"),
        KeySignatureInfo("E", 4, true, "Mi Maior / Dó# menor"),
        KeySignatureInfo("B", 5, true, "Si Maior / Sol# menor"),
        KeySignatureInfo("F#", 6, true, "Fá# Maior / Ré# menor"),
        KeySignatureInfo("F", 1, false, "Fá Maior / Ré menor"),
        KeySignatureInfo("Bb", 2, false, "Sib Maior / Sol menor"),
        KeySignatureInfo("Eb", 3, false, "Mib Maior / Dó menor"),
        KeySignatureInfo("Ab", 4, false, "Láb Maior / Fá menor"),
        KeySignatureInfo("Db", 5, false, "Réb Maior / Sib menor"),
        KeySignatureInfo("Gb", 6, false, "Solb Maior / Mib menor")
    )
    
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "key_shooter" }
            
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
            val rounds = config?.get("rounds")?.jsonPrimitive?.int ?: 10
            val timePerTarget = config?.get("time_per_target")?.jsonPrimitive?.int ?: 5
            val maxAccidentals = config?.get("max_accidentals")?.jsonPrimitive?.int ?: 3
            
            val availableKeys = keySignatures.filter { it.accidentalCount <= maxAccidentals }
            
            _state.update { 
                it.copy(
                    currentLevel = level, gamePhase = KeyShooterPhase.PLAYING,
                    totalRounds = rounds, currentRound = 0, timePerTarget = timePerTarget,
                    availableKeys = availableKeys, score = 0, combo = 0, maxCombo = 0,
                    correctCount = 0, wrongCount = 0
                )
            }
            
            val session = gamesRepository.startGameSession(
                userId = userId, gameTypeId = _state.value.gameType?.id ?: "", gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
            
            nextTarget()
        }
    }
    
    private fun nextTarget() {
        val currentState = _state.value
        
        if (currentState.currentRound >= currentState.totalRounds) {
            endGame()
            return
        }
        
        // Criar novo alvo
        val target = currentState.availableKeys.random()
        val position = TargetPosition(Random.nextFloat() * 0.6f + 0.2f, 0f)
        
        // Criar opções de resposta
        val wrongOptions = currentState.availableKeys
            .filter { it.key != target.key }
            .shuffled()
            .take(3)
            .map { it.key }
        val options = (listOf(target.key) + wrongOptions).shuffled()
        
        _state.update { 
            it.copy(
                currentRound = it.currentRound + 1,
                currentTarget = target,
                targetPosition = position,
                answerOptions = options,
                timeRemaining = it.timePerTarget.toFloat(),
                roundResult = null
            )
        }
        
        startTargetTimer()
    }
    
    private fun startTargetTimer() {
        gameLoop?.cancel()
        gameLoop = viewModelScope.launch {
            while (_state.value.timeRemaining > 0 && _state.value.roundResult == null) {
                delay(50)
                _state.update { 
                    it.copy(
                        timeRemaining = (it.timeRemaining - 0.05f).coerceAtLeast(0f),
                        targetPosition = it.targetPosition.copy(y = it.targetPosition.y + 0.005f)
                    )
                }
            }
            
            if (_state.value.roundResult == null) {
                // Tempo esgotado
                handleWrong()
            }
        }
    }
    
    fun shoot(keyName: String) {
        val currentState = _state.value
        val isCorrect = keyName == currentState.currentTarget?.key
        
        gameLoop?.cancel()
        
        if (isCorrect) {
            handleCorrect()
        } else {
            handleWrong()
        }
    }
    
    private fun handleCorrect() {
        val timeBonus = (_state.value.timeRemaining * 20).toInt()
        val newCombo = _state.value.combo + 1
        val points = 100 + timeBonus + (newCombo * 15)
        
        _state.update { 
            it.copy(
                score = it.score + points, combo = newCombo,
                maxCombo = maxOf(it.maxCombo, newCombo), correctCount = it.correctCount + 1,
                roundResult = RoundResult.SUCCESS
            )
        }
        
        viewModelScope.launch {
            delay(800)
            nextTarget()
        }
    }
    
    private fun handleWrong() {
        _state.update { 
            it.copy(combo = 0, wrongCount = it.wrongCount + 1, roundResult = RoundResult.FAIL)
        }
        
        viewModelScope.launch {
            delay(1000)
            nextTarget()
        }
    }
    
    private fun endGame() {
        gameLoop?.cancel()
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
                    gamePhase = KeyShooterPhase.RESULT,
                    stars = result?.stars ?: 2, xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    fun getKeyDescription(key: String): String {
        return keySignatures.find { it.key == key }?.description ?: key
    }
    
    fun pauseGame() { gameLoop?.cancel(); _state.update { it.copy(gamePhase = KeyShooterPhase.PAUSED) } }
    fun resumeGame() { _state.update { it.copy(gamePhase = KeyShooterPhase.PLAYING) }; startTargetTimer() }
    fun backToLevelSelect() { gameLoop?.cancel(); _state.update { it.copy(gamePhase = KeyShooterPhase.LEVEL_SELECT) } }
    fun restartLevel(userId: String) { _state.value.currentLevel?.let { startLevel(userId, it) } }
    
    override fun onCleared() { gameLoop?.cancel(); super.onCleared() }
}

data class KeySignatureInfo(val key: String, val accidentalCount: Int, val isSharps: Boolean, val description: String)
data class TargetPosition(val x: Float, val y: Float)

data class KeyShooterState(
    val isLoading: Boolean = false, val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(), val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0, val gamePhase: KeyShooterPhase = KeyShooterPhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null, val totalRounds: Int = 10, val currentRound: Int = 0,
    val timePerTarget: Int = 5, val availableKeys: List<KeySignatureInfo> = emptyList(),
    val currentTarget: KeySignatureInfo? = null, val targetPosition: TargetPosition = TargetPosition(0.5f, 0f),
    val answerOptions: List<String> = emptyList(), val timeRemaining: Float = 5f,
    val roundResult: RoundResult? = null,
    val score: Int = 0, val combo: Int = 0, val maxCombo: Int = 0,
    val correctCount: Int = 0, val wrongCount: Int = 0,
    val stars: Int = 0, val xpEarned: Int = 0, val coinsEarned: Int = 0
)

enum class KeyShooterPhase { LEVEL_SELECT, PLAYING, PAUSED, RESULT }
