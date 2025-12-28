package com.musimind.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

/**
 * Placement Test Screen - Initial Assessment
 * 
 * Determines user's starting level through adaptive questions
 * covering all musical categories.
 */

@Composable
fun PlacementTestScreen(
    onComplete: (level: Int) -> Unit,
    onSkip: () -> Unit,
    viewModel: PlacementTestViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    when (val currentState = state) {
        is PlacementTestState.Intro -> IntroScreen(
            onStartTest = { viewModel.startTest() },
            onSkip = onSkip
        )
        is PlacementTestState.Question -> QuestionScreen(
            question = currentState.question,
            currentIndex = currentState.currentIndex,
            totalQuestions = currentState.totalQuestions,
            timeRemaining = currentState.timeRemaining,
            onAnswer = { viewModel.submitAnswer(it) }
        )
        is PlacementTestState.Calculating -> CalculatingScreen()
        is PlacementTestState.Result -> ResultScreen(
            determinedLevel = currentState.level,
            score = currentState.score,
            correctAnswers = currentState.correctAnswers,
            totalQuestions = currentState.totalQuestions,
            onContinue = { onComplete(currentState.level) }
        )
    }
}

// ============================================
// Intro Screen
// ============================================

@Composable
private fun IntroScreen(
    onStartTest: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎯",
                    fontSize = 56.sp
                )
            }
            
            Text(
                text = "Teste de Nivelamento",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Vamos descobrir seu nível musical para personalizar sua experiência de aprendizado!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        // Features list
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureItem(
                icon = "⏱️",
                title = "5-10 minutos",
                description = "Teste rápido e adaptativo"
            )
            FeatureItem(
                icon = "🎵",
                title = "Várias categorias",
                description = "Intervalos, ritmo, teoria e mais"
            )
            FeatureItem(
                icon = "📊",
                title = "Personalizado",
                description = "App ajustado ao seu nível"
            )
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar Teste", fontWeight = FontWeight.Bold)
            }
            
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pular e começar do nível 1")
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 28.sp)
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// ============================================
// Question Screen
// ============================================

@Composable
private fun QuestionScreen(
    question: PlacementQuestion,
    currentIndex: Int,
    totalQuestions: Int,
    timeRemaining: Int,
    onAnswer: (String) -> Unit
) {
    var selectedAnswer by remember(question.id) { mutableStateOf<String?>(null) }
    var isSubmitted by remember(question.id) { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header with progress and timer
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Questão ${currentIndex + 1}/$totalQuestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (timeRemaining < 10) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${timeRemaining}s",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (timeRemaining < 10) Color.Red else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = (currentIndex + 1).toFloat() / totalQuestions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            
            // Difficulty indicator
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(10) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < question.difficulty)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nível ${question.difficulty}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Question
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = question.text,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Audio player placeholder (if question has audio)
            if (question.hasAudio) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { /* Play audio */ },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Tocar áudio",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Ouvir",
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Answer options
            question.options.forEach { option ->
                val isSelected = selectedAnswer == option
                
                AnswerOption(
                    text = option,
                    isSelected = isSelected,
                    isSubmitted = isSubmitted,
                    isCorrect = isSubmitted && option == question.correctAnswer,
                    onClick = {
                        if (!isSubmitted) {
                            selectedAnswer = option
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // Submit button
        Button(
            onClick = {
                if (!isSubmitted && selectedAnswer != null) {
                    isSubmitted = true
                    // Small delay before moving to next question
                    onAnswer(selectedAnswer!!)
                }
            },
            enabled = selectedAnswer != null && !isSubmitted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Confirmar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun AnswerOption(
    text: String,
    isSelected: Boolean,
    isSubmitted: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSubmitted && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        isSubmitted && isSelected && !isCorrect -> Color(0xFFF44336).copy(alpha = 0.2f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val borderColor = when {
        isSubmitted && isCorrect -> Color(0xFF4CAF50)
        isSubmitted && isSelected && !isCorrect -> Color(0xFFF44336)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(),
        label = "optionScale"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = !isSubmitted, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        
        if (isSubmitted && isCorrect) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Correto",
                tint = Color(0xFF4CAF50)
            )
        } else if (isSubmitted && isSelected && !isCorrect) {
            Icon(
                Icons.Default.Cancel,
                contentDescription = "Incorreto",
                tint = Color(0xFFF44336)
            )
        } else if (isSelected) {
            Icon(
                Icons.Default.RadioButtonChecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ============================================
// Calculating Screen
// ============================================

@Composable
private fun CalculatingScreen() {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(50)
            progress += 0.02f
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "calc")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation },
                strokeWidth = 8.dp
            )
            
            Text(
                text = "🎵",
                fontSize = 40.sp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Analisando suas respostas...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Calculando seu nível ideal",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .width(200.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

// ============================================
// Result Screen
// ============================================

@Composable
private fun ResultScreen(
    determinedLevel: Int,
    score: Int,
    correctAnswers: Int,
    totalQuestions: Int,
    onContinue: () -> Unit
) {
    var showLevel by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(500)
        showLevel = true
    }
    
    val levelScale by animateFloatAsState(
        targetValue = if (showLevel) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "levelScale"
    )
    
    val levelEmoji = when (determinedLevel) {
        in 1..2 -> "🌱"
        in 3..4 -> "🌿"
        in 5..6 -> "🌳"
        in 7..8 -> "⭐"
        else -> "👑"
    }
    
    val levelDescription = when (determinedLevel) {
        in 1..2 -> "Iniciante"
        in 3..4 -> "Básico"
        in 5..6 -> "Intermediário"
        in 7..8 -> "Avançado"
        else -> "Expert"
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Teste Concluído!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Level display
            Box(
                modifier = Modifier
                    .scale(levelScale)
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = levelEmoji, fontSize = 48.sp)
                    Text(
                        text = "Nível $determinedLevel",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = levelDescription,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Stats
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Seu Desempenho",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    value = "$correctAnswers/$totalQuestions",
                    label = "Acertos"
                )
                StatCard(
                    value = "${(correctAnswers.toFloat() / totalQuestions * 100).toInt()}%",
                    label = "Precisão"
                )
                StatCard(
                    value = "$score",
                    label = "Pontos"
                )
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Você começará no Nível $determinedLevel. Os exercícios serão adaptados ao seu conhecimento!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Começar a Aprender!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun StatCard(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

// ============================================
// Data Classes
// ============================================

data class PlacementQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctAnswer: String,
    val difficulty: Int,
    val category: String,
    val hasAudio: Boolean = false,
    val audioUrl: String? = null
)

sealed class PlacementTestState {
    object Intro : PlacementTestState()
    
    data class Question(
        val question: PlacementQuestion,
        val currentIndex: Int,
        val totalQuestions: Int,
        val timeRemaining: Int
    ) : PlacementTestState()
    
    object Calculating : PlacementTestState()
    
    data class Result(
        val level: Int,
        val score: Int,
        val correctAnswers: Int,
        val totalQuestions: Int
    ) : PlacementTestState()
}
