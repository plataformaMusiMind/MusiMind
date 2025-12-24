package com.musimind.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.musimind.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Splash screen destination after checking auth status
 */
sealed class SplashDestination {
    data object Loading : SplashDestination()
    data object Login : SplashDestination()
    data object Onboarding : SplashDestination()
    data object Home : SplashDestination()
}

/**
 * ViewModel for Splash Screen
 * Handles checking authentication state and determining navigation
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Check authentication status and user profile completeness
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser
            
            if (currentUser == null) {
                // Not logged in
                _destination.value = SplashDestination.Login
                return@launch
            }
            
            // User is logged in, check if profile is complete
            val user = userRepository.getUserById(currentUser.uid)
            
            when {
                user == null -> {
                    // User exists in Auth but not in Firestore - needs onboarding
                    _destination.value = SplashDestination.Onboarding
                }
                user.fullName.isBlank() -> {
                    // User hasn't completed profile setup
                    _destination.value = SplashDestination.Onboarding
                }
                else -> {
                    // User is fully setup, go to home
                    _destination.value = SplashDestination.Home
                }
            }
        }
    }

    /**
     * Check if user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
