package com.musimind.domain.model

import kotlinx.serialization.Serializable

/**
 * Achievement system for gamification
 */
@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val category: AchievementCategory,
    val tier: AchievementTier,
    val xpReward: Int,
    val requirement: AchievementRequirement,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Float = 0f // 0-1 progress towards achievement
)

@Serializable
enum class AchievementCategory {
    LEARNING,       // Study-related
    PRACTICE,       // Exercise completion
    STREAK,         // Consistency
    PERFORMANCE,    // Accuracy
    SOCIAL,         // Friends, multiplayer
    EXPLORATION,    // Trying new features
    MASTERY         // Expert-level
}

@Serializable
enum class AchievementTier(val multiplier: Float) {
    BRONZE(1f),
    SILVER(1.5f),
    GOLD(2f),
    PLATINUM(3f),
    DIAMOND(5f)
}

@Serializable
data class AchievementRequirement(
    val type: RequirementType,
    val targetValue: Int,
    val extraData: Map<String, String> = emptyMap()
)

@Serializable
enum class RequirementType {
    COMPLETE_EXERCISES,     // Complete X exercises
    COMPLETE_LESSONS,       // Complete X lessons
    STREAK_DAYS,            // Maintain X day streak
    TOTAL_XP,               // Earn X total XP
    ACCURACY_PERCENTAGE,    // Achieve X% accuracy in exercise
    PERFECT_SCORES,         // Get X perfect scores
    UNLOCK_ACHIEVEMENTS,    // Unlock X other achievements
    FRIENDS_COUNT,          // Have X friends
    WIN_DUELS,              // Win X duels
    PLAY_TIME_MINUTES,      // Play for X minutes total
    CATEGORY_MASTERY,       // Master a category
    FIRST_ACTION            // Complete first action of type
}

/**
 * Predefined achievements
 */
object Achievements {
    val ALL = listOf(
        // Learning
        Achievement(
            id = "first_lesson",
            title = "Primeira Nota",
            description = "Complete sua primeira lição",
            iconName = "school",
            category = AchievementCategory.LEARNING,
            tier = AchievementTier.BRONZE,
            xpReward = 50,
            requirement = AchievementRequirement(
                type = RequirementType.FIRST_ACTION,
                targetValue = 1,
                extraData = mapOf("action" to "complete_lesson")
            )
        ),
        Achievement(
            id = "theory_explorer",
            title = "Explorador Teórico",
            description = "Complete 10 lições de teoria",
            iconName = "menu_book",
            category = AchievementCategory.LEARNING,
            tier = AchievementTier.SILVER,
            xpReward = 150,
            requirement = AchievementRequirement(
                type = RequirementType.COMPLETE_LESSONS,
                targetValue = 10
            )
        ),
        Achievement(
            id = "music_scholar",
            title = "Músico Estudioso",
            description = "Complete 50 lições",
            iconName = "local_library",
            category = AchievementCategory.LEARNING,
            tier = AchievementTier.GOLD,
            xpReward = 500,
            requirement = AchievementRequirement(
                type = RequirementType.COMPLETE_LESSONS,
                targetValue = 50
            )
        ),
        
        // Practice
        Achievement(
            id = "first_exercise",
            title = "Primeiros Passos",
            description = "Complete seu primeiro exercício",
            iconName = "music_note",
            category = AchievementCategory.PRACTICE,
            tier = AchievementTier.BRONZE,
            xpReward = 50,
            requirement = AchievementRequirement(
                type = RequirementType.FIRST_ACTION,
                targetValue = 1,
                extraData = mapOf("action" to "complete_exercise")
            )
        ),
        Achievement(
            id = "practice_10",
            title = "Praticante",
            description = "Complete 10 exercícios",
            iconName = "sports_score",
            category = AchievementCategory.PRACTICE,
            tier = AchievementTier.BRONZE,
            xpReward = 100,
            requirement = AchievementRequirement(
                type = RequirementType.COMPLETE_EXERCISES,
                targetValue = 10
            )
        ),
        Achievement(
            id = "practice_50",
            title = "Dedicado",
            description = "Complete 50 exercícios",
            iconName = "emoji_events",
            category = AchievementCategory.PRACTICE,
            tier = AchievementTier.SILVER,
            xpReward = 300,
            requirement = AchievementRequirement(
                type = RequirementType.COMPLETE_EXERCISES,
                targetValue = 50
            )
        ),
        Achievement(
            id = "practice_100",
            title = "Virtuose",
            description = "Complete 100 exercícios",
            iconName = "workspace_premium",
            category = AchievementCategory.PRACTICE,
            tier = AchievementTier.GOLD,
            xpReward = 750,
            requirement = AchievementRequirement(
                type = RequirementType.COMPLETE_EXERCISES,
                targetValue = 100
            )
        ),
        
        // Streak
        Achievement(
            id = "streak_3",
            title = "Consistente",
            description = "Mantenha uma sequência de 3 dias",
            iconName = "local_fire_department",
            category = AchievementCategory.STREAK,
            tier = AchievementTier.BRONZE,
            xpReward = 75,
            requirement = AchievementRequirement(
                type = RequirementType.STREAK_DAYS,
                targetValue = 3
            )
        ),
        Achievement(
            id = "streak_7",
            title = "Semana Perfeita",
            description = "Mantenha uma sequência de 7 dias",
            iconName = "whatshot",
            category = AchievementCategory.STREAK,
            tier = AchievementTier.SILVER,
            xpReward = 200,
            requirement = AchievementRequirement(
                type = RequirementType.STREAK_DAYS,
                targetValue = 7
            )
        ),
        Achievement(
            id = "streak_30",
            title = "Mestre da Disciplina",
            description = "Mantenha uma sequência de 30 dias",
            iconName = "military_tech",
            category = AchievementCategory.STREAK,
            tier = AchievementTier.GOLD,
            xpReward = 1000,
            requirement = AchievementRequirement(
                type = RequirementType.STREAK_DAYS,
                targetValue = 30
            )
        ),
        Achievement(
            id = "streak_100",
            title = "Lenda",
            description = "Mantenha uma sequência de 100 dias",
            iconName = "diamond",
            category = AchievementCategory.STREAK,
            tier = AchievementTier.DIAMOND,
            xpReward = 5000,
            requirement = AchievementRequirement(
                type = RequirementType.STREAK_DAYS,
                targetValue = 100
            )
        ),
        
        // Performance
        Achievement(
            id = "perfect_1",
            title = "Primeira Perfeição",
            description = "Obtenha 100% em um exercício",
            iconName = "grade",
            category = AchievementCategory.PERFORMANCE,
            tier = AchievementTier.BRONZE,
            xpReward = 100,
            requirement = AchievementRequirement(
                type = RequirementType.PERFECT_SCORES,
                targetValue = 1
            )
        ),
        Achievement(
            id = "perfect_10",
            title = "Perfeccionista",
            description = "Obtenha 100% em 10 exercícios",
            iconName = "stars",
            category = AchievementCategory.PERFORMANCE,
            tier = AchievementTier.SILVER,
            xpReward = 400,
            requirement = AchievementRequirement(
                type = RequirementType.PERFECT_SCORES,
                targetValue = 10
            )
        ),
        Achievement(
            id = "accuracy_90",
            title = "Alta Precisão",
            description = "Alcance 90% de precisão geral",
            iconName = "gps_fixed",
            category = AchievementCategory.PERFORMANCE,
            tier = AchievementTier.GOLD,
            xpReward = 500,
            requirement = AchievementRequirement(
                type = RequirementType.ACCURACY_PERCENTAGE,
                targetValue = 90
            )
        ),
        
        // Social
        Achievement(
            id = "first_friend",
            title = "Sociável",
            description = "Adicione seu primeiro amigo",
            iconName = "person_add",
            category = AchievementCategory.SOCIAL,
            tier = AchievementTier.BRONZE,
            xpReward = 50,
            requirement = AchievementRequirement(
                type = RequirementType.FRIENDS_COUNT,
                targetValue = 1
            )
        ),
        Achievement(
            id = "win_duel",
            title = "Primeiro Duelo",
            description = "Vença seu primeiro duelo",
            iconName = "sports_kabaddi",
            category = AchievementCategory.SOCIAL,
            tier = AchievementTier.BRONZE,
            xpReward = 75,
            requirement = AchievementRequirement(
                type = RequirementType.WIN_DUELS,
                targetValue = 1
            )
        ),
        Achievement(
            id = "win_10_duels",
            title = "Duelista",
            description = "Vença 10 duelos",
            iconName = "sports_martial_arts",
            category = AchievementCategory.SOCIAL,
            tier = AchievementTier.SILVER,
            xpReward = 300,
            requirement = AchievementRequirement(
                type = RequirementType.WIN_DUELS,
                targetValue = 10
            )
        ),
        
        // XP Milestones
        Achievement(
            id = "xp_1000",
            title = "Iniciante",
            description = "Acumule 1.000 XP",
            iconName = "trending_up",
            category = AchievementCategory.EXPLORATION,
            tier = AchievementTier.BRONZE,
            xpReward = 100,
            requirement = AchievementRequirement(
                type = RequirementType.TOTAL_XP,
                targetValue = 1000
            )
        ),
        Achievement(
            id = "xp_10000",
            title = "Experiente",
            description = "Acumule 10.000 XP",
            iconName = "insights",
            category = AchievementCategory.EXPLORATION,
            tier = AchievementTier.SILVER,
            xpReward = 500,
            requirement = AchievementRequirement(
                type = RequirementType.TOTAL_XP,
                targetValue = 10000
            )
        ),
        Achievement(
            id = "xp_50000",
            title = "Mestre",
            description = "Acumule 50.000 XP",
            iconName = "psychology",
            category = AchievementCategory.EXPLORATION,
            tier = AchievementTier.GOLD,
            xpReward = 2000,
            requirement = AchievementRequirement(
                type = RequirementType.TOTAL_XP,
                targetValue = 50000
            )
        )
    )
    
    fun getById(id: String): Achievement? = ALL.find { it.id == id }
    fun getByCategory(category: AchievementCategory): List<Achievement> = 
        ALL.filter { it.category == category }
}
