package com.musimind.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.musimind.data.repository.UserRepository
import com.musimind.domain.model.AuthProvider
import com.musimind.domain.model.Plan
import com.musimind.domain.model.User
import com.musimind.domain.model.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * Supports Email/Password and Google Sign-In
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        // Firebase Web Client ID from Firebase Console > Authentication > Sign-in method > Google
        private const val WEB_CLIENT_ID = "719869906914-bo1uqofmeoej5ntaatea3gajoqbke8hd.apps.googleusercontent.com"
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val credentialManager = CredentialManager.create(context)

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
                    val userExists = userRepository.userExists(userId)
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isLoginSuccess = true,
                            isNewUser = !userExists
                        )
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
     * Initiate Google Sign-In using Credential Manager
     * @param activityContext The Activity Context required to show the sign-in UI
     */
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                val result = credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )
                
                handleGoogleSignInResult(result)
                
            } catch (e: GetCredentialCancellationException) {
                _uiState.update { 
                    it.copy(isLoading = false, errorMessage = null) 
                }
            } catch (e: GetCredentialException) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Erro ao fazer login com Google: ${e.message}"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Erro inesperado: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Handle the Google Sign-In result
     */
    private suspend fun handleGoogleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential
        
        if (credential is CustomCredential && 
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                // Sign in to Firebase with the Google ID token
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // Check if user exists in Firestore
                    val existingUser = userRepository.getUserById(firebaseUser.uid)
                    
                    if (existingUser == null) {
                        // Create new user in Firestore
                        val newUser = User(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            fullName = firebaseUser.displayName ?: "",
                            avatarUrl = firebaseUser.photoUrl?.toString(),
                            authProvider = AuthProvider.GOOGLE
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
                        // Update last active time
                        userRepository.updateUserFields(firebaseUser.uid, mapOf(
                            "lastActiveAt" to System.currentTimeMillis()
                        ))
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoginSuccess = true,
                                isNewUser = false,
                                currentUser = existingUser
                            )
                        }
                    }
                }
            } catch (e: GoogleIdTokenParsingException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erro ao processar credenciais do Google"
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Tipo de credencial inválido"
                )
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
                    val newUser = User(
                        id = userId,
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
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        viewModelScope.launch {
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
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        viewModelScope.launch {
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
        firebaseAuth.signOut()
        _uiState.value = AuthUiState()
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
