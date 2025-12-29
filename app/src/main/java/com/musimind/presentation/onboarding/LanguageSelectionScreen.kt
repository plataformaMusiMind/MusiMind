package com.musimind.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
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
import com.musimind.domain.locale.AppLanguage
import com.musimind.ui.theme.Primary
import com.musimind.ui.theme.PrimaryVariant
import com.musimind.ui.theme.XpGold
import kotlinx.coroutines.launch

/**
 * Language Selection Screen
 * 
 * Shown BEFORE login on first app launch.
 * Beautiful, musical-themed language picker.
 */
@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: () -> Unit,
    viewModel: LanguageSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Animated background notes
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val noteOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "noteRotation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        Primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Logo with animation
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Primary, PrimaryVariant))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = "MusiMind",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle in multiple languages
            AnimatedLanguageSubtitle()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Language prompt
            Text(
                text = "Choose your language",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Escolha seu idioma • Elige tu idioma",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Language Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(AppLanguage.entries.toList()) { language ->
                    LanguageCard(
                        language = language,
                        isSelected = state.selectedLanguage == language,
                        onClick = { viewModel.selectLanguage(language) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Continue button
            Button(
                onClick = {
                    scope.launch {
                        viewModel.confirmSelection()
                        onLanguageSelected()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state.selectedLanguage != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                )
            ) {
                Text(
                    text = getContinueText(state.selectedLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Musical note hint
            AnimatedVisibility(
                visible = state.selectedLanguage != null,
                enter = fadeIn() + slideInVertically { it }
            ) {
                Text(
                    text = getMusicalHint(state.selectedLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LanguageCard(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        label = "scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Primary else Color.Transparent,
        label = "border"
    )
    
    Card(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Primary.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Flag emoji
                Text(
                    text = language.flag,
                    fontSize = 48.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Native name
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface
                )
                
                // Musical tradition hint
                Text(
                    text = getMusicalTraditionHint(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AnimatedLanguageSubtitle() {
    val subtitles = listOf(
        "Learn music your way",
        "Aprenda música do seu jeito",
        "Aprende música a tu manera",
        "Lerne Musik auf deine Art",
        "Apprenez la musique à votre façon",
        "用你的方式学音乐"
    )
    
    var currentIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2500)
            currentIndex = (currentIndex + 1) % subtitles.size
        }
    }
    
    AnimatedContent(
        targetState = subtitles[currentIndex],
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) + 
            slideInVertically { -it / 2 } togetherWith 
            fadeOut(animationSpec = tween(500)) + 
            slideOutVertically { it / 2 }
        },
        label = "subtitle"
    ) { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun getMusicalTraditionHint(language: AppLanguage): String {
    return when (language) {
        AppLanguage.PORTUGUESE_BR -> "Dó-Ré-Mi"
        AppLanguage.ENGLISH_US -> "C-D-E"
        AppLanguage.SPANISH -> "Do-Re-Mi"
        AppLanguage.GERMAN -> "C-D-E-H"
        AppLanguage.FRENCH -> "Do-Ré-Mi"
        AppLanguage.CHINESE_SIMPLIFIED -> "1-2-3"
    }
}

private fun getContinueText(language: AppLanguage?): String {
    return when (language) {
        AppLanguage.PORTUGUESE_BR -> "Continuar"
        AppLanguage.ENGLISH_US -> "Continue"
        AppLanguage.SPANISH -> "Continuar"
        AppLanguage.GERMAN -> "Weiter"
        AppLanguage.FRENCH -> "Continuer"
        AppLanguage.CHINESE_SIMPLIFIED -> "继续"
        null -> "Continue"
    }
}

private fun getMusicalHint(language: AppLanguage?): String {
    return when (language) {
        AppLanguage.PORTUGUESE_BR -> "🎵 Você usará a notação Dó-Ré-Mi"
        AppLanguage.ENGLISH_US -> "🎵 You'll use letter notation (C-D-E)"
        AppLanguage.SPANISH -> "🎵 Usarás la notación Do-Re-Mi"
        AppLanguage.GERMAN -> "🎵 Du verwendest C-D-E-H Notation"
        AppLanguage.FRENCH -> "🎵 Vous utiliserez la notation Do-Ré-Mi"
        AppLanguage.CHINESE_SIMPLIFIED -> "🎵 您将使用数字记谱法 (1-2-3)"
        null -> ""
    }
}
