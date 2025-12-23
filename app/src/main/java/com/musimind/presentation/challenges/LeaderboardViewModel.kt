package com.musimind.presentation.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.GamificationRepository
import com.musimind.domain.model.LeaderboardEntry
import com.musimind.domain.model.LeaderboardType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: GamificationRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(LeaderboardState())
    val state: StateFlow<LeaderboardState> = _state.asStateFlow()
    
    fun loadLeaderboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            val entries = repository.getLeaderboard(_state.value.selectedType)
            
            _state.value = _state.value.copy(
                isLoading = false,
                entries = entries
            )
        }
    }
    
    fun selectType(type: LeaderboardType) {
        _state.value = _state.value.copy(selectedType = type)
        loadLeaderboard()
    }
}

data class LeaderboardState(
    val isLoading: Boolean = true,
    val selectedType: LeaderboardType = LeaderboardType.WEEKLY,
    val entries: List<LeaderboardEntry> = emptyList()
)
