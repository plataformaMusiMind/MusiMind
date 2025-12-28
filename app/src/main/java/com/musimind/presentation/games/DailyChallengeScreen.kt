package com.musimind.presentation.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DailyChallengeScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: DailyChallengeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) { viewModel.loadDailyChallenge(userId) }
    
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFF6B6B), Color(0xFFEE5A5A))))) {
        when (state.gamePhase) {
            DailyChallengePhase.MENU -> DailyChallengeMenu(state, viewModel, userId, onBack)
            DailyChallengePhase.PLAYING -> DailyChallengePlay(state, viewModel)
            DailyChallengePhase.RESULT -> DailyChallengeResult(state, viewModel, onBack)
        }
    }
}

@Composable
private fun DailyChallengeMenu(state: DailyChallengeState, viewModel: DailyChallengeViewModel, userId: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
        }
        
        Spacer(Modifier.weight(0.3f))
        
        // Ícone do desafio
        val pulseAnim = rememberInfiniteTransition(label = "pulse")
        val scale by pulseAnim.animateFloat(1f, 1.1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "iconPulse")
        
        Text(state.icon, fontSize = 80.sp, modifier = Modifier.scale(scale))
        
        Spacer(Modifier.height(24.dp))
        
        Text("DESAFIO DIÁRIO", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text(state.dateFormatted, color = Color.White.copy(alpha = 0.8f))
        
        Spacer(Modifier.height(32.dp))
        
        // Card do desafio
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(state.description, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                
                Spacer(Modifier.height(16.dp))
                
                // Dificuldade
                Row {
                    repeat(3) { i ->
                        Icon(
                            if (i < state.difficulty) Icons.Default.Star else Icons.Default.StarBorder,
                            null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Recompensas
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚡", fontSize = 24.sp)
                        Text("+${state.xpReward} XP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🪙", fontSize = 24.sp)
                        Text("+${state.coinsReward}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(Modifier.weight(0.5f))
        
        // Botão de iniciar
        if (state.alreadyCompleted) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))) {
                Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Concluído hoje!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(
                onClick = { viewModel.startChallenge(userId) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color(0xFFEE5A5A))
                Spacer(Modifier.width(8.dp))
                Text("INICIAR DESAFIO", color = Color(0xFFEE5A5A), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DailyChallengePlay(state: DailyChallengeState, viewModel: DailyChallengeViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("PONTOS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Text("${state.score}", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("${state.currentQuestion}/${state.totalQuestions}", color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = if (state.timeRemaining <= 10) Color.Red else Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("${state.timeRemaining}s", color = if (state.timeRemaining <= 10) Color.Red else Color.White, fontWeight = FontWeight.Bold)
            }
            if (state.combo > 1) Text("🔥 x${state.combo}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
        }
        
        // Progresso
        LinearProgressIndicator(progress = { state.currentQuestion.toFloat() / state.totalQuestions }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFF4CAF50), trackColor = Color.Black.copy(alpha = 0.3f))
        
        Spacer(Modifier.weight(1f))
        
        // Pergunta atual
        state.currentQuestionData?.let { question ->
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Categoria
                val categoryColor = when (question.category) {
                    QuestionCategory.INTERVAL -> Color(0xFF8B5CF6)
                    QuestionCategory.CHORD -> Color(0xFFF59E0B)
                    QuestionCategory.RHYTHM -> Color(0xFFEF4444)
                    QuestionCategory.MELODY -> Color(0xFF06B6D4)
                    QuestionCategory.THEORY -> Color(0xFF10B981)
                }
                
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(categoryColor).padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(question.category.name, color = Color.White, fontSize = 12.sp)
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(question.visual, fontSize = 48.sp)
                
                Spacer(Modifier.height(16.dp))
                
                Text(question.question, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                
                // Feedback
                state.roundResult?.let { result ->
                    Spacer(Modifier.height(16.dp))
                    val (text, color) = when (result) {
                        RoundResult.SUCCESS -> "✓ Correto!" to Color(0xFF4CAF50)
                        RoundResult.FAIL -> "✗ ${question.correctAnswer}" to Color(0xFFE91E63)
                    }
                    Text(text, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        // Opções de resposta
        Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f)).padding(12.dp)) {
            state.currentQuestionData?.options?.chunked(2)?.forEach { rowOptions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowOptions.forEach { option ->
                        Button(
                            onClick = { viewModel.submitAnswer(option) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = state.roundResult == null
                        ) {
                            Text(option, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DailyChallengeResult(state: DailyChallengeState, viewModel: DailyChallengeViewModel, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Resultado
        val (icon, title, color) = if (state.success) {
            Triple("🏆", "DESAFIO CONCLUÍDO!", Color(0xFF4CAF50))
        } else {
            Triple("💪", "CONTINUE TENTANDO!", Color(0xFFFF9800))
        }
        
        Text(icon, fontSize = 80.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(32.dp))
        
        // Pontuação
        Text("${state.score}", style = MaterialTheme.typography.displayLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text("PONTOS", color = Color.White.copy(alpha = 0.7f))
        
        Spacer(Modifier.height(24.dp))
        
        // Estatísticas
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${state.correctCount}", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                Text("Acertos", color = Color.White.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${state.wrongCount}", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
                Text("Erros", color = Color.White.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("x${state.maxCombo}", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                Text("Max Combo", color = Color.White.copy(alpha = 0.7f))
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Recompensas
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚡", fontSize = 32.sp)
                    Text("+${state.finalXp} XP", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🪙", fontSize = 32.sp)
                    Text("+${state.finalCoins}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("VOLTAR", color = Color(0xFFEE5A5A), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
