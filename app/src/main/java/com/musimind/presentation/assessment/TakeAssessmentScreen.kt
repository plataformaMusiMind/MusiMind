package com.musimind.presentation.assessment

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.data.repository.AssessmentEntity
import com.musimind.data.repository.AssessmentQuestionEntity
import com.musimind.domain.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Screen for students to take an assessment
 * 
 * Features:
 * - Timer per question
 * - Various question types (theory, audio, score)
 * - Real-time feedback
 * - Score calculation
 */
@Composable
fun TakeAssessmentScreen(
    assessmentId: String,
    onBack: () -> Unit,
    onComplete: (score: Int, total: Int, passed: Boolean) -> Unit,
    viewModel: TakeAssessmentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(assessmentId) {
        viewModel.loadAssessment(assessmentId)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        when (state.phase) {
            AssessmentPhase.LOADING -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            AssessmentPhase.INTRO -> {
                AssessmentIntroScreen(
                    assessment = state.assessment,
                    questionCount = state.questions.size,
                    totalPoints = state.totalPoints,
                    onStart = { viewModel.startAssessment() },
                    onBack = onBack
                )
            }
            AssessmentPhase.QUESTION -> {
                QuestionScreen(
                    question = state.currentQuestion,
                    questionIndex = state.currentQuestionIndex,
                    totalQuestions = state.questions.size,
                    timeRemaining = state.timeRemaining,
                    maxTime = state.maxTime,
                    selectedAnswer = state.selectedAnswer,
                    showFeedback = state.showFeedback,
                    isCorrect = state.isCorrect,
                    onSelectAnswer = { viewModel.selectAnswer(it) },
                    onPlayAudio = { viewModel.playAudio() }
                )
            }
            AssessmentPhase.RESULT -> {
                ResultScreen(
                    score = state.score,
                    totalPoints = state.totalPoints,
                    percentage = state.percentage,
                    passed = state.passed,
                    passingScore = state.assessment?.passingScore ?: 60,
                    correctCount = state.correctCount,
                    totalQuestions = state.questions.size,
                    onBack = {
                        onComplete(state.score, state.totalPoints, state.passed)
                    }
                )
            }
        }
    }
}

@Composable
private fun AssessmentIntroScreen(
    assessment: AssessmentEntity?,
    questionCount: Int,
    totalPoints: Int,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(Icons.Default.ArrowBack, "Voltar")
        }
        
        Spacer(Modifier.weight(1f))
        
        // Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Assignment,
                null,
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            assessment?.title ?: "Avaliação",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        assessment?.description?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Stats
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Quiz,
                    value = "$questionCount",
                    label = "Perguntas"
                )
                StatItem(
                    icon = Icons.Default.Star,
                    value = "$totalPoints",
                    label = "Pontos"
                )
                StatItem(
                    icon = Icons.Default.Check,
                    value = "${assessment?.passingScore ?: 60}%",
                    label = "Aprovação"
                )
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("Iniciar Avaliação", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestionScreen(
    question: AssessmentQuestionEntity?,
    questionIndex: Int,
    totalQuestions: Int,
    timeRemaining: Int,
    maxTime: Int,
    selectedAnswer: Int?,
    showFeedback: Boolean,
    isCorrect: Boolean?,
    onSelectAnswer: (Int) -> Unit,
    onPlayAudio: () -> Unit
) {
    if (question == null) return
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with progress and timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress
            Text(
                "${questionIndex + 1}/$totalQuestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Timer
            val timerColor = when {
                timeRemaining <= 5 -> Color.Red
                timeRemaining <= 10 -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.primary
            }
            
            val timerProgress = timeRemaining.toFloat() / maxTime
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { timerProgress },
                    modifier = Modifier.size(40.dp),
                    color = timerColor,
                    strokeWidth = 4.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${timeRemaining}s",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = timerColor
                )
            }
        }
        
        // Progress bar
        LinearProgressIndicator(
            progress = { (questionIndex + 1).toFloat() / totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Question card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Question type indicator
                val typeLabel = when (question.questionType) {
                    "multiple_choice", "theory_text" -> "📝 Teoria"
                    "note_identification" -> "🎼 Identificar nota"
                    "interval_recognition" -> "🎵 Intervalo"
                    "chord_recognition" -> "🎹 Acorde"
                    else -> "❓ Pergunta"
                }
                
                Text(
                    typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    question.questionText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                
                // Audio button if applicable
                if (question.audioUrl != null || question.questionType in listOf(
                    "interval_recognition", "chord_recognition"
                )) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onPlayAudio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Ouvir",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
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
                        tint = Color(0xFFFFD700)
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
        
        Spacer(Modifier.height(24.dp))
        
        // Answer options
        val options = try {
            question.options?.let { optionsJson ->
                Json.parseToJsonElement(optionsJson).jsonArray.map { element ->
                    val obj = element.jsonObject
                    QuestionOption(
                        id = obj["id"]?.jsonPrimitive?.content ?: "",
                        text = obj["text"]?.jsonPrimitive?.content ?: "",
                        isCorrect = obj["isCorrect"]?.jsonPrimitive?.content?.toBoolean() ?: false
                    )
                }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(options.size) { index ->
                val option = options.getOrNull(index) ?: return@items
                val isSelected = selectedAnswer == index
                val isCorrectOption = option.isCorrect
                
                val backgroundColor = when {
                    showFeedback && isCorrectOption -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    showFeedback && isSelected && !isCorrectOption -> Color(0xFFE91E63).copy(alpha = 0.2f)
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                
                val borderColor = when {
                    showFeedback && isCorrectOption -> Color(0xFF4CAF50)
                    showFeedback && isSelected && !isCorrectOption -> Color(0xFFE91E63)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = selectedAnswer == null) { onSelectAnswer(index) }
                        .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Option letter
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                ('A' + index).toString(),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Text(
                            option.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (showFeedback) {
                            Icon(
                                if (isCorrectOption) Icons.Default.Check else Icons.Default.Close,
                                null,
                                tint = if (isCorrectOption) Color(0xFF4CAF50) else Color(0xFFE91E63)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(
    score: Int,
    totalPoints: Int,
    percentage: Float,
    passed: Boolean,
    passingScore: Int,
    correctCount: Int,
    totalQuestions: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Result icon
        val iconColor = if (passed) Color(0xFF4CAF50) else Color(0xFFE91E63)
        val icon = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(80.dp),
                tint = iconColor
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            if (passed) "Parabéns! 🎉" else "Continue tentando 💪",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            if (passed) "Você foi aprovado(a)!" else "Você não atingiu a nota mínima",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Score card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$score/$totalPoints",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                
                Text(
                    "Pontuação",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(16.dp))
                
                Divider()
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${percentage.toInt()}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Aproveitamento",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$correctCount/$totalQuestions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Acertos",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$passingScore%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Mínimo",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continuar", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// Phase enum
enum class AssessmentPhase {
    LOADING, INTRO, QUESTION, RESULT
}
