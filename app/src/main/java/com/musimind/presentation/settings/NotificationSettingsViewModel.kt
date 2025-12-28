package com.musimind.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musimind.domain.notification.MusiMindNotificationManager
import com.musimind.domain.notification.NotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * ViewModel for Notification Settings
 */

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationManager: MusiMindNotificationManager
) : ViewModel() {
    
    private val _preferences = MutableStateFlow(NotificationPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()
    
    init {
        loadPreferences()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            _preferences.value = notificationManager.loadNotificationPreferences()
        }
    }
    
    fun updateDailyReminderEnabled(enabled: Boolean) {
        updatePreferences { it.copy(dailyReminderEnabled = enabled) }
    }
    
    fun updateDailyReminderTime(time: LocalTime) {
        updatePreferences { it.copy(dailyReminderTime = time) }
    }
    
    fun updateStreakReminder(enabled: Boolean) {
        updatePreferences { it.copy(streakReminder = enabled) }
    }
    
    fun updateAchievementAlerts(enabled: Boolean) {
        updatePreferences { it.copy(achievementAlerts = enabled) }
    }
    
    fun updateChallengeAlerts(enabled: Boolean) {
        updatePreferences { it.copy(challengeAlerts = enabled) }
    }
    
    fun updateLifeRefillAlert(enabled: Boolean) {
        updatePreferences { it.copy(lifeRefillAlert = enabled) }
    }
    
    fun updateQuietHoursEnabled(enabled: Boolean) {
        updatePreferences { it.copy(quietHoursEnabled = enabled) }
    }
    
    fun updateQuietStart(time: LocalTime) {
        updatePreferences { it.copy(quietStart = time) }
    }
    
    fun updateQuietEnd(time: LocalTime) {
        updatePreferences { it.copy(quietEnd = time) }
    }
    
    private fun updatePreferences(update: (NotificationPreferences) -> NotificationPreferences) {
        val newPreferences = update(_preferences.value)
        _preferences.value = newPreferences
        
        viewModelScope.launch {
            notificationManager.saveNotificationPreferences(newPreferences)
        }
    }
}
