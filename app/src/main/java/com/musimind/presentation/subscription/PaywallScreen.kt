package com.musimind.presentation.subscription

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.domain.model.SubscriptionPricing
import com.musimind.domain.model.SubscriptionTier
import com.musimind.ui.theme.Primary
import com.musimind.ui.theme.PrimaryVariant
import com.musimind.ui.theme.XpGold

/**
 * Premium Paywall Screen
 * 
 * Beautiful, conversion-optimized paywall with:
 * - Plan comparison
 * - Monthly/Yearly toggle with savings highlight
 * - Feature breakdown by tier
 * - Clear CTAs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onBack: () -> Unit,
    onSubscriptionComplete: () -> Unit,
    highlightTier: SubscriptionTier? = null,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    
    var isYearly by remember { mutableStateOf(true) }
    var selectedTier by remember { mutableStateOf(highlightTier ?: SubscriptionTier.MAESTRO) }
    
    // Handle checkout URL
    LaunchedEffect(state.checkoutUrl) {
        state.checkoutUrl?.let { url ->
            uriHandler.openUri(url)
            viewModel.clearCheckoutUrl()
        }
    }
    
    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(state.error, state.message) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    
    // Determine currency (would be based on user locale in production)
    val currency = "BRL"
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Fechar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Primary.copy(alpha = 0.1f), Color.Transparent)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Logo/Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Primary, PrimaryVariant))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Desbloqueie Todo o Potencial",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Acesse 100% das funcionalidades e acelere seu aprendizado musical",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Billing Toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp)
                    ) {
                        // Monthly
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (!isYearly) Primary else Color.Transparent
                                )
                                .clickable { isYearly = false }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Mensal",
                                color = if (!isYearly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (!isYearly) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        
                        // Yearly
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isYearly) Primary else Color.Transparent
                                )
                                .clickable { isYearly = true }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Anual",
                                    color = if (isYearly) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isYearly) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isYearly) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(
                                        containerColor = XpGold
                                    ) {
                                        Text(
                                            text = "-17%",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Plan Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Spalla Plan
                PlanCard(
                    tier = SubscriptionTier.SPALLA,
                    pricing = SubscriptionPricing.forTier(SubscriptionTier.SPALLA, currency),
                    isYearly = isYearly,
                    isSelected = selectedTier == SubscriptionTier.SPALLA,
                    isCurrentPlan = state.currentTier == SubscriptionTier.SPALLA,
                    onSelect = { selectedTier = SubscriptionTier.SPALLA }
                )
                
                // Maestro Plan (Highlighted)
                PlanCard(
                    tier = SubscriptionTier.MAESTRO,
                    pricing = SubscriptionPricing.forTier(SubscriptionTier.MAESTRO, currency),
                    isYearly = isYearly,
                    isSelected = selectedTier == SubscriptionTier.MAESTRO,
                    isCurrentPlan = state.currentTier == SubscriptionTier.MAESTRO,
                    isMostPopular = true,
                    onSelect = { selectedTier = SubscriptionTier.MAESTRO }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Features Comparison
            FeaturesComparison(selectedTier = selectedTier)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // CTA Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.startCheckout(selectedTier, isYearly, currency)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !state.isCheckoutLoading && state.currentTier != selectedTier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTier == SubscriptionTier.MAESTRO) 
                            XpGold else Primary
                    )
                ) {
                    if (state.isCheckoutLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = if (state.currentTier == selectedTier) 
                                "Plano Atual" 
                            else 
                                "Começar 7 dias grátis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTier == SubscriptionTier.MAESTRO) 
                                Color.Black else Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Trial info
            Text(
                text = "7 dias grátis, depois ${
                    if (isYearly) 
                        SubscriptionPricing.forTier(selectedTier, currency).formattedYearly + "/ano"
                    else 
                        SubscriptionPricing.forTier(selectedTier, currency).formattedMonthly + "/mês"
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cancel anytime
            Text(
                text = "Cancele quando quiser • Sem compromisso",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Restore purchase link
            TextButton(
                onClick = { /* TODO: Restore purchase */ },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Restaurar compra",
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PlanCard(
    tier: SubscriptionTier,
    pricing: SubscriptionPricing,
    isYearly: Boolean,
    isSelected: Boolean,
    isCurrentPlan: Boolean,
    isMostPopular: Boolean = false,
    onSelect: () -> Unit
) {
    val borderColor = when {
        isSelected && tier == SubscriptionTier.MAESTRO -> XpGold
        isSelected -> Primary
        else -> Color.Transparent
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 0.dp
        )
    ) {
        Box {
            // Most popular badge
            if (isMostPopular) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(XpGold)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "MAIS POPULAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            
            // Current plan badge
            if (isCurrentPlan) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4CAF50))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PLANO ATUAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Plan icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                when (tier) {
                                    SubscriptionTier.SPALLA -> Color(0xFF6366F1).copy(alpha = 0.2f)
                                    SubscriptionTier.MAESTRO -> XpGold.copy(alpha = 0.2f)
                                    else -> Color.Gray.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (tier) {
                                SubscriptionTier.SPALLA -> Icons.Default.MusicNote
                                SubscriptionTier.MAESTRO -> Icons.Default.Star
                                else -> Icons.Default.Person
                            },
                            contentDescription = null,
                            tint = when (tier) {
                                SubscriptionTier.SPALLA -> Color(0xFF6366F1)
                                SubscriptionTier.MAESTRO -> XpGold
                                else -> Color.Gray
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = tier.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tier.percentage}% de acesso",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Price
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (isYearly) pricing.formattedMonthlyInYearly.substringBefore("/")
                               else pricing.formattedMonthly,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isYearly) "/mês" else "/mês",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isYearly) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(cobrado anualmente)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                // Yearly total
                if (isYearly) {
                    Text(
                        text = "Total: ${pricing.formattedYearly}/ano",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Key features
                val features = when (tier) {
                    SubscriptionTier.SPALLA -> listOf(
                        "70% de todas as funcionalidades",
                        "Até 15 exercícios por dia",
                        "Sem anúncios",
                        "30 dias de histórico"
                    )
                    SubscriptionTier.MAESTRO -> listOf(
                        "100% ILIMITADO",
                        "Modo Professor",
                        "Modo Offline",
                        "Histórico completo"
                    )
                    else -> emptyList()
                }
                
                features.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = when (tier) {
                                SubscriptionTier.MAESTRO -> XpGold
                                else -> Color(0xFF4CAF50)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturesComparison(selectedTier: SubscriptionTier) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "O que você ganha",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val features = listOf(
            Triple("Exercícios diários", Icons.Default.FitnessCenter, "3 → Ilimitado"),
            Triple("Mini-games", Icons.Default.SportsEsports, "2 → Todos"),
            Triple("Histórico", Icons.Default.History, "7 dias → Completo"),
            Triple("Detecção de pitch", Icons.Default.Mic, "Incluído"),
            Triple("Quiz Multiplayer", Icons.Default.Groups, "Incluído"),
            Triple("Duelos 1v1", Icons.Default.Sports, "Incluído"),
            Triple("Modo Professor", Icons.Default.School, "Maestro"),
            Triple("Modo Offline", Icons.Default.CloudOff, "Maestro")
        )
        
        features.forEach { (name, icon, value) ->
            FeatureRow(
                name = name,
                icon = icon,
                value = value,
                isHighlighted = selectedTier == SubscriptionTier.MAESTRO || 
                               value != "Maestro"
            )
        }
    }
}

@Composable
private fun FeatureRow(
    name: String,
    icon: ImageVector,
    value: String,
    isHighlighted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isHighlighted) Primary else MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (isHighlighted) 
                MaterialTheme.colorScheme.onSurface 
            else 
                MaterialTheme.colorScheme.outline
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = when {
                value == "Maestro" && isHighlighted -> XpGold
                value == "Maestro" -> MaterialTheme.colorScheme.outline
                else -> Color(0xFF4CAF50)
            }
        )
    }
}
