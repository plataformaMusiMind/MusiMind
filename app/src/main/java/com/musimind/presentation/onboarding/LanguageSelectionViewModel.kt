package com.musimind.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.domain.locale.AppLanguage
import com.musimind.domain.locale.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    private val localeManager: LocaleManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(LanguageSelectionState())
    val state: StateFlow<LanguageSelectionState> = _state.asStateFlow()
    
    init {
        // Pre-select based on system language if available
        val systemLanguage = localeManager.getCurrentLanguageSync()
        _state.update { it.copy(selectedLanguage = systemLanguage) }
    }
    
    /**
     * Select a language (preview, not yet confirmed)
     */
    fun selectLanguage(language: AppLanguage) {
        _state.update { it.copy(selectedLanguage = language) }
        
        // Apply immediately for preview
        localeManager.applyLocale(language)
    }
    
    /**
     * Confirm and persist the language selection
     */
    suspend fun confirmSelection() {
        val language = _state.value.selectedLanguage ?: return
        
        _state.update { it.copy(isConfirming = true) }
        
        try {
            localeManager.setLanguage(language)
            _state.update { it.copy(isConfirmed = true) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        } finally {
            _state.update { it.copy(isConfirming = false) }
        }
    }
    
    /**
     * Check if user needs to select language
     */
    fun checkIfNeedsSelection() {
        viewModelScope.launch {
            val needsSelection = localeManager.needsLanguageSelection()
            _state.update { it.copy(needsSelection = needsSelection) }
        }
    }
}

data class LanguageSelectionState(
    val selectedLanguage: AppLanguage? = null,
    val isConfirming: Boolean = false,
    val isConfirmed: Boolean = false,
    val needsSelection: Boolean = true,
    val error: String? = null
)
