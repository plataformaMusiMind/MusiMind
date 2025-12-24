package com.musimind.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.domain.model.Plan
import com.musimind.ui.theme.PlanAprendiz
import com.musimind.ui.theme.PlanMaestro
import com.musimind.ui.theme.PlanSpalla

/**
 * Plan Selection Screen - Part of onboarding flow
 * Uses Scaffold to ensure button is always visible at the bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSelectionScreen(
    onPlanSelected: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var selectedPlan by remember { mutableStateOf<Plan?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escolha seu Plano") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // FIXED BOTTOM BUTTON - Always visible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        selectedPlan?.let { plan ->
                            viewModel.savePlan(plan)
                            onPlanSelected()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = selectedPlan != null
                ) {
                    Text(
                        text = when (selectedPlan) {
                            Plan.APRENDIZ -> "Começar Grátis"
                            null -> "Selecione um plano"
                            else -> "Continuar"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Desbloqueie todo o potencial da sua jornada musical",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Plan Cards
            PlanCard(
                plan = Plan.APRENDIZ,
                title = "Aprendiz",
                price = "Grátis",
                description = "Comece sua jornada",
                features = listOf(
                    "Acesso à trilha básica",
                    "5 exercícios por dia",
                    "Solfejo básico"
                ),
                color = PlanAprendiz,
                isSelected = selectedPlan == Plan.APRENDIZ,
                onClick = { selectedPlan = Plan.APRENDIZ }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PlanCard(
                plan = Plan.SPALLA,
                title = "Spalla",
                price = "R$ 19,90/mês",
                description = "Para estudantes dedicados",
                features = listOf(
                    "Trilha completa desbloqueada",
                    "Exercícios ilimitados",
                    "Solfejo avançado",
                    "Percepção melódica",
                    "Desafios diários"
                ),
                color = PlanSpalla,
                isSelected = selectedPlan == Plan.SPALLA,
                isPopular = true,
                onClick = { selectedPlan = Plan.SPALLA }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PlanCard(
                plan = Plan.MAESTRO,
                title = "Maestro",
                price = "R$ 29,90/mês",
                description = "A experiência completa",
                features = listOf(
                    "Tudo do plano Spalla",
                    "Progressões harmônicas",
                    "Modo offline",
                    "Relatórios detalhados",
                    "Suporte prioritário",
                    "Sem anúncios"
                ),
                color = PlanMaestro,
                isSelected = selectedPlan == Plan.MAESTRO,
                onClick = { selectedPlan = Plan.MAESTRO }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    title: String,
    price: String,
    description: String,
    features: List<String>,
    color: Color,
    isSelected: Boolean,
    isPopular: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = color,
                        shape = RoundedCornerShape(20.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                color.copy(alpha = 0.15f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (isPopular) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "POPULAR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Features
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
