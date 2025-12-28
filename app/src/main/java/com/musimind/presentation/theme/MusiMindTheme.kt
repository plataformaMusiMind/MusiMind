package com.musimind.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * MusiMind Premium Theme System
 * 
 * Features:
 * - Light and Dark modes
 * - Multiple color schemes (Classic, Ocean, Forest, Sunset, Galaxy)
 * - Dynamic colors on Android 12+
 * - Animated color transitions
 * - Custom typography and shapes
 */

// ============================================
// Color Schemes
// ============================================

enum class MusiMindThemeStyle {
    CLASSIC,    // Purple/Teal
    OCEAN,      // Blue/Cyan
    FOREST,     // Green/Lime
    SUNSET,     // Orange/Amber
    GALAXY      // Deep Purple/Pink
}

// Classic Theme (Default)
private val ClassicLightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA7F3EC),
    onTertiaryContainer = Color(0xFF00201D),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onError = Color.White,
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private val ClassicDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFF4DB6AC),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFFA7F3EC),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    onError = Color(0xFF601410),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// Ocean Theme
private val OceanLightColors = lightColorScheme(
    primary = Color(0xFF0277BD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB3E5FC),
    onPrimaryContainer = Color(0xFF01579B),
    secondary = Color(0xFF00ACC1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFF26A69A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF00695C),
    background = Color(0xFFFAFCFD),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFAFCFD),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E8EC),
    onSurfaceVariant = Color(0xFF42474A)
)

private val OceanDarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF01579B),
    primaryContainer = Color(0xFF0288D1),
    onPrimaryContainer = Color(0xFFB3E5FC),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF006064),
    secondaryContainer = Color(0xFF00838F),
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF00695C),
    tertiaryContainer = Color(0xFF00897B),
    onTertiaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E3E5),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E3E5),
    surfaceVariant = Color(0xFF42474A),
    onSurfaceVariant = Color(0xFFC2C7CB)
)

// Forest Theme
private val ForestLightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF558B2F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5E1A5),
    onSecondaryContainer = Color(0xFF33691E),
    tertiary = Color(0xFF00796B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF80CBC4),
    onTertiaryContainer = Color(0xFF004D40),
    background = Color(0xFFFAFDF7),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFFAFDF7),
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFE1E8DC),
    onSurfaceVariant = Color(0xFF424940)
)

private val ForestDarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF388E3C),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFF9CCC65),
    onSecondary = Color(0xFF33691E),
    secondaryContainer = Color(0xFF689F38),
    onSecondaryContainer = Color(0xFFC5E1A5),
    tertiary = Color(0xFF4DB6AC),
    onTertiary = Color(0xFF004D40),
    tertiaryContainer = Color(0xFF00897B),
    onTertiaryContainer = Color(0xFF80CBC4),
    background = Color(0xFF1A1C18),
    onBackground = Color(0xFFE2E4DD),
    surface = Color(0xFF1A1C18),
    onSurface = Color(0xFFE2E4DD),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BE)
)

// Sunset Theme
private val SunsetLightColors = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCC80),
    onPrimaryContainer = Color(0xFFBF360C),
    secondary = Color(0xFFF57C00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFFE65100),
    tertiary = Color(0xFFD84315),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFAB91),
    onTertiaryContainer = Color(0xFFBF360C),
    background = Color(0xFFFFFDF9),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF0E6DA),
    onSurfaceVariant = Color(0xFF4F4539)
)

private val SunsetDarkColors = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFFBF360C),
    primaryContainer = Color(0xFFF57C00),
    onPrimaryContainer = Color(0xFFFFCC80),
    secondary = Color(0xFFFFCC80),
    onSecondary = Color(0xFFE65100),
    secondaryContainer = Color(0xFFFB8C00),
    onSecondaryContainer = Color(0xFFFFE0B2),
    tertiary = Color(0xFFFF8A65),
    onTertiary = Color(0xFFBF360C),
    tertiaryContainer = Color(0xFFE64A19),
    onTertiaryContainer = Color(0xFFFFAB91),
    background = Color(0xFF1F1B16),
    onBackground = Color(0xFFEBE1D9),
    surface = Color(0xFF1F1B16),
    onSurface = Color(0xFFEBE1D9),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4)
)

// Galaxy Theme
private val GalaxyLightColors = lightColorScheme(
    primary = Color(0xFF7B1FA2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1BEE7),
    onPrimaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFFE91E63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8BBD9),
    onSecondaryContainer = Color(0xFF880E4F),
    tertiary = Color(0xFF3F51B5),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC5CAE9),
    onTertiaryContainer = Color(0xFF1A237E),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1B1021),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1B1021),
    surfaceVariant = Color(0xFFE9E0F0),
    onSurfaceVariant = Color(0xFF4A4453)
)

private val GalaxyDarkColors = darkColorScheme(
    primary = Color(0xFFCE93D8),
    onPrimary = Color(0xFF4A148C),
    primaryContainer = Color(0xFF8E24AA),
    onPrimaryContainer = Color(0xFFE1BEE7),
    secondary = Color(0xFFF48FB1),
    onSecondary = Color(0xFF880E4F),
    secondaryContainer = Color(0xFFD81B60),
    onSecondaryContainer = Color(0xFFF8BBD9),
    tertiary = Color(0xFF9FA8DA),
    onTertiary = Color(0xFF1A237E),
    tertiaryContainer = Color(0xFF5C6BC0),
    onTertiaryContainer = Color(0xFFC5CAE9),
    background = Color(0xFF1B1021),
    onBackground = Color(0xFFE6E0EB),
    surface = Color(0xFF1B1021),
    onSurface = Color(0xFFE6E0EB),
    surfaceVariant = Color(0xFF4A4453),
    onSurfaceVariant = Color(0xFFCCC4D6)
)

// ============================================
// Theme State Management
// ============================================

@Stable
class MusiMindThemeState(
    initialStyle: MusiMindThemeStyle = MusiMindThemeStyle.CLASSIC,
    initialDarkMode: Boolean? = null // null = follow system
) {
    var style by mutableStateOf(initialStyle)
    var darkModeOverride by mutableStateOf(initialDarkMode)
    
    fun setThemeStyle(newStyle: MusiMindThemeStyle) {
        style = newStyle
    }
    
    fun setDarkMode(isDark: Boolean?) {
        darkModeOverride = isDark
    }
    
    fun toggleDarkMode(systemIsDark: Boolean) {
        darkModeOverride = when (darkModeOverride) {
            null -> !systemIsDark
            true -> false
            false -> null
        }
    }
}

val LocalMusiMindTheme = staticCompositionLocalOf { 
    MusiMindThemeState() 
}

// ============================================
// Theme Provider
// ============================================

@Composable
fun MusiMindTheme(
    themeState: MusiMindThemeState = remember { MusiMindThemeState() },
    useDynamicColors: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = themeState.darkModeOverride ?: systemDarkTheme
    
    val context = LocalContext.current
    
    val colorScheme = when {
        // Use dynamic colors on Android 12+ if enabled
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        else -> getColorScheme(themeState.style, darkTheme)
    }
    
    // Animate color transitions
    val animatedColorScheme = colorScheme.copy(
        primary = animateColorAsState(
            colorScheme.primary, 
            animationSpec = tween(500),
            label = "primary"
        ).value,
        secondary = animateColorAsState(
            colorScheme.secondary, 
            animationSpec = tween(500),
            label = "secondary"
        ).value,
        tertiary = animateColorAsState(
            colorScheme.tertiary, 
            animationSpec = tween(500),
            label = "tertiary"
        ).value,
        background = animateColorAsState(
            colorScheme.background, 
            animationSpec = tween(500),
            label = "background"
        ).value,
        surface = animateColorAsState(
            colorScheme.surface, 
            animationSpec = tween(500),
            label = "surface"
        ).value
    )
    
    // Update system bars
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = animatedColorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    CompositionLocalProvider(LocalMusiMindTheme provides themeState) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = MusiMindTypography,
            content = content
        )
    }
}

private fun getColorScheme(style: MusiMindThemeStyle, darkTheme: Boolean): ColorScheme {
    return when (style) {
        MusiMindThemeStyle.CLASSIC -> if (darkTheme) ClassicDarkColors else ClassicLightColors
        MusiMindThemeStyle.OCEAN -> if (darkTheme) OceanDarkColors else OceanLightColors
        MusiMindThemeStyle.FOREST -> if (darkTheme) ForestDarkColors else ForestLightColors
        MusiMindThemeStyle.SUNSET -> if (darkTheme) SunsetDarkColors else SunsetLightColors
        MusiMindThemeStyle.GALAXY -> if (darkTheme) GalaxyDarkColors else GalaxyLightColors
    }
}

// ============================================
// Typography
// ============================================

private val MusiMindTypography = Typography(
    // Uses default Material3 typography
    // Can be customized with Google Fonts like Inter, Outfit, etc.
)

// ============================================
// Theme Preview Helpers
// ============================================

@Composable
fun ThemePreviewCard(
    style: MusiMindThemeStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = getColorScheme(style, false)
    
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.primaryContainer else colors.surface
        ),
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color preview row
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = style.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
