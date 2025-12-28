package com.musimind.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Boot Receiver
 * 
 * Restarts notification workers when device boots up.
 * This ensures daily reminders and other scheduled notifications
 * continue to work after device restarts.
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            rescheduleNotifications(context)
        }
    }
    
    private fun rescheduleNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        // Reschedule daily reminder worker
        val dailyReminderRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS
        )
            .addTag(MusiMindNotificationManager.WORK_DAILY_REMINDER)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            MusiMindNotificationManager.WORK_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyReminderRequest
        )
        
        // Reschedule streak check worker
        val streakCheckRequest = PeriodicWorkRequestBuilder<StreakCheckWorker>(
            24, TimeUnit.HOURS
        )
            .addTag(MusiMindNotificationManager.WORK_STREAK_CHECK)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            MusiMindNotificationManager.WORK_STREAK_CHECK,
            ExistingPeriodicWorkPolicy.KEEP,
            streakCheckRequest
        )
    }
}
