package com.musimind.domain.auth

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized authentication manager for MusiMind
 * Provides easy access to current user ID and authentication state
 */
@Singleton
class AuthManager @Inject constructor(
    private val auth: Auth
) {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    /**
     * Get current user ID or empty string if not logged in
     */
    val currentUserId: String
        get() = auth.currentSessionOrNull()?.user?.id ?: ""
    
    /**
     * Get current user ID or null if not logged in
     */
    val currentUserIdOrNull: String?
        get() = auth.currentSessionOrNull()?.user?.id
    
    /**
     * Check if user is currently logged in
     */
    val isLoggedIn: Boolean
        get() = auth.currentSessionOrNull() != null
    
    /**
     * Get current user email
     */
    val currentUserEmail: String?
        get() = auth.currentSessionOrNull()?.user?.email
    
    /**
     * Sign out current user
     */
    suspend fun signOut() {
        auth.signOut()
        _isAuthenticated.value = false
    }
    
    /**
     * Reset password for the given email
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Refresh authentication state
     */
    fun refreshAuthState() {
        _isAuthenticated.value = auth.currentSessionOrNull() != null
    }
}
