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

/**
 * ViewModel para o jogo Tempo Run (Corrida do Andamento)
 * 
 * O jogador deve identificar o andamento (BPM) de músicas que tocam,
 * movendo um personagem para a faixa correta. Estilo endless runner.
 */
@HiltViewModel
class TempoRunViewModel @Inject constructor(
    private val gamesRepository: GamesRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(TempoRunState())
    val state: StateFlow<TempoRunState> = _state.asStateFlow()
    
    private var sessionId: String? = null
    private var gameLoop: Job? = null
    
    // Andamentos e suas faixas
    private val tempoRanges = listOf(
        TempoRange("Largo", 40, 60, "🐢"),
        TempoRange("Adagio", 66, 76, "🚶"),
        TempoRange("Andante", 76, 108, "🏃"),
        TempoRange("Moderato", 108, 120, "🚴"),
        TempoRange("Allegro", 120, 156, "🏎️"),
        TempoRange("Presto", 168, 200, "🚀")
    )
    
    // Músicas/exemplos de cada andamento
    private val tempoExamples = mapOf(
        "Largo" to listOf("Marcha Fúnebre", "Adagio de Albinoni"),
        "Adagio" to listOf("Moonlight Sonata", "Air de Bach"),
        "Andante" to listOf("Eine Kleine Nachtmusik", "Canon de Pachelbel"),
        "Moderato" to listOf("Für Elise", "The Entertainer"),
        "Allegro" to listOf("Alla Turca", "Rondo Alla Turca"),
        "Presto" to listOf("Flight of the Bumblebee", "Presto de Vivaldi")
    )
    
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "tempo_run" }
            
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
            val rounds = config?.get("rounds")?.jsonPrimitive?.int ?: 8
            val speed = config?.get("speed")?.jsonPrimitive?.int ?: 1
            
            // Determinar quais faixas de andamento usar baseado na dificuldade
            val activeTempos = when (speed) {
                1 -> tempoRanges.take(3) // Lento, Médio, Rápido básico
                2 -> tempoRanges.take(4)
                else -> tempoRanges
            }
            
            _state.update { 
                it.copy(
                    currentLevel = level, gamePhase = TempoRunPhase.PLAYING,
                    totalRounds = rounds, currentRound = 0, gameSpeed = speed,
                    activeTempos = activeTempos, playerLane = activeTempos.size / 2,
                    distance = 0f, score = 0, lives = 3, maxLives = 3,
                    combo = 0, maxCombo = 0, correctCount = 0, wrongCount = 0,
                    currentTempo = null, isPlaying = false
                )
            }
            
            val session = gamesRepository.startGameSession(
                userId = userId, gameTypeId = _state.value.gameType?.id ?: "", gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
            
            startGame()
        }
    }
    
    private fun startGame() {
        gameLoop?.cancel()
        nextTempo()
        
        gameLoop = viewModelScope.launch {
            while (_state.value.gamePhase == TempoRunPhase.PLAYING && _state.value.lives > 0) {
                delay(50)
                _state.update { 
                    it.copy(distance = it.distance + (it.gameSpeed * 0.1f))
                }
            }
        }
    }
    
    private fun nextTempo() {
        val currentState = _state.value
        
        if (currentState.currentRound >= currentState.totalRounds || currentState.lives <= 0) {
            endGame()
            return
        }
        
        val tempo = currentState.activeTempos.random()
        val bpm = (tempo.minBpm..tempo.maxBpm).random()
        
        _state.update { 
            it.copy(
                currentRound = it.currentRound + 1,
                currentTempo = tempo,
                currentBpm = bpm,
                correctLane = currentState.activeTempos.indexOf(tempo),
                timeToAnswer = 5f,
                roundResult = null
            )
        }
        
        playTempo(bpm)
        startAnswerTimer()
    }
    
    private fun playTempo(bpm: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isPlaying = true) }
            // TODO: Tocar metrônomo ou música no tempo indicado
            delay(3000) // Tocar por 3 segundos
            _state.update { it.copy(isPlaying = false) }
        }
    }
    
    private fun startAnswerTimer() {
        viewModelScope.launch {
            while (_state.value.timeToAnswer > 0 && _state.value.roundResult == null) {
                delay(100)
                _state.update { it.copy(timeToAnswer = (it.timeToAnswer - 0.1f).coerceAtLeast(0f)) }
            }
            
            if (_state.value.roundResult == null) {
                checkAnswer()
            }
        }
    }
    
    fun moveLane(direction: Int) {
        val currentState = _state.value
        val newLane = (currentState.playerLane + direction).coerceIn(0, currentState.activeTempos.size - 1)
        _state.update { it.copy(playerLane = newLane) }
    }
    
    fun confirmAnswer() {
        checkAnswer()
    }
    
    private fun checkAnswer() {
        val currentState = _state.value
        val isCorrect = currentState.playerLane == currentState.correctLane
        
        if (isCorrect) {
            val newCombo = currentState.combo + 1
            val points = 100 + (newCombo * 20) + (currentState.timeToAnswer * 10).toInt()
            
            _state.update { 
                it.copy(
                    score = it.score + points, combo = newCombo,
                    maxCombo = maxOf(it.maxCombo, newCombo), correctCount = it.correctCount + 1,
                    roundResult = RoundResult.SUCCESS
                )
            }
        } else {
            _state.update { 
                it.copy(
                    lives = it.lives - 1, combo = 0, wrongCount = it.wrongCount + 1,
                    roundResult = RoundResult.FAIL
                )
            }
        }
        
        viewModelScope.launch {
            delay(1500)
            nextTempo()
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
                    gamePhase = TempoRunPhase.RESULT,
                    stars = result?.stars ?: 2, xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    fun pauseGame() { gameLoop?.cancel(); _state.update { it.copy(gamePhase = TempoRunPhase.PAUSED) } }
    fun resumeGame() { _state.update { it.copy(gamePhase = TempoRunPhase.PLAYING) }; startGame() }
    fun backToLevelSelect() { gameLoop?.cancel(); _state.update { it.copy(gamePhase = TempoRunPhase.LEVEL_SELECT) } }
    fun restartLevel(userId: String) { _state.value.currentLevel?.let { startLevel(userId, it) } }
    
    override fun onCleared() { gameLoop?.cancel(); super.onCleared() }
}

data class TempoRange(val name: String, val minBpm: Int, val maxBpm: Int, val emoji: String)

data class TempoRunState(
    val isLoading: Boolean = false, val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(), val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0, val gamePhase: TempoRunPhase = TempoRunPhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null, val totalRounds: Int = 8, val currentRound: Int = 0,
    val gameSpeed: Int = 1, val activeTempos: List<TempoRange> = emptyList(),
    val playerLane: Int = 0, val correctLane: Int = 0, val distance: Float = 0f,
    val currentTempo: TempoRange? = null, val currentBpm: Int = 100,
    val isPlaying: Boolean = false, val timeToAnswer: Float = 5f,
    val lives: Int = 3, val maxLives: Int = 3, val roundResult: RoundResult? = null,
    val score: Int = 0, val combo: Int = 0, val maxCombo: Int = 0,
    val correctCount: Int = 0, val wrongCount: Int = 0,
    val stars: Int = 0, val xpEarned: Int = 0, val coinsEarned: Int = 0
)

enum class TempoRunPhase { LEVEL_SELECT, PLAYING, PAUSED, RESULT }
