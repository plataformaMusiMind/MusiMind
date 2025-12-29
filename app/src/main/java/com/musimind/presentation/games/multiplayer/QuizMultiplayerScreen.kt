package com.musimind.presentation.games.multiplayer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.presentation.games.qr.QRCodeDisplay
import com.musimind.presentation.games.qr.QRCodeGenerator
import com.musimind.ui.theme.Primary
import com.musimind.ui.theme.PrimaryVariant
import com.musimind.ui.theme.XpGold

/**
 * Quiz Multiplayer Screen - Create or join multiplayer quiz rooms
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizMultiplayerScreen(
    onBack: () -> Unit,
    viewModel: QuizMultiplayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Multiplayer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.screenState) {
                QuizScreenState.LOBBY -> LobbyScreen(
                    onCreateRoom = { viewModel.createRoom() },
                    onJoinRoom = { code -> viewModel.joinRoom(code) },
                    isLoading = state.isLoading,
                    error = state.error
                )
                QuizScreenState.WAITING_ROOM -> WaitingRoomScreen(
                    roomCode = state.roomCode,
                    players = state.players,
                    isHost = state.isHost,
                    onStartGame = { viewModel.startGame() },
                    onLeave = { viewModel.leaveRoom() }
                )
                QuizScreenState.PLAYING -> PlayingScreen(
                    question = state.currentQuestion,
                    questionNumber = state.questionIndex + 1,
                    totalQuestions = state.totalQuestions,
                    timeRemaining = state.timeRemaining,
                    maxTime = state.maxTime,
                    selectedAnswer = state.selectedAnswer,
                    showExplanation = state.showExplanation,
                    isCorrect = state.isAnswerCorrect,
                    earnedPoints = state.earnedPoints,
                    onSelectAnswer = { viewModel.selectAnswer(it) },
                    onReplayAudio = { viewModel.replayAudio() }
                )
                QuizScreenState.RESULTS -> ResultsScreen(
                    players = state.players,
                    onPlayAgain = { viewModel.playAgain() },
                    onExit = onBack
                )
            }
        }
    }
}

@Composable
private fun LobbyScreen(
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Quiz Multiplayer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Desafie seus amigos em tempo real!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Create Room Button
        Button(
            onClick = onCreateRoom,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Criar Sala", style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Join Room Button
        OutlinedButton(
            onClick = { showJoinDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.Login, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Entrar em Sala", style = MaterialTheme.typography.titleMedium)
        }
        
        // Error message
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    
    // Join Dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Entrar na Sala") },
            text = {
                Column {
                    Text("Digite o código de 6 caracteres:")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { 
                            if (it.length <= 6) joinCode = it.uppercase()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ABC123") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (joinCode.length == 6) {
                                    onJoinRoom(joinCode)
                                    showJoinDialog = false
                                }
                            }
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onJoinRoom(joinCode)
                        showJoinDialog = false
                    },
                    enabled = joinCode.length == 6
                ) {
                    Text("Entrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun WaitingRoomScreen(
    roomCode: String,
    players: List<QuizPlayer>,
    isHost: Boolean,
    onStartGame: () -> Unit,
    onLeave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // QR Code and Room Code
        QRCodeDisplay(
            roomCode = roomCode,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Players list
        Text(
            text = "Jogadores (${players.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(players) { player ->
                PlayerListItem(player = player)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        if (isHost) {
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = players.size >= 2
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar Jogo")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Aguardando o host iniciar o jogo...")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sair da Sala", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PlayerListItem(player: QuizPlayer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (player.isHost) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (player.isHost) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Host",
                            modifier = Modifier.size(16.dp),
                            tint = XpGold
                        )
                    }
                }
                if (player.isReady) {
                    Text(
                        text = "Pronto",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF22C55E)
                    )
                }
            }
            
            // Score (during game)
            if (player.score > 0) {
                Text(
                    text = "${player.score} pts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = XpGold
                )
            }
        }
    }
}

@Composable
private fun PlayingScreen(
    question: com.musimind.domain.model.RichQuestion?,
    questionNumber: Int,
    totalQuestions: Int,
    timeRemaining: Int,
    maxTime: Int = 15,
    selectedAnswer: Int?,
    showExplanation: Boolean = false,
    isCorrect: Boolean? = null,
    earnedPoints: Int = 0,
    onSelectAnswer: (Int) -> Unit,
    onReplayAudio: () -> Unit = {}
) {
    if (question == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Questão $questionNumber/$totalQuestions",
                style = MaterialTheme.typography.labelLarge
            )
            
            // Question type badge
            val typeLabel = when (question.type) {
                com.musimind.domain.model.QuestionType.NOTE_IDENTIFICATION -> "🎼 Nota"
                com.musimind.domain.model.QuestionType.INTERVAL_RECOGNITION -> "🎵 Intervalo"
                com.musimind.domain.model.QuestionType.CHORD_RECOGNITION -> "🎹 Acorde"
                com.musimind.domain.model.QuestionType.RHYTHM_IDENTIFICATION -> "🥁 Ritmo"
                com.musimind.domain.model.QuestionType.KEY_SIGNATURE -> "🔑 Armadura"
                com.musimind.domain.model.QuestionType.SYMBOL_IDENTIFICATION -> "📝 Símbolo"
                else -> "📚 Teoria"
            }
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Timer
            val timerProgress = timeRemaining.toFloat() / maxTime
            val timerColor = when {
                timeRemaining > 10 -> Color(0xFF22C55E)
                timeRemaining > 5 -> Color(0xFFFBBF24)
                else -> Color(0xFFEF4444)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { timerProgress },
                    modifier = Modifier.size(40.dp),
                    color = timerColor,
                    strokeWidth = 4.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${timeRemaining}s",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = timerColor
                )
            }
        }
        
        // Progress bar
        LinearProgressIndicator(
            progress = { questionNumber.toFloat() / totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Question Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Audio button for questions with audio
                if (question.audioData != null) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onReplayAudio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.VolumeUp, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ouvir Novamente")
                    }
                }
                
                // Points indicator
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = XpGold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${question.points} pontos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Options
        question.options.forEachIndexed { index, option ->
            val isSelected = selectedAnswer == index
            val isCorrectOption = option.isCorrect
            
            val backgroundColor = when {
                showExplanation && isCorrectOption -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                showExplanation && isSelected && !isCorrectOption -> Color(0xFFE91E63).copy(alpha = 0.2f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            
            val borderColor = when {
                showExplanation && isCorrectOption -> Color(0xFF4CAF50)
                showExplanation && isSelected && !isCorrectOption -> Color(0xFFE91E63)
                isSelected -> Primary
                else -> Color.Transparent
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(enabled = selectedAnswer == null) { onSelectAnswer(index) },
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                shape = RoundedCornerShape(12.dp),
                border = if (borderColor != Color.Transparent) 
                    androidx.compose.foundation.BorderStroke(2.dp, borderColor) 
                else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ('A' + index).toString(),
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = option.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (showExplanation) {
                        Icon(
                            if (isCorrectOption) Icons.Default.Check else Icons.Default.Close,
                            null,
                            tint = if (isCorrectOption) Color(0xFF4CAF50) else Color(0xFFE91E63)
                        )
                    }
                }
            }
        }
        
        // Feedback / Explanation
        AnimatedVisibility(
            visible = showExplanation,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect == true) 
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else 
                        Color(0xFFE91E63).copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isCorrect == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            null,
                            tint = if (isCorrect == true) Color(0xFF4CAF50) else Color(0xFFE91E63)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isCorrect == true) "Correto! +$earnedPoints pontos" else "Incorreto",
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect == true) Color(0xFF4CAF50) else Color(0xFFE91E63)
                        )
                    }
                    
                    question.explanation?.let { explanation ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsScreen(
    players: List<QuizPlayer>,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val sortedPlayers = players.sortedByDescending { it.score }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Winner announcement
        if (sortedPlayers.isNotEmpty()) {
            val winner = sortedPlayers.first()
            
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = XpGold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "🎉 Vencedor!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = winner.name,
                style = MaterialTheme.typography.titleLarge,
                color = Primary
            )
            
            Text(
                text = "${winner.score} pontos",
                style = MaterialTheme.typography.titleMedium,
                color = XpGold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Ranking
        Text(
            text = "Ranking",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedPlayers.withIndex().toList()) { (index, player) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (index) {
                                0 -> XpGold.copy(alpha = 0.2f)
                                1 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                                2 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}º",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp)
                    )
                    
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = "${player.score} pts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, 
                        color = XpGold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Jogar Novamente")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Sair")
        }
    }
}

// Data classes
enum class QuizScreenState {
    LOBBY, WAITING_ROOM, PLAYING, RESULTS
}

data class QuizPlayer(
    val id: String,
    val name: String,
    val isHost: Boolean = false,
    val isReady: Boolean = false,
    val score: Int = 0,
    val lastAnswerCorrect: Boolean? = null
)

data class QuizQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)
