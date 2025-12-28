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

/**
 * ViewModel para o jogo Note Catcher (Caça-Notas)
 * 
 * O jogador deve identificar notas musicais que "caem" na pauta,
 * selecionando o nome correto antes que elas saiam da tela.
 * 
 * Este jogo reforça a leitura de notas na pauta e posição das notas
 * nas linhas e espaços.
 */
@HiltViewModel
class NoteCatcherViewModel @Inject constructor(
    private val gamesRepository: GamesRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(NoteCatcherState())
    val state: StateFlow<NoteCatcherState> = _state.asStateFlow()
    
    private var gameLoopJob: Job? = null
    private var sessionId: String? = null
    
    // Mapeamento de notas para MIDI
    private val noteToMidi = mapOf(
        "C2" to 36, "D2" to 38, "E2" to 40, "F2" to 41, "G2" to 43, "A2" to 45, "B2" to 47,
        "C3" to 48, "D3" to 50, "E3" to 52, "F3" to 53, "G3" to 55, "A3" to 57, "B3" to 59,
        "C4" to 60, "D4" to 62, "E4" to 64, "F4" to 65, "G4" to 67, "A4" to 69, "B4" to 71,
        "C5" to 72, "D5" to 74, "E5" to 76, "F5" to 77, "G5" to 79, "A5" to 81, "B5" to 83,
        "C6" to 84, "D6" to 86, "E6" to 88
    )
    
    // Nomes de notas para exibição
    private val noteDisplayNames = mapOf(
        "C" to "Dó", "D" to "Ré", "E" to "Mi", "F" to "Fá", "G" to "Sol", "A" to "Lá", "B" to "Si"
    )
    
    /**
     * Carrega os níveis do jogo
     */
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Buscar tipo do jogo
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "note_catcher" }
            
            if (gameType != null) {
                // Carregar progresso
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
     * Inicia um nível específico
     */
    fun startLevel(userId: String, level: GameLevel) {
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    currentLevel = level,
                    gamePhase = GamePhase.PLAYING,
                    score = 0,
                    combo = 0,
                    maxCombo = 0,
                    correctCount = 0,
                    wrongCount = 0,
                    fallingNotes = emptyList(),
                    remainingNotes = 0,
                    timeRemaining = level.timeLimitSeconds ?: 0
                )
            }
            
            // Criar sessão no servidor
            val session = gamesRepository.startGameSession(
                userId = userId,
                gameTypeId = _state.value.gameType?.id ?: "",
                gameLevelId = level.id
            ).getOrNull()
            
            sessionId = session?.id
            
            // Extrair configurações do nível
            val config = level.config
            val notes = config?.get("notes")?.jsonArray?.map { it.jsonPrimitive.content } ?: listOf("C4", "D4", "E4")
            val speed = config?.get("speed")?.jsonPrimitive?.content?.toFloatOrNull() ?: 1.0f
            val notesCount = config?.get("notes_count")?.jsonPrimitive?.int ?: 10
            
            _state.update { 
                it.copy(
                    availableNotes = notes,
                    noteSpeed = speed,
                    remainingNotes = notesCount,
                    totalNotesInLevel = notesCount
                )
            }
            
            // Iniciar loop do jogo
            startGameLoop()
        }
    }
    
    /**
     * Loop principal do jogo
     */
    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            val state = _state.value
            val spawnInterval = (2000 / state.noteSpeed).toLong()
            var lastSpawnTime = 0L
            var noteIndex = 0
            
            // Timer countdown
            val startTime = System.currentTimeMillis()
            val timeLimit = (state.currentLevel?.timeLimitSeconds ?: 60) * 1000L
            
            while (_state.value.gamePhase == GamePhase.PLAYING) {
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - startTime
                
                // Atualizar tempo restante
                if (timeLimit > 0) {
                    val remaining = ((timeLimit - elapsed) / 1000).toInt().coerceAtLeast(0)
                    _state.update { it.copy(timeRemaining = remaining) }
                    
                    if (remaining <= 0) {
                        endGame()
                        return@launch
                    }
                }
                
                // Spawnar nova nota
                if (currentTime - lastSpawnTime >= spawnInterval && noteIndex < _state.value.totalNotesInLevel) {
                    spawnNote()
                    noteIndex++
                    lastSpawnTime = currentTime
                }
                
                // Mover notas
                updateNotePositions()
                
                // Verificar notas que saíram da tela
                checkMissedNotes()
                
                // Verificar se acabou
                if (noteIndex >= _state.value.totalNotesInLevel && _state.value.fallingNotes.isEmpty()) {
                    endGame()
                    return@launch
                }
                
                delay(16) // ~60 FPS
            }
        }
    }
    
    /**
     * Spawna uma nova nota caindo
     */
    private fun spawnNote() {
        val notes = _state.value.availableNotes
        if (notes.isEmpty()) return
        
        val randomNote = notes.random()
        val midiNumber = noteToMidi[randomNote] ?: 60
        
        val newNote = FallingNote(
            id = UUID.randomUUID().toString(),
            noteName = randomNote,
            midiNumber = midiNumber,
            yPosition = -50f // Começa acima da tela
        )
        
        _state.update { 
            it.copy(
                fallingNotes = it.fallingNotes + newNote,
                remainingNotes = it.remainingNotes - 1
            )
        }
    }
    
    /**
     * Atualiza posição das notas
     */
    private fun updateNotePositions() {
        val speed = _state.value.noteSpeed * 2f // Pixels por frame
        
        _state.update { state ->
            state.copy(
                fallingNotes = state.fallingNotes.map { note ->
                    note.copy(yPosition = note.yPosition + speed)
                }
            )
        }
    }
    
    /**
     * Verifica notas que saíram da tela sem resposta
     */
    private fun checkMissedNotes() {
        val screenHeight = 800f // Altura da área de jogo
        
        val missedNotes = _state.value.fallingNotes.filter { 
            it.yPosition > screenHeight && it.isCorrect == null
        }
        
        if (missedNotes.isNotEmpty()) {
            _state.update { state ->
                state.copy(
                    fallingNotes = state.fallingNotes.filter { it !in missedNotes },
                    wrongCount = state.wrongCount + missedNotes.size,
                    combo = 0 // Reset combo
                )
            }
        }
    }
    
    /**
     * Jogador selecionou uma resposta
     */
    fun selectAnswer(selectedNoteName: String) {
        val currentNotes = _state.value.fallingNotes.filter { it.isCorrect == null }
        if (currentNotes.isEmpty()) return
        
        // Pegar a nota mais baixa (mais próxima de sair)
        val targetNote = currentNotes.maxByOrNull { it.yPosition } ?: return
        
        // Verificar se a resposta está correta
        val targetBaseNote = targetNote.noteName.first().toString()
        val selectedBaseNote = selectedNoteName.first().toString()
        
        val isCorrect = targetBaseNote == selectedBaseNote
        
        if (isCorrect) {
            // Resposta correta
            val newCombo = _state.value.combo + 1
            val points = 100 + (newCombo * 10) // Bonus por combo
            
            _state.update { state ->
                state.copy(
                    fallingNotes = state.fallingNotes.map { 
                        if (it.id == targetNote.id) it.copy(isCorrect = true) else it
                    },
                    score = state.score + points,
                    combo = newCombo,
                    maxCombo = maxOf(state.maxCombo, newCombo),
                    correctCount = state.correctCount + 1,
                    lastFeedback = FeedbackType.CORRECT
                )
            }
            
            // Remover nota após animação
            viewModelScope.launch {
                delay(300)
                _state.update { state ->
                    state.copy(
                        fallingNotes = state.fallingNotes.filter { it.id != targetNote.id }
                    )
                }
            }
        } else {
            // Resposta errada
            _state.update { state ->
                state.copy(
                    fallingNotes = state.fallingNotes.map { 
                        if (it.id == targetNote.id) it.copy(isCorrect = false) else it
                    },
                    combo = 0,
                    wrongCount = state.wrongCount + 1,
                    lastFeedback = FeedbackType.WRONG
                )
            }
            
            // Remover nota após animação
            viewModelScope.launch {
                delay(300)
                _state.update { state ->
                    state.copy(
                        fallingNotes = state.fallingNotes.filter { it.id != targetNote.id }
                    )
                }
            }
        }
        
        // Limpar feedback após um tempo
        viewModelScope.launch {
            delay(500)
            _state.update { it.copy(lastFeedback = null) }
        }
    }
    
    /**
     * Finaliza o jogo e calcula resultado
     */
    private fun endGame() {
        gameLoopJob?.cancel()
        
        viewModelScope.launch {
            val currentState = _state.value
            
            // Enviar resultado para o servidor
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
                    gamePhase = GamePhase.RESULT,
                    stars = result?.stars ?: calculateLocalStars(),
                    xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    /**
     * Calcula estrelas localmente (fallback)
     */
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
    
    /**
     * Pausa o jogo
     */
    fun pauseGame() {
        gameLoopJob?.cancel()
        _state.update { it.copy(gamePhase = GamePhase.PAUSED) }
    }
    
    /**
     * Resume o jogo
     */
    fun resumeGame() {
        _state.update { it.copy(gamePhase = GamePhase.PLAYING) }
        startGameLoop()
    }
    
    /**
     * Volta para seleção de níveis
     */
    fun backToLevelSelect() {
        gameLoopJob?.cancel()
        _state.update { 
            it.copy(
                gamePhase = GamePhase.LEVEL_SELECT,
                currentLevel = null,
                fallingNotes = emptyList()
            )
        }
    }
    
    /**
     * Reinicia o nível atual
     */
    fun restartLevel(userId: String) {
        val level = _state.value.currentLevel ?: return
        startLevel(userId, level)
    }
    
    /**
     * Obtém nome de exibição da nota
     */
    fun getNoteDisplayName(noteName: String): String {
        val letter = noteName.first().toString()
        return noteDisplayNames[letter] ?: letter
    }
    
    /**
     * Obtém lista de opções de resposta para a UI
     */
    fun getAnswerOptions(): List<String> {
        return listOf("Dó", "Ré", "Mi", "Fá", "Sol", "Lá", "Si")
    }
    
    /**
     * Converte nome de exibição para letra da nota
     */
    fun displayNameToNote(displayName: String): String {
        return when (displayName) {
            "Dó" -> "C"
            "Ré" -> "D"
            "Mi" -> "E"
            "Fá" -> "F"
            "Sol" -> "G"
            "Lá" -> "A"
            "Si" -> "B"
            else -> displayName
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}

/**
 * Estado do jogo Note Catcher
 */
data class NoteCatcherState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Dados do jogo
    val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(),
    val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0,
    
    // Estado atual
    val gamePhase: GamePhase = GamePhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null,
    
    // Configurações
    val availableNotes: List<String> = emptyList(),
    val noteSpeed: Float = 1.0f,
    val totalNotesInLevel: Int = 10,
    
    // Gameplay
    val fallingNotes: List<FallingNote> = emptyList(),
    val remainingNotes: Int = 0,
    val timeRemaining: Int = 0,
    
    // Pontuação
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    
    // Feedback
    val lastFeedback: FeedbackType? = null,
    
    // Resultado
    val stars: Int = 0,
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0
)

enum class GamePhase {
    LEVEL_SELECT,
    PLAYING,
    PAUSED,
    RESULT
}

enum class FeedbackType {
    CORRECT,
    WRONG
}
