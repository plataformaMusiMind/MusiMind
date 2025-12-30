package com.musimind.domain.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resultado do login com Google
 */
sealed class GoogleSignInResult {
    data class Success(
        val idToken: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?
    ) : GoogleSignInResult()
    
    data class Error(val message: String) : GoogleSignInResult()
    object Cancelled : GoogleSignInResult()
}

/**
 * Helper para Google Sign-In usando Credential Manager (API moderna)
 * 
 * Esta implementação usa o Credential Manager que:
 * - Não abre navegador externo
 * - Mostra uma UI nativa bonita
 * - Integra com as contas Google do dispositivo
 * - É a forma recomendada pelo Google desde Android 14
 */
@Singleton
class GoogleSignInHelper @Inject constructor() {
    
    companion object {
        private const val TAG = "GoogleSignInHelper"
        
        // Web Client ID do Google Cloud Console
        // Configurado em: https://console.cloud.google.com/apis/credentials
        // Projeto: MusiMind (rare-bloom-482616-p8)
        const val WEB_CLIENT_ID = "198570071594-6ov8kriog9r5sons56qgghtc1mdl10pp.apps.googleusercontent.com"
    }
    
    /**
     * Inicia o fluxo de login com Google usando Credential Manager
     */
    suspend fun signIn(context: Context): GoogleSignInResult = withContext(Dispatchers.Main) {
        try {
            val credentialManager = CredentialManager.create(context)
            
            // Configura a opção de login com Google
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Permite selecionar qualquer conta
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false) // Permite ao usuário escolher qual conta usar
                .build()
            
            // Cria a requisição de credencial
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            // Obtém a credencial
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            
            handleSignInResult(result)
            
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Login cancelado pelo usuário")
            GoogleSignInResult.Cancelled
            
        } catch (e: NoCredentialException) {
            Log.e(TAG, "Nenhuma credencial disponível", e)
            GoogleSignInResult.Error("Nenhuma conta Google encontrada. Adicione uma conta Google ao dispositivo.")
            
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Erro ao obter credencial", e)
            GoogleSignInResult.Error("Erro ao fazer login com Google: ${e.message}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado", e)
            GoogleSignInResult.Error("Erro inesperado: ${e.message}")
        }
    }
    
    /**
     * Processa o resultado do login
     */
    private fun handleSignInResult(result: GetCredentialResponse): GoogleSignInResult {
        val credential = result.credential
        
        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        
                        Log.d(TAG, "Login bem-sucedido: ${googleIdTokenCredential.displayName}")
                        
                        GoogleSignInResult.Success(
                            idToken = googleIdTokenCredential.idToken,
                            email = googleIdTokenCredential.id, // O ID é o email
                            displayName = googleIdTokenCredential.displayName,
                            photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                        )
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Erro ao parsear token Google", e)
                        GoogleSignInResult.Error("Erro ao processar credencial do Google")
                    }
                } else {
                    Log.e(TAG, "Tipo de credencial inesperado: ${credential.type}")
                    GoogleSignInResult.Error("Tipo de credencial não suportado")
                }
            }
            else -> {
                Log.e(TAG, "Credencial não reconhecida")
                GoogleSignInResult.Error("Credencial não reconhecida")
            }
        }
    }
}
