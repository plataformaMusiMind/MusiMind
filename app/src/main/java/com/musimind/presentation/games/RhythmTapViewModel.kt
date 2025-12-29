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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import com.musimind.music.audio.GameAudioManager

/**
 * ViewModel para o jogo Rhythm Tap (Batida Perfeita)
 * 
 * O jogador ouve e vê um padrão rítmico e deve reproduzí-lo
 * batendo no tempo correto. Reforça subdivisão do tempo,
 * síncope e precisão rítmica.
 * 
 * Este jogo aproveita a funcionalidade de percepção rítmica
 * existente no app.
 */
@HiltViewModel
class RhythmTapViewModel @Inject constructor(
    private val gamesRepository: GamesRepository,
    private val audioManager: GameAudioManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(RhythmTapState())
    val state: StateFlow<RhythmTapState> = _state.asStateFlow()
    
    private var gameLoopJob: Job? = null
    private var sessionId: String? = null
    private var patternStartTime: Long = 0L
    
    // Padrões rítmicos pré-definidos
    // q = semínima, e = colcheia, s = semicolcheia, r = pausa, t = tercina
    private val rhythmPatterns = mapOf(
        "q q q q" to listOf(0L, 1000L, 2000L, 3000L), // 4 semínimas
        "q q ee q" to listOf(0L, 1000L, 2000L, 2500L, 3000L),
        "ee ee q q" to listOf(0L, 500L, 1000L, 1500L, 2000L, 3000L),
        "q r q q" to listOf(0L, 2000L, 3000L), // com pausa
        "ee r ee q" to listOf(0L, 500L, 2000L, 2500L, 3000L),
        "ssss q q q" to listOf(0L, 250L, 500L, 750L, 1000L, 2000L, 3000L),
        "q ssss ee q" to listOf(0L, 1000L, 1250L, 1500L, 1750L, 2000L, 2500L, 3000L),
        "e q e q q" to listOf(0L, 500L, 1500L, 2000L, 3000L), // síncope
        "q. q." to listOf(0L, 1500L), // compasso composto
        "eee eee" to listOf(0L, 333L, 666L, 1000L, 1333L, 1666L), // colcheias em 6/8
        "ttt q q q" to listOf(0L, 333L, 666L, 1000L, 2000L, 3000L), // tercinas
        "q ttt q q" to listOf(0L, 1000L, 1333L, 1666L, 2000L, 3000L)
    )
    
    /**
     * Carrega os níveis do jogo
     */
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "rhythm_tap" }
            
            if (gameType != null) {
                val progress = gamesRepository.getGameProgress(userId, gameType.id).getOrNull()
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        gameType = gameType,
                        levels = progress?.levels ?: emptyList(),
                        highScores = progress?.highScores ?: emptyMap(),
                        totalStars = progress?.totalStars ?: 0
                    ) 
                }
            } else {
                _state.update { it.copy(isLoading = false, error = "Jogo não encontrado") }
            }
        }
    }
    
    /**
     * Inicia um nível
     */
    fun startLevel(userId: String, level: GameLevel) {
        viewModelScope.launch {
            // Extrair configurações
            val config = level.config
            val tempo = config?.get("tempo")?.jsonPrimitive?.int ?: 80
            val patterns = config?.get("patterns")?.jsonArray?.map { it.jsonPrimitive.content } 
                ?: listOf("q q q q")
            val rounds = config?.get("rounds")?.jsonPrimitive?.int ?: 4
            
            _state.update { 
                it.copy(
                    currentLevel = level,
                    gamePhase = RhythmGamePhase.COUNTDOWN,
                    score = 0,
                    combo = 0,
                    maxCombo = 0,
                    correctCount = 0,
                    wrongCount = 0,
                    currentRound = 0,
                    totalRounds = rounds,
                    tempo = tempo,
                    availablePatterns = patterns,
                    currentPattern = null,
                    expectedBeats = emptyList(),
                    playerBeats = emptyList(),
                    isListening = false
                )
            }
            
            // Criar sessão
            val session = gamesRepository.startGameSession(
                userId = userId,
                gameTypeId = _state.value.gameType?.id ?: "",
                gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
            
            // Countdown e iniciar
            startCountdown()
        }
    }
    
    /**
     * Countdown inicial (3, 2, 1...)
     */
    private fun startCountdown() {
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _state.update { it.copy(countdownValue = i) }
                delay(1000)
            }
            _state.update { it.copy(countdownValue = 0, gamePhase = RhythmGamePhase.PLAYING) }
            startNextRound()
        }
    }
    
    /**
     * Inicia a próxima rodada
     */
    private fun startNextRound() {
        val currentState = _state.value
        
        if (currentState.currentRound >= currentState.totalRounds) {
            endGame()
            return
        }
        
        // Selecionar padrão
        val patternKey = currentState.availablePatterns.randomOrNull() ?: "q q q q"
        val beatTimes = rhythmPatterns[patternKey] ?: listOf(0L, 1000L, 2000L, 3000L)
        
        // Ajustar tempos baseado no BPM
        val beatDuration = 60000L / currentState.tempo // ms por batida
        val adjustedBeats = beatTimes.map { (it * beatDuration) / 1000 }
        
        _state.update { 
            it.copy(
                currentRound = it.currentRound + 1,
                currentPattern = patternKey,
                expectedBeats = adjustedBeats,
                playerBeats = emptyList(),
                roundPhase = RoundPhase.LISTENING,
                isListening = false,
                beatIndicatorIndex = -1
            )
        }
        
        // Tocar o padrão para o usuário ouvir
        playPattern(adjustedBeats)
    }
    
    /**
     * Reproduz o padrão rítmico
     */
    private fun playPattern(beats: List<Long>) {
        viewModelScope.launch {
            val patternDuration = beats.maxOrNull() ?: 0L
            val startTime = System.currentTimeMillis()
            
            var beatIndex = 0
            while (beatIndex < beats.size) {
                val elapsed = System.currentTimeMillis() - startTime
                
                if (elapsed >= beats[beatIndex]) {
                    // "Tocar" a batida (feedback visual)
                    val isStrong = beatIndex == 0 || (beatIndex % 4 == 0)
                    _state.update { it.copy(beatIndicatorIndex = beatIndex) }
                    
                    // Tocar som de batida usando audioManager
                    audioManager.playMetronomeTick(isStrong = isStrong)
                    
                    beatIndex++
                }
                
                delay(16)
            }
            
            // Esperar um pouco e então permitir resposta
            delay(500)
            
            _state.update { 
                it.copy(
                    roundPhase = RoundPhase.YOUR_TURN,
                    isListening = true,
                    beatIndicatorIndex = -1
                )
            }
            
            patternStartTime = System.currentTimeMillis()
            
            // Timeout para resposta
            delay((beats.maxOrNull() ?: 4000L) + 2000L)
            
            if (_state.value.roundPhase == RoundPhase.YOUR_TURN) {
                evaluateRound()
            }
        }
    }
    
    /**
     * Jogador fez uma batida
     */
    fun onTap() {
        if (!_state.value.isListening) return
        
        val tapTime = System.currentTimeMillis() - patternStartTime
        
        _state.update { 
            it.copy(playerBeats = it.playerBeats + tapTime)
        }
        
        // Se completou todas as batidas esperadas, avaliar
        if (_state.value.playerBeats.size >= _state.value.expectedBeats.size) {
            evaluateRound()
        }
    }
    
    /**
     * Avalia a performance do jogador na rodada
     */
    private fun evaluateRound() {
        val state = _state.value
        val expected = state.expectedBeats
        val player = state.playerBeats
        
        // Calcular precisão
        var totalError = 0L
        val tolerance = 150L // ms de tolerância
        var perfectHits = 0
        var goodHits = 0
        var missedHits = 0
        
        expected.forEachIndexed { index, expectedTime ->
            val playerTime = player.getOrNull(index)
            
            if (playerTime == null) {
                missedHits++
            } else {
                val error = abs(expectedTime - playerTime)
                totalError += error
                
                when {
                    error <= tolerance / 2 -> perfectHits++
                    error <= tolerance -> goodHits++
                    else -> missedHits++
                }
            }
        }
        
        // Batidas extras
        val extraBeats = (player.size - expected.size).coerceAtLeast(0)
        missedHits += extraBeats
        
        // Calcular pontuação
        val roundScore = (perfectHits * 100) + (goodHits * 50) - (missedHits * 25)
        val isSuccess = perfectHits + goodHits >= expected.size / 2
        
        // Atualizar estado
        _state.update { 
            val newCombo = if (isSuccess) it.combo + 1 else 0
            it.copy(
                score = it.score + roundScore.coerceAtLeast(0),
                combo = newCombo,
                maxCombo = maxOf(it.maxCombo, newCombo),
                correctCount = it.correctCount + perfectHits + goodHits,
                wrongCount = it.wrongCount + missedHits,
                roundPhase = RoundPhase.FEEDBACK,
                lastRoundResult = if (isSuccess) RoundResult.SUCCESS else RoundResult.FAIL,
                isListening = false
            )
        }
        
        // Mostrar feedback e ir para próxima rodada
        viewModelScope.launch {
            delay(1500)
            
            _state.update { it.copy(lastRoundResult = null) }
            startNextRound()
        }
    }
    
    /**
     * Finaliza o jogo
     */
    private fun endGame() {
        viewModelScope.launch {
            val currentState = _state.value
            
            val result = sessionId?.let { sid ->
                gamesRepository.completeGameSession(
                    sessionId = sid,
                    score = currentState.score,
                    correctAnswers = currentState.correctCount,
                    wrongAnswers = currentState.wrongCount,
                    maxCombo = currentState.maxCombo
                ).getOrNull()
            }
            
            _state.update { 
                it.copy(
                    gamePhase = RhythmGamePhase.RESULT,
                    stars = result?.stars ?: calculateLocalStars(),
                    xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    private fun calculateLocalStars(): Int {
        val state = _state.value
        val total = state.correctCount + state.wrongCount
        if (total == 0) return 0
        
        val accuracy = state.correctCount.toFloat() / total
        return when {
            accuracy >= 0.9f -> 3
            accuracy >= 0.7f -> 2
            accuracy >= 0.5f -> 1
            else -> 0
        }
    }
    
    fun pauseGame() {
        gameLoopJob?.cancel()
        _state.update { it.copy(gamePhase = RhythmGamePhase.PAUSED) }
    }
    
    fun resumeGame() {
        _state.update { it.copy(gamePhase = RhythmGamePhase.PLAYING) }
        startNextRound()
    }
    
    fun backToLevelSelect() {
        gameLoopJob?.cancel()
        _state.update { 
            it.copy(
                gamePhase = RhythmGamePhase.LEVEL_SELECT,
                currentLevel = null
            )
        }
    }
    
    fun restartLevel(userId: String) {
        val level = _state.value.currentLevel ?: return
        startLevel(userId, level)
    }
    
    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}

/**
 * Estado do jogo Rhythm Tap
 */
data class RhythmTapState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Dados do jogo
    val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(),
    val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0,
    
    // Estado atual
    val gamePhase: RhythmGamePhase = RhythmGamePhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null,
    
    // Configurações
    val tempo: Int = 80,
    val availablePatterns: List<String> = emptyList(),
    val totalRounds: Int = 4,
    
    // Rodada atual
    val currentRound: Int = 0,
    val roundPhase: RoundPhase = RoundPhase.LISTENING,
    val currentPattern: String? = null,
    val expectedBeats: List<Long> = emptyList(),
    val playerBeats: List<Long> = emptyList(),
    val isListening: Boolean = false,
    val beatIndicatorIndex: Int = -1,
    val countdownValue: Int = 0,
    val lastRoundResult: RoundResult? = null,
    
    // Pontuação
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    
    // Resultado
    val stars: Int = 0,
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0
)

enum class RhythmGamePhase {
    LEVEL_SELECT,
    COUNTDOWN,
    PLAYING,
    PAUSED,
    RESULT
}

enum class RoundPhase {
    LISTENING,  // Ouvindo o padrão
    YOUR_TURN,  // Vez do jogador reproduzir
    FEEDBACK    // Mostrando resultado
}
