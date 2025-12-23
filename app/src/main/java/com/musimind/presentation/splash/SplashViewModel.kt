package com.musimind.presentation.splash

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for Splash Screen
 * Handles checking authentication state
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    /**
     * Check if user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Check if onboarding is complete
     * TODO: Implement proper check from user data
     */
    fun isOnboardingComplete(): Boolean {
        // For now, return true if user exists
        // Later will check if user has completed all onboarding steps
        return firebaseAuth.currentUser != null
    }
}
