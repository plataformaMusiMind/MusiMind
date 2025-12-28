package com.musimind.domain.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.musimind.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Manager for MusiMind
 * 
 * Features:
 * - Daily study reminders (like Duolingo)
 * - Streak protection alerts
 * - Achievement notifications
 * - Life refill notifications
 * - Custom notification scheduling
 */

@Singleton
class MusiMindNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val postgrest: Postgrest,
    private val auth: Auth
) {
    companion object {
        // Channel IDs
        const val CHANNEL_REMINDERS = "musimind_reminders"
        const val CHANNEL_STREAKS = "musimind_streaks"
        const val CHANNEL_ACHIEVEMENTS = "musimind_achievements"
        const val CHANNEL_LIVES = "musimind_lives"
        
        // Notification IDs
        const val NOTIFICATION_DAILY_REMINDER = 1001
        const val NOTIFICATION_STREAK_WARNING = 1002
        const val NOTIFICATION_ACHIEVEMENT = 1003
        const val NOTIFICATION_LIVES_REFILL = 1004
        
        // Work tags
        const val WORK_DAILY_REMINDER = "daily_reminder"
        const val WORK_STREAK_CHECK = "streak_check"
        const val WORK_LIFE_CHECK = "life_check"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Initialize notification channels (required for Android 8.0+)
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Lembretes de Estudo",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Lembretes diários para praticar"
                },
                NotificationChannel(
                    CHANNEL_STREAKS,
                    "Alertas de Streak",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas para manter seu streak"
                },
                NotificationChannel(
                    CHANNEL_ACHIEVEMENTS,
                    "Conquistas",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificações de conquistas desbloqueadas"
                },
                NotificationChannel(
                    CHANNEL_LIVES,
                    "Vidas",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Alertas quando vidas são recuperadas"
                }
            )
            
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { systemManager.createNotificationChannel(it) }
        }
    }
    
    /**
     * Schedule daily reminder at user's preferred time
     */
    fun scheduleDailyReminder(hourOfDay: Int = 19, minute: Int = 0) {
        val now = java.time.LocalDateTime.now()
        val targetTime = now.withHour(hourOfDay).withMinute(minute).withSecond(0)
        
        val delayMinutes = if (now.isAfter(targetTime)) {
            // Schedule for tomorrow
            ChronoUnit.MINUTES.between(now, targetTime.plusDays(1))
        } else {
            ChronoUnit.MINUTES.between(now, targetTime)
        }
        
        val dailyReminderRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(WORK_DAILY_REMINDER)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyReminderRequest
        )
    }
    
    /**
     * Schedule streak check (runs at 21:00 if user hasn't practiced)
     */
    fun scheduleStreakCheck() {
        val streakCheckRequest = PeriodicWorkRequestBuilder<StreakCheckWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(calculateDelayUntil(21, 0), TimeUnit.MINUTES)
            .addTag(WORK_STREAK_CHECK)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_STREAK_CHECK,
            ExistingPeriodicWorkPolicy.KEEP,
            streakCheckRequest
        )
    }
    
    /**
     * Schedule life refill notification (30 mins after life lost)
     */
    fun scheduleLifeRefillNotification() {
        val lifeCheckRequest = OneTimeWorkRequestBuilder<LifeRefillWorker>()
            .setInitialDelay(30, TimeUnit.MINUTES)
            .addTag(WORK_LIFE_CHECK)
            .build()
        
        workManager.enqueue(lifeCheckRequest)
    }
    
    /**
     * Cancel all scheduled notifications
     */
    fun cancelAllNotifications() {
        workManager.cancelAllWork()
        notificationManager.cancelAll()
    }
    
    /**
     * Cancel specific notification type
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Show daily reminder notification
     */
    fun showDailyReminder(streakDays: Int) {
        if (!hasNotificationPermission()) return
        
        val message = getDailyReminderMessage(streakDays)
        val emoji = if (streakDays > 0) "🔥" else "🎵"
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("$emoji Hora de Praticar!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_media_play,
                "Começar",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(NOTIFICATION_DAILY_REMINDER, notification)
    }
    
    /**
     * Show streak warning notification
     */
    fun showStreakWarning(streakDays: Int) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_STREAKS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Seu streak está em perigo!")
            .setContentText("Você tem um streak de $streakDays dias! Complete uma lição antes da meia-noite!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Você construiu um streak incrível de $streakDays dias! 🔥\n\nNão deixe ele acabar - complete pelo menos uma lição antes da meia-noite!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_media_play,
                "Praticar Agora",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(NOTIFICATION_STREAK_WARNING, notification)
    }
    
    /**
     * Show achievement unlocked notification
     */
    fun showAchievementUnlocked(achievementTitle: String, xpReward: Int) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle("🏆 Conquista Desbloqueada!")
            .setContentText("$achievementTitle (+$xpReward XP)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ACHIEVEMENT, notification)
    }
    
    /**
     * Show life refill notification
     */
    fun showLivesRefilled(newLives: Int) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_LIVES)
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentTitle("❤️ Vidas Recarregadas!")
            .setContentText("Você tem $newLives vidas. Continue praticando!")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_LIVES_REFILL, notification)
    }
    
    /**
     * Check notification permission
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * Get personalized daily reminder message
     */
    private fun getDailyReminderMessage(streakDays: Int): String {
        return when {
            streakDays == 0 -> "Comece uma nova jornada musical hoje!"
            streakDays == 1 -> "Você começou um streak ontem! Mantenha o ritmo hoje!"
            streakDays < 7 -> "Você está em uma sequência de $streakDays dias! Continue assim!"
            streakDays < 30 -> "Incrível! $streakDays dias seguidos praticando! 🔥"
            streakDays < 100 -> "Impressionante! $streakDays dias de streak! Você é dedicado!"
            else -> "Lendário! $streakDays dias! Você é um verdadeiro músico! 👑"
        }
    }
    
    /**
     * Calculate delay in minutes until specific time
     */
    private fun calculateDelayUntil(hour: Int, minute: Int): Long {
        val now = java.time.LocalDateTime.now()
        val targetTime = now.withHour(hour).withMinute(minute).withSecond(0)
        
        return if (now.isAfter(targetTime)) {
            ChronoUnit.MINUTES.between(now, targetTime.plusDays(1))
        } else {
            ChronoUnit.MINUTES.between(now, targetTime)
        }
    }
    
    /**
     * Save notification preferences to Supabase
     */
    suspend fun saveNotificationPreferences(preferences: NotificationPreferences) {
        val userId = auth.currentSessionOrNull()?.user?.id ?: return
        
        try {
            postgrest.from("notification_preferences").upsert(
                mapOf(
                    "user_id" to userId,
                    "daily_reminder_enabled" to preferences.dailyReminderEnabled,
                    "daily_reminder_time" to preferences.dailyReminderTime.toString(),
                    "streak_reminder" to preferences.streakReminder,
                    "achievement_alerts" to preferences.achievementAlerts,
                    "challenge_alerts" to preferences.challengeAlerts,
                    "life_refill_alert" to preferences.lifeRefillAlert
                )
            )
            
            // Update scheduled notifications based on preferences
            if (preferences.dailyReminderEnabled) {
                scheduleDailyReminder(
                    preferences.dailyReminderTime.hour,
                    preferences.dailyReminderTime.minute
                )
            } else {
                workManager.cancelUniqueWork(WORK_DAILY_REMINDER)
            }
            
            if (preferences.streakReminder) {
                scheduleStreakCheck()
            } else {
                workManager.cancelUniqueWork(WORK_STREAK_CHECK)
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    /**
     * Load notification preferences from Supabase
     */
    suspend fun loadNotificationPreferences(): NotificationPreferences {
        val userId = auth.currentSessionOrNull()?.user?.id ?: return NotificationPreferences()
        
        return try {
            val entity = postgrest.from("notification_preferences")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeSingleOrNull<NotificationPreferencesEntity>()
            
            entity?.toDomain() ?: NotificationPreferences()
        } catch (e: Exception) {
            NotificationPreferences()
        }
    }
}

// ============================================
// Preference Data Classes
// ============================================

data class NotificationPreferences(
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTime: LocalTime = LocalTime.of(19, 0),
    val streakReminder: Boolean = true,
    val achievementAlerts: Boolean = true,
    val challengeAlerts: Boolean = true,
    val lifeRefillAlert: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStart: LocalTime = LocalTime.of(22, 0),
    val quietEnd: LocalTime = LocalTime.of(8, 0)
)

@Serializable
data class NotificationPreferencesEntity(
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    @kotlinx.serialization.SerialName("daily_reminder_enabled")
    val dailyReminderEnabled: Boolean = true,
    @kotlinx.serialization.SerialName("daily_reminder_time")
    val dailyReminderTime: String = "19:00:00",
    @kotlinx.serialization.SerialName("streak_reminder")
    val streakReminder: Boolean = true,
    @kotlinx.serialization.SerialName("achievement_alerts")
    val achievementAlerts: Boolean = true,
    @kotlinx.serialization.SerialName("challenge_alerts")
    val challengeAlerts: Boolean = true,
    @kotlinx.serialization.SerialName("life_refill_alert")
    val lifeRefillAlert: Boolean = true
) {
    fun toDomain() = NotificationPreferences(
        dailyReminderEnabled = dailyReminderEnabled,
        dailyReminderTime = LocalTime.parse(dailyReminderTime),
        streakReminder = streakReminder,
        achievementAlerts = achievementAlerts,
        challengeAlerts = challengeAlerts,
        lifeRefillAlert = lifeRefillAlert
    )
}

// ============================================
// Workers for Background Tasks
// ============================================

class DailyReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        // Get user's streak (simplified - would inject dependencies)
        val streakDays = 0 // Would fetch from repository
        
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(
                applicationContext,
                MusiMindNotificationManager.CHANNEL_REMINDERS
            )
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("🎵 Hora de Praticar!")
                .setContentText("Dedique alguns minutos à música hoje!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(
                MusiMindNotificationManager.NOTIFICATION_DAILY_REMINDER,
                notification
            )
        }
        
        return Result.success()
    }
}

class StreakCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        // Check if user has practiced today
        val hasPracticedToday = false // Would fetch from repository
        val streakDays = 5 // Would fetch from repository
        
        if (!hasPracticedToday && streakDays > 0) {
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(applicationContext, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val notification = NotificationCompat.Builder(
                    applicationContext,
                    MusiMindNotificationManager.CHANNEL_STREAKS
                )
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("⚠️ Seu streak está em perigo!")
                    .setContentText("Complete uma lição antes da meia-noite!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
                
                notificationManager.notify(
                    MusiMindNotificationManager.NOTIFICATION_STREAK_WARNING,
                    notification
                )
            }
        }
        
        return Result.success()
    }
}

class LifeRefillWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(
                applicationContext,
                MusiMindNotificationManager.CHANNEL_LIVES
            )
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setContentTitle("❤️ Vida Recuperada!")
                .setContentText("Você ganhou uma vida! Continue praticando!")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(
                MusiMindNotificationManager.NOTIFICATION_LIVES_REFILL,
                notification
            )
        }
        
        return Result.success()
    }
}
