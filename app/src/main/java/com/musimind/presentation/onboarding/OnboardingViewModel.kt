package com.musimind.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.local.OnboardingManager
import com.musimind.data.repository.UserRepository
import com.musimind.domain.model.Plan
import com.musimind.domain.model.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for onboarding screens
 * Manages state and saves progress to OnboardingManager
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingManager: OnboardingManager,
    private val userRepository: UserRepository,
    private val auth: Auth
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Save user type selection and update user in database
     */
    fun saveUserType(userType: UserType) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Save to local cache
                onboardingManager.saveUserType(userType.name)
                
                // Update user in database
                val userId = auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    userRepository.updateUserField(userId, "user_type", userType.name.lowercase())
                }
                
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erro ao salvar tipo de usuário: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Save plan selection and update user in database
     */
    fun savePlan(plan: Plan) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Save to local cache
                onboardingManager.savePlan(plan.name)
                
                // Update user in database
                val userId = auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    userRepository.updateUserField(userId, "subscription_plan", plan.name)
                }
                
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erro ao salvar plano: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Save avatar selection and update user in database
     */
    fun saveAvatar(avatarUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Save to local cache
                onboardingManager.saveAvatar(avatarUrl)
                
                // Update user in database
                val userId = auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    userRepository.updateUserField(userId, "avatar_url", avatarUrl)
                }
                
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erro ao salvar avatar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Complete the tutorial and mark onboarding as finished
     */
    fun completeTutorial() {
        viewModelScope.launch {
            try {
                onboardingManager.completeTutorial()
                
                // Update user to mark onboarding as complete
                val userId = auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    userRepository.updateUserField(userId, "onboarding_completed", true)
                }
            } catch (e: Exception) {
                _error.value = "Erro ao completar onboarding: ${e.message}"
            }
        }
    }

    /**
     * Clear any error
     */
    fun clearError() {
        _error.value = null
    }
}
