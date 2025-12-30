package com.musimind.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.UserRepository
import com.musimind.domain.auth.GoogleSignInHelper
import com.musimind.domain.auth.GoogleSignInResult
import com.musimind.domain.model.AuthProvider
import com.musimind.domain.model.Plan
import com.musimind.domain.model.User
import com.musimind.domain.model.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
 * Uses Supabase Auth for authentication
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: Auth,
    private val userRepository: UserRepository,
    private val googleSignInHelper: GoogleSignInHelper
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
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                val session = auth.currentSessionOrNull()
                val userId = session?.user?.id
                
                if (userId != null) {
                    val userExists = userRepository.userExists(userId)
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isLoginSuccess = true,
                            isNewUser = !userExists
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = "Erro ao obter sessão do usuário"
                        ) 
                    }
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("Invalid login credentials") == true -> "E-mail ou senha incorretos"
                    e.message?.contains("Email not confirmed") == true -> "E-mail não confirmado. Verifique sua caixa de entrada."
                    else -> "Erro ao fazer login: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            }
        }
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
        
        // Validação de senha forte: letras maiúsculas, minúsculas, números e símbolos
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        
        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSymbol) {
            _uiState.update { 
                it.copy(errorMessage = "A senha deve conter letras maiúsculas, minúsculas, números e símbolos") 
            }
            return
        }
        
        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "As senhas não coincidem") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                val session = auth.currentSessionOrNull()
                val userId = session?.user?.id
                
                if (userId != null) {
                    val newUser = User(
                        authId = userId,
                        email = email,
                        fullName = fullName,
                        phone = phone.ifBlank { null },
                        authProvider = AuthProvider.EMAIL
                    )
                    
                    userRepository.createOrUpdateUser(newUser)
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isLoginSuccess = true,
                            isNewUser = true,
                            currentUser = newUser
                        ) 
                    }
                } else {
                    // User created but needs email confirmation
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = "Conta criada! Verifique seu e-mail para confirmar."
                        ) 
                    }
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("already registered") == true -> "Este e-mail já está em uso"
                    e.message?.contains("weak password") == true -> "Senha muito fraca"
                    else -> "Erro ao criar conta: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            }
        }
    }

    /**
     * Sign in with Google using native Credential Manager
     * This provides a beautiful native UI without opening a browser
     */
    fun signInWithGoogle(activityContext: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                when (val result = googleSignInHelper.signIn(activityContext)) {
                    is GoogleSignInResult.Success -> {
                        // Use the ID token to sign in with Supabase
                        auth.signInWith(io.github.jan.supabase.auth.providers.Google) {
                            idToken = result.idToken
                        }
                        
                        val session = auth.currentSessionOrNull()
                        val userId = session?.user?.id
                        
                        if (userId != null) {
                            val userExists = userRepository.userExists(userId)
                            
                            if (!userExists) {
                                // Create new user profile for Google sign-in
                                val googleUser = User(
                                    authId = userId,
                                    email = result.email,
                                    fullName = result.displayName ?: result.email.substringBefore('@'),
                                    avatarUrl = result.photoUrl,
                                    authProvider = AuthProvider.GOOGLE
                                )
                                userRepository.createOrUpdateUser(googleUser)
                            }
                            
                            _uiState.update { 
                                it.copy(
                                    isLoading = false, 
                                    isLoginSuccess = true,
                                    isNewUser = !userExists
                                )
                            }
                        } else {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false, 
                                    errorMessage = "Erro ao criar sessão no servidor"
                                ) 
                            }
                        }
                    }
                    
                    is GoogleSignInResult.Cancelled -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                errorMessage = null // Não mostra erro quando usuário cancela
                            ) 
                        }
                    }
                    
                    is GoogleSignInResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                errorMessage = result.message
                            ) 
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("configuration", ignoreCase = true) == true -> 
                        "Erro de configuração. Verifique o Google Cloud Console."
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Erro de conexão. Verifique sua internet."
                    else -> "Erro ao fazer login com Google: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            }
        }
    }


    /**
     * Send password reset email
     */
    fun forgotPassword(email: String) {
        if (!validateEmail(email)) {
            _uiState.update { it.copy(errorMessage = "E-mail inválido") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                auth.resetPasswordForEmail(email)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "✉️ E-mail de recuperação enviado! Verifique sua caixa de entrada."
                    ) 
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("not found") == true -> 
                        "E-mail não encontrado"
                    e.message?.contains("rate limit") == true -> 
                        "Muitas tentativas. Aguarde alguns minutos."
                    else -> "Erro ao enviar e-mail: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            }
        }
    }

    /**
     * Update user type after registration
     */
    fun updateUserType(userType: UserType) {
        viewModelScope.launch {
            val userId = auth.currentSessionOrNull()?.user?.id ?: return@launch
            
            try {
                userRepository.updateUserFields(userId, mapOf("userType" to userType.name))
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
        viewModelScope.launch {
            val userId = auth.currentSessionOrNull()?.user?.id ?: return@launch
            
            try {
                userRepository.updateUserFields(userId, mapOf("plan" to plan.name))
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
        viewModelScope.launch {
            val userId = auth.currentSessionOrNull()?.user?.id ?: return@launch
            
            try {
                userRepository.updateUserFields(userId, mapOf("avatarUrl" to avatarUrl))
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
     * Sign out
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _uiState.value = AuthUiState()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Erro ao fazer logout")
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
