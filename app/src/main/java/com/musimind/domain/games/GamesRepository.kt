package com.musimind.domain.games

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório para gerenciar jogos educativos.
 * 
 * Os jogos são "wrappers gamificados" que aproveitam as funcionalidades
 * existentes do app (solfejo, percepção rítmica, etc.) adicionando
 * mecânicas de gamificação (pontos, combos, estrelas).
 * 
 * A integração com os nós de aprendizado garante que os jogos
 * apareçam de forma lógica conforme o usuário avança no conteúdo.
 */
@Singleton
class GamesRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    private val _gameTypes = MutableStateFlow<List<GameType>>(emptyList())
    val gameTypes: StateFlow<List<GameType>> = _gameTypes.asStateFlow()
    
    private val _currentGame = MutableStateFlow<GameType?>(null)
    val currentGame: StateFlow<GameType?> = _currentGame.asStateFlow()
    
    /**
     * Carrega todos os tipos de jogos disponíveis
     */
    suspend fun loadGameTypes(): Result<List<GameType>> {
        return try {
            val games = postgrest.from("game_types")
                .select()
                .decodeList<GameType>()
                .filter { it.isActive }
                .sortedBy { it.sortOrder }
            
            _gameTypes.value = games
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Carrega níveis de um jogo específico
     */
    suspend fun loadGameLevels(gameTypeId: String): Result<List<GameLevel>> {
        return try {
            val levels = postgrest.from("game_levels")
                .select {
                    filter { eq("game_type_id", gameTypeId) }
                }
                .decodeList<GameLevel>()
                .sortedBy { it.levelNumber }
            
            Result.success(levels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Carrega high scores do usuário para um jogo
     */
    suspend fun loadUserHighScores(userId: String, gameTypeId: String): Result<List<GameHighScore>> {
        return try {
            val scores = postgrest.from("game_high_scores")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("game_type_id", gameTypeId)
                    }
                }
                .decodeList<GameHighScore>()
            
            Result.success(scores)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Inicia uma nova sessão de jogo
     */
    suspend fun startGameSession(
        userId: String,
        gameTypeId: String,
        gameLevelId: String?
    ): Result<GameSession> {
        return try {
            val session = postgrest.from("game_sessions")
                .insert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("game_type_id", gameTypeId)
                        gameLevelId?.let { put("game_level_id", it) }
                    }
                )
                .decodeSingle<GameSession>()
            
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Completa uma sessão de jogo e calcula recompensas
     */
    suspend fun completeGameSession(
        sessionId: String,
        score: Int,
        correctAnswers: Int,
        wrongAnswers: Int,
        maxCombo: Int
    ): Result<GameCompleteResult> {
        return try {
            val result = postgrest.rpc(
                "complete_game_session",
                buildJsonObject {
                    put("p_session_id", sessionId)
                    put("p_score", score)
                    put("p_correct", correctAnswers)
                    put("p_wrong", wrongAnswers)
                    put("p_max_combo", maxCombo)
                }
            ).decodeSingle<GameCompleteResult>()
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Verifica quais jogos estão desbloqueados para o usuário
     * baseado nos nós de aprendizado que ele completou
     */
    suspend fun getUnlockedGames(userId: String): Result<List<String>> {
        return try {
            // Buscar nós completados pelo usuário
            val completedNodes = postgrest.from("user_node_progress")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_completed", true)
                    }
                }
                .decodeList<UserNodeProgress>()
                .map { it.nodeId }
            
            // Mapear nós para jogos desbloqueados
            val unlockedGames = mutableListOf<String>()
            
            // Lógica de desbloqueio baseada nos nós
            // Note Catcher: Desbloqueado após "Pauta e Clave" (nó 6)
            if ("b0000001-0000-0000-0000-000000000006" in completedNodes) {
                unlockedGames.add("note_catcher")
            }
            
            // Rhythm Tap: Desbloqueado após "Pulsação e Ritmo" (nó 2)
            if ("b0000001-0000-0000-0000-000000000002" in completedNodes) {
                unlockedGames.add("rhythm_tap")
            }
            
            // Melody Memory: Desbloqueado após "Sol e Mi" (nó 4)
            if ("b0000001-0000-0000-0000-000000000004" in completedNodes) {
                unlockedGames.add("melody_memory")
            }
            
            // Interval Hero: Desbloqueado após "Intervalos Justos" (nó 23)
            if ("b0000001-0000-0000-0000-000000000023" in completedNodes) {
                unlockedGames.add("interval_hero")
            }
            
            // Scale Puzzle: Desbloqueado após "Escala Maior Completa" (nó 20)
            if ("b0000001-0000-0000-0000-000000000020" in completedNodes) {
                unlockedGames.add("scale_puzzle")
            }
            
            // Chord Match: Desbloqueado após "Tríades" (nó 51)
            if ("b0000001-0000-0000-0000-000000000051" in completedNodes) {
                unlockedGames.add("chord_match")
                unlockedGames.add("chord_builder")
            }
            
            // Solfege Sing: Desbloqueado desde o início (aproveita funcionalidade de solfejo)
            unlockedGames.add("solfege_sing")
            
            // Key Signature Shooter: Após escala maior
            if ("b0000001-0000-0000-0000-000000000020" in completedNodes) {
                unlockedGames.add("key_shooter")
            }
            
            // Tempo Run: Após "Subdivisão Binária" (nó 15)
            if ("b0000001-0000-0000-0000-000000000015" in completedNodes) {
                unlockedGames.add("tempo_run")
            }
            
            // Progression Quest: Após "Funções I, IV, V" (nó 53)
            if ("b0000001-0000-0000-0000-000000000053" in completedNodes) {
                unlockedGames.add("progression_quest")
            }
            
            // Daily Challenge: Desbloqueado após completar Fase 1 (nó 12)
            if ("b0000001-0000-0000-0000-000000000012" in completedNodes) {
                unlockedGames.add("daily_challenge")
            }
            
            Result.success(unlockedGames)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Carrega conquistas de jogos do usuário
     */
    suspend fun loadUserGameAchievements(userId: String): Result<List<UserGameAchievement>> {
        return try {
            val achievements = postgrest.from("user_game_achievements")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<UserGameAchievement>()
            
            Result.success(achievements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Carrega todas as conquistas disponíveis
     */
    suspend fun loadAllGameAchievements(): Result<List<GameAchievement>> {
        return try {
            val achievements = postgrest.from("game_achievements")
                .select()
                .decodeList<GameAchievement>()
            
            Result.success(achievements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Retorna o progresso completo do usuário em um jogo
     */
    suspend fun getGameProgress(userId: String, gameTypeId: String): Result<GameProgress> {
        return try {
            // Buscar tipo do jogo
            val gameType = _gameTypes.value.find { it.id == gameTypeId }
                ?: postgrest.from("game_types")
                    .select {
                        filter { eq("id", gameTypeId) }
                    }
                    .decodeSingle<GameType>()
            
            // Buscar níveis
            val levels = loadGameLevels(gameTypeId).getOrThrow()
            
            // Buscar high scores
            val highScores = loadUserHighScores(userId, gameTypeId).getOrThrow()
                .associateBy { it.gameLevelId ?: "" }
            
            // Calcular total de estrelas
            val totalStars = highScores.values.sumOf { it.bestStars }
            
            // Verificar se está desbloqueado
            val unlockedGames = getUnlockedGames(userId).getOrThrow()
            val isUnlocked = gameType.name in unlockedGames
            
            Result.success(
                GameProgress(
                    gameType = gameType,
                    levels = levels,
                    highScores = highScores,
                    totalStars = totalStars,
                    isUnlocked = isUnlocked
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Define o jogo atual sendo jogado
     */
    fun setCurrentGame(game: GameType?) {
        _currentGame.value = game
    }
    
    /**
     * Verifica se o desafio diário foi completado
     */
    suspend fun checkDailyChallengeCompleted(userId: String, date: String): Result<Boolean> {
        return try {
            val result = postgrest.from("game_daily_challenge_attempts")
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("created_at", "${date}T00:00:00")
                        lte("created_at", "${date}T23:59:59")
                    }
                }
                .decodeList<DailyChallengeAttemptEntity>()
            
            Result.success(result.isNotEmpty())
        } catch (e: Exception) {
            Result.success(false) // Assume não completou se houver erro
        }
    }
    
    companion object {
        // Mapeamento de jogos para funcionalidades existentes
        val GAME_TO_EXERCISE_MAPPING = mapOf(
            "solfege_sing" to "solfege",      // Usa exercícios de solfejo
            "rhythm_tap" to "rhythm",          // Usa exercícios de ritmo
            "melody_memory" to "melody",       // Usa exercícios de percepção melódica
            "interval_hero" to "intervals",    // Usa exercícios de intervalos
            "chord_match" to "harmony",        // Usa exercícios de harmonia
            "chord_builder" to "harmony",      // Usa exercícios de harmonia
            "progression_quest" to "harmony"   // Usa exercícios de harmonia
        )
        
        // Mapeamento de nós para jogos desbloqueados
        val NODE_TO_GAME_UNLOCK = mapOf(
            "b0000001-0000-0000-0000-000000000002" to listOf("rhythm_tap"),
            "b0000001-0000-0000-0000-000000000004" to listOf("melody_memory", "solfege_sing"),
            "b0000001-0000-0000-0000-000000000006" to listOf("note_catcher"),
            "b0000001-0000-0000-0000-000000000012" to listOf("daily_challenge"),
            "b0000001-0000-0000-0000-000000000015" to listOf("tempo_run"),
            "b0000001-0000-0000-0000-000000000020" to listOf("scale_puzzle", "key_shooter"),
            "b0000001-0000-0000-0000-000000000023" to listOf("interval_hero"),
            "b0000001-0000-0000-0000-000000000051" to listOf("chord_match", "chord_builder"),
            "b0000001-0000-0000-0000-000000000053" to listOf("progression_quest")
        )
    }
}

/**
 * Modelo auxiliar para progresso do usuário em nós
 */
@kotlinx.serialization.Serializable
data class UserNodeProgress(
    val id: String,
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("node_id")
    val nodeId: String,
    @kotlinx.serialization.SerialName("is_completed")
    val isCompleted: Boolean = false
)

/**
 * Entidade para tentativas de desafio diário
 */
@kotlinx.serialization.Serializable
data class DailyChallengeAttemptEntity(
    val id: String,
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("challenge_id")
    val challengeId: String? = null,
    val score: Int = 0,
    @kotlinx.serialization.SerialName("created_at")
    val createdAt: String? = null
)
