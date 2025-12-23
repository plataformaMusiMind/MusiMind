package com.musimind.presentation.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.TeacherRepository
import com.musimind.domain.model.StudentClass
import com.musimind.domain.model.TeacherStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherDashboardViewModel @Inject constructor(
    private val repository: TeacherRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(TeacherDashboardState())
    val state: StateFlow<TeacherDashboardState> = _state.asStateFlow()
    
    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val classes = repository.getTeacherClasses()
                val stats = repository.getTeacherStats()
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    classes = classes,
                    stats = stats
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun createClass(name: String, description: String) {
        viewModelScope.launch {
            try {
                repository.createClass(name, description)
                loadDashboard() // Reload after creating
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }
}

data class TeacherDashboardState(
    val isLoading: Boolean = true,
    val classes: List<StudentClass> = emptyList(),
    val stats: TeacherStats = TeacherStats(),
    val error: String? = null
)
