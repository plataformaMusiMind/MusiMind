package com.musimind.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.domain.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val postgrest: Postgrest
) : ViewModel() {
    
    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()
    
    private val userId = authManager.currentUserId
    
    init {
        loadProfile()
    }
    
    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val user = postgrest.from("users")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<UserProfileEntity>()
                
                user?.let {
                    _state.update { state ->
                        state.copy(
                            fullName = it.fullName ?: "",
                            email = it.email ?: "",
                            phone = it.phone ?: "",
                            bio = it.bio ?: "",
                            avatarUrl = it.avatarUrl
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erro ao carregar perfil") }
            }
        }
    }
    
    fun updateFullName(name: String) {
        _state.update { it.copy(fullName = name) }
    }
    
    fun updatePhone(phone: String) {
        _state.update { it.copy(phone = phone) }
    }
    
    fun updateBio(bio: String) {
        _state.update { it.copy(bio = bio) }
    }
    
    fun saveProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            
            try {
                postgrest.from("users")
                    .update({
                        set("full_name", _state.value.fullName)
                        set("phone", _state.value.phone.ifBlank { null })
                        set("bio", _state.value.bio.ifBlank { null })
                    }) {
                        filter { eq("id", userId) }
                    }
                
                _state.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isSaving = false, 
                        error = "Erro ao salvar: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

@kotlinx.serialization.Serializable
private data class UserProfileEntity(
    val id: String,
    val email: String? = null,
    @kotlinx.serialization.SerialName("full_name")
    val fullName: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    @kotlinx.serialization.SerialName("avatar_url")
    val avatarUrl: String? = null
)
