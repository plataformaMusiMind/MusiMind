package com.musimind.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.data.repository.GamificationRepository
import com.musimind.data.repository.UserRepository
import com.musimind.domain.auth.AuthManager
import com.musimind.domain.model.LevelSystem
import com.musimind.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Profile Screen - Loads real user data
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gamificationRepository: GamificationRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadUserProfile()
        loadStatistics()
        loadAchievements()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val user = userRepository.getCurrentUser()
                
                if (user != null) {
                    val level = LevelSystem.getLevelForXp(user.xp)
                    val progress = LevelSystem.getXpProgress(user.xp)
                    val xpToNext = LevelSystem.getXpForLevel(level + 1)
                    val currentLevelXp = LevelSystem.getXpForLevel(level)
                    
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            userName = user.fullName ?: "Usuário MusiMind",
                            userEmail = user.email,
                            avatarUrl = user.avatarUrl,
                            userType = user.userType.displayName,
                            planType = user.planType.displayName,
                            level = level,
                            totalXp = user.xp,
                            currentXpInLevel = user.xp - currentLevelXp,
                            xpToNextLevel = xpToNext - currentLevelXp,
                            levelProgress = progress,
                            streak = user.streak,
                            lives = user.lives,
                            coins = user.coins
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Erro ao carregar perfil: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val userId = authManager.currentUserId
                if (userId.isBlank()) return@launch
                
                // Load statistics from gamification repository
                val stats = gamificationRepository.getUserStatistics(userId)
                
                _state.update { 
                    it.copy(
                        totalStudyTimeMinutes = stats.totalStudyTimeMinutes,
                        exercisesCompleted = stats.exercisesCompleted,
                        accuracyRate = stats.accuracyRate,
                        duelsWon = stats.duelsWon,
                        duelsPlayed = stats.duelsPlayed,
                        longestStreak = stats.longestStreak
                    )
                }
            } catch (e: Exception) {
                // Fallback to defaults if stats can't be loaded
            }
        }
    }
    
    private fun loadAchievements() {
        viewModelScope.launch {
            try {
                val userId = authManager.currentUserId
                if (userId.isBlank()) return@launch
                
                val achievements = gamificationRepository.getRecentAchievements(userId, 4)
                
                _state.update { 
                    it.copy(recentAchievements = achievements.map { achievement ->
                        AchievementUi(
                            id = achievement.id,
                            title = achievement.displayName,
                            description = achievement.description,
                            iconName = achievement.icon
                        )
                    })
                }
            } catch (e: Exception) {
                // Use default achievements if can't load
            }
        }
    }
    
    fun refresh() {
        loadUserProfile()
        loadStatistics()
        loadAchievements()
    }
}

data class ProfileState(
    val isLoading: Boolean = true,
    val error: String? = null,
    
    // User Info
    val userName: String = "Usuário MusiMind",
    val userEmail: String = "",
    val avatarUrl: String? = null,
    val userType: String = "Estudante",
    val planType: String = "Gratuito",
    
    // Level & Progress
    val level: Int = 1,
    val totalXp: Int = 0,
    val currentXpInLevel: Int = 0,
    val xpToNextLevel: Int = 100,
    val levelProgress: Float = 0f,
    val streak: Int = 0,
    val lives: Int = 5,
    val coins: Int = 0,
    
    // Statistics
    val totalStudyTimeMinutes: Int = 0,
    val exercisesCompleted: Int = 0,
    val accuracyRate: Float = 0f,
    val duelsWon: Int = 0,
    val duelsPlayed: Int = 0,
    val longestStreak: Int = 0,
    
    // Achievements
    val recentAchievements: List<AchievementUi> = emptyList()
) {
    val studyTimeFormatted: String
        get() {
            val hours = totalStudyTimeMinutes / 60
            val minutes = totalStudyTimeMinutes % 60
            return "${hours}h ${minutes}min"
        }
    
    val accuracyRateFormatted: String
        get() = "${(accuracyRate * 100).toInt()}%"
    
    val duelsFormatted: String
        get() = "$duelsWon/$duelsPlayed"
}

data class AchievementUi(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String
)
