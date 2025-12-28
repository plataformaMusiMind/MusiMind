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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import kotlin.random.Random

/**
 * ViewModel para o jogo Interval Hero (Herói dos Intervalos)
 * 
 * Interface gamificada de batalha onde o jogador identifica intervalos
 * musicais para "atacar" monstros. Reforça percepção intervalar.
 * 
 * Aproveita a funcionalidade de percepção intervalar existente.
 */
@HiltViewModel
class IntervalHeroViewModel @Inject constructor(
    private val gamesRepository: GamesRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(IntervalHeroState())
    val state: StateFlow<IntervalHeroState> = _state.asStateFlow()
    
    private var sessionId: String? = null
    
    // Intervalos e suas características
    private val intervals = listOf(
        IntervalInfo("1J", "Uníssono", 0, "🎯"),
        IntervalInfo("2m", "2ª Menor", 1, "👿"),
        IntervalInfo("2M", "2ª Maior", 2, "😈"),
        IntervalInfo("3m", "3ª Menor", 3, "🐺"),
        IntervalInfo("3M", "3ª Maior", 4, "🦊"),
        IntervalInfo("4J", "4ª Justa", 5, "🐲"),
        IntervalInfo("4A", "Trítono", 6, "👹"),
        IntervalInfo("5J", "5ª Justa", 7, "🦁"),
        IntervalInfo("6m", "6ª Menor", 8, "🐙"),
        IntervalInfo("6M", "6ª Maior", 9, "🦑"),
        IntervalInfo("7m", "7ª Menor", 10, "🐉"),
        IntervalInfo("7M", "7ª Maior", 11, "🔥"),
        IntervalInfo("8J", "Oitava", 12, "⭐")
    )
    
    // Músicas de referência para ajuda
    private val referenceHints = mapOf(
        "2m" to "Tubarão (Jaws)",
        "2M" to "Parabéns pra Você",
        "3m" to "Greensleeves",
        "3M" to "A Canoa Virou",
        "4J" to "Hino Nacional",
        "4A" to "Os Simpsons",
        "5J" to "Star Wars",
        "6m" to "Love Story",
        "6M" to "My Bonnie",
        "7m" to "Winner Takes All",
        "7M" to "Take on Me",
        "8J" to "Over the Rainbow"
    )
    
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "interval_hero" }
            
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
            }
        }
    }
    
    fun startLevel(userId: String, level: GameLevel) {
        viewModelScope.launch {
            val config = level.config
            val activeIntervals = config?.get("intervals")?.jsonArray?.map { it.jsonPrimitive.content }
                ?: listOf("2m", "2M", "3m", "3M")
            val rounds = config?.get("rounds")?.jsonPrimitive?.int ?: 10
            val isHarmonic = config?.get("harmonic")?.jsonPrimitive?.content?.toBoolean() ?: false
            
            _state.update { 
                it.copy(
                    currentLevel = level,
                    gamePhase = IntervalGamePhase.PLAYING,
                    activeIntervals = activeIntervals,
                    totalRounds = rounds,
                    currentRound = 0,
                    isHarmonic = isHarmonic,
                    monsterHealth = 100,
                    playerHealth = 100,
                    score = 0,
                    combo = 0,
                    maxCombo = 0,
                    correctCount = 0,
                    wrongCount = 0
                )
            }
            
            val session = gamesRepository.startGameSession(
                userId = userId,
                gameTypeId = _state.value.gameType?.id ?: "",
                gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
            
            nextQuestion()
        }
    }
    
    private fun nextQuestion() {
        val currentState = _state.value
        
        if (currentState.currentRound >= currentState.totalRounds || currentState.playerHealth <= 0) {
            endGame()
            return
        }
        
        // Selecionar intervalo aleatório
        val intervalCode = currentState.activeIntervals.random()
        val interval = intervals.find { it.code == intervalCode } ?: intervals[0]
        
        // Gerar nota base e calcular nota alvo
        val baseNote = Random.nextInt(48, 72) // C3 a C5
        val targetNote = baseNote + interval.semitones
        val isAscending = Random.nextBoolean()
        
        // Criar opções de resposta (4 opções)
        val correctOption = interval.code
        val wrongOptions = currentState.activeIntervals
            .filter { it != correctOption }
            .shuffled()
            .take(3)
        val options = (listOf(correctOption) + wrongOptions).shuffled()
        
        _state.update { 
            it.copy(
                currentRound = it.currentRound + 1,
                currentInterval = interval,
                baseNoteMidi = baseNote,
                targetNoteMidi = if (isAscending) targetNote else baseNote,
                isAscending = isAscending,
                answerOptions = options,
                showingHint = false,
                roundResult = null
            )
        }
        
        // Tocar o intervalo
        playInterval()
    }
    
    private fun playInterval() {
        viewModelScope.launch {
            val state = _state.value
            
            // Primeiro, mostrar que está tocando
            _state.update { it.copy(isPlaying = true) }
            
            // TODO: Tocar as notas via áudio engine
            // Se harmônico: tocar simultaneamente
            // Se melódico: tocar sequencialmente
            
            delay(if (state.isHarmonic) 1000 else 1500)
            
            _state.update { it.copy(isPlaying = false) }
        }
    }
    
    fun selectAnswer(intervalCode: String) {
        val currentState = _state.value
        val isCorrect = intervalCode == currentState.currentInterval?.code
        
        if (isCorrect) {
            val damage = 20 + (currentState.combo * 5)
            val newMonsterHealth = (currentState.monsterHealth - damage).coerceAtLeast(0)
            val newCombo = currentState.combo + 1
            val points = 100 + (newCombo * 20)
            
            _state.update { 
                it.copy(
                    score = it.score + points,
                    combo = newCombo,
                    maxCombo = maxOf(it.maxCombo, newCombo),
                    correctCount = it.correctCount + 1,
                    monsterHealth = newMonsterHealth,
                    roundResult = RoundResult.SUCCESS
                )
            }
        } else {
            val damage = 15
            val newPlayerHealth = (currentState.playerHealth - damage).coerceAtLeast(0)
            
            _state.update { 
                it.copy(
                    combo = 0,
                    wrongCount = it.wrongCount + 1,
                    playerHealth = newPlayerHealth,
                    roundResult = RoundResult.FAIL
                )
            }
        }
        
        // Próxima pergunta após feedback
        viewModelScope.launch {
            delay(1500)
            _state.update { it.copy(roundResult = null) }
            
            // Verificar se monstro morreu
            if (_state.value.monsterHealth <= 0) {
                // Regenerar monstro para próxima rodada
                _state.update { it.copy(monsterHealth = 100) }
            }
            
            nextQuestion()
        }
    }
    
    fun showHint() {
        val hint = referenceHints[_state.value.currentInterval?.code] ?: "Ouça com atenção!"
        _state.update { it.copy(showingHint = true, hintText = hint) }
    }
    
    fun replayInterval() {
        playInterval()
    }
    
    private fun endGame() {
        viewModelScope.launch {
            val currentState = _state.value
            val won = currentState.playerHealth > 0
            
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
                    gamePhase = IntervalGamePhase.RESULT,
                    victory = won,
                    stars = result?.stars ?: if (won) 2 else 0,
                    xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    fun pauseGame() { _state.update { it.copy(gamePhase = IntervalGamePhase.PAUSED) } }
    fun resumeGame() { 
        _state.update { it.copy(gamePhase = IntervalGamePhase.PLAYING) }
        playInterval()
    }
    fun backToLevelSelect() { _state.update { it.copy(gamePhase = IntervalGamePhase.LEVEL_SELECT) } }
    fun restartLevel(userId: String) { _state.value.currentLevel?.let { startLevel(userId, it) } }
    
    fun getIntervalDisplayName(code: String): String {
        return intervals.find { it.code == code }?.name ?: code
    }
}

data class IntervalInfo(
    val code: String,
    val name: String,
    val semitones: Int,
    val monsterEmoji: String
)

data class IntervalHeroState(
    val isLoading: Boolean = false,
    val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(),
    val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0,
    
    val gamePhase: IntervalGamePhase = IntervalGamePhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null,
    
    val activeIntervals: List<String> = emptyList(),
    val totalRounds: Int = 10,
    val currentRound: Int = 0,
    val isHarmonic: Boolean = false,
    
    val currentInterval: IntervalInfo? = null,
    val baseNoteMidi: Int = 60,
    val targetNoteMidi: Int = 64,
    val isAscending: Boolean = true,
    val answerOptions: List<String> = emptyList(),
    val isPlaying: Boolean = false,
    
    val monsterHealth: Int = 100,
    val playerHealth: Int = 100,
    
    val showingHint: Boolean = false,
    val hintText: String = "",
    val roundResult: RoundResult? = null,
    
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    
    val victory: Boolean = false,
    val stars: Int = 0,
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0
)

enum class IntervalGamePhase {
    LEVEL_SELECT, PLAYING, PAUSED, RESULT
}
