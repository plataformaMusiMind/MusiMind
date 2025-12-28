package com.musimind.domain.games

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Categoria do jogo
 */
enum class GameCategory {
    RHYTHM,
    MELODY,
    HARMONY,
    THEORY,
    MIXED
}

/**
 * Tipo de jogo disponível no MusiMind
 */
@Serializable
data class GameType(
    val id: String,
    val name: String,
    @SerialName("display_name")
    val displayName: String,
    val description: String? = null,
    val icon: String = "game_controller",
    val color: String = "#6366F1",
    val category: String = "mixed",
    @SerialName("min_level")
    val minLevel: Int = 1,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("sort_order")
    val sortOrder: Int = 0
) {
    val gameCategory: GameCategory
        get() = when (category.lowercase()) {
            "rhythm" -> GameCategory.RHYTHM
            "melody" -> GameCategory.MELODY
            "harmony" -> GameCategory.HARMONY
            "theory" -> GameCategory.THEORY
            else -> GameCategory.MIXED
        }
}

/**
 * Nível de um jogo com configurações específicas
 */
@Serializable
data class GameLevel(
    val id: String,
    @SerialName("game_type_id")
    val gameTypeId: String,
    @SerialName("level_number")
    val levelNumber: Int,
    val title: String,
    val description: String? = null,
    val difficulty: Int = 1,
    val config: JsonObject? = null,
    @SerialName("required_stars_to_unlock")
    val requiredStarsToUnlock: Int = 0,
    @SerialName("xp_reward")
    val xpReward: Int = 10,
    @SerialName("coins_reward")
    val coinsReward: Int = 5,
    @SerialName("time_limit_seconds")
    val timeLimitSeconds: Int? = null,
    @SerialName("target_score")
    val targetScore: Int = 100
)

/**
 * Sessão de jogo em andamento ou finalizada
 */
@Serializable
data class GameSession(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("game_type_id")
    val gameTypeId: String,
    @SerialName("game_level_id")
    val gameLevelId: String? = null,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("ended_at")
    val endedAt: String? = null,
    val score: Int = 0,
    @SerialName("max_combo")
    val maxCombo: Int = 0,
    @SerialName("correct_answers")
    val correctAnswers: Int = 0,
    @SerialName("wrong_answers")
    val wrongAnswers: Int = 0,
    @SerialName("stars_earned")
    val starsEarned: Int = 0,
    @SerialName("xp_earned")
    val xpEarned: Int = 0,
    @SerialName("coins_earned")
    val coinsEarned: Int = 0,
    @SerialName("is_completed")
    val isCompleted: Boolean = false
)

/**
 * High score de um usuário em um jogo/nível
 */
@Serializable
data class GameHighScore(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("game_type_id")
    val gameTypeId: String,
    @SerialName("game_level_id")
    val gameLevelId: String? = null,
    @SerialName("high_score")
    val highScore: Int,
    @SerialName("best_combo")
    val bestCombo: Int = 0,
    @SerialName("best_stars")
    val bestStars: Int = 0,
    @SerialName("total_plays")
    val totalPlays: Int = 1,
    @SerialName("last_played_at")
    val lastPlayedAt: String? = null
)

/**
 * Conquista de jogo
 */
@Serializable
data class GameAchievement(
    val id: String,
    val name: String,
    @SerialName("display_name")
    val displayName: String,
    val description: String,
    val icon: String = "trophy",
    @SerialName("game_type_id")
    val gameTypeId: String? = null,
    @SerialName("condition_type")
    val conditionType: String,
    @SerialName("condition_value")
    val conditionValue: Int,
    @SerialName("xp_reward")
    val xpReward: Int = 50,
    @SerialName("coins_reward")
    val coinsReward: Int = 25,
    @SerialName("is_hidden")
    val isHidden: Boolean = false
)

/**
 * Conquista desbloqueada pelo usuário
 */
@Serializable
data class UserGameAchievement(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("achievement_id")
    val achievementId: String,
    @SerialName("unlocked_at")
    val unlockedAt: String,
    @SerialName("session_id")
    val sessionId: String? = null
)

/**
 * Resultado de completar uma sessão de jogo
 */
@Serializable
data class GameCompleteResult(
    val success: Boolean,
    val stars: Int = 0,
    @SerialName("xp_earned")
    val xpEarned: Int = 0,
    @SerialName("coins_earned")
    val coinsEarned: Int = 0,
    val score: Int = 0,
    val error: String? = null
)

/**
 * Estado do progresso em um jogo
 */
data class GameProgress(
    val gameType: GameType,
    val levels: List<GameLevel>,
    val highScores: Map<String, GameHighScore>, // levelId -> highScore
    val totalStars: Int,
    val isUnlocked: Boolean
)

/**
 * Configurações específicas para Note Catcher
 */
@Serializable
data class NoteCatcherConfig(
    val clef: String = "treble", // "treble", "bass", "mixed"
    val notes: List<String> = listOf("C4", "D4", "E4", "F4", "G4"),
    val speed: Float = 1.0f,
    @SerialName("notes_count")
    val notesCount: Int = 10
)

/**
 * Configurações específicas para Rhythm Tap
 */
@Serializable
data class RhythmTapConfig(
    val tempo: Int = 80,
    @SerialName("time_sig")
    val timeSignature: String = "4/4",
    val patterns: List<String> = listOf("q q q q"),
    val rounds: Int = 4
)

/**
 * Configurações específicas para Melody Memory
 */
@Serializable
data class MelodyMemoryConfig(
    @SerialName("notes_count")
    val notesCount: Int = 4,
    val range: List<String> = listOf("C4", "C5"),
    val tempo: Int = 70,
    val lives: Int = 5
)

/**
 * Representa uma nota no jogo Note Catcher
 */
data class FallingNote(
    val id: String,
    val noteName: String, // "C4", "D4", etc.
    val midiNumber: Int,
    var yPosition: Float = -100f, // Começa fora da tela
    var isCorrect: Boolean? = null, // null = não respondido ainda
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Representa um padrão rítmico no jogo Rhythm Tap
 */
data class RhythmPattern(
    val id: String,
    val beats: List<BeatEvent>,
    var isPlayed: Boolean = false
)

data class BeatEvent(
    val timeMs: Long, // Tempo desde o início do padrão
    val duration: Float, // Duração em beats (0.25 = semicolcheia, 0.5 = colcheia, etc.)
    val isRest: Boolean = false
)

/**
 * Representa uma melodia no jogo Melody Memory
 */
data class MemoryMelody(
    val id: String,
    val notes: List<String>, // ["C4", "E4", "G4", ...]
    val tempo: Int
)
