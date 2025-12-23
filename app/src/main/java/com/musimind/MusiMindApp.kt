package com.musimind

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

/**
 * MusiMind Application class.
 * Serves as the entry point for Hilt dependency injection
 * and handles app-wide initialization.
 */
@HiltAndroidApp
class MusiMindApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /**
     * Creates notification channels required for the app.
     * Channels are required on Android 8.0 (API 26) and above.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Default channel for general notifications
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_description)
                enableVibration(true)
            }

            // Channel for daily reminders
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Lembretes Diários",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes para praticar teoria musical"
                enableVibration(true)
            }

            // Channel for challenges and multiplayer
            val challengesChannel = NotificationChannel(
                CHANNEL_ID_CHALLENGES,
                "Desafios",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Convites para duelos e desafios multiplayer"
                enableVibration(true)
            }

            // Channel for social interactions
            val socialChannel = NotificationChannel(
                CHANNEL_ID_SOCIAL,
                "Social",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Solicitações de amizade e atualizações sociais"
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, reminderChannel, challengesChannel, socialChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_ID_DEFAULT = "musimind_default_channel"
        const val CHANNEL_ID_REMINDERS = "musimind_reminders_channel"
        const val CHANNEL_ID_CHALLENGES = "musimind_challenges_channel"
        const val CHANNEL_ID_SOCIAL = "musimind_social_channel"
    }
}
