package com.musimind.presentation.theory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tela de Lição de Teoria
 * 
 * Exibe o conteúdo teórico de um nó de aprendizado.
 * Por enquanto, mostra informações básicas do nó.
 * Futuramente pode exibir markdown/rich content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheoryLessonScreen(
    lessonId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: TheoryLessonViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📚",
                                style = MaterialTheme.typography.displayMedium
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            if (state.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row {
                                AssistChip(
                                    onClick = { },
                                    label = { Text("+${state.xpReward} XP") },
                                    leadingIcon = { Text("⭐") }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Content Section
                    Text(
                        text = "Conteúdo da Lição",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Placeholder content - in the future, this could load markdown
                            Text(
                                text = if (state.content.isNotEmpty()) {
                                    state.content
                                } else {
                                    "Este é um nó de aprendizado da trilha musical.\n\n" +
                                    "ID: ${state.lessonId}\n\n" +
                                    "O conteúdo detalhado desta lição ainda será carregado. " +
                                    "Por enquanto, clique em 'Completar' para avançar na trilha."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Complete Button
                    Button(
                        onClick = {
                            viewModel.completeLesson()
                            onComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Completar Lição",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@HiltViewModel
class TheoryLessonViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(TheoryLessonState())
    val state: StateFlow<TheoryLessonState> = _state.asStateFlow()
    
    fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, lessonId = lessonId) }
            
            try {
                // Tentar carregar do banco de dados
                val exercise = exerciseRepository.getExerciseById(lessonId)
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        title = exercise?.title ?: "Lição de Teoria",
                        description = exercise?.description ?: "Aprenda mais sobre teoria musical",
                        xpReward = exercise?.xpReward ?: 10,
                        content = "" // TODO: Carregar conteúdo markdown
                    )
                }
            } catch (e: Exception) {
                // Fallback para valores padrão
                _state.update {
                    it.copy(
                        isLoading = false,
                        title = "Lição de Teoria",
                        description = "Conteúdo da trilha de aprendizado",
                        error = e.message
                    )
                }
            }
        }
    }
    
    fun completeLesson() {
        viewModelScope.launch {
            // TODO: Marcar como completo no backend
        }
    }
}

data class TheoryLessonState(
    val isLoading: Boolean = true,
    val lessonId: String = "",
    val title: String = "",
    val description: String = "",
    val content: String = "",
    val xpReward: Int = 10,
    val error: String? = null
)

