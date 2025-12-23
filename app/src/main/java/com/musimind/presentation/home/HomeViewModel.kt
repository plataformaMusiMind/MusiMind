package com.musimind.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.musimind.domain.model.LearningNode
import com.musimind.domain.model.LevelSystem
import com.musimind.domain.model.MusicCategory
import com.musimind.domain.model.NodePosition
import com.musimind.domain.model.NodeStatus
import com.musimind.domain.model.NodeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing a node in the UI
 */
data class LearningNodeUi(
    val id: String,
    val title: String,
    val status: NodeStatus,
    val xpReward: Int,
    val type: NodeType,
    val category: MusicCategory
)

/**
 * UI State for Home Screen
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val xp: Int = 0,
    val level: Int = 1,
    val levelProgress: Float = 0f,
    val xpToNextLevel: Int = 100,
    val streak: Int = 0,
    val lives: Int = 5,
    val nodes: List<LearningNodeUi> = emptyList()
)

/**
 * ViewModel for Home Screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        loadLearningPath()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            // TODO: Load from Firebase
            // For now, use sample data
            val sampleXp = 150
            val level = LevelSystem.getLevelForXp(sampleXp)
            val progress = LevelSystem.getXpProgress(sampleXp)
            val xpToNext = LevelSystem.getXpForLevel(level + 1)
            
            _uiState.update {
                it.copy(
                    xp = sampleXp,
                    level = level,
                    levelProgress = progress,
                    xpToNextLevel = xpToNext,
                    streak = 3,
                    lives = 5,
                    isLoading = false
                )
            }
        }
    }

    private fun loadLearningPath() {
        viewModelScope.launch {
            // Sample learning path nodes
            val sampleNodes = listOf(
                LearningNodeUi(
                    id = "basics_1",
                    title = "Básico 1",
                    status = NodeStatus.COMPLETED,
                    xpReward = 10,
                    type = NodeType.THEORY,
                    category = MusicCategory.MUSIC_THEORY
                ),
                LearningNodeUi(
                    id = "basics_2",
                    title = "Básico 2",
                    status = NodeStatus.COMPLETED,
                    xpReward = 10,
                    type = NodeType.THEORY,
                    category = MusicCategory.MUSIC_THEORY
                ),
                LearningNodeUi(
                    id = "notes_intro",
                    title = "Notas Musicais",
                    status = NodeStatus.AVAILABLE,
                    xpReward = 15,
                    type = NodeType.THEORY,
                    category = MusicCategory.MUSIC_THEORY
                ),
                LearningNodeUi(
                    id = "solfege_1",
                    title = "Solfejo Básico",
                    status = NodeStatus.LOCKED,
                    xpReward = 20,
                    type = NodeType.EXERCISE,
                    category = MusicCategory.SOLFEGE
                ),
                LearningNodeUi(
                    id = "rhythm_1",
                    title = "Ritmo Básico",
                    status = NodeStatus.LOCKED,
                    xpReward = 20,
                    type = NodeType.EXERCISE,
                    category = MusicCategory.RHYTHMIC_PERCEPTION
                ),
                LearningNodeUi(
                    id = "intervals_1",
                    title = "Intervalos",
                    status = NodeStatus.LOCKED,
                    xpReward = 25,
                    type = NodeType.THEORY,
                    category = MusicCategory.INTERVAL_PERCEPTION
                ),
                LearningNodeUi(
                    id = "melody_1",
                    title = "Melodia Básica",
                    status = NodeStatus.LOCKED,
                    xpReward = 25,
                    type = NodeType.EXERCISE,
                    category = MusicCategory.MELODIC_PERCEPTION
                ),
                LearningNodeUi(
                    id = "quiz_1",
                    title = "Quiz Nível 1",
                    status = NodeStatus.LOCKED,
                    xpReward = 50,
                    type = NodeType.QUIZ,
                    category = MusicCategory.MUSIC_THEORY
                )
            )
            
            _uiState.update {
                it.copy(nodes = sampleNodes)
            }
        }
    }
}
