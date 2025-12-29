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
import javax.inject.Inject
import kotlin.random.Random
import com.musimind.music.audio.GameAudioManager

/**
 * ViewModel para o jogo Melody Memory (Memória Melódica)
 * 
 * Versão musical do "Simon Says". Uma melodia é tocada e o jogador
 * deve reproduzí-la no teclado virtual. Reforça memória auditiva,
 * reconhecimento de padrões melódicos e altura das notas.
 * 
 * Este jogo aproveita a funcionalidade de percepção melódica
 * existente no app.
 */
@HiltViewModel
class MelodyMemoryViewModel @Inject constructor(
    private val gamesRepository: GamesRepository,
    private val audioManager: GameAudioManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(MelodyMemoryState())
    val state: StateFlow<MelodyMemoryState> = _state.asStateFlow()
    
    private var sessionId: String? = null
    
    // Mapeamento de notas para MIDI
    private val noteToMidi = mapOf(
        "C3" to 48, "D3" to 50, "E3" to 52, "F3" to 53, "G3" to 55, "A3" to 57, "B3" to 59,
        "C4" to 60, "D4" to 62, "E4" to 64, "F4" to 65, "G4" to 67, "A4" to 69, "B4" to 71,
        "C5" to 72, "D5" to 74, "E5" to 76, "F5" to 77, "G5" to 79, "A5" to 81, "B5" to 83,
        "C6" to 84
    )
    
    private val midiToNote = noteToMidi.entries.associate { (k, v) -> v to k }
    
    /**
     * Carrega os níveis do jogo
     */
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "melody_memory" }
            
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
            val config = level.config
            val notesCount = config?.get("notes_count")?.jsonPrimitive?.int ?: 4
            val range = config?.get("range")?.jsonArray?.map { it.jsonPrimitive.content } 
                ?: listOf("C4", "C5")
            val tempo = config?.get("tempo")?.jsonPrimitive?.int ?: 70
            val lives = config?.get("lives")?.jsonPrimitive?.int ?: 5
            
            // Gerar notas disponíveis baseado no range
            val minMidi = noteToMidi[range.firstOrNull()] ?: 60
            val maxMidi = noteToMidi[range.lastOrNull()] ?: 72
            val availableNotes = noteToMidi.filter { it.value in minMidi..maxMidi }.keys.toList()
            
            _state.update { 
                it.copy(
                    currentLevel = level,
                    gamePhase = MelodyGamePhase.PLAYING,
                    notesCount = notesCount,
                    tempo = tempo,
                    livesRemaining = lives,
                    maxLives = lives,
                    availableNotes = availableNotes,
                    currentMelody = emptyList(),
                    playerInput = emptyList(),
                    round = 0,
                    score = 0,
                    combo = 0,
                    maxCombo = 0,
                    correctCount = 0,
                    wrongCount = 0
                )
            }
            
            // Criar sessão
            val session = gamesRepository.startGameSession(
                userId = userId,
                gameTypeId = _state.value.gameType?.id ?: "",
                gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
            
            // Começar primeira rodada
            startNextRound()
        }
    }
    
    /**
     * Inicia próxima rodada (adiciona uma nota à melodia)
     */
    private fun startNextRound() {
        val currentState = _state.value
        
        // Gerar nova nota aleatória
        val newNote = currentState.availableNotes.random()
        val newMelody = currentState.currentMelody + newNote
        
        _state.update { 
            it.copy(
                round = it.round + 1,
                currentMelody = newMelody,
                playerInput = emptyList(),
                roundPhase = MelodyRoundPhase.LISTENING,
                highlightedNote = null
            )
        }
        
        // Tocar a melodia completa
        playMelody(newMelody)
    }
    
    /**
     * Toca a melodia para o jogador
     */
    private fun playMelody(melody: List<String>) {
        viewModelScope.launch {
            val noteDelay = (60000L / _state.value.tempo) // Duração de cada nota baseada no BPM
            
            delay(500) // Pausa antes de começar
            
            for (note in melody) {
                _state.update { it.copy(highlightedNote = note) }
                
                // Tocar o som da nota usando audioManager
                audioManager.playNote(note, noteDelay)
                
                delay(noteDelay)
                
                _state.update { it.copy(highlightedNote = null) }
                
                delay(100) // Pequena pausa entre notas
            }
            
            // Agora é a vez do jogador
            delay(300)
            _state.update { 
                it.copy(
                    roundPhase = MelodyRoundPhase.YOUR_TURN,
                    playerInput = emptyList()
                )
            }
        }
    }
    
    /**
     * Jogador tocou uma nota no teclado
     */
    fun onNotePressed(note: String) {
        if (_state.value.roundPhase != MelodyRoundPhase.YOUR_TURN) return
        
        val currentState = _state.value
        val newInput = currentState.playerInput + note
        val expectedNote = currentState.currentMelody.getOrNull(newInput.size - 1)
        
        // Verificar se a nota está correta
        val isCorrect = note == expectedNote
        
        _state.update { 
            it.copy(
                playerInput = newInput,
                highlightedNote = note,
                lastNoteCorrect = isCorrect
            )
        }
        
        // Limpar highlight após um momento
        viewModelScope.launch {
            delay(200)
            _state.update { it.copy(highlightedNote = null, lastNoteCorrect = null) }
        }
        
        if (!isCorrect) {
            // Errou!
            handleWrongAnswer()
        } else if (newInput.size == currentState.currentMelody.size) {
            // Completou a melodia corretamente!
            handleCorrectCompletion()
        }
    }
    
    /**
     * Trata resposta errada
     */
    private fun handleWrongAnswer() {
        viewModelScope.launch {
            val newLives = _state.value.livesRemaining - 1
            
            _state.update { 
                it.copy(
                    livesRemaining = newLives,
                    combo = 0,
                    wrongCount = it.wrongCount + 1,
                    roundPhase = MelodyRoundPhase.FEEDBACK,
                    feedbackMessage = "❌ Errou!"
                )
            }
            
            delay(1500)
            
            if (newLives <= 0) {
                endGame()
            } else {
                // Repetir a mesma rodada
                _state.update { it.copy(feedbackMessage = null) }
                playMelody(_state.value.currentMelody)
            }
        }
    }
    
    /**
     * Trata conclusão correta da rodada
     */
    private fun handleCorrectCompletion() {
        viewModelScope.launch {
            val currentMelodySize = _state.value.currentMelody.size
            val roundScore = currentMelodySize * 100 + (_state.value.combo * 10)
            val newCombo = _state.value.combo + 1
            
            _state.update { 
                it.copy(
                    score = it.score + roundScore,
                    combo = newCombo,
                    maxCombo = maxOf(it.maxCombo, newCombo),
                    correctCount = it.correctCount + currentMelodySize,
                    roundPhase = MelodyRoundPhase.FEEDBACK,
                    feedbackMessage = "✅ Perfeito!"
                )
            }
            
            delay(1000)
            
            // Verificar se completou o nível
            if (_state.value.currentMelody.size >= _state.value.notesCount) {
                endGame()
            } else {
                _state.update { it.copy(feedbackMessage = null) }
                startNextRound()
            }
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
                    gamePhase = MelodyGamePhase.RESULT,
                    stars = result?.stars ?: calculateLocalStars(),
                    xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    private fun calculateLocalStars(): Int {
        val state = _state.value
        val melodySize = state.currentMelody.size
        val targetSize = state.notesCount
        
        return when {
            state.livesRemaining == state.maxLives && melodySize >= targetSize -> 3
            melodySize >= targetSize -> 2
            melodySize >= targetSize / 2 -> 1
            else -> 0
        }
    }
    
    fun pauseGame() {
        _state.update { it.copy(gamePhase = MelodyGamePhase.PAUSED) }
    }
    
    fun resumeGame() {
        _state.update { it.copy(gamePhase = MelodyGamePhase.PLAYING) }
        playMelody(_state.value.currentMelody)
    }
    
    fun backToLevelSelect() {
        _state.update { 
            it.copy(
                gamePhase = MelodyGamePhase.LEVEL_SELECT,
                currentLevel = null,
                currentMelody = emptyList()
            )
        }
    }
    
    fun restartLevel(userId: String) {
        val level = _state.value.currentLevel ?: return
        startLevel(userId, level)
    }
    
    /**
     * Repete a melodia atual (helper para o jogador)
     */
    fun repeatMelody() {
        if (_state.value.roundPhase == MelodyRoundPhase.YOUR_TURN) {
            _state.update { 
                it.copy(
                    playerInput = emptyList(),
                    roundPhase = MelodyRoundPhase.LISTENING
                )
            }
            playMelody(_state.value.currentMelody)
        }
    }
    
    /**
     * Obtém nome de exibição da nota
     */
    fun getNoteDisplayName(note: String): String {
        val letter = note.first()
        return when (letter) {
            'C' -> "Dó"
            'D' -> "Ré"
            'E' -> "Mi"
            'F' -> "Fá"
            'G' -> "Sol"
            'A' -> "Lá"
            'B' -> "Si"
            else -> note
        }
    }
    
    /**
     * Retorna as teclas do teclado virtual
     */
    fun getKeyboardKeys(): List<String> {
        // Teclas de uma oitava (C4 a B4)
        return listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4")
    }
}

/**
 * Estado do jogo Melody Memory
 */
data class MelodyMemoryState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Dados do jogo
    val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(),
    val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0,
    
    // Estado atual
    val gamePhase: MelodyGamePhase = MelodyGamePhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null,
    
    // Configurações
    val notesCount: Int = 4,
    val tempo: Int = 70,
    val maxLives: Int = 5,
    val availableNotes: List<String> = emptyList(),
    
    // Rodada atual
    val round: Int = 0,
    val roundPhase: MelodyRoundPhase = MelodyRoundPhase.LISTENING,
    val currentMelody: List<String> = emptyList(),
    val playerInput: List<String> = emptyList(),
    val highlightedNote: String? = null,
    val lastNoteCorrect: Boolean? = null,
    val feedbackMessage: String? = null,
    
    // Status
    val livesRemaining: Int = 5,
    
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

enum class MelodyGamePhase {
    LEVEL_SELECT,
    PLAYING,
    PAUSED,
    RESULT
}

enum class MelodyRoundPhase {
    LISTENING,  // Ouvindo a melodia
    YOUR_TURN,  // Reproduzindo
    FEEDBACK    // Mostrando resultado
}
