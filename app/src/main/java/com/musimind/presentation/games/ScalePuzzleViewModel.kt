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

/**
 * ViewModel para o jogo Scale Puzzle (Quebra-Cabeça de Escalas)
 * 
 * O jogador deve montar escalas arrastando notas para as posições corretas.
 * Reforça estrutura de escalas, tons e semitons.
 */
@HiltViewModel
class ScalePuzzleViewModel @Inject constructor(
    private val gamesRepository: GamesRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ScalePuzzleState())
    val state: StateFlow<ScalePuzzleState> = _state.asStateFlow()
    
    private var sessionId: String? = null
    
    // Estruturas das escalas (em semitons a partir da tônica)
    private val scaleStructures = mapOf(
        "major" to listOf(0, 2, 4, 5, 7, 9, 11, 12), // T T S T T T S
        "natural_minor" to listOf(0, 2, 3, 5, 7, 8, 10, 12), // T S T T S T T
        "harmonic_minor" to listOf(0, 2, 3, 5, 7, 8, 11, 12), // T S T T S 1.5 S
        "melodic_minor" to listOf(0, 2, 3, 5, 7, 9, 11, 12), // T S T T T T S (ascendente)
        "pentatonic_major" to listOf(0, 2, 4, 7, 9, 12), // Dó Ré Mi Sol Lá Dó
        "pentatonic_minor" to listOf(0, 3, 5, 7, 10, 12) // Lá Dó Ré Mi Sol Lá
    )
    
    private val scaleNames = mapOf(
        "major" to "Escala Maior",
        "natural_minor" to "Menor Natural",
        "harmonic_minor" to "Menor Harmônica",
        "melodic_minor" to "Menor Melódica",
        "pentatonic_major" to "Pentatônica Maior",
        "pentatonic_minor" to "Pentatônica Menor"
    )
    
    private val noteNames = listOf("Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá", "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si")
    
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "scale_puzzle" }
            
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
            val scaleTypes = config?.get("scales")?.jsonArray?.map { it.jsonPrimitive.content }
                ?: listOf("major")
            val rounds = config?.get("rounds")?.jsonPrimitive?.int ?: 5
            val timeLimit = config?.get("time_limit")?.jsonPrimitive?.int
            
            _state.update { 
                it.copy(
                    currentLevel = level, gamePhase = ScalePuzzlePhase.PLAYING,
                    scaleTypes = scaleTypes, totalRounds = rounds, currentRound = 0,
                    timeLimit = timeLimit, score = 0, combo = 0, maxCombo = 0,
                    correctCount = 0, wrongCount = 0
                )
            }
            
            val session = gamesRepository.startGameSession(
                userId = userId, gameTypeId = _state.value.gameType?.id ?: "", gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
            
            nextPuzzle()
        }
    }
    
    private fun nextPuzzle() {
        val currentState = _state.value
        
        if (currentState.currentRound >= currentState.totalRounds) {
            endGame()
            return
        }
        
        // Escolher tipo de escala e tônica
        val scaleType = currentState.scaleTypes.random()
        val tonicIndex = (0..11).random()
        val structure = scaleStructures[scaleType] ?: scaleStructures["major"]!!
        
        // Calcular notas corretas da escala
        val correctNotes = structure.map { semitone ->
            (tonicIndex + semitone) % 12
        }
        
        // Criar peças embaralhadas (todas as 12 notas ou apenas as corretas + algumas extras)
        val availablePieces = (correctNotes + (0..11).toList().shuffled().take(4))
            .distinct()
            .shuffled()
            .mapIndexed { index, noteIndex ->
                PuzzlePiece(id = index, noteIndex = noteIndex, noteName = noteNames[noteIndex])
            }
        
        // Slots vazios para colocar as peças
        val slots = correctNotes.indices.map { index ->
            PuzzleSlot(id = index, expectedNoteIndex = correctNotes[index], currentPiece = null)
        }
        
        _state.update { 
            it.copy(
                currentRound = it.currentRound + 1,
                currentScaleType = scaleType,
                currentTonicIndex = tonicIndex,
                currentTonicName = noteNames[tonicIndex],
                correctNotes = correctNotes,
                availablePieces = availablePieces,
                slots = slots,
                roundComplete = false
            )
        }
    }
    
    fun placePiece(pieceId: Int, slotId: Int) {
        val currentState = _state.value
        val piece = currentState.availablePieces.find { it.id == pieceId } ?: return
        val slot = currentState.slots.find { it.id == slotId } ?: return
        
        // Se o slot já tem uma peça, devolver para disponíveis
        val previousPiece = slot.currentPiece
        val updatedAvailable = if (previousPiece != null) {
            currentState.availablePieces + previousPiece
        } else {
            currentState.availablePieces
        }.filter { it.id != pieceId }
        
        val updatedSlots = currentState.slots.map {
            if (it.id == slotId) it.copy(currentPiece = piece) else it
        }
        
        _state.update { 
            it.copy(availablePieces = updatedAvailable, slots = updatedSlots)
        }
        
        // Verificar se todos os slots estão preenchidos
        checkCompletion()
    }
    
    fun removePiece(slotId: Int) {
        val currentState = _state.value
        val slot = currentState.slots.find { it.id == slotId } ?: return
        val piece = slot.currentPiece ?: return
        
        val updatedSlots = currentState.slots.map {
            if (it.id == slotId) it.copy(currentPiece = null) else it
        }
        val updatedAvailable = currentState.availablePieces + piece
        
        _state.update { 
            it.copy(availablePieces = updatedAvailable, slots = updatedSlots)
        }
    }
    
    private fun checkCompletion() {
        val slots = _state.value.slots
        
        // Verificar se todos os slots estão preenchidos
        if (slots.all { it.currentPiece != null }) {
            // Verificar se está correto
            val isCorrect = slots.all { slot ->
                slot.currentPiece?.noteIndex == slot.expectedNoteIndex
            }
            
            if (isCorrect) {
                handleCorrect()
            } else {
                handleWrong()
            }
        }
    }
    
    private fun handleCorrect() {
        viewModelScope.launch {
            val newCombo = _state.value.combo + 1
            val points = 200 + (newCombo * 50)
            
            _state.update { 
                it.copy(
                    score = it.score + points, combo = newCombo,
                    maxCombo = maxOf(it.maxCombo, newCombo), correctCount = it.correctCount + 1,
                    roundComplete = true, lastRoundCorrect = true
                )
            }
            
            delay(1500)
            nextPuzzle()
        }
    }
    
    private fun handleWrong() {
        viewModelScope.launch {
            _state.update { 
                it.copy(combo = 0, wrongCount = it.wrongCount + 1, lastRoundCorrect = false)
            }
            
            // Destacar erros
            delay(2000)
            
            // Limpar slots errados
            val currentSlots = _state.value.slots
            val piecesToReturn = mutableListOf<PuzzlePiece>()
            val updatedSlots = currentSlots.map { slot ->
                if (slot.currentPiece?.noteIndex != slot.expectedNoteIndex) {
                    slot.currentPiece?.let { piecesToReturn.add(it) }
                    slot.copy(currentPiece = null)
                } else {
                    slot
                }
            }
            
            _state.update { 
                it.copy(
                    slots = updatedSlots,
                    availablePieces = it.availablePieces + piecesToReturn,
                    lastRoundCorrect = null
                )
            }
        }
    }
    
    fun playScale() {
        viewModelScope.launch {
            _state.update { it.copy(isPlayingScale = true) }
            // TODO: Tocar as notas da escala atual
            delay(2000)
            _state.update { it.copy(isPlayingScale = false) }
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
                    gamePhase = ScalePuzzlePhase.RESULT,
                    stars = result?.stars ?: 2, xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    fun getScaleDisplayName(type: String) = scaleNames[type] ?: type
    
    fun pauseGame() { _state.update { it.copy(gamePhase = ScalePuzzlePhase.PAUSED) } }
    fun resumeGame() { _state.update { it.copy(gamePhase = ScalePuzzlePhase.PLAYING) } }
    fun backToLevelSelect() { _state.update { it.copy(gamePhase = ScalePuzzlePhase.LEVEL_SELECT) } }
    fun restartLevel(userId: String) { _state.value.currentLevel?.let { startLevel(userId, it) } }
}

data class PuzzlePiece(val id: Int, val noteIndex: Int, val noteName: String)
data class PuzzleSlot(val id: Int, val expectedNoteIndex: Int, val currentPiece: PuzzlePiece?)

data class ScalePuzzleState(
    val isLoading: Boolean = false, val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(), val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0, val gamePhase: ScalePuzzlePhase = ScalePuzzlePhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null, val scaleTypes: List<String> = emptyList(),
    val totalRounds: Int = 5, val currentRound: Int = 0, val timeLimit: Int? = null,
    val currentScaleType: String = "", val currentTonicIndex: Int = 0, val currentTonicName: String = "",
    val correctNotes: List<Int> = emptyList(), val availablePieces: List<PuzzlePiece> = emptyList(),
    val slots: List<PuzzleSlot> = emptyList(), val isPlayingScale: Boolean = false,
    val roundComplete: Boolean = false, val lastRoundCorrect: Boolean? = null,
    val score: Int = 0, val combo: Int = 0, val maxCombo: Int = 0,
    val correctCount: Int = 0, val wrongCount: Int = 0,
    val stars: Int = 0, val xpEarned: Int = 0, val coinsEarned: Int = 0
)

enum class ScalePuzzlePhase { LEVEL_SELECT, PLAYING, PAUSED, RESULT }
