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
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
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
    private val exerciseRepository: ExerciseRepository,
    private val postgrest: Postgrest,
    private val auth: Auth
) : ViewModel() {
    
    private val _state = MutableStateFlow(TheoryLessonState())
    val state: StateFlow<TheoryLessonState> = _state.asStateFlow()
    
    private val currentUserId: String? get() = auth.currentSessionOrNull()?.user?.id
    
    fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, lessonId = lessonId) }
            
            try {
                // Tentar carregar do banco de dados
                val exercise = exerciseRepository.getExerciseById(lessonId)
                
                // Tentar carregar conteúdo de teoria
                val theoryContent = loadTheoryContent(lessonId)
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        title = exercise?.title ?: "Lição de Teoria",
                        description = exercise?.description ?: "Aprenda mais sobre teoria musical",
                        xpReward = exercise?.xpReward ?: 10,
                        content = theoryContent ?: generateDefaultContent(exercise?.title, exercise?.category?.name?.lowercase())
                    )
                }
            } catch (e: Exception) {
                // Fallback para valores padrão
                _state.update {
                    it.copy(
                        isLoading = false,
                        title = "Lição de Teoria",
                        description = "Conteúdo da trilha de aprendizado",
                        content = generateDefaultContent(null, null),
                        error = e.message
                    )
                }
            }
        }
    }
    
    private suspend fun loadTheoryContent(lessonId: String): String? {
        return try {
            postgrest.from("theory_content")
                .select {
                    filter { eq("node_id", lessonId) }
                }
                .decodeSingleOrNull<TheoryContentEntity>()?.content
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateDefaultContent(title: String?, category: String?): String {
        return when (category?.lowercase()) {
            "solfejo" -> """
                📚 **Introdução ao Solfejo**
                
                O solfejo é a prática de cantar notas musicais usando sílabas específicas:
                
                **As 7 notas do solfejo:**
                - **Dó** - A primeira nota da escala
                - **Ré** - Segunda nota
                - **Mi** - Terceira nota
                - **Fá** - Quarta nota
                - **Sol** - Quinta nota
                - **Lá** - Sexta nota
                - **Si** - Sétima nota
                
                **Dicas para praticar:**
                1. Comece cantando lentamente
                2. Use um piano ou app para referência
                3. Pratique diariamente
                
                ✅ Complete esta lição para ganhar XP!
            """.trimIndent()
            
            "ritmo" -> """
                🥁 **Fundamentos do Ritmo**
                
                O ritmo é a organização dos sons no tempo.
                
                **Figuras rítmicas principais:**
                - **Semibreve** (4 tempos) 𝅝
                - **Mínima** (2 tempos) 𝅗𝅥
                - **Semínima** (1 tempo) ♩
                - **Colcheia** (½ tempo) ♪
                - **Semicolcheia** (¼ tempo) ♬
                
                **Exercício prático:**
                1. Bata palmas no ritmo: TÁ TÁ TÁ TÁ
                2. Experimente: TÁ-TÁ TÁ TÁ-TÁ TÁ
                
                ✅ Complete esta lição para ganhar XP!
            """.trimIndent()
            
            "intervalos" -> """
                🎵 **O que são Intervalos?**
                
                Intervalo é a distância entre duas notas musicais.
                
                **Intervalos básicos:**
                - **2ª menor** - 1 semitom
                - **2ª maior** - 2 semitons
                - **3ª menor** - 3 semitons
                - **3ª maior** - 4 semitons
                - **4ª justa** - 5 semitons
                - **5ª justa** - 7 semitons
                
                **Dica:** Associe intervalos a músicas conhecidas!
                - 4ª justa: início de "Parabéns pra Você"
                - 5ª justa: tema de Star Wars
                
                ✅ Complete esta lição para ganhar XP!
            """.trimIndent()
            
            "acordes" -> """
                🎹 **Formação de Acordes**
                
                Acordes são três ou mais notas tocadas simultaneamente.
                
                **Acordes maiores (tríades):**
                - Fórmula: Tônica + 3ª Maior + 5ª Justa
                - Exemplo: Dó Maior = Dó + Mi + Sol
                
                **Acordes menores:**
                - Fórmula: Tônica + 3ª Menor + 5ª Justa
                - Exemplo: Lá menor = Lá + Dó + Mi
                
                **Pratique construindo acordes:**
                1. Escolha uma nota (ex: Sol)
                2. Adicione a 3ª maior (Si)
                3. Adicione a 5ª justa (Ré)
                4. Resultado: Sol Maior!
                
                ✅ Complete esta lição para ganhar XP!
            """.trimIndent()
            
            else -> """
                📖 **${title ?: "Lição de Teoria Musical"}**
                
                Bem-vindo a esta lição de teoria musical!
                
                A música é composta por vários elementos fundamentais:
                
                **1. Melodia** 🎵
                Sequência de notas que formam uma linha musical.
                
                **2. Harmonia** 🎹
                Combinação de notas tocadas simultaneamente.
                
                **3. Ritmo** 🥁
                Organização dos sons no tempo.
                
                **4. Dinâmica** 📢
                Variação de intensidade (forte/fraco).
                
                **Por que aprender teoria?**
                - Entender como a música funciona
                - Tocar melhor seu instrumento
                - Criar suas próprias músicas
                - Comunicar-se com outros músicos
                
                ✅ Complete esta lição para avançar na trilha!
            """.trimIndent()
        }
    }
    
    fun completeLesson() {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            val lessonId = _state.value.lessonId
            val xpReward = _state.value.xpReward
            
            try {
                // 1. Marcar nó como completo em user_node_unlocks
                postgrest.from("user_node_unlocks").upsert(
                    mapOf(
                        "user_id" to userId,
                        "node_id" to lessonId,
                        "is_complete" to true,
                        "completed_at" to java.time.Instant.now().toString()
                    )
                )
                
                // 2. Atualizar XP do usuário
                val currentUser = postgrest.from("users")
                    .select { filter { eq("auth_id", userId) } }
                    .decodeSingleOrNull<UserXpEntity>()
                
                if (currentUser != null) {
                    val newXp = currentUser.xp + xpReward
                    val newLevel = calculateLevel(newXp)
                    
                    postgrest.from("users").update(
                        mapOf(
                            "xp" to newXp,
                            "level" to newLevel
                        )
                    ) {
                        filter { eq("auth_id", userId) }
                    }
                }
                
                _state.update { it.copy(isCompleted = true) }
                
            } catch (e: Exception) {
                // Silently handle error - lesson will still appear completed locally
                _state.update { it.copy(isCompleted = true) }
            }
        }
    }
    
    private fun calculateLevel(xp: Int): Int {
        var level = 1
        var xpNeeded = 100
        var totalXp = xp
        while (totalXp >= xpNeeded) {
            totalXp -= xpNeeded
            level++
            xpNeeded = (xpNeeded * 1.2).toInt()
        }
        return level
    }
}

@kotlinx.serialization.Serializable
private data class TheoryContentEntity(
    val id: String? = null,
    @kotlinx.serialization.SerialName("node_id")
    val nodeId: String,
    val content: String,
    @kotlinx.serialization.SerialName("content_type")
    val contentType: String = "markdown"
)

@kotlinx.serialization.Serializable
private data class UserXpEntity(
    val id: String,
    @kotlinx.serialization.SerialName("auth_id")
    val authId: String? = null,
    val xp: Int = 0,
    val level: Int = 1
)

data class TheoryLessonState(
    val isLoading: Boolean = true,
    val lessonId: String = "",
    val title: String = "",
    val description: String = "",
    val content: String = "",
    val xpReward: Int = 10,
    val isCompleted: Boolean = false,
    val error: String? = null
)

