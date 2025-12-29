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
import com.musimind.music.audio.GameAudioManager

/**
 * ViewModel para o jogo Progression Quest (Missão das Progressões)
 * 
 * O jogador ouve progressões harmônicas e deve identificar os graus
 * ou cifras dos acordes em uma aventura estilo RPG.
 */
@HiltViewModel
class ProgressionQuestViewModel @Inject constructor(
    private val gamesRepository: GamesRepository,
    private val audioManager: GameAudioManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(ProgressionQuestState())
    val state: StateFlow<ProgressionQuestState> = _state.asStateFlow()
    
    private var sessionId: String? = null
    
    // Progressões comuns
    private val progressions = listOf(
        ProgressionInfo("I-IV-V-I", listOf("I", "IV", "V", "I"), "Pop/Rock básico", "🎸"),
        ProgressionInfo("I-V-vi-IV", listOf("I", "V", "vi", "IV"), "Pop moderno", "🎤"),
        ProgressionInfo("ii-V-I", listOf("ii", "V", "I"), "Jazz básico", "🎷"),
        ProgressionInfo("I-vi-IV-V", listOf("I", "vi", "IV", "V"), "Anos 50", "🎵"),
        ProgressionInfo("I-IV-vi-V", listOf("I", "IV", "vi", "V"), "Pop variação", "🎹"),
        ProgressionInfo("vi-IV-I-V", listOf("vi", "IV", "I", "V"), "Pop melancólico", "😢"),
        ProgressionInfo("I-bVII-IV-I", listOf("I", "bVII", "IV", "I"), "Rock modal", "🤘"),
        ProgressionInfo("I-V-IV-V", listOf("I", "V", "IV", "V"), "Country", "🤠"),
        ProgressionInfo("I-II-IV-I", listOf("I", "II", "IV", "I"), "Blues rock", "🎸"),
        ProgressionInfo("i-VII-VI-V", listOf("i", "VII", "VI", "V"), "Flamenco", "💃")
    )
    
    private val allDegrees = listOf("I", "II", "ii", "III", "iii", "IV", "V", "vi", "VI", "VII", "vii", "bVII", "bVI", "bIII")
    
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "progression_quest" }
            
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
            val rounds = config?.get("rounds")?.jsonPrimitive?.int ?: 5
            val difficulty = config?.get("difficulty")?.jsonPrimitive?.int ?: 1
            
            val enabledProgressions = when (difficulty) {
                1 -> progressions.take(3)
                2 -> progressions.take(6)
                else -> progressions
            }
            
            _state.update { 
                it.copy(
                    currentLevel = level, gamePhase = ProgressionQuestPhase.PLAYING,
                    totalRounds = rounds, currentRound = 0, difficulty = difficulty,
                    enabledProgressions = enabledProgressions, playerHealth = 100,
                    bossHealth = 100, score = 0, combo = 0, maxCombo = 0,
                    correctCount = 0, wrongCount = 0
                )
            }
            
            val session = gamesRepository.startGameSession(
                userId = userId, gameTypeId = _state.value.gameType?.id ?: "", gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
            
            nextProgression()
        }
    }
    
    private fun nextProgression() {
        val currentState = _state.value
        
        if (currentState.currentRound >= currentState.totalRounds || currentState.playerHealth <= 0 || currentState.bossHealth <= 0) {
            endGame()
            return
        }
        
        val progression = currentState.enabledProgressions.random()
        val degrees = progression.degrees
        
        // Sortear qual acorde da progressão deve ser identificado
        val targetIndex = degrees.indices.random()
        val targetDegree = degrees[targetIndex]
        
        // Criar opções de resposta
        val wrongOptions = allDegrees.filter { it != targetDegree }.shuffled().take(3)
        val options = (listOf(targetDegree) + wrongOptions).shuffled()
        
        _state.update { 
            it.copy(
                currentRound = it.currentRound + 1,
                currentProgression = progression,
                targetChordIndex = targetIndex,
                targetDegree = targetDegree,
                answerOptions = options,
                currentChordPlaying = -1,
                roundResult = null
            )
        }
        
        playProgression(degrees, targetIndex)
    }
    
    private fun playProgression(degrees: List<String>, targetIndex: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isPlaying = true) }
            
            degrees.forEachIndexed { index, degree ->
                _state.update { it.copy(currentChordPlaying = index) }
                // Tocar acorde baseado no grau
                val chordName = degreeToChord(degree)
                audioManager.playChordByName(chordName, octave = 4, durationMs = 700)
                delay(800)
            }
            
            // Repetir destacando o alvo
            delay(300)
            repeat(2) {
                _state.update { it.copy(currentChordPlaying = targetIndex) }
                delay(600)
                _state.update { it.copy(currentChordPlaying = -1) }
                delay(200)
            }
            
            _state.update { it.copy(isPlaying = false, currentChordPlaying = -1) }
        }
    }
    
    fun selectAnswer(degree: String) {
        val currentState = _state.value
        val isCorrect = degree == currentState.targetDegree
        
        if (isCorrect) {
            val damage = 25 + (currentState.combo * 5)
            val newBossHealth = (currentState.bossHealth - damage).coerceAtLeast(0)
            val newCombo = currentState.combo + 1
            val points = 100 + (newCombo * 25)
            
            _state.update { 
                it.copy(
                    score = it.score + points, combo = newCombo,
                    maxCombo = maxOf(it.maxCombo, newCombo), correctCount = it.correctCount + 1,
                    bossHealth = newBossHealth, roundResult = RoundResult.SUCCESS
                )
            }
        } else {
            val damage = 20
            val newPlayerHealth = (currentState.playerHealth - damage).coerceAtLeast(0)
            
            _state.update { 
                it.copy(
                    combo = 0, wrongCount = it.wrongCount + 1,
                    playerHealth = newPlayerHealth, roundResult = RoundResult.FAIL
                )
            }
        }
        
        viewModelScope.launch {
            delay(1500)
            nextProgression()
        }
    }
    
    fun replayProgression() {
        val state = _state.value
        state.currentProgression?.let { progression ->
            playProgression(progression.degrees, state.targetChordIndex)
        }
    }
    
    private fun endGame() {
        viewModelScope.launch {
            val currentState = _state.value
            val victory = currentState.bossHealth <= 0 || (currentState.playerHealth > 0 && currentState.currentRound >= currentState.totalRounds)
            
            val result = sessionId?.let { sid ->
                gamesRepository.completeGameSession(
                    sessionId = sid, score = currentState.score,
                    correctAnswers = currentState.correctCount, wrongAnswers = currentState.wrongCount,
                    maxCombo = currentState.maxCombo
                ).getOrNull()
            }
            
            _state.update { 
                it.copy(
                    gamePhase = ProgressionQuestPhase.RESULT, victory = victory,
                    stars = result?.stars ?: if (victory) 2 else 0, xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    /**
     * Converte grau da escala para nome do acorde (em C maior por padrão)
     */
    private fun degreeToChord(degree: String): String {
        return when (degree) {
            "I" -> "C"
            "II" -> "D"
            "ii" -> "Dm"
            "III" -> "E"
            "iii" -> "Em"
            "IV" -> "F"
            "V" -> "G"
            "VI" -> "A"
            "vi" -> "Am"
            "VII" -> "B"
            "vii" -> "Bdim"
            "bVII" -> "Bb"
            "bVI" -> "Ab"
            "bIII" -> "Eb"
            "i" -> "Cm" // Para tonalidades menores
            else -> "C"
        }
    }
    
    fun pauseGame() { _state.update { it.copy(gamePhase = ProgressionQuestPhase.PAUSED) } }
    fun resumeGame() { _state.update { it.copy(gamePhase = ProgressionQuestPhase.PLAYING) } }
    fun backToLevelSelect() { _state.update { it.copy(gamePhase = ProgressionQuestPhase.LEVEL_SELECT) } }
    fun restartLevel(userId: String) { _state.value.currentLevel?.let { startLevel(userId, it) } }
}

data class ProgressionInfo(val name: String, val degrees: List<String>, val style: String, val emoji: String)

data class ProgressionQuestState(
    val isLoading: Boolean = false, val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(), val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0, val gamePhase: ProgressionQuestPhase = ProgressionQuestPhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null, val totalRounds: Int = 5, val currentRound: Int = 0,
    val difficulty: Int = 1, val enabledProgressions: List<ProgressionInfo> = emptyList(),
    val currentProgression: ProgressionInfo? = null, val targetChordIndex: Int = 0,
    val targetDegree: String = "", val answerOptions: List<String> = emptyList(),
    val isPlaying: Boolean = false, val currentChordPlaying: Int = -1,
    val playerHealth: Int = 100, val bossHealth: Int = 100,
    val roundResult: RoundResult? = null, val victory: Boolean = false,
    val score: Int = 0, val combo: Int = 0, val maxCombo: Int = 0,
    val correctCount: Int = 0, val wrongCount: Int = 0,
    val stars: Int = 0, val xpEarned: Int = 0, val coinsEarned: Int = 0
)

enum class ProgressionQuestPhase { LEVEL_SELECT, PLAYING, PAUSED, RESULT }
