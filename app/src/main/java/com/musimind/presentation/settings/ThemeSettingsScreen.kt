package com.musimind.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musimind.presentation.theme.*

/**
 * Theme Settings Screen
 * 
 * Allows users to customize:
 * - Color scheme (Classic, Ocean, Forest, Sunset, Galaxy)
 * - Dark/Light mode preference
 * - Dynamic colors toggle (Android 12+)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    themeState: MusiMindThemeState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aparência") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Dark Mode Section
            DarkModeSection(
                darkModeOverride = themeState.darkModeOverride,
                onDarkModeChanged = { themeState.setDarkMode(it) }
            )
            
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // Theme Colors Section
            ThemeColorsSection(
                selectedTheme = themeState.style,
                onThemeSelected = { themeState.setThemeStyle(it) }
            )
            
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // Preview Section
            ThemePreviewSection()
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ============================================
// Dark Mode Section
// ============================================

@Composable
private fun DarkModeSection(
    darkModeOverride: Boolean?,
    onDarkModeChanged: (Boolean?) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Modo de Exibição",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DarkModeOption(
                title = "Sistema",
                icon = Icons.Default.Smartphone,
                isSelected = darkModeOverride == null,
                onClick = { onDarkModeChanged(null) },
                modifier = Modifier.weight(1f)
            )
            
            DarkModeOption(
                title = "Claro",
                icon = Icons.Default.LightMode,
                isSelected = darkModeOverride == false,
                onClick = { onDarkModeChanged(false) },
                modifier = Modifier.weight(1f)
            )
            
            DarkModeOption(
                title = "Escuro",
                icon = Icons.Default.DarkMode,
                isSelected = darkModeOverride == true,
                onClick = { onDarkModeChanged(true) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DarkModeOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.95f,
        label = "darkModeScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant,
        label = "darkModeBg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else Color.Transparent,
        label = "darkModeBorder"
    )
    
    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================
// Theme Colors Section  
// ============================================

@Composable
private fun ThemeColorsSection(
    selectedTheme: MusiMindThemeStyle,
    onThemeSelected: (MusiMindThemeStyle) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Cores do Tema",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Theme options
        MusiMindThemeStyle.entries.forEach { theme ->
            ThemeColorOption(
                theme = theme,
                isSelected = selectedTheme == theme,
                onClick = { onThemeSelected(theme) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ThemeColorOption(
    theme: MusiMindThemeStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = getThemePreviewColors(theme)
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                      else MaterialTheme.colorScheme.surface,
        label = "themeOptionBg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary
                      else Color.Transparent,
        label = "themeOptionBorder"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color preview dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(colors.primary, colors.secondary, colors.tertiary).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Theme name and description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getThemeName(theme),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = getThemeDescription(theme),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Selection indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selecionado",
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================
// Theme Preview Section
// ============================================

@Composable
private fun ThemePreviewSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Preview card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = "Exercício de Solfejo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Nível 3 • 15 XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Buttons preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Primário")
                    }
                    
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Secundário")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Progress bar preview
                LinearProgressIndicator(
                    progress = 0.65f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Chips preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Tag 1") }
                    )
                    AssistChip(
                        onClick = {},  
                        label = { Text("Tag 2") }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("Tag 3") }
                    )
                }
            }
        }
    }
}

// ============================================
// Helper Functions
// ============================================

private data class ThemeColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

private fun getThemePreviewColors(theme: MusiMindThemeStyle): ThemeColors {
    return when (theme) {
        MusiMindThemeStyle.CLASSIC -> ThemeColors(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF00897B)
        )
        MusiMindThemeStyle.OCEAN -> ThemeColors(
            primary = Color(0xFF0277BD),
            secondary = Color(0xFF00ACC1),
            tertiary = Color(0xFF26A69A)
        )
        MusiMindThemeStyle.FOREST -> ThemeColors(
            primary = Color(0xFF2E7D32),
            secondary = Color(0xFF558B2F),
            tertiary = Color(0xFF00796B)
        )
        MusiMindThemeStyle.SUNSET -> ThemeColors(
            primary = Color(0xFFE65100),
            secondary = Color(0xFFF57C00),
            tertiary = Color(0xFFD84315)
        )
        MusiMindThemeStyle.GALAXY -> ThemeColors(
            primary = Color(0xFF7B1FA2),
            secondary = Color(0xFFE91E63),
            tertiary = Color(0xFF3F51B5)
        )
    }
}

private fun getThemeName(theme: MusiMindThemeStyle): String {
    return when (theme) {
        MusiMindThemeStyle.CLASSIC -> "Clássico"
        MusiMindThemeStyle.OCEAN -> "Oceano"
        MusiMindThemeStyle.FOREST -> "Floresta"
        MusiMindThemeStyle.SUNSET -> "Pôr do Sol"
        MusiMindThemeStyle.GALAXY -> "Galáxia"
    }
}

private fun getThemeDescription(theme: MusiMindThemeStyle): String {
    return when (theme) {
        MusiMindThemeStyle.CLASSIC -> "Roxo e verde-água, elegante e moderno"
        MusiMindThemeStyle.OCEAN -> "Tons de azul, calmo e refrescante"
        MusiMindThemeStyle.FOREST -> "Verdes naturais, sereno e equilibrado"
        MusiMindThemeStyle.SUNSET -> "Laranjas vibrantes, energético e quente"
        MusiMindThemeStyle.GALAXY -> "Roxo e rosa, cósmico e criativo"
    }
}
