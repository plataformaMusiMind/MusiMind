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
 * ViewModel para o jogo Chord Builder (Construtor de Acordes)
 * 
 * O jogador constrói acordes nota por nota em um teclado virtual.
 * Dado o nome do acorde, deve selecionar as notas corretas.
 */
@HiltViewModel
class ChordBuilderViewModel @Inject constructor(
    private val gamesRepository: GamesRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ChordBuilderState())
    val state: StateFlow<ChordBuilderState> = _state.asStateFlow()
    
    private var sessionId: String? = null
    
    // Estruturas de acordes (intervalos em semitons a partir da fundamental)
    private val chordStructures = mapOf(
        "M" to listOf(0, 4, 7),         // Maior
        "m" to listOf(0, 3, 7),         // Menor
        "dim" to listOf(0, 3, 6),       // Diminuto
        "aug" to listOf(0, 4, 8),       // Aumentado
        "7" to listOf(0, 4, 7, 10),     // Dominante
        "M7" to listOf(0, 4, 7, 11),    // Maior com 7ª maior
        "m7" to listOf(0, 3, 7, 10),    // Menor com 7ª
        "dim7" to listOf(0, 3, 6, 9),   // Diminuto com 7ª
        "sus2" to listOf(0, 2, 7),      // Suspensa 2
        "sus4" to listOf(0, 5, 7)       // Suspensa 4
    )
    
    private val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val noteDisplayNames = listOf("Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá", "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si")
    
    private val chordTypeNames = mapOf(
        "M" to "Maior", "m" to "Menor", "dim" to "Diminuto", "aug" to "Aumentado",
        "7" to "Dominante", "M7" to "Maior 7", "m7" to "Menor 7", "dim7" to "Diminuto 7",
        "sus2" to "Sus2", "sus4" to "Sus4"
    )
    
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "chord_builder" }
            
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
            val rounds = config?.get("rounds")?.jsonPrimitive?.int ?: 6
            val difficulty = config?.get("difficulty")?.jsonPrimitive?.int ?: 1
            
            val enabledTypes = when (difficulty) {
                1 -> listOf("M", "m")
                2 -> listOf("M", "m", "dim", "aug")
                3 -> listOf("M", "m", "dim", "aug", "7", "M7", "m7")
                else -> chordStructures.keys.toList()
            }
            
            _state.update { 
                it.copy(
                    currentLevel = level, gamePhase = ChordBuilderPhase.PLAYING,
                    totalRounds = rounds, currentRound = 0, enabledChordTypes = enabledTypes,
                    score = 0, combo = 0, maxCombo = 0, correctCount = 0, wrongCount = 0
                )
            }
            
            val session = gamesRepository.startGameSession(
                userId = userId, gameTypeId = _state.value.gameType?.id ?: "", gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
            
            nextChord()
        }
    }
    
    private fun nextChord() {
        val currentState = _state.value
        
        if (currentState.currentRound >= currentState.totalRounds) {
            endGame()
            return
        }
        
        // Sortear acorde
        val rootIndex = (0..11).random()
        val chordType = currentState.enabledChordTypes.random()
        val structure = chordStructures[chordType] ?: listOf(0, 4, 7)
        
        // Calcular notas corretas
        val correctNotes = structure.map { (rootIndex + it) % 12 }.toSet()
        
        val chordName = "${noteDisplayNames[rootIndex]} ${chordTypeNames[chordType]}"
        
        _state.update { 
            it.copy(
                currentRound = it.currentRound + 1,
                currentChordName = chordName,
                rootNote = rootIndex,
                chordType = chordType,
                correctNotes = correctNotes,
                selectedNotes = emptySet(),
                roundResult = null
            )
        }
    }
    
    fun toggleNote(noteIndex: Int) {
        if (_state.value.roundResult != null) return
        
        _state.update { 
            val newSelection = if (noteIndex in it.selectedNotes) {
                it.selectedNotes - noteIndex
            } else {
                it.selectedNotes + noteIndex
            }
            it.copy(selectedNotes = newSelection)
        }
    }
    
    fun submitAnswer() {
        val currentState = _state.value
        val isCorrect = currentState.selectedNotes == currentState.correctNotes
        
        if (isCorrect) {
            val newCombo = currentState.combo + 1
            val points = 150 + (newCombo * 30)
            
            _state.update { 
                it.copy(
                    score = it.score + points, combo = newCombo,
                    maxCombo = maxOf(it.maxCombo, newCombo), correctCount = it.correctCount + 1,
                    roundResult = RoundResult.SUCCESS
                )
            }
        } else {
            _state.update { 
                it.copy(combo = 0, wrongCount = it.wrongCount + 1, roundResult = RoundResult.FAIL)
            }
        }
        
        viewModelScope.launch {
            delay(1500)
            nextChord()
        }
    }
    
    fun playChord() {
        viewModelScope.launch {
            _state.update { it.copy(isPlayingChord = true) }
            // TODO: Tocar o acorde via audio engine
            delay(1500)
            _state.update { it.copy(isPlayingChord = false) }
        }
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
                    gamePhase = ChordBuilderPhase.RESULT,
                    stars = result?.stars ?: 2, xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    fun getNoteDisplayName(index: Int) = noteDisplayNames[index]
    fun isNoteCorrect(index: Int) = index in _state.value.correctNotes
    fun isNoteSelected(index: Int) = index in _state.value.selectedNotes
    
    fun pauseGame() { _state.update { it.copy(gamePhase = ChordBuilderPhase.PAUSED) } }
    fun resumeGame() { _state.update { it.copy(gamePhase = ChordBuilderPhase.PLAYING) } }
    fun backToLevelSelect() { _state.update { it.copy(gamePhase = ChordBuilderPhase.LEVEL_SELECT) } }
    fun restartLevel(userId: String) { _state.value.currentLevel?.let { startLevel(userId, it) } }
}

data class ChordBuilderState(
    val isLoading: Boolean = false, val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(), val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0, val gamePhase: ChordBuilderPhase = ChordBuilderPhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null, val totalRounds: Int = 6, val currentRound: Int = 0,
    val enabledChordTypes: List<String> = emptyList(),
    val currentChordName: String = "", val rootNote: Int = 0, val chordType: String = "",
    val correctNotes: Set<Int> = emptySet(), val selectedNotes: Set<Int> = emptySet(),
    val isPlayingChord: Boolean = false, val roundResult: RoundResult? = null,
    val score: Int = 0, val combo: Int = 0, val maxCombo: Int = 0,
    val correctCount: Int = 0, val wrongCount: Int = 0,
    val stars: Int = 0, val xpEarned: Int = 0, val coinsEarned: Int = 0
)

enum class ChordBuilderPhase { LEVEL_SELECT, PLAYING, PAUSED, RESULT }
