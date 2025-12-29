package com.musimind.presentation.games

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.domain.games.*
import com.musimind.music.audio.core.*
import com.musimind.music.audio.engine.SolfegeAudioEngine
import com.musimind.music.audio.midi.MidiPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * ViewModel para o jogo Solfege Sing (Cante o Solfejo)
 * 
 * O app toca uma melodia e o jogador canta. O microfone detecta
 * a afinação e o ritmo usando o SolfegeAudioEngine existente.
 * 
 * INTEGRADO com o sistema de detecção de pitch real via SolfegeAudioEngine!
 */
@HiltViewModel
class SolfegeSingViewModel @Inject constructor(
    private val gamesRepository: GamesRepository,
    private val midiPlayer: MidiPlayer,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(SolfegeSingState())
    val state: StateFlow<SolfegeSingState> = _state.asStateFlow()
    
    // Engine de áudio com detecção de pitch real
    private val audioEngine = SolfegeAudioEngine(context, midiPlayer)
    
    // Expose feedback state from audio engine for real-time pitch visualization
    val audioFeedbackState: StateFlow<SolfegeFeedbackState> = audioEngine.feedbackState
    
    private var sessionId: String? = null
    
    // Melodias simples para cantar (graus da escala -> MIDI base C4)
    private val easyMelodies = listOf(
        listOf(60, 62, 64, 62, 60),           // Dó Ré Mi Ré Dó
        listOf(60, 64, 67, 64, 60),           // Dó Mi Sol Mi Dó
        listOf(67, 65, 64, 62, 60),           // Sol Fá Mi Ré Dó
        listOf(60, 62, 64, 65, 67, 65, 64, 62, 60) // Escala subindo e descendo
    )
    
    private val mediumMelodies = listOf(
        listOf(60, 64, 67, 72, 67, 64, 60),   // Dó Mi Sol Dó' Sol Mi Dó
        listOf(60, 67, 64, 72, 67, 60),       // Dó Sol Mi Dó' Sol Dó
        listOf(72, 71, 69, 67, 65, 64, 62, 60) // Dó' Si Lá Sol Fá Mi Ré Dó
    )
    
    private val hardMelodies = listOf(
        listOf(60, 62, 64, 65, 67, 69, 71, 72, 71, 69, 67, 65, 64, 62, 60),
        listOf(60, 64, 62, 65, 64, 67, 65, 69, 67, 71, 69, 72)
    )
    
    private val midiToSolfege = mapOf(
        60 to "Dó", 62 to "Ré", 64 to "Mi", 65 to "Fá",
        67 to "Sol", 69 to "Lá", 71 to "Si", 72 to "Dó"
    )
    
    init {
        // Observe audio engine feedback and update state
        viewModelScope.launch {
            audioEngine.feedbackState.collect { feedback ->
                updateStateFromFeedback(feedback)
            }
        }
    }
    
    private fun updateStateFromFeedback(feedback: SolfegeFeedbackState) {
        _state.update { state ->
            // Calculate accuracy from note feedbacks
            val correctNotes = feedback.noteFeedbacks.count { 
                it.pitchStatus == PitchStatus.CORRECT 
            }
            val totalNotes = feedback.noteFeedbacks.size
            val accuracy = if (totalNotes > 0) correctNotes.toFloat() / totalNotes else 0f
            
            state.copy(
                currentNoteIndex = feedback.currentNoteIndex,
                isPlayingMelody = feedback.phase == SolfegePhase.PLAYING,
                isRecording = feedback.phase == SolfegePhase.LISTENING,
                detectedPitchHz = if (feedback.isVoiceDetected) feedback.currentPitchHz else null,
                detectedMidiNote = if (feedback.isVoiceDetected) feedback.currentMidiNote else null,
                centDeviation = feedback.currentCentDeviation.toInt(),
                isCurrentPitchCorrect = feedback.isCurrentPitchCorrect,
                realtimeAccuracy = accuracy,
                statusText = when (feedback.phase) {
                    SolfegePhase.IDLE -> "Pronto"
                    SolfegePhase.COUNTDOWN -> "Contando... ${feedback.countdownNumber}"
                    SolfegePhase.PLAYING -> "Ouça a melodia..."
                    SolfegePhase.LISTENING -> "Cante! 🎤"
                    SolfegePhase.COMPLETED -> "Concluído!"
                }
            )
        }
        
        // Check for completion
        if (feedback.phase == SolfegePhase.COMPLETED && _state.value.isRecording) {
            evaluatePerformance()
        }
    }
    
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun loadLevels(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            gamesRepository.loadGameTypes()
            val gameType = gamesRepository.gameTypes.value.find { it.name == "solfege_sing" }
            
            if (gameType != null) {
                val progress = gamesRepository.getGameProgress(userId, gameType.id).getOrNull()
                _state.update { 
                    it.copy(
                        isLoading = false, gameType = gameType,
                        levels = progress?.levels ?: emptyList(),
                        highScores = progress?.highScores ?: emptyMap(),
                        totalStars = progress?.totalStars ?: 0,
                        hasPermission = hasPermission()
                    ) 
                }
            }
        }
    }
    
    fun startLevel(userId: String, level: GameLevel) {
        viewModelScope.launch {
            val config = level.config
            val difficulty = config?.get("difficulty")?.jsonPrimitive?.int ?: 1
            val rounds = config?.get("rounds")?.jsonPrimitive?.int ?: 5
            
            val melodies = when (difficulty) {
                1 -> easyMelodies
                2 -> mediumMelodies
                else -> hardMelodies
            }
            
            _state.update { 
                it.copy(
                    currentLevel = level, gamePhase = SolfegeSingPhase.PLAYING,
                    difficulty = difficulty, totalRounds = rounds, currentRound = 0,
                    availableMelodies = melodies, score = 0, combo = 0, maxCombo = 0,
                    correctCount = 0, wrongCount = 0
                )
            }
            
            val session = gamesRepository.startGameSession(
                userId = userId, gameTypeId = _state.value.gameType?.id ?: "", gameLevelId = level.id
            ).getOrNull()
            sessionId = session?.id
            
            nextMelody()
        }
    }
    
    private fun nextMelody() {
        val currentState = _state.value
        
        if (currentState.currentRound >= currentState.totalRounds) {
            endGame()
            return
        }
        
        val melody = currentState.availableMelodies.random()
        val solfegeNotation = melody.map { midiToSolfege[it] ?: midiToSolfege[it % 12 + 60] ?: "?" }
        
        _state.update { 
            it.copy(
                currentRound = it.currentRound + 1,
                currentMelody = melody,
                currentSolfege = solfegeNotation,
                melodyPhase = MelodySingPhase.LISTENING,
                detectedNotes = emptyList(),
                accuracy = 0f,
                statusText = "Ouça a melodia..."
            )
        }
        
        playMelody(melody)
    }
    
    private fun playMelody(melody: List<Int>) {
        viewModelScope.launch {
            _state.update { it.copy(isPlayingMelody = true, currentNoteIndex = -1) }
            
            // Converter melodia para ExpectedNotes
            val expectedNotes = melody.mapIndexed { index, midiNote ->
                ExpectedNote(
                    id = "note_$index",
                    midiNote = midiNote,
                    startBeat = index.toDouble(),
                    durationBeats = 1.0,
                    solfegeName = midiToSolfege[midiNote] ?: "?"
                )
            }
            
            // Configurar engine para tocar a melodia
            audioEngine.configure(
                notes = expectedNotes,
                tempo = 80.0,
                timeSignatureNumerator = 4,
                timeSignatureDenominator = 4
            )
            
            // Tocar com piano (modo playback)
            audioEngine.startPlayback(playPiano = true)
            
            // Esperar a melodia terminar
            val melodyDurationMs = (melody.size * 60000 / 80) + 2000 // tempo + margem
            delay(melodyDurationMs.toLong())
            
            _state.update { 
                it.copy(
                    isPlayingMelody = false, 
                    currentNoteIndex = -1,
                    melodyPhase = MelodySingPhase.SINGING,
                    statusText = "Agora é sua vez! 🎤"
                )
            }
        }
    }
    
    fun startRecording() {
        if (!hasPermission()) {
            _state.update { it.copy(error = "Permissão de microfone necessária") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isRecording = true, detectedNotes = emptyList()) }
            
            // Converter melodia para ExpectedNotes
            val melody = _state.value.currentMelody
            val expectedNotes = melody.mapIndexed { index, midiNote ->
                ExpectedNote(
                    id = "note_$index",
                    midiNote = midiNote,
                    startBeat = index.toDouble(),
                    durationBeats = 1.0,
                    solfegeName = midiToSolfege[midiNote] ?: "?"
                )
            }
            
            // Configurar engine para modo listening (sem piano)
            audioEngine.configure(
                notes = expectedNotes,
                tempo = 80.0,
                timeSignatureNumerator = 4,
                timeSignatureDenominator = 4
            )
            
            // Iniciar escuta com detecção de pitch REAL
            val success = audioEngine.startListening()
            
            if (!success) {
                _state.update { 
                    it.copy(
                        isRecording = false,
                        error = "Não foi possível iniciar o microfone"
                    )
                }
            }
        }
    }
    
    fun stopRecording() {
        audioEngine.stop()
        _state.update { it.copy(isRecording = false) }
        evaluatePerformance()
    }
    
    private fun evaluatePerformance() {
        // Obter feedback do engine
        val feedback = audioEngine.feedbackState.value
        
        // Calcular accuracy baseado nos feedbacks reais
        val noteFeedbacks = feedback.noteFeedbacks
        val correctNotes = noteFeedbacks.count { it.pitchStatus == PitchStatus.CORRECT }
        val totalNotes = noteFeedbacks.size.coerceAtLeast(1)
        
        val accuracy = correctNotes.toFloat() / totalNotes
        val points = (accuracy * 300).toInt()
        
        val medal = when {
            accuracy >= 0.95f -> "🥇 Ouro"
            accuracy >= 0.85f -> "🥈 Prata"
            accuracy >= 0.75f -> "🥉 Bronze"
            else -> null
        }
        
        val isSuccess = accuracy >= 0.60f
        
        viewModelScope.launch {
            val newCombo = if (isSuccess) _state.value.combo + 1 else 0
            
            _state.update { 
                it.copy(
                    accuracy = accuracy, medal = medal,
                    score = it.score + points, combo = newCombo,
                    maxCombo = maxOf(it.maxCombo, newCombo),
                    correctCount = it.correctCount + if (isSuccess) 1 else 0,
                    wrongCount = it.wrongCount + if (!isSuccess) 1 else 0,
                    melodyPhase = MelodySingPhase.RESULT,
                    statusText = if (isSuccess) "Muito bem! 🎉" else "Tente novamente 💪"
                )
            }
            
            delay(2500)
            nextMelody()
        }
    }
    
    fun replayMelody() {
        playMelody(_state.value.currentMelody)
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
                    gamePhase = SolfegeSingPhase.RESULT,
                    stars = result?.stars ?: 2, xpEarned = result?.xpEarned ?: 0,
                    coinsEarned = result?.coinsEarned ?: 0
                )
            }
        }
    }
    
    fun pauseGame() { 
        audioEngine.stop()
        _state.update { it.copy(gamePhase = SolfegeSingPhase.PAUSED) }
    }
    
    fun resumeGame() { 
        _state.update { it.copy(gamePhase = SolfegeSingPhase.PLAYING) }
    }
    
    fun backToLevelSelect() { 
        audioEngine.stop()
        _state.update { it.copy(gamePhase = SolfegeSingPhase.LEVEL_SELECT) }
    }
    
    fun restartLevel(userId: String) { 
        _state.value.currentLevel?.let { startLevel(userId, it) }
    }
    
    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}

data class SolfegeSingState(
    val isLoading: Boolean = false, 
    val gameType: GameType? = null,
    val levels: List<GameLevel> = emptyList(), 
    val highScores: Map<String, GameHighScore> = emptyMap(),
    val totalStars: Int = 0, 
    val gamePhase: SolfegeSingPhase = SolfegeSingPhase.LEVEL_SELECT,
    val currentLevel: GameLevel? = null, 
    val difficulty: Int = 1,
    val totalRounds: Int = 5, 
    val currentRound: Int = 0,
    val availableMelodies: List<List<Int>> = emptyList(),
    val currentMelody: List<Int> = emptyList(), 
    val currentSolfege: List<String> = emptyList(),
    val melodyPhase: MelodySingPhase = MelodySingPhase.LISTENING,
    val isPlayingMelody: Boolean = false, 
    val currentNoteIndex: Int = -1,
    val isRecording: Boolean = false, 
    val detectedNotes: List<Int> = emptyList(),
    val accuracy: Float = 0f, 
    val medal: String? = null,
    val score: Int = 0, 
    val combo: Int = 0, 
    val maxCombo: Int = 0,
    val correctCount: Int = 0, 
    val wrongCount: Int = 0,
    val stars: Int = 0, 
    val xpEarned: Int = 0, 
    val coinsEarned: Int = 0,
    // Real-time pitch detection state
    val hasPermission: Boolean = false,
    val detectedPitchHz: Float? = null,
    val detectedMidiNote: Int? = null,
    val centDeviation: Int = 0,
    val isCurrentPitchCorrect: Boolean = false,
    val realtimeAccuracy: Float = 0f,
    val statusText: String = "Pronto",
    val error: String? = null
)

enum class SolfegeSingPhase { LEVEL_SELECT, PLAYING, PAUSED, RESULT }
enum class MelodySingPhase { LISTENING, SINGING, RESULT }
