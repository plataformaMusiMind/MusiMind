package com.musimind.presentation.onboarding

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.R
import com.musimind.domain.locale.AppLanguage
import com.musimind.ui.theme.Primary
import com.musimind.ui.theme.PrimaryVariant
import com.musimind.ui.theme.XpGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Language Selection Screen
 * 
 * Premium design with beautiful animations.
 * Shown BEFORE login on first app launch.
 */
@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: () -> Unit,
    viewModel: LanguageSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Animated background elements
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient"
    )
    
    // Dynamic gradient colors
    val gradientColors = listOf(
        Primary.copy(alpha = 0.08f),
        PrimaryVariant.copy(alpha = 0.12f),
        Primary.copy(alpha = 0.05f),
        Color(0xFF6B48FF).copy(alpha = 0.08f)
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Animated background circles
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-50).dp)
                .scale(pulseScale)
                .blur(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .scale(1.2f - (pulseScale - 1f))
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryVariant.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Animated Logo with glow effect
            Box(contentAlignment = Alignment.Center) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .blur(25.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Primary.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // Main logo circle
                Card(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            ambientColor = Primary,
                            spotColor = Primary
                        ),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Primary, PrimaryVariant)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(55.dp),
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Title with gradient effect
            Text(
                text = "MusiMind",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 36.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Animated subtitle
            AnimatedLanguageSubtitle()
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Language selector header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Choose your language",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Escolha seu idioma • Elige tu idioma",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Language List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(AppLanguage.entries.toList()) { language ->
                    LanguageCard(
                        language = language,
                        isSelected = state.selectedLanguage == language,
                        onClick = { viewModel.selectLanguage(language) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Continue button with premium style
            AnimatedVisibility(
                visible = state.selectedLanguage != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.confirmSelection()
                                onLanguageSelected()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = Primary.copy(alpha = 0.4f),
                                spotColor = Primary.copy(alpha = 0.4f)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        ),
                        contentPadding = PaddingValues(horizontal = 32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = getContinueText(state.selectedLanguage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Musical notation hint
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = getMusicalHint(state.selectedLanguage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.5.dp else 0.dp,
        label = "border"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        label = "elevation"
    )
    
    Card(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isSelected) Primary.copy(alpha = 0.3f) else Color.Transparent,
                spotColor = if (isSelected) Primary.copy(alpha = 0.3f) else Color.Transparent
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = borderWidth,
                        brush = Brush.linearGradient(
                            colors = listOf(Primary, PrimaryVariant)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Primary.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag emoji with background
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected)
                            Primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = language.flag,
                    fontSize = 28.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Language name and tradition hint
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = getMusicalTraditionHint(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Selection indicator
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Primary, PrimaryVariant)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
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
            delay(2500)
            currentIndex = (currentIndex + 1) % subtitles.size
        }
    }
    
    AnimatedContent(
        targetState = subtitles[currentIndex],
        transitionSpec = {
            (fadeIn(animationSpec = tween(600)) + 
             slideInVertically { -it / 3 }) togetherWith 
            (fadeOut(animationSpec = tween(400)) + 
             slideOutVertically { it / 3 })
        },
        label = "subtitle"
    ) { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getMusicalTraditionHint(language: AppLanguage): String {
    return when (language) {
        AppLanguage.PORTUGUESE_BR -> "Notação: Dó-Ré-Mi-Fá-Sol-Lá-Si"
        AppLanguage.ENGLISH_US -> "Notation: C-D-E-F-G-A-B"
        AppLanguage.SPANISH -> "Notación: Do-Re-Mi-Fa-Sol-La-Si"
        AppLanguage.GERMAN -> "Notation: C-D-E-F-G-A-H"
        AppLanguage.FRENCH -> "Notation: Do-Ré-Mi-Fa-Sol-La-Si"
        AppLanguage.CHINESE_SIMPLIFIED -> "记谱法: 1-2-3-4-5-6-7"
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
        AppLanguage.PORTUGUESE_BR -> "🎵 Você usará a notação Dó-Ré-Mi (solfejo fixo)"
        AppLanguage.ENGLISH_US -> "🎵 You'll use letter notation C-D-E (movable Do)"
        AppLanguage.SPANISH -> "🎵 Usarás la notación Do-Re-Mi (solfeo fijo)"
        AppLanguage.GERMAN -> "🎵 Du verwendest C-D-E-H Notation"
        AppLanguage.FRENCH -> "🎵 Vous utiliserez la notation Do-Ré-Mi"
        AppLanguage.CHINESE_SIMPLIFIED -> "🎵 您将使用数字记谱法 (1-2-3)"
        null -> ""
    }
}
