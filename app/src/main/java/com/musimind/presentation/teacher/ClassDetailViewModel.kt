package com.musimind.presentation.teacher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.TeacherRepository
import com.musimind.domain.model.StudentProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassDetailViewModel @Inject constructor(
    private val repository: TeacherRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _state = MutableStateFlow(ClassDetailState())
    val state: StateFlow<ClassDetailState> = _state.asStateFlow()
    
    private var currentClassId: String = ""
    
    fun loadClass(classId: String) {
        currentClassId = classId
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                // Get class info
                val classes = repository.getTeacherClasses()
                val studentClass = classes.find { it.id == classId }
                
                // Get students
                val students = repository.getClassStudents(classId)
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    className = studentClass?.name ?: "Turma",
                    inviteCode = studentClass?.inviteCode ?: "",
                    students = students,
                    averageAccuracy = if (students.isNotEmpty()) 
                        students.map { it.averageAccuracy }.average().toFloat() 
                    else 0f,
                    activeStudents = students.count { it.isActive }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun toggleInactiveFilter() {
        _state.value = _state.value.copy(
            showOnlyInactive = !_state.value.showOnlyInactive
        )
    }
    
    fun copyInviteCode() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Código MusiMind", _state.value.inviteCode)
        clipboard.setPrimaryClip(clip)
    }
    
    fun removeStudent(studentId: String) {
        viewModelScope.launch {
            try {
                repository.removeStudentFromClass(currentClassId, studentId)
                loadClass(currentClassId) // Reload
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }
}

data class ClassDetailState(
    val isLoading: Boolean = true,
    val className: String = "",
    val inviteCode: String = "",
    val students: List<StudentProgress> = emptyList(),
    val averageAccuracy: Float = 0f,
    val activeStudents: Int = 0,
    val showOnlyInactive: Boolean = false,
    val error: String? = null
)
