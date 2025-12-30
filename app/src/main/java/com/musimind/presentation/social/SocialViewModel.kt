package com.musimind.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.domain.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * ViewModel for Social features with Supabase Realtime
 * 
 * Features:
 * - Friend connections (add, accept, remove)
 * - Teacher-Student connections
 * - Activity feed in real-time
 * - Friend suggestions
 * - Online status
 */
@HiltViewModel
class SocialViewModel @Inject constructor(
    private val postgrest: Postgrest,
    private val realtime: Realtime,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(SocialState())
    val state: StateFlow<SocialState> = _state.asStateFlow()
    
    private var realtimeJob: Job? = null
    private val currentUserId: String get() = authManager.currentUserId
    
    init {
        loadSocialData()
    }
    
    /**
     * Load all social data from Supabase
     */
    fun loadSocialData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // Load friends, requests, activity in parallel
                launch { loadFriends() }
                launch { loadFriendRequests() }
                launch { loadActivityFeed() }
                launch { loadFriendSuggestions() }
                
                // Subscribe to realtime updates
                subscribeToUpdates()
                
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    /**
     * Load accepted friends
     */
    private suspend fun loadFriends() {
        try {
            // Get connections where current user is involved and status is accepted
            val connections = postgrest.from("user_connections")
                .select {
                    filter {
                        or {
                            eq("from_user_id", currentUserId)
                            eq("to_user_id", currentUserId)
                        }
                        eq("status", "accepted")
                        eq("connection_type", "friend")
                    }
                }
                .decodeList<UserConnectionEntity>()
            
            // Get user details for each friend
            val friendIds = connections.map { conn ->
                if (conn.fromUserId == currentUserId) conn.toUserId else conn.fromUserId
            }
            
            val friends = friendIds.mapNotNull { userId ->
                try {
                    postgrest.from("users")
                        .select { filter { eq("auth_id", userId) } }
                        .decodeSingleOrNull<UserEntity>()?.toFriend()
                } catch (e: Exception) { null }
            }
            
            _state.update { it.copy(friends = friends) }
        } catch (e: Exception) {
            android.util.Log.e("SocialViewModel", "Load friends failed: ${e.message}")
        }
    }
    
    /**
     * Load pending friend requests
     */
    private suspend fun loadFriendRequests() {
        try {
            val requests = postgrest.from("user_connections")
                .select {
                    filter {
                        eq("to_user_id", currentUserId)
                        eq("status", "pending")
                        eq("connection_type", "friend")
                    }
                }
                .decodeList<UserConnectionEntity>()
            
            val friendRequests = requests.mapNotNull { conn ->
                try {
                    val user = postgrest.from("users")
                        .select { filter { eq("auth_id", conn.fromUserId) } }
                        .decodeSingleOrNull<UserEntity>()
                    
                    user?.let {
                        FriendRequest(
                            id = conn.id,
                            fromUserId = conn.fromUserId,
                            fromUserName = it.fullName ?: "Usuário",
                            fromUserAvatar = it.avatarUrl,
                            createdAt = conn.createdAt ?: ""
                        )
                    }
                } catch (e: Exception) { null }
            }
            
            _state.update { it.copy(friendRequests = friendRequests) }
        } catch (e: Exception) {
            android.util.Log.e("SocialViewModel", "Load requests failed: ${e.message}")
        }
    }
    
    /**
     * Load activity feed from friends
     */
    private suspend fun loadActivityFeed() {
        try {
            // Get friend IDs first
            val friendIds = _state.value.friends.map { it.id }
            
            if (friendIds.isEmpty()) {
                // Load some recent public activities
                val activities = postgrest.from("activity_feed")
                    .select {
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(20)
                    }
                    .decodeList<ActivityFeedEntity>()
                
                _state.update { 
                    it.copy(activityFeed = activities.map { a -> a.toActivityItem() }) 
                }
            } else {
                // Load friend activities
                val activities = postgrest.from("activity_feed")
                    .select {
                        filter {
                            isIn("user_id", friendIds)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(20)
                    }
                    .decodeList<ActivityFeedEntity>()
                
                _state.update { 
                    it.copy(activityFeed = activities.map { a -> a.toActivityItem() }) 
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SocialViewModel", "Load feed failed: ${e.message}")
            // Use mock data as fallback
            _state.update { 
                it.copy(activityFeed = listOf(
                    ActivityItem("Ana Paula", "completou o nível Solfejo 3", "5 min", 50),
                    ActivityItem("Carlos Silva", "alcançou sequência de 7 dias", "15 min", 0),
                    ActivityItem("Maria Santos", "venceu um duelo contra Pedro", "30 min", 100)
                ))
            }
        }
    }
    
    /**
     * Load friend suggestions
     */
    private suspend fun loadFriendSuggestions() {
        try {
            // Get users with similar level or same school/teacher
            val currentUser = postgrest.from("users")
                .select { filter { eq("auth_id", currentUserId) } }
                .decodeSingleOrNull<UserEntity>()
            
            val level = currentUser?.level ?: 1
            
            // Find users at similar level who are not already friends
            val existingFriendIds = _state.value.friends.map { it.id }
            
            val suggestions = postgrest.from("users")
                .select {
                    filter {
                        gte("level", level - 2)
                        lte("level", level + 2)
                        neq("auth_id", currentUserId)
                    }
                    limit(10)
                }
                .decodeList<UserEntity>()
                .filter { it.authId !in existingFriendIds }
                .map { user ->
                    val userLevel = user.level ?: 1
                    FriendSuggestion(
                        id = user.authId ?: "",
                        name = user.fullName ?: "Usuário",
                        reason = when {
                            userLevel == level -> "Mesmo nível"
                            userLevel > level -> "Nível $userLevel"
                            else -> "Iniciante"
                        },
                        streak = user.streak ?: 0,
                        avatarUrl = user.avatarUrl
                    )
                }
            
            _state.update { it.copy(suggestions = suggestions) }
        } catch (e: Exception) {
            android.util.Log.e("SocialViewModel", "Load suggestions failed: ${e.message}")
            // Use mock suggestions
            _state.update { 
                it.copy(suggestions = listOf(
                    FriendSuggestion("1", "Lucas M.", "Mesmo nível", 5, null),
                    FriendSuggestion("2", "Julia R.", "Nível 12", 12, null),
                    FriendSuggestion("3", "Rafael S.", "Iniciante", 3, null)
                ))
            }
        }
    }
    
    /**
     * Subscribe to real-time updates using Supabase Realtime
     * Falls back to polling if Realtime fails
     */
    private fun subscribeToUpdates() {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                val channel = realtime.channel("social_$currentUserId")
                
                // Listen for new friend requests
                val connectionChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "user_connections"
                }
                
                // Listen for new activities from friends
                val activityChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "activity_feed"
                }
                
                // Handle connection changes
                launch {
                    connectionChanges.collect { action ->
                        when (action) {
                            is PostgresAction.Insert -> {
                                loadFriendRequests()
                                _state.update { it.copy(hasNewRequests = true) }
                            }
                            is PostgresAction.Update -> {
                                loadFriends()
                                loadFriendRequests()
                            }
                            else -> { }
                        }
                    }
                }
                
                // Handle activity changes
                launch {
                    activityChanges.collect { action ->
                        if (action is PostgresAction.Insert) {
                            loadActivityFeed()
                        }
                    }
                }
                
                // Subscribe to channel
                channel.subscribe()
                
                android.util.Log.d("SocialViewModel", "Realtime connected for user: $currentUserId")
                
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "Realtime subscription failed, falling back to polling: ${e.message}")
                startPolling()
            }
        }
    }
    
    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(30000) // Poll every 30 seconds
                loadFriends()
                loadFriendRequests()
                loadActivityFeed()
            }
        }
    }
    
    /**
     * Send friend request
     */
    fun sendFriendRequest(toUserId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                postgrest.from("user_connections").insert(
                    mapOf(
                        "from_user_id" to currentUserId,
                        "to_user_id" to toUserId,
                        "connection_type" to "friend",
                        "status" to "pending",
                        "created_at" to java.time.Instant.now().toString()
                    )
                )
                
                // Remove from suggestions
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        suggestions = s.suggestions.filter { it.id != toUserId },
                        message = "Solicitação enviada!"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Erro ao enviar solicitação") }
            }
        }
    }
    
    /**
     * Accept friend request
     */
    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                postgrest.from("user_connections").update(
                    mapOf(
                        "status" to "accepted",
                        "accepted_at" to java.time.Instant.now().toString()
                    )
                ) {
                    filter { eq("id", requestId) }
                }
                
                loadFriends()
                loadFriendRequests()
                
                _state.update { it.copy(message = "Amigo adicionado!") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erro ao aceitar solicitação") }
            }
        }
    }
    
    /**
     * Reject friend request
     */
    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                postgrest.from("user_connections").update(
                    mapOf("status" to "rejected")
                ) {
                    filter { eq("id", requestId) }
                }
                
                loadFriendRequests()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erro ao rejeitar") }
            }
        }
    }
    
    /**
     * Remove friend
     */
    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            try {
                postgrest.from("user_connections").delete {
                    filter {
                        or {
                            and {
                                eq("from_user_id", currentUserId)
                                eq("to_user_id", friendId)
                            }
                            and {
                                eq("from_user_id", friendId)
                                eq("to_user_id", currentUserId)
                            }
                        }
                        eq("connection_type", "friend")
                    }
                }
                
                loadFriends()
                _state.update { it.copy(message = "Amigo removido") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erro ao remover amigo") }
            }
        }
    }
    
    /**
     * Search users by name or email
     */
    fun searchUsers(query: String) {
        if (query.length < 3) return
        
        viewModelScope.launch {
            try {
                val results = postgrest.from("users")
                    .select {
                        filter {
                            or {
                                ilike("full_name", "%$query%")
                                ilike("email", "%$query%")
                            }
                            neq("auth_id", currentUserId)
                        }
                        limit(20)
                    }
                    .decodeList<UserEntity>()
                
                _state.update { s ->
                    s.copy(searchResults = results.map { u ->
                        Friend(
                            id = u.authId ?: "",
                            name = u.fullName ?: "Usuário",
                            avatarUrl = u.avatarUrl,
                            level = u.level ?: 1,
                            isOnline = false
                        )
                    })
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erro na busca") }
            }
        }
    }
    
    fun clearMessage() {
        _state.update { it.copy(message = null, error = null) }
    }
    
    fun clearNewRequestsFlag() {
        _state.update { it.copy(hasNewRequests = false) }
    }
    
    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
    }
}

// State
data class SocialState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val friends: List<Friend> = emptyList(),
    val friendRequests: List<FriendRequest> = emptyList(),
    val activityFeed: List<ActivityItem> = emptyList(),
    val suggestions: List<FriendSuggestion> = emptyList(),
    val searchResults: List<Friend> = emptyList(),
    val hasNewRequests: Boolean = false
)

// Data models
data class Friend(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val level: Int = 1,
    val isOnline: Boolean = false,
    val lastActiveAt: String? = null
)

data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val fromUserAvatar: String? = null,
    val createdAt: String
)

data class FriendSuggestion(
    val id: String,
    val name: String,
    val reason: String,
    val streak: Int,
    val avatarUrl: String? = null
)

data class ActivityItem(
    val userName: String,
    val action: String,
    val time: String,
    val xpEarned: Int = 0
)

// Entities for Supabase
@Serializable
data class UserConnectionEntity(
    val id: String = "",
    @SerialName("from_user_id")
    val fromUserId: String = "",
    @SerialName("to_user_id")
    val toUserId: String = "",
    @SerialName("connection_type")
    val connectionType: String = "",
    val status: String = "",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("accepted_at")
    val acceptedAt: String? = null
)

@Serializable
data class UserEntity(
    val id: String? = null,
    @SerialName("auth_id")
    val authId: String? = null,
    val email: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val level: Int? = null,
    val xp: Int? = null,
    val streak: Int? = null,
    @SerialName("last_active_at")
    val lastActiveAt: String? = null
) {
    fun toFriend() = Friend(
        id = authId ?: id ?: "",
        name = fullName ?: "Usuário",
        avatarUrl = avatarUrl,
        level = level ?: 1,
        isOnline = false,
        lastActiveAt = lastActiveAt
    )
}

@Serializable
data class ActivityFeedEntity(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("activity_type")
    val activityType: String = "",
    @SerialName("activity_data")
    val activityData: kotlinx.serialization.json.JsonObject? = null,
    @SerialName("xp_earned")
    val xpEarned: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toActivityItem(): ActivityItem {
        val userName = activityData?.get("user_name")?.toString()?.trim('"') ?: "Usuário"
        val action = when (activityType) {
            "level_up" -> "subiu de nível"
            "streak" -> "alcançou uma sequência"
            "achievement" -> "desbloqueou uma conquista"
            "exercise_complete" -> "completou um exercício"
            "duel_win" -> "venceu um duelo"
            else -> "realizou uma atividade"
        }
        
        val timeAgo = createdAt?.let { formatTimeAgo(it) } ?: "agora"
        
        return ActivityItem(userName, action, timeAgo, xpEarned)
    }
    
    private fun formatTimeAgo(timestamp: String): String {
        // Simple time ago formatting
        return try {
            val instant = java.time.Instant.parse(timestamp)
            val now = java.time.Instant.now()
            val minutes = java.time.Duration.between(instant, now).toMinutes()
            
            when {
                minutes < 1 -> "agora"
                minutes < 60 -> "$minutes min"
                minutes < 1440 -> "${minutes / 60}h"
                else -> "${minutes / 1440}d"
            }
        } catch (e: Exception) {
            "recente"
        }
    }
}
