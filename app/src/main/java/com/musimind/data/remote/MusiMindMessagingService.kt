package com.musimind.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

/**
 * Firebase Cloud Messaging Service for MusiMind
 * Handles push notifications for reminders, challenges, social updates
 */
@AndroidEntryPoint
class MusiMindMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MusiMindMessaging"
    }

    /**
     * Called when a new FCM token is generated
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // TODO: Send token to server for the current user
        sendTokenToServer(token)
    }

    /**
     * Called when a message is received
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            handleNotification(notification)
        }
    }

    /**
     * Handle data messages (silent notifications)
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return
        
        when (type) {
            "duel_invite" -> {
                // Handle duel invitation
                val challengerId = data["challenger_id"]
                val challengerName = data["challenger_name"]
                // TODO: Show notification and navigate to duel screen
            }
            "friend_request" -> {
                // Handle friend request
                val senderId = data["sender_id"]
                val senderName = data["sender_name"]
                // TODO: Show notification
            }
            "streak_reminder" -> {
                // Handle streak reminder
                // TODO: Show reminder notification
            }
            "challenge_complete" -> {
                // Handle challenge completion by friend
                val friendName = data["friend_name"]
                val xpEarned = data["xp_earned"]
                // TODO: Update social feed
            }
            else -> {
                Log.d(TAG, "Unknown message type: $type")
            }
        }
    }

    /**
     * Handle notification messages
     */
    private fun handleNotification(notification: RemoteMessage.Notification) {
        val title = notification.title ?: "MusiMind"
        val body = notification.body ?: ""
        
        // TODO: Create and show notification using NotificationManager
        showNotification(title, body)
    }

    /**
     * Send FCM token to server
     */
    private fun sendTokenToServer(token: String) {
        // TODO: Implement sending token to Firebase Firestore
        // userRepository.updateFcmToken(token)
    }

    /**
     * Show local notification
     */
    private fun showNotification(title: String, body: String) {
        // TODO: Implement notification display using NotificationCompat
    }
}
