package com.musimind.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.musimind.domain.model.AuthProvider
import com.musimind.domain.model.Plan
import com.musimind.domain.model.User
import com.musimind.domain.model.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * UI State for Authentication screens
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoginSuccess: Boolean = false,
    val isNewUser: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: User? = null
)

/**
 * ViewModel for Login and Register screens
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Login with email and password
     */
    fun loginWithEmail(email: String, password: String) {
        if (!validateEmail(email)) {
            _uiState.update { it.copy(errorMessage = "E-mail inválido") }
            return
        }
        
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "A senha deve ter pelo menos 6 caracteres") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid
                
                if (userId != null) {
                    // Check if user exists in Firestore
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    
                    if (userDoc.exists()) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                isLoginSuccess = true,
                                isNewUser = false
                            )
                        }
                    } else {
                        // User exists in Auth but not in Firestore - needs onboarding
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                isLoginSuccess = true,
                                isNewUser = true
                            )
                        }
                    }
                }
            } catch (e: FirebaseAuthException) {
                val errorMessage = when (e.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "Usuário não encontrado"
                    "ERROR_WRONG_PASSWORD" -> "Senha incorreta"
                    "ERROR_USER_DISABLED" -> "Usuário desativado"
                    "ERROR_INVALID_EMAIL" -> "E-mail inválido"
                    else -> "Erro ao fazer login: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Erro ao fazer login. Verifique sua conexão."
                    ) 
                }
            }
        }
    }

    /**
     * Login with Google
     * Note: This requires Activity result handling in the screen
     */
    fun loginWithGoogle() {
        // TODO: Implement Google Sign-In
        // This will be triggered via Activity Result API
        _uiState.update { it.copy(errorMessage = "Login com Google será implementado em breve") }
    }

    /**
     * Register new user with email and password
     */
    fun registerWithEmail(
        email: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        phone: String
    ) {
        // Validation
        if (fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nome é obrigatório") }
            return
        }
        
        if (!validateEmail(email)) {
            _uiState.update { it.copy(errorMessage = "E-mail inválido") }
            return
        }
        
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "A senha deve ter pelo menos 6 caracteres") }
            return
        }
        
        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "As senhas não coincidem") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid
                
                if (userId != null) {
                    // Create user in Firestore with basic info
                    val newUser = User(
                        id = userId,
                        email = email,
                        fullName = fullName,
                        phone = phone.ifBlank { null },
                        authProvider = AuthProvider.EMAIL
                    )
                    
                    firestore.collection("users")
                        .document(userId)
                        .set(newUser)
                        .await()
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isLoginSuccess = true,
                            isNewUser = true,
                            currentUser = newUser
                        ) 
                    }
                }
            } catch (e: FirebaseAuthException) {
                val errorMessage = when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Este e-mail já está em uso"
                    "ERROR_WEAK_PASSWORD" -> "Senha muito fraca"
                    "ERROR_INVALID_EMAIL" -> "E-mail inválido"
                    else -> "Erro ao criar conta: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Erro ao criar conta. Verifique sua conexão."
                    ) 
                }
            }
        }
    }

    /**
     * Update user type after registration
     */
    fun updateUserType(userType: UserType) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .update("userType", userType.name)
                    .await()
                    
                _uiState.update { 
                    it.copy(currentUser = it.currentUser?.copy(userType = userType))
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Erro ao atualizar tipo de usuário")
                }
            }
        }
    }

    /**
     * Update user plan after selection
     */
    fun updateUserPlan(plan: Plan) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .update("plan", plan.name)
                    .await()
                    
                _uiState.update { 
                    it.copy(currentUser = it.currentUser?.copy(plan = plan))
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Erro ao atualizar plano")
                }
            }
        }
    }

    /**
     * Update user avatar
     */
    fun updateUserAvatar(avatarUrl: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .update("avatarUrl", avatarUrl)
                    .await()
                    
                _uiState.update { 
                    it.copy(currentUser = it.currentUser?.copy(avatarUrl = avatarUrl))
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Erro ao atualizar avatar")
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Reset state
     */
    fun resetState() {
        _uiState.value = AuthUiState()
    }

    /**
     * Validate email format
     */
    private fun validateEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
