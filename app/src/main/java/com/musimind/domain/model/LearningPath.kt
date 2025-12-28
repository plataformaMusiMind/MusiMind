package com.musimind.domain.model

import kotlinx.serialization.Serializable

/**
 * Status of a learning node in the path
 */
@Serializable
enum class NodeStatus {
    LOCKED,         // Prerequisites not met
    AVAILABLE,      // Ready to start
    IN_PROGRESS,    // Started but not completed
    COMPLETED       // Successfully completed
}

/**
 * Type of content in a learning node
 */
@Serializable
enum class NodeType {
    THEORY,         // Theory lesson
    EXERCISE,       // Practice exercise
    QUIZ,           // Quiz/test
    CHALLENGE,      // Special challenge
    BOSS            // Boss level / milestone
}

/**
 * Category of musical content
 */
@Serializable
enum class MusicCategory(val displayName: String) {
    SOLFEGE("Solfejo"),                           // Solfejo
    RHYTHMIC_PERCEPTION("Percepção Rítmica"),     // Percepção Rítmica
    MELODIC_PERCEPTION("Percepção Melódica"),     // Percepção Melódica
    INTERVAL_PERCEPTION("Percepção Intervalar"),  // Percepção Intervalar
    HARMONIC_PROGRESSIONS("Progressões Harmônicas"), // Progressões Harmônicas
    MUSIC_THEORY("Teoria Musical"),               // Teoria Musical
    EAR_TRAINING("Treinamento Auditivo"),         // Treinamento Auditivo
    // Additional categories for compatibility
    RHYTHM("Ritmo"),
    INTERVALS("Intervalos"),
    THEORY("Teoria"),
    HARMONIC_PERCEPTION("Percepção Harmônica")
}

/**
 * Position of a node on the learning path
 */
@Serializable
data class NodePosition(
    val x: Float,   // Horizontal position (0-1)
    val y: Int      // Vertical position (row number)
)

/**
 * A node in the learning path (like Duolingo's skill bubbles)
 */
@Serializable
data class LearningNode(
    val id: String,
    val title: String,
    val description: String = "",
    val type: NodeType = NodeType.THEORY,
    val category: MusicCategory = MusicCategory.SOLFEGE,
    val position: NodePosition = NodePosition(0.5f, 0),
    val requiredNodeIds: List<String> = emptyList(),
    val xpReward: Int = 10,
    val coinReward: Int = 0,
    val iconName: String = "music_note", // Icon identifier
    val difficulty: Int = 1, // 1-5 difficulty rating
    // Additional fields for Supabase compatibility
    @kotlinx.serialization.SerialName("sort_order")
    val sortOrder: Int = 0,
    @kotlinx.serialization.SerialName("required_xp")
    val requiredXp: Int = 0,
    @kotlinx.serialization.SerialName("node_type")
    val nodeType: String = "theory" // String type from Supabase
)

/**
 * User's progress on a specific node
 */
@Serializable
data class NodeProgress(
    val userId: String,
    val nodeId: String,
    val status: NodeStatus,
    val bestScore: Int? = null,
    val attempts: Int = 0,
    val completedAt: Long? = null,
    val lastAttemptAt: Long? = null
)

/**
 * Complete learning path data
 */
@Serializable
data class LearningPath(
    val id: String = "main_path",
    val title: String = "Jornada Musical",
    val nodes: List<LearningNode> = emptyList()
)

/**
 * Combined node with user progress for UI display
 */
data class LearningNodeWithProgress(
    val node: LearningNode,
    val progress: NodeProgress?
) {
    val status: NodeStatus
        get() = progress?.status ?: NodeStatus.LOCKED
    
    val isCompleted: Boolean
        get() = status == NodeStatus.COMPLETED
    
    val isAvailable: Boolean
        get() = status == NodeStatus.AVAILABLE || status == NodeStatus.IN_PROGRESS
}
