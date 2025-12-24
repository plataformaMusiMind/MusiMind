package com.musimind.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the different types of users in MusiMind
 */
@Serializable
enum class UserType {
    STUDENT,    // Aluno - learns music theory
    TEACHER,    // Professor - monitors student progress
    SCHOOL      // Escola de Música - manages teachers and students
}

/**
 * Represents the subscription plans available
 */
@Serializable
enum class Plan(val price: Double, val displayName: String) {
    APRENDIZ(0.0, "Aprendiz"),      // Free tier
    SPALLA(19.90, "Spalla"),         // Premium tier
    MAESTRO(29.90, "Maestro")        // Ultimate tier
}

/**
 * Authentication provider type
 */
@Serializable
enum class AuthProvider {
    EMAIL,
    GOOGLE
}

/**
 * Main User domain model
 */
@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val phone: String? = null,
    val avatarUrl: String? = null,
    val userType: UserType = UserType.STUDENT,
    val plan: Plan = Plan.APRENDIZ,
    val authProvider: AuthProvider = AuthProvider.EMAIL,
    
    // Gamification stats
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val coins: Int = 0,
    val lives: Int = 5,
    
    // Relationships
    val teacherId: String? = null,      // For students linked to a teacher
    val schoolId: String? = null,       // For members of a school
    val friendIds: List<String> = emptyList(),
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val lastStreakDate: String? = null  // ISO date format
)

/**
 * User profile summary for displaying in lists, leaderboards, etc.
 */
@Serializable
data class UserSummary(
    val id: String,
    val fullName: String,
    val avatarUrl: String?,
    val level: Int,
    val xp: Int,
    val streak: Int,
    val userType: UserType
)

/**
 * Extension to convert User to UserSummary
 */
fun User.toSummary() = UserSummary(
    id = id,
    fullName = fullName,
    avatarUrl = avatarUrl,
    level = level,
    xp = xp,
    streak = streak,
    userType = userType
)

/**
 * XP thresholds for each level
 * Level up when XP reaches threshold
 */
object LevelSystem {
    private val xpThresholds = listOf(
        0,      // Level 1
        100,    // Level 2
        250,    // Level 3
        500,    // Level 4
        850,    // Level 5
        1300,   // Level 6
        1850,   // Level 7
        2500,   // Level 8
        3250,   // Level 9
        4100,   // Level 10
        5050,   // Level 11
        6100,   // Level 12
        7250,   // Level 13
        8500,   // Level 14
        9850,   // Level 15
        11300,  // Level 16
        12850,  // Level 17
        14500,  // Level 18
        16250,  // Level 19
        18100   // Level 20
    )

    fun getLevelForXp(xp: Int): Int {
        return xpThresholds.indexOfLast { xp >= it } + 1
    }

    fun getXpForLevel(level: Int): Int {
        return xpThresholds.getOrElse(level - 1) { xpThresholds.last() }
    }

    fun getXpProgress(xp: Int): Float {
        val currentLevel = getLevelForXp(xp)
        val currentThreshold = getXpForLevel(currentLevel)
        val nextThreshold = getXpForLevel(currentLevel + 1)
        
        return if (nextThreshold > currentThreshold) {
            (xp - currentThreshold).toFloat() / (nextThreshold - currentThreshold)
        } else {
            1f
        }
    }
    
    /**
     * Get level title and description
     */
    fun getLevelInfo(level: Int): Pair<String, String> {
        val titles = listOf(
            "Iniciante" to "Primeiros passos",
            "Aprendiz" to "Dominando o básico", 
            "Estudante" to "Progredindo bem",
            "Praticante" to "Prática constante",
            "Dedicado" to "Compromisso musical",
            "Habilidoso" to "Técnica refinada",
            "Talentoso" to "Talento natural",
            "Virtuoso" to "Execução excepcional",
            "Mestre" to "Domínio completo",
            "Maestro" to "Lenda musical"
        )
        val index = ((level - 1) / 2).coerceIn(0, titles.lastIndex)
        return titles[index]
    }
    
    /**
     * Get progress to next level (0.0 to 1.0)
     */
    fun getProgressToNextLevel(xp: Int): Float {
        return getXpProgress(xp)
    }
}
