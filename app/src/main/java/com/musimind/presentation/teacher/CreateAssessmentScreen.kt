package com.musimind.presentation.teacher

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.data.repository.AssessmentEntity
import com.musimind.domain.model.*

/**
 * Screen for teachers/schools to create and manage assessments
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAssessmentScreen(
    onBack: () -> Unit,
    viewModel: AssessmentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("theory") }
    
    // Snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(state.message, state.error) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (state.currentAssessment != null) "Editar Avaliação" 
                        else "Minhas Avaliações"
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.currentAssessment != null) {
                            viewModel.clearCurrentAssessment()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    if (state.currentAssessment == null) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, "Nova Avaliação")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.currentAssessment != null) {
                // Editing an assessment
                AssessmentEditor(
                    assessment = state.currentAssessment!!,
                    questions = state.currentQuestions,
                    onAddQuestion = { showAddQuestionDialog = true },
                    onAssignToClass = { showAssignDialog = true },
                    onDelete = { viewModel.deleteAssessment(it) },
                    onGenerateQuestions = { count, types, difficulty ->
                        viewModel.generateQuestions(count, types, difficulty)
                    }
                )
            } else {
                // List of assessments
                AssessmentList(
                    assessments = state.assessments,
                    onSelect = { viewModel.selectAssessment(it) },
                    onCreate = { showCreateDialog = true }
                )
            }
        }
    }
    
    // Create Assessment Dialog
    if (showCreateDialog) {
        CreateAssessmentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, description, category, isPublic, passingScore ->
                viewModel.createAssessment(title, description, category, isPublic, passingScore)
                showCreateDialog = false
            }
        )
    }
    
    // Add Question Dialog
    if (showAddQuestionDialog) {
        AddQuestionDialog(
            onDismiss = { showAddQuestionDialog = false },
            onAdd = { question ->
                viewModel.addQuestion(question)
                showAddQuestionDialog = false
            }
        )
    }
    
    // Assign to Class Dialog
    if (showAssignDialog && state.classes.isNotEmpty()) {
        AssignToClassDialog(
            classes = state.classes,
            onDismiss = { showAssignDialog = false },
            onAssign = { classId, dueDate ->
                viewModel.assignToClass(classId, dueDate)
                showAssignDialog = false
            }
        )
    }
}

@Composable
private fun AssessmentList(
    assessments: List<AssessmentEntity>,
    onSelect: (AssessmentEntity) -> Unit,
    onCreate: () -> Unit
) {
    if (assessments.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Assignment,
                null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Nenhuma avaliação criada",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onCreate) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Criar Avaliação")
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(assessments) { assessment ->
                AssessmentCard(assessment, onClick = { onSelect(assessment) })
            }
        }
    }
}

@Composable
private fun AssessmentCard(
    assessment: AssessmentEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (assessment.category) {
                        "theory" -> Icons.Default.MenuBook
                        "solfege" -> Icons.Default.MusicNote
                        "rhythm" -> Icons.Default.Timer
                        "intervals" -> Icons.Default.MultipleStop
                        else -> Icons.Default.Assignment
                    },
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    assessment.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                assessment.description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (assessment.isPublic) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Pública", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Public, null, Modifier.size(14.dp)) },
                            modifier = Modifier.height(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        "Aprovação: ${assessment.passingScore}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun AssessmentEditor(
    assessment: AssessmentEntity,
    questions: List<com.musimind.data.repository.AssessmentQuestionEntity>,
    onAddQuestion: () -> Unit,
    onAssignToClass: () -> Unit,
    onDelete: (String) -> Unit,
    onGenerateQuestions: (Int, List<QuestionType>, Int) -> Unit
) {
    var showGenerateDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Assessment info
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        assessment.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    assessment.description?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${questions.size}", style = MaterialTheme.typography.headlineMedium)
                            Text("Perguntas", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${questions.sumOf { it.points }}", style = MaterialTheme.typography.headlineMedium)
                            Text("Pontos", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${assessment.passingScore}%", style = MaterialTheme.typography.headlineMedium)
                            Text("Aprovação", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        
        // Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddQuestion,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Pergunta")
                }
                
                OutlinedButton(
                    onClick = { showGenerateDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Gerar")
                }
                
                OutlinedButton(
                    onClick = onAssignToClass,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.GroupAdd, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Atribuir")
                }
            }
        }
        
        // Questions
        item {
            Text(
                "Perguntas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        if (questions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.QuestionMark,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Nenhuma pergunta adicionada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(questions) { question ->
                QuestionCard(question)
            }
        }
        
        // Delete button
        item {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onDelete(assessment.id) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(8.dp))
                Text("Excluir Avaliação")
            }
        }
    }
    
    // Generate Questions Dialog
    if (showGenerateDialog) {
        GenerateQuestionsDialog(
            onDismiss = { showGenerateDialog = false },
            onGenerate = { count, types, difficulty ->
                onGenerateQuestions(count, types, difficulty)
                showGenerateDialog = false
            }
        )
    }
}

@Composable
private fun QuestionCard(
    question: com.musimind.data.repository.AssessmentQuestionEntity
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Type badge
                val typeIcon = when (question.questionType) {
                    "multiple_choice", "theory_text" -> Icons.Default.List
                    "note_identification" -> Icons.Default.MusicNote
                    "interval_recognition" -> Icons.Default.MultipleStop
                    "chord_recognition" -> Icons.Default.Piano
                    else -> Icons.Default.QuestionMark
                }
                
                Icon(
                    typeIcon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                
                Text(
                    question.questionText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    "${question.points} pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (question.audioUrl != null) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Com áudio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateAssessmentDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Boolean, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("theory") }
    var isPublic by remember { mutableStateOf(false) }
    var passingScore by remember { mutableStateOf("60") }
    
    val categories = listOf(
        "theory" to "Teoria Musical",
        "solfege" to "Solfejo",
        "rhythm" to "Ritmo",
        "intervals" to "Intervalos",
        "chords" to "Acordes",
        "mixed" to "Misto"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Avaliação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                // Category selector
                Text("Categoria", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { (key, label) ->
                        FilterChip(
                            selected = category == key,
                            onClick = { category = key },
                            label = { Text(label) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = passingScore,
                    onValueChange = { passingScore = it.filter { c -> c.isDigit() } },
                    label = { Text("% Aprovação") },
                    modifier = Modifier.width(120.dp)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Tornar pública")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        title,
                        description,
                        category,
                        isPublic,
                        passingScore.toIntOrNull() ?: 60
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AddQuestionDialog(
    onDismiss: () -> Unit,
    onAdd: (AssessmentQuestionInput) -> Unit
) {
    var questionType by remember { mutableStateOf(QuestionType.MULTIPLE_CHOICE) }
    var questionText by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "", "", "")) }
    var correctIndex by remember { mutableStateOf(0) }
    var points by remember { mutableStateOf("10") }
    var timeLimit by remember { mutableStateOf("15") }
    
    val questionTypes = listOf(
        QuestionType.MULTIPLE_CHOICE to "Múltipla escolha",
        QuestionType.NOTE_IDENTIFICATION to "Identificar nota",
        QuestionType.INTERVAL_RECOGNITION to "Reconhecer intervalo",
        QuestionType.CHORD_RECOGNITION to "Reconhecer acorde",
        QuestionType.THEORY_TEXT to "Teoria musical",
        QuestionType.KEY_SIGNATURE to "Armadura de clave",
        QuestionType.SYMBOL_IDENTIFICATION to "Identificar símbolo"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Pergunta") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Question type
                Text("Tipo de pergunta", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(questionTypes) { (type, label) ->
                        FilterChip(
                            selected = questionType == type,
                            onClick = { questionType = type },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Pergunta") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                // Options
                Text("Opções de resposta", style = MaterialTheme.typography.labelMedium)
                options.forEachIndexed { index, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = correctIndex == index,
                            onClick = { correctIndex = index }
                        )
                        OutlinedTextField(
                            value = option,
                            onValueChange = { newValue ->
                                options = options.toMutableList().apply { set(index, newValue) }
                            },
                            label = { Text("Opção ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = points,
                        onValueChange = { points = it.filter { c -> c.isDigit() } },
                        label = { Text("Pontos") },
                        modifier = Modifier.width(100.dp)
                    )
                    OutlinedTextField(
                        value = timeLimit,
                        onValueChange = { timeLimit = it.filter { c -> c.isDigit() } },
                        label = { Text("Tempo (s)") },
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val questionOptions = options.mapIndexed { index, text ->
                        QuestionOption(
                            id = index.toString(),
                            text = text,
                            isCorrect = index == correctIndex
                        )
                    }.filter { it.text.isNotBlank() }
                    
                    onAdd(
                        AssessmentQuestionInput(
                            type = questionType,
                            text = questionText,
                            options = questionOptions,
                            correctAnswer = correctIndex.toString(),
                            timeLimit = timeLimit.toIntOrNull() ?: 15,
                            points = points.toIntOrNull() ?: 10
                        )
                    )
                },
                enabled = questionText.isNotBlank() && options.any { it.isNotBlank() }
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun GenerateQuestionsDialog(
    onDismiss: () -> Unit,
    onGenerate: (Int, List<QuestionType>, Int) -> Unit
) {
    var count by remember { mutableStateOf("10") }
    var difficulty by remember { mutableStateOf(1) }
    var selectedTypes by remember { mutableStateOf(setOf(QuestionType.MULTIPLE_CHOICE)) }
    
    val allTypes = listOf(
        QuestionType.MULTIPLE_CHOICE to "Múltipla escolha",
        QuestionType.NOTE_IDENTIFICATION to "Identificar nota",
        QuestionType.INTERVAL_RECOGNITION to "Intervalos",
        QuestionType.CHORD_RECOGNITION to "Acordes",
        QuestionType.KEY_SIGNATURE to "Armaduras",
        QuestionType.SYMBOL_IDENTIFICATION to "Símbolos"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gerar Perguntas Automaticamente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it.filter { c -> c.isDigit() } },
                    label = { Text("Quantidade") },
                    modifier = Modifier.width(120.dp)
                )
                
                Text("Dificuldade", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Fácil", 2 to "Médio", 3 to "Difícil").forEach { (level, label) ->
                        FilterChip(
                            selected = difficulty == level,
                            onClick = { difficulty = level },
                            label = { Text(label) }
                        )
                    }
                }
                
                Text("Tipos de pergunta", style = MaterialTheme.typography.labelMedium)
                allTypes.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (type, label) ->
                            FilterChip(
                                selected = type in selectedTypes,
                                onClick = {
                                    selectedTypes = if (type in selectedTypes) {
                                        selectedTypes - type
                                    } else {
                                        selectedTypes + type
                                    }
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGenerate(
                        count.toIntOrNull() ?: 10,
                        selectedTypes.toList(),
                        difficulty
                    )
                },
                enabled = selectedTypes.isNotEmpty()
            ) {
                Text("Gerar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun AssignToClassDialog(
    classes: List<StudentClass>,
    onDismiss: () -> Unit,
    onAssign: (String, String?) -> Unit
) {
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atribuir à Turma") },
        text = {
            Column {
                classes.forEach { studentClass ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedClassId = studentClass.id }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedClassId == studentClass.id,
                            onClick = { selectedClassId = studentClass.id }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(studentClass.name, fontWeight = FontWeight.Medium)
                            studentClass.description?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedClassId?.let { onAssign(it, null) } },
                enabled = selectedClassId != null
            ) {
                Text("Atribuir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun rememberScrollState() = androidx.compose.foundation.rememberScrollState()
