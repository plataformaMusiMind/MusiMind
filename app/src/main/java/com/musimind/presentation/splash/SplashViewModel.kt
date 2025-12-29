package com.musimind.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.local.OnboardingManager
import com.musimind.data.local.OnboardingStep
import com.musimind.data.repository.UserRepository
import com.musimind.domain.locale.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Splash screen destination after checking auth status
 */
sealed class SplashDestination {
    data object Loading : SplashDestination()
    data object LanguageSelection : SplashDestination()
    data object Login : SplashDestination()
    data object UserType : SplashDestination()
    data object PlanSelection : SplashDestination()
    data object AvatarSelection : SplashDestination()
    data object OnboardingTutorial : SplashDestination()
    data object Home : SplashDestination()
}

/**
 * ViewModel for Splash Screen
 * Handles checking authentication state and determining navigation
 * Based on both auth status AND onboarding completion
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: Auth,
    private val userRepository: UserRepository,
    private val onboardingManager: OnboardingManager,
    private val localeManager: LocaleManager
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkInitialState()
    }

    /**
     * Check language selection first, then auth status
     */
    private fun checkInitialState() {
        viewModelScope.launch {
            // First check if user has selected a language
            val needsLanguageSelection = localeManager.needsLanguageSelection()
            
            if (needsLanguageSelection) {
                _destination.value = SplashDestination.LanguageSelection
                return@launch
            }
            
            // Apply saved locale
            localeManager.applySavedLocale()
            
            // Then check auth status
            checkAuthStatus()
        }
    }

    /**
     * Check authentication status and onboarding progress
     */
    private suspend fun checkAuthStatus() {
        val session = auth.currentSessionOrNull()
        
        if (session == null) {
            // Not logged in - go to login
            _destination.value = SplashDestination.Login
            return
        }
        
        val userId = session.user?.id
        if (userId == null) {
            _destination.value = SplashDestination.Login
            return
        }
        
        // User is logged in, check onboarding status
        val currentStep = onboardingManager.currentStep.first()
        
        when (currentStep) {
            OnboardingStep.NOT_STARTED -> {
                // Start onboarding for this user
                onboardingManager.startOnboarding(userId)
                _destination.value = SplashDestination.UserType
            }
            OnboardingStep.USER_TYPE -> {
                _destination.value = SplashDestination.UserType
            }
            OnboardingStep.PLAN_SELECTION -> {
                _destination.value = SplashDestination.PlanSelection
            }
            OnboardingStep.AVATAR -> {
                _destination.value = SplashDestination.AvatarSelection
            }
            OnboardingStep.TUTORIAL -> {
                _destination.value = SplashDestination.OnboardingTutorial
            }
            OnboardingStep.COMPLETED -> {
                // Verify user exists in database
                val user = userRepository.getUserById(userId)
                if (user != null) {
                    _destination.value = SplashDestination.Home
                } else {
                    // User in auth but not in DB - restart onboarding
                    onboardingManager.startOnboarding(userId)
                    _destination.value = SplashDestination.UserType
                }
            }
        }
    }

    /**
     * Check if user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentSessionOrNull() != null
    }
}
