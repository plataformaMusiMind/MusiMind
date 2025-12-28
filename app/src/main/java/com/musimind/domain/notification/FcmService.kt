package com.musimind.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.musimind.MainActivity
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FCM Service for Push Notifications
 * 
 * Note: This is a simplified version that works without Firebase.
 * For full FCM support, add the google-services plugin and firebase-messaging dependency.
 * 
 * To enable full FCM:
 * 1. Add google-services.json to app/ folder
 * 2. Add google-services plugin to build.gradle
 * 3. Uncomment firebase-messaging dependency
 * 4. Extend FirebaseMessagingService instead
 */

/**
 * Token Manager for FCM
 * 
 * Handles saving and updating FCM tokens in Supabase
 */
class FcmTokenManager(
    private val postgrest: Postgrest,
    private val auth: Auth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Save FCM token to Supabase
     * Called when a new token is generated or refreshed
     */
    fun saveToken(token: String) {
        scope.launch {
            try {
                val userId = auth.currentSessionOrNull()?.user?.id ?: return@launch
                
                postgrest.from("notification_preferences").upsert(
                    mapOf(
                        "user_id" to userId,
                        "fcm_token" to token,
                        "token_updated_at" to java.time.Instant.now().toString()
                    )
                )
            } catch (e: Exception) {
                // Log error
            }
        }
    }
    
    /**
     * Clear FCM token (on logout)
     */
    fun clearToken() {
        scope.launch {
            try {
                val userId = auth.currentSessionOrNull()?.user?.id ?: return@launch
                
                postgrest.from("notification_preferences").update(
                    mapOf(
                        "fcm_token" to null,
                        "token_updated_at" to java.time.Instant.now().toString()
                    )
                ) {
                    filter { eq("user_id", userId) }
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}

/**
 * Local Push Notification Helper
 * 
 * For sending local notifications that simulate push behavior
 * Useful for development and testing without Firebase
 */
class LocalPushNotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "musimind_push"
        const val NOTIFICATION_ID_BASE = 5000
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Push Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Remote notifications from MusiMind"
                enableVibration(true)
                enableLights(true)
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show a push-style notification
     */
    fun showNotification(
        id: Int = System.currentTimeMillis().toInt(),
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
    
    /**
     * Common notification types that would come from server
     */
    
    fun showNewChallengeNotification(challengeId: String, challengerName: String) {
        showNotification(
            id = NOTIFICATION_ID_BASE + 1,
            title = "🎯 Novo Desafio!",
            message = "$challengerName te desafiou para um duelo musical!",
            data = mapOf("type" to "challenge", "challenge_id" to challengeId)
        )
    }
    
    fun showFriendActivityNotification(friendName: String, activity: String) {
        showNotification(
            id = NOTIFICATION_ID_BASE + 2,
            title = "👥 Atividade de amigo",
            message = "$friendName $activity",
            data = mapOf("type" to "friend_activity")
        )
    }
    
    fun showWeeklyReportNotification(xpGained: Int, exercisesCompleted: Int) {
        showNotification(
            id = NOTIFICATION_ID_BASE + 3,
            title = "📊 Relatório Semanal",
            message = "Você ganhou $xpGained XP e completou $exercisesCompleted exercícios esta semana!",
            data = mapOf("type" to "weekly_report")
        )
    }
    
    fun showNewContentNotification(contentTitle: String) {
        showNotification(
            id = NOTIFICATION_ID_BASE + 4,
            title = "✨ Novo Conteúdo!",
            message = "\"$contentTitle\" foi adicionado. Confira agora!",
            data = mapOf("type" to "new_content")
        )
    }
    
    fun showLeaderboardUpdateNotification(newRank: Int, leaderboardType: String) {
        showNotification(
            id = NOTIFICATION_ID_BASE + 5,
            title = "🏆 Ranking Atualizado",
            message = "Você está em #$newRank no ranking $leaderboardType!",
            data = mapOf("type" to "leaderboard", "rank" to newRank.toString())
        )
    }
}

/**
 * Push Notification Data Types
 */
enum class PushNotificationType(val key: String) {
    CHALLENGE("challenge"),
    FRIEND_REQUEST("friend_request"),
    FRIEND_ACTIVITY("friend_activity"),
    ACHIEVEMENT("achievement"),
    LEVEL_UP("level_up"),
    NEW_CONTENT("new_content"),
    WEEKLY_REPORT("weekly_report"),
    LEADERBOARD("leaderboard"),
    REMINDER("reminder"),
    PROMOTION("promotion")
}

/**
 * Push notification payload data class
 */
data class PushPayload(
    val type: PushNotificationType,
    val title: String,
    val message: String,
    val deepLink: String? = null,
    val imageUrl: String? = null,
    val data: Map<String, String> = emptyMap()
)

/*
 * ================================================
 * FIREBASE MESSAGING SERVICE (Template)
 * ================================================
 * 
 * When you're ready to use Firebase, create this file:
 * MusiMindFirebaseMessagingService.kt
 * 
 * And extend FirebaseMessagingService:
 * 
 * ```kotlin
 * @AndroidEntryPoint
 * class MusiMindFirebaseMessagingService : FirebaseMessagingService() {
 *
 *     @Inject
 *     lateinit var tokenManager: FcmTokenManager
 *     
 *     @Inject
 *     lateinit var notificationHelper: LocalPushNotificationHelper
 *     
 *     override fun onNewToken(token: String) {
 *         super.onNewToken(token)
 *         tokenManager.saveToken(token)
 *     }
 *     
 *     override fun onMessageReceived(remoteMessage: RemoteMessage) {
 *         super.onMessageReceived(remoteMessage)
 *         
 *         // Handle data messages
 *         remoteMessage.data.isNotEmpty().let {
 *             val type = remoteMessage.data["type"] ?: "general"
 *             handleDataMessage(type, remoteMessage.data)
 *         }
 *         
 *         // Handle notification messages
 *         remoteMessage.notification?.let {
 *             notificationHelper.showNotification(
 *                 title = it.title ?: "MusiMind",
 *                 message = it.body ?: ""
 *             )
 *         }
 *     }
 *     
 *     private fun handleDataMessage(type: String, data: Map<String, String>) {
 *         when (type) {
 *             "challenge" -> notificationHelper.showNewChallengeNotification(
 *                 data["challenge_id"] ?: "",
 *                 data["challenger_name"] ?: "Alguém"
 *             )
 *             // ... handle other types
 *         }
 *     }
 * }
 * ```
 * 
 * Don't forget to add to AndroidManifest.xml:
 * 
 * <service
 *     android:name=".domain.notification.MusiMindFirebaseMessagingService"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *     </intent-filter>
 * </service>
 */
