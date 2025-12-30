package com.musimind.presentation.games.multiplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.domain.auth.AuthManager
import com.musimind.domain.model.*
import com.musimind.music.audio.GameAudioManager
import com.musimind.presentation.games.qr.QRCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Quiz Multiplayer with REAL Supabase Realtime
 * 
 * Features:
 * - Create/Join quiz rooms
 * - Real-time player synchronization via Supabase Realtime
 * - Live score updates
 * - Room persistence in database
 * - Rich questions with audio, score rendering
 * - Kahoot-style timer with speed bonus
 */
@HiltViewModel
class QuizMultiplayerViewModel @Inject constructor(
    private val postgrest: Postgrest,
    private val realtime: Realtime,
    private val authManager: AuthManager,
    private val audioManager: GameAudioManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(QuizMultiplayerState())
    val state: StateFlow<QuizMultiplayerState> = _state.asStateFlow()
    
    private var timerJob: Job? = null
    private var realtimeJob: Job? = null
    private var audioPlaybackJob: Job? = null
    
    // Rich questions with various types
    private var questions: List<RichQuestion> = emptyList()
    
    /**
     * Create a new quiz room
     */
    fun createRoom(difficulty: Int = 1, questionCount: Int = 10) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val roomCode = QRCodeGenerator.generateRoomCode()
                val hostPlayer = QuizPlayer(
                    id = authManager.currentUserId,
                    name = authManager.currentUserEmail?.substringBefore('@') ?: "Jogador",
                    isHost = true,
                    isReady = true
                )
                
                // Generate rich questions
                questions = QuestionGenerator.generateQuizQuestions(
                    count = questionCount,
                    difficulty = difficulty
                )
                
                // Save room to Supabase
                try {
                    postgrest.from("quiz_rooms").insert(
                        mapOf(
                            "code" to roomCode,
                            "host_id" to authManager.currentUserId,
                            "status" to "waiting",
                            "created_at" to java.time.Instant.now().toString()
                        )
                    )
                    
                    // Add host as player
                    postgrest.from("quiz_players").insert(
                        mapOf(
                            "room_code" to roomCode,
                            "user_id" to authManager.currentUserId,
                            "name" to hostPlayer.name,
                            "is_host" to true,
                            "is_ready" to true,
                            "score" to 0
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.w("QuizMultiplayer", "DB insert failed: ${e.message}")
                }
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        roomCode = roomCode,
                        isHost = true,
                        players = listOf(hostPlayer),
                        screenState = QuizScreenState.WAITING_ROOM,
                        difficulty = difficulty
                    )
                }
                
                // Subscribe to realtime
                subscribeToRoom(roomCode)
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(isLoading = false, error = "Erro ao criar sala: ${e.message}") 
                }
            }
        }
    }
    
    /**
     * Subscribe to real-time updates for a room using Supabase Realtime
     * Falls back to polling if Realtime fails
     */
    private fun subscribeToRoom(roomCode: String) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                val channel = realtime.channel("quiz_room_$roomCode")
                
                // Listen for player changes
                val playerChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "quiz_players"
                }
                
                // Listen for room changes
                val roomChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "quiz_rooms"
                }
                
                // Handle player changes
                launch {
                    playerChanges.collect { action ->
                        when (action) {
                            is PostgresAction.Insert, 
                            is PostgresAction.Update,
                            is PostgresAction.Delete -> refreshPlayers(roomCode)
                            else -> { }
                        }
                    }
                }
                
                // Handle room changes
                launch {
                    roomChanges.collect { action ->
                        when (action) {
                            is PostgresAction.Update -> refreshRoomStatus(roomCode)
                            else -> { }
                        }
                    }
                }
                
                // Subscribe to channel
                channel.subscribe()
                
                // Initial load
                refreshPlayers(roomCode)
                
                android.util.Log.d("QuizMultiplayer", "Realtime connected for room: $roomCode")
                
            } catch (e: Exception) {
                android.util.Log.e("QuizMultiplayer", "Realtime error, falling back to polling: ${e.message}")
                startPolling(roomCode)
            }
        }
    }
    
    private fun startPolling(roomCode: String) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            while (true) {
                refreshPlayers(roomCode)
                refreshRoomStatus(roomCode)
                delay(2000)
            }
        }
    }
    
    private suspend fun refreshPlayers(roomCode: String) {
        try {
            val players = postgrest.from("quiz_players")
                .select { filter { eq("room_code", roomCode) } }
                .decodeList<QuizPlayerEntity>()
            
            val updatedPlayers = players.map { entity ->
                QuizPlayer(
                    id = entity.userId,
                    name = entity.name,
                    isHost = entity.isHost,
                    isReady = entity.isReady,
                    score = entity.score
                )
            }
            
            if (updatedPlayers.isNotEmpty()) {
                _state.update { it.copy(players = updatedPlayers) }
            }
        } catch (e: Exception) {
            android.util.Log.w("QuizMultiplayer", "Refresh players failed: ${e.message}")
        }
    }
    
    private suspend fun refreshRoomStatus(roomCode: String) {
        try {
            val room = postgrest.from("quiz_rooms")
                .select { filter { eq("code", roomCode) } }
                .decodeSingleOrNull<QuizRoomEntity>()
            
            room?.let { r ->
                if (r.status == "playing" && _state.value.screenState == QuizScreenState.WAITING_ROOM) {
                    startGameInternal()
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("QuizMultiplayer", "Refresh room failed: ${e.message}")
        }
    }
    
    /**
     * Join an existing quiz room
     */
    fun joinRoom(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                if (code.length != 6) {
                    _state.update { it.copy(isLoading = false, error = "Código inválido") }
                    return@launch
                }
                
                val room = postgrest.from("quiz_rooms")
                    .select {
                        filter { 
                            eq("code", code)
                            eq("status", "waiting")
                        }
                    }
                    .decodeSingleOrNull<QuizRoomEntity>()
                
                if (room == null) {
                    _state.update { 
                        it.copy(isLoading = false, error = "Sala não encontrada ou já iniciou") 
                    }
                    return@launch
                }
                
                val player = QuizPlayer(
                    id = authManager.currentUserId,
                    name = authManager.currentUserEmail?.substringBefore('@') ?: "Jogador",
                    isHost = false,
                    isReady = true
                )
                
                try {
                    postgrest.from("quiz_players").insert(
                        mapOf(
                            "room_code" to code,
                            "user_id" to authManager.currentUserId,
                            "name" to player.name,
                            "is_host" to false,
                            "is_ready" to true,
                            "score" to 0
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.w("QuizMultiplayer", "Join failed: ${e.message}")
                }
                
                // Generate same questions (in production, sync from host)
                questions = QuestionGenerator.generateQuizQuestions(count = 10, difficulty = 1)
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        roomCode = code,
                        isHost = false,
                        screenState = QuizScreenState.WAITING_ROOM
                    )
                }
                
                subscribeToRoom(code)
                
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Sala não encontrada") }
            }
        }
    }
    
    /**
     * Start the game (host only)
     */
    fun startGame() {
        if (!_state.value.isHost) return
        
        viewModelScope.launch {
            try {
                postgrest.from("quiz_rooms").update(
                    mapOf(
                        "status" to "playing",
                        "started_at" to java.time.Instant.now().toString()
                    )
                ) {
                    filter { eq("code", _state.value.roomCode) }
                }
            } catch (e: Exception) {
                android.util.Log.w("QuizMultiplayer", "Start game DB update failed: ${e.message}")
            }
            
            startGameInternal()
        }
    }
    
    private fun startGameInternal() {
        if (questions.isEmpty()) {
            questions = QuestionGenerator.generateQuizQuestions(count = 10, difficulty = _state.value.difficulty)
        }
        
        val firstQuestion = questions.firstOrNull() ?: return
        
        _state.update { 
            it.copy(
                screenState = QuizScreenState.PLAYING,
                questionIndex = 0,
                totalQuestions = questions.size,
                currentQuestion = firstQuestion,
                timeRemaining = firstQuestion.timeLimit,
                maxTime = firstQuestion.timeLimit,
                selectedAnswer = null,
                showExplanation = false
            )
        }
        
        // Play audio if question has audio
        playQuestionAudio(firstQuestion)
        
        startTimer()
    }
    
    /**
     * Play audio for questions that require it
     */
    private fun playQuestionAudio(question: RichQuestion) {
        audioPlaybackJob?.cancel()
        
        question.audioData?.let { audioData ->
            audioPlaybackJob = viewModelScope.launch {
                delay(500) // Small delay before playing
                
                when (audioData.type) {
                    AudioType.SINGLE_NOTE -> {
                        audioData.notes.firstOrNull()?.let { note ->
                            audioManager.playNote(note, audioData.duration.toLong(), 0.85f)
                        }
                    }
                    AudioType.INTERVAL -> {
                        audioData.notes.forEach { note ->
                            audioManager.playNote(note, audioData.duration.toLong(), 0.85f)
                            delay(audioData.duration.toLong() + 200)
                        }
                    }
                    AudioType.CHORD -> {
                        // Play all notes simultaneously
                        audioData.notes.forEach { note ->
                            audioManager.playNote(note, audioData.duration.toLong(), 0.7f)
                        }
                    }
                    AudioType.MELODY -> {
                        audioData.melody.forEach { melodyNote ->
                            val durationMs = (melodyNote.duration * 60000 / audioData.tempo).toLong()
                            audioManager.playNote(melodyNote.note, durationMs, 0.85f)
                            delay(durationMs)
                        }
                    }
                    AudioType.RHYTHM -> {
                        // TODO: Play rhythm pattern
                    }
                }
            }
        }
    }
    
    /**
     * Replay audio for current question
     */
    fun replayAudio() {
        _state.value.currentQuestion?.let { playQuestionAudio(it) }
    }
    
    /**
     * Select an answer for current question
     */
    fun selectAnswer(index: Int) {
        if (_state.value.selectedAnswer != null) return
        
        val currentQuestion = _state.value.currentQuestion ?: return
        val isCorrect = index == currentQuestion.correctAnswerIndex
        
        // Calculate score based on time remaining (Kahoot-style)
        val timeBonus = (_state.value.timeRemaining.toFloat() / _state.value.maxTime.toFloat() * 500).toInt()
        val points = if (isCorrect) {
            currentQuestion.points + timeBonus
        } else {
            0
        }
        
        // Update player score
        val currentUserId = authManager.currentUserId
        val updatedPlayers = _state.value.players.map { player ->
            if (player.id == currentUserId) {
                player.copy(
                    score = player.score + points,
                    lastAnswerCorrect = isCorrect
                )
            } else player
        }
        
        _state.update { 
            it.copy(
                selectedAnswer = index,
                players = updatedPlayers,
                showExplanation = true,
                isAnswerCorrect = isCorrect,
                earnedPoints = points
            ) 
        }
        
        // Sync score to database
        viewModelScope.launch {
            val currentPlayer = updatedPlayers.find { it.id == currentUserId }
            currentPlayer?.let { p ->
                try {
                    postgrest.from("quiz_players").update(
                        mapOf("score" to p.score)
                    ) {
                        filter { 
                            eq("room_code", _state.value.roomCode)
                            eq("user_id", currentUserId)
                        }
                    }
                } catch (e: Exception) { }
            }
        }
        
        // Wait and move to next question
        viewModelScope.launch {
            delay(3000) // Show explanation for 3 seconds
            moveToNextQuestion()
        }
    }
    
    private fun moveToNextQuestion() {
        timerJob?.cancel()
        audioPlaybackJob?.cancel()
        
        val nextIndex = _state.value.questionIndex + 1
        
        if (nextIndex >= questions.size) {
            // Game over
            viewModelScope.launch {
                try {
                    postgrest.from("quiz_rooms").update(
                        mapOf(
                            "status" to "finished",
                            "finished_at" to java.time.Instant.now().toString()
                        )
                    ) {
                        filter { eq("code", _state.value.roomCode) }
                    }
                } catch (e: Exception) { }
            }
            
            _state.update { it.copy(screenState = QuizScreenState.RESULTS) }
        } else {
            val nextQuestion = questions[nextIndex]
            _state.update { 
                it.copy(
                    questionIndex = nextIndex,
                    currentQuestion = nextQuestion,
                    timeRemaining = nextQuestion.timeLimit,
                    maxTime = nextQuestion.timeLimit,
                    selectedAnswer = null,
                    showExplanation = false,
                    isAnswerCorrect = null,
                    earnedPoints = 0
                )
            }
            
            playQuestionAudio(nextQuestion)
            startTimer()
        }
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val maxTime = _state.value.maxTime
            for (time in maxTime downTo 0) {
                _state.update { it.copy(timeRemaining = time) }
                
                if (time == 0) {
                    if (_state.value.selectedAnswer == null) {
                        selectAnswer(-1) // Timeout
                    }
                    break
                }
                
                delay(1000)
            }
        }
    }
    
    fun leaveRoom() {
        viewModelScope.launch {
            try {
                postgrest.from("quiz_players").delete {
                    filter { 
                        eq("room_code", _state.value.roomCode)
                        eq("user_id", authManager.currentUserId)
                    }
                }
            } catch (e: Exception) { }
        }
        
        timerJob?.cancel()
        realtimeJob?.cancel()
        audioPlaybackJob?.cancel()
        _state.value = QuizMultiplayerState()
    }
    
    fun playAgain() {
        viewModelScope.launch {
            try {
                postgrest.from("quiz_players").update(
                    mapOf("score" to 0)
                ) {
                    filter { eq("room_code", _state.value.roomCode) }
                }
                
                postgrest.from("quiz_rooms").update(
                    mapOf("status" to "waiting")
                ) {
                    filter { eq("code", _state.value.roomCode) }
                }
            } catch (e: Exception) { }
        }
        
        // Generate new questions
        questions = QuestionGenerator.generateQuizQuestions(
            count = 10,
            difficulty = _state.value.difficulty
        )
        
        val resetPlayers = _state.value.players.map { it.copy(score = 0) }
        
        _state.update { 
            it.copy(
                screenState = QuizScreenState.WAITING_ROOM,
                players = resetPlayers,
                questionIndex = 0,
                currentQuestion = null,
                selectedAnswer = null
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        realtimeJob?.cancel()
        audioPlaybackJob?.cancel()
    }
}

data class QuizMultiplayerState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val roomCode: String = "",
    val isHost: Boolean = false,
    val players: List<QuizPlayer> = emptyList(),
    val screenState: QuizScreenState = QuizScreenState.LOBBY,
    val difficulty: Int = 1,
    val questionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val currentQuestion: RichQuestion? = null,
    val timeRemaining: Int = 15,
    val maxTime: Int = 15,
    val selectedAnswer: Int? = null,
    val showExplanation: Boolean = false,
    val isAnswerCorrect: Boolean? = null,
    val earnedPoints: Int = 0
)

@kotlinx.serialization.Serializable
data class QuizPlayerEntity(
    val id: String? = null,
    @kotlinx.serialization.SerialName("room_code")
    val roomCode: String,
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    val name: String,
    @kotlinx.serialization.SerialName("is_host")
    val isHost: Boolean = false,
    @kotlinx.serialization.SerialName("is_ready")
    val isReady: Boolean = false,
    val score: Int = 0
)

@kotlinx.serialization.Serializable
data class QuizRoomEntity(
    val id: String? = null,
    val code: String,
    @kotlinx.serialization.SerialName("host_id")
    val hostId: String,
    val status: String = "waiting",
    @kotlinx.serialization.SerialName("current_question")
    val currentQuestion: Int = 0,
    @kotlinx.serialization.SerialName("created_at")
    val createdAt: String? = null,
    @kotlinx.serialization.SerialName("started_at")
    val startedAt: String? = null,
    @kotlinx.serialization.SerialName("finished_at")
    val finishedAt: String? = null
)
