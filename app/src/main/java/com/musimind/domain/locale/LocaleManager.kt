package com.musimind.domain.locale

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supported languages in MusiMind
 * Each language has specific musical notation traditions
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val musicalNotation: MusicalNotation
) {
    PORTUGUESE_BR(
        code = "pt",
        displayName = "Portuguese (Brazil)",
        nativeName = "Português (Brasil)",
        flag = "🇧🇷",
        musicalNotation = MusicalNotation.LATIN_FIXED
    ),
    ENGLISH_US(
        code = "en",
        displayName = "English (US)",
        nativeName = "English (US)",
        flag = "🇺🇸",
        musicalNotation = MusicalNotation.LETTER_MOVABLE
    ),
    SPANISH(
        code = "es",
        displayName = "Spanish",
        nativeName = "Español",
        flag = "🇪🇸",
        musicalNotation = MusicalNotation.LATIN_FIXED
    ),
    GERMAN(
        code = "de",
        displayName = "German",
        nativeName = "Deutsch",
        flag = "🇩🇪",
        musicalNotation = MusicalNotation.GERMAN
    ),
    FRENCH(
        code = "fr",
        displayName = "French",
        nativeName = "Français",
        flag = "🇫🇷",
        musicalNotation = MusicalNotation.LATIN_FIXED
    ),
    CHINESE_SIMPLIFIED(
        code = "zh",
        displayName = "Chinese (Simplified)",
        nativeName = "简体中文",
        flag = "🇨🇳",
        musicalNotation = MusicalNotation.NUMERIC
    );
    
    val locale: Locale
        get() = Locale.forLanguageTag(code)
    
    val languageTag: String
        get() = code
    
    companion object {
        fun fromCode(code: String): AppLanguage {
            // Busca por código exato ou pelo código base da linguagem
            return entries.find { it.code == code || it.languageTag == code }
                ?: entries.find { code.startsWith(it.code) || it.code.startsWith(code.take(2)) }
                ?: PORTUGUESE_BR
        }
        
        fun fromLocale(locale: Locale): AppLanguage {
            val language = locale.language // Código base (ex: "en", "pt")
            return entries.find { it.code == language }
                ?: PORTUGUESE_BR
        }
    }
}

/**
 * Musical notation traditions
 * Determines how notes, rhythms, and terms are displayed
 */
enum class MusicalNotation {
    /**
     * Latin/Romance tradition with fixed Do
     * Used in: Brazil, Spain, France, Italy, Portugal
     * Notes: Do, Re, Mi, Fa, Sol, La, Si
     * Solfege: Fixed (Do is always C)
     */
    LATIN_FIXED,
    
    /**
     * Anglo-Saxon letter notation with movable Do
     * Used in: USA, UK, Australia
     * Notes: C, D, E, F, G, A, B
     * Solfege: Movable (Do is the tonic)
     */
    LETTER_MOVABLE,
    
    /**
     * German notation (letter-based with H instead of B)
     * Used in: Germany, Austria, Nordic countries
     * Notes: C, D, E, F, G, A, H (B = B flat)
     * Solfege: Mixed approach
     */
    GERMAN,
    
    /**
     * Numeric notation (numbered musical notation)
     * Used in: China, Japan (partially)
     * Notes: 1, 2, 3, 4, 5, 6, 7
     * Solfege: Numeric with octave dots
     */
    NUMERIC
}

// DataStore extension
private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "language_preferences"
)

/**
 * Manager for app language/locale settings
 * Handles persistence, application, and musical terminology
 */
@Singleton
class LocaleManager @Inject constructor(
    private val context: Context
) {
    private val SELECTED_LANGUAGE_KEY = stringPreferencesKey("selected_language")
    private val HAS_SELECTED_LANGUAGE_KEY = stringPreferencesKey("has_selected_language")
    
    /**
     * Flow of the currently selected language
     */
    val selectedLanguage: Flow<AppLanguage> = context.languageDataStore.data.map { prefs ->
        val code = prefs[SELECTED_LANGUAGE_KEY] ?: getSystemLanguageCode()
        AppLanguage.fromCode(code)
    }
    
    /**
     * Check if user has already selected a language
     */
    val hasSelectedLanguage: Flow<Boolean> = context.languageDataStore.data.map { prefs ->
        prefs[HAS_SELECTED_LANGUAGE_KEY] == "true"
    }
    
    /**
     * Get current language synchronously (for non-coroutine contexts)
     */
    fun getCurrentLanguageSync(): AppLanguage {
        val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AppCompatDelegate.getApplicationLocales().get(0)
        } else {
            context.resources.configuration.locales.get(0)
        }
        return if (currentLocale != null) {
            AppLanguage.fromLocale(currentLocale)
        } else {
            AppLanguage.PORTUGUESE_BR
        }
    }
    
    /**
     * Save and apply the selected language
     */
    suspend fun setLanguage(language: AppLanguage) {
        // Persist to DataStore
        context.languageDataStore.edit { prefs ->
            prefs[SELECTED_LANGUAGE_KEY] = language.code
            prefs[HAS_SELECTED_LANGUAGE_KEY] = "true"
        }
        
        // Apply locale globally
        applyLocale(language)
    }
    
    /**
     * Apply locale using AppCompatDelegate (works on all Android versions)
     */
    fun applyLocale(language: AppLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
    
    /**
     * Apply saved locale on app start
     */
    suspend fun applySavedLocale() {
        val savedLanguage = selectedLanguage.first()
        applyLocale(savedLanguage)
    }
    
    /**
     * Check if language selection is needed
     */
    suspend fun needsLanguageSelection(): Boolean {
        return !hasSelectedLanguage.first()
    }
    
    /**
     * Get system language code
     */
    private fun getSystemLanguageCode(): String {
        val systemLocale = Locale.getDefault()
        return AppLanguage.fromLocale(systemLocale).code
    }
    
    /**
     * Get musical notation style for current language
     */
    suspend fun getMusicalNotation(): MusicalNotation {
        return selectedLanguage.first().musicalNotation
    }
    
    /**
     * Get note names array for current language
     */
    fun getNoteNames(language: AppLanguage = getCurrentLanguageSync()): Array<String> {
        return when (language.musicalNotation) {
            MusicalNotation.LATIN_FIXED -> arrayOf("Dó", "Ré", "Mi", "Fá", "Sol", "Lá", "Si")
            MusicalNotation.LETTER_MOVABLE -> arrayOf("C", "D", "E", "F", "G", "A", "B")
            MusicalNotation.GERMAN -> arrayOf("C", "D", "E", "F", "G", "A", "H")
            MusicalNotation.NUMERIC -> arrayOf("1", "2", "3", "4", "5", "6", "7")
        }
    }
    
    /**
     * Get chromatic note names (with sharps)
     */
    fun getChromaticNotes(language: AppLanguage = getCurrentLanguageSync()): Array<String> {
        return when (language.musicalNotation) {
            MusicalNotation.LATIN_FIXED -> arrayOf(
                "Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá", "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si"
            )
            MusicalNotation.LETTER_MOVABLE -> arrayOf(
                "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
            )
            MusicalNotation.GERMAN -> arrayOf(
                "C", "Cis", "D", "Dis", "E", "F", "Fis", "G", "Gis", "A", "Ais", "H"
            )
            MusicalNotation.NUMERIC -> arrayOf(
                "1", "♯1", "2", "♯2", "3", "4", "♯4", "5", "♯5", "6", "♯6", "7"
            )
        }
    }
}
