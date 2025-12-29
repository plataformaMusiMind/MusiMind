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
 * ViewModel para o jogo Chord Match (Match de Acordes)
 * 
 * Jogo estilo memory game onde o jogador vira cartas para encontrar pares:
 * som do acorde + símbolo/nome do acorde.
 */
@HiltViewModel
class ChordMatchViewModel @Inject constructor(
    private val gamesRepository: GamesRepository,
    private val audioManager: GameAudioManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(ChordMatchState())
    val state: StateFlow<ChordMatchState> = _state.asStateFlow()
    
    private var sessionId: String? = null
    
    private val chords = listOf(
        ChordInfo("C", "Dó Maior", "major", "🎹"),
        ChordInfo("Cm", "Dó Menor", "minor", "🎹"),
        ChordInfo("G", "Sol Maior", "major", "🎸"),
        ChordInfo("Gm", "Sol Menor", "minor", "🎸"),
        ChordInfo("Am", "Lá Menor", "minor", "🎵"),
        ChordInfo("F", "Fá Maior", "major", "🎵"),
        ChordInfo("Dm", "Ré Menor", "minor", "🎶"),
        ChordInfo("E", "Mi Maior", "major", "🎶"),
        ChordInfo("Em", "Mi Menor", "minor", "🎼"),
        ChordInfo("D", "Ré Maior", "major", "🎼"),
        ChordInfo("A", "Lá Maior", "major", "🎺"),
        ChordInfo("Bm", "Si Menor", "minor", "🎺")
    )
    
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "chord_match" }
            
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
            val pairs = config?.get("pairs")?.jsonPrimitive?.int ?: 4
            val timeLimit = config?.get("time_limit")?.jsonPrimitive?.int
            
            // Selecionar acordes para este jogo
            val selectedChords = chords.shuffled().take(pairs)
            
            // Criar cartas (cada acorde tem 2 cartas: uma com nome, outra com som)
            val cards = mutableListOf<MatchCard>()
            var cardId = 0
            selectedChords.forEach { chord ->
                cards.add(MatchCard(cardId++, chord.symbol, CardType.SYMBOL, chord.symbol, false, false))
                cards.add(MatchCard(cardId++, chord.name, CardType.SOUND, chord.symbol, false, false))
            }
            
            _state.update { 
                it.copy(
                    currentLevel = level, gamePhase = ChordMatchPhase.PLAYING,
                    totalPairs = pairs, matchedPairs = 0,
                    cards = cards.shuffled(), timeLimit = timeLimit,
                    firstCard = null, secondCard = null,
                    score = 0, moves = 0, combo = 0, maxCombo = 0,
                    correctCount = 0, wrongCount = 0, isProcessing = false
                )
            }
            
            val session = gamesRepository.startGameSession(
                userId = userId, gameTypeId = _state.value.gameType?.id ?: "", gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
        }
    }
    
    fun flipCard(cardId: Int) {
        val currentState = _state.value
        if (currentState.isProcessing) return
        
        val card = currentState.cards.find { it.id == cardId } ?: return
        if (card.isFlipped || card.isMatched) return
        
        // Virar a carta
        val updatedCards = currentState.cards.map { if (it.id == cardId) it.copy(isFlipped = true) else it }
        
        when {
            currentState.firstCard == null -> {
                // Primeira carta virada
                _state.update { it.copy(cards = updatedCards, firstCard = card.copy(isFlipped = true)) }
                
                if (card.type == CardType.SOUND) {
                    playChordSound(card.matchId)
                }
            }
            currentState.secondCard == null -> {
                // Segunda carta virada
                val first = currentState.firstCard
                _state.update { 
                    it.copy(
                        cards = updatedCards, 
                        secondCard = card.copy(isFlipped = true),
                        moves = it.moves + 1,
                        isProcessing = true
                    )
                }
                
                if (card.type == CardType.SOUND) {
                    playChordSound(card.matchId)
                }
                
                // Verificar match
                checkMatch(first, card.copy(isFlipped = true))
            }
        }
    }
    
    private fun playChordSound(chordSymbol: String) {
        // Tocar o acorde usando audioManager
        audioManager.playChordByName(chordSymbol, octave = 4, durationMs = 800)
    }
    
    private fun checkMatch(first: MatchCard, second: MatchCard) {
        viewModelScope.launch {
            delay(1000)
            
            val isMatch = first.matchId == second.matchId && first.type != second.type
            
            if (isMatch) {
                // Match correto!
                val newCombo = _state.value.combo + 1
                val points = 100 + (newCombo * 25)
                
                val updatedCards = _state.value.cards.map { card ->
                    if (card.id == first.id || card.id == second.id) card.copy(isMatched = true)
                    else card
                }
                
                _state.update { 
                    it.copy(
                        cards = updatedCards, firstCard = null, secondCard = null,
                        matchedPairs = it.matchedPairs + 1, score = it.score + points,
                        combo = newCombo, maxCombo = maxOf(it.maxCombo, newCombo),
                        correctCount = it.correctCount + 1, isProcessing = false
                    )
                }
                
                // Verificar vitória
                if (_state.value.matchedPairs >= _state.value.totalPairs) {
                    endGame()
                }
            } else {
                // Não é match, desvirar cartas
                delay(500)
                
                val updatedCards = _state.value.cards.map { card ->
                    if (card.id == first.id || card.id == second.id) card.copy(isFlipped = false)
                    else card
                }
                
                _state.update { 
                    it.copy(
                        cards = updatedCards, firstCard = null, secondCard = null,
                        combo = 0, wrongCount = it.wrongCount + 1, isProcessing = false
                    )
                }
            }
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
                    gamePhase = ChordMatchPhase.RESULT,
                    stars = result?.stars ?: 2, xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    fun pauseGame() { _state.update { it.copy(gamePhase = ChordMatchPhase.PAUSED) } }
    fun resumeGame() { _state.update { it.copy(gamePhase = ChordMatchPhase.PLAYING) } }
    fun backToLevelSelect() { _state.update { it.copy(gamePhase = ChordMatchPhase.LEVEL_SELECT) } }
    fun restartLevel(userId: String) { _state.value.currentLevel?.let { startLevel(userId, it) } }
}

data class ChordInfo(val symbol: String, val name: String, val quality: String, val icon: String)
data class MatchCard(val id: Int, val displayText: String, val type: CardType, val matchId: String, val isFlipped: Boolean, val isMatched: Boolean)
enum class CardType { SYMBOL, SOUND }

data class ChordMatchState(
    val isLoading: Boolean = false, val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(), val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0, val gamePhase: ChordMatchPhase = ChordMatchPhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null, val totalPairs: Int = 4, val matchedPairs: Int = 0,
    val cards: List<MatchCard> = emptyList(), val timeLimit: Int? = null,
    val firstCard: MatchCard? = null, val secondCard: MatchCard? = null,
    val isProcessing: Boolean = false, val moves: Int = 0,
    val score: Int = 0, val combo: Int = 0, val maxCombo: Int = 0,
    val correctCount: Int = 0, val wrongCount: Int = 0,
    val stars: Int = 0, val xpEarned: Int = 0, val coinsEarned: Int = 0
)

enum class ChordMatchPhase { LEVEL_SELECT, PLAYING, PAUSED, RESULT }
