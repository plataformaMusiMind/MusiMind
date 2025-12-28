package com.musimind.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musimind.domain.notification.NotificationPreferences
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Notification Settings Screen
 * 
 * Allows users to configure:
 * - Daily reminder time
 * - Notification types
 * - Quiet hours
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.preferences.collectAsState()
    val scrollState = rememberScrollState()
    
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerFor by remember { mutableStateOf<TimePickerType?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificações") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Daily Reminder Section
            SectionHeader(title = "Lembrete Diário", icon = Icons.Default.Alarm)
            
            NotificationToggle(
                title = "Lembrete de Estudo",
                description = "Receba um lembrete diário para praticar",
                isEnabled = preferences.dailyReminderEnabled,
                onToggle = { viewModel.updateDailyReminderEnabled(it) }
            )
            
            if (preferences.dailyReminderEnabled) {
                TimeSelector(
                    title = "Horário do Lembrete",
                    time = preferences.dailyReminderTime,
                    onClick = {
                        timePickerFor = TimePickerType.DAILY_REMINDER
                        showTimePicker = true
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
            
            // Alert Types Section
            SectionHeader(title = "Tipos de Alerta", icon = Icons.Default.Notifications)
            
            NotificationToggle(
                title = "Alertas de Streak",
                description = "Aviso quando seu streak está em perigo",
                isEnabled = preferences.streakReminder,
                onToggle = { viewModel.updateStreakReminder(it) }
            )
            
            NotificationToggle(
                title = "Conquistas",
                description = "Notificação ao desbloquear conquistas",
                isEnabled = preferences.achievementAlerts,
                onToggle = { viewModel.updateAchievementAlerts(it) }
            )
            
            NotificationToggle(
                title = "Desafios Diários",
                description = "Aviso de novos desafios disponíveis",
                isEnabled = preferences.challengeAlerts,
                onToggle = { viewModel.updateChallengeAlerts(it) }
            )
            
            NotificationToggle(
                title = "Vidas Recuperadas",
                description = "Aviso quando suas vidas são restauradas",
                isEnabled = preferences.lifeRefillAlert,
                onToggle = { viewModel.updateLifeRefillAlert(it) }
            )
            
            Divider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
            
            // Quiet Hours Section
            SectionHeader(title = "Horário Silencioso", icon = Icons.Default.DoNotDisturb)
            
            NotificationToggle(
                title = "Ativar Horário Silencioso",
                description = "Não receba notificações durante este período",
                isEnabled = preferences.quietHoursEnabled,
                onToggle = { viewModel.updateQuietHoursEnabled(it) }
            )
            
            if (preferences.quietHoursEnabled) {
                TimeSelector(
                    title = "Início", 
                    time = preferences.quietStart,
                    onClick = {
                        timePickerFor = TimePickerType.QUIET_START
                        showTimePicker = true
                    }
                )
                
                TimeSelector(
                    title = "Fim",
                    time = preferences.quietEnd,
                    onClick = {
                        timePickerFor = TimePickerType.QUIET_END
                        showTimePicker = true
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Dica",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manter o lembrete diário ativo aumenta suas chances de manter um streak longo! Usuários com lembretes têm 3x mais chances de estudar diariamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker && timePickerFor != null) {
        TimePickerDialog(
            initialTime = when (timePickerFor) {
                TimePickerType.DAILY_REMINDER -> preferences.dailyReminderTime
                TimePickerType.QUIET_START -> preferences.quietStart
                TimePickerType.QUIET_END -> preferences.quietEnd
                null -> LocalTime.of(19, 0)
            },
            onConfirm = { time ->
                when (timePickerFor) {
                    TimePickerType.DAILY_REMINDER -> viewModel.updateDailyReminderTime(time)
                    TimePickerType.QUIET_START -> viewModel.updateQuietStart(time)
                    TimePickerType.QUIET_END -> viewModel.updateQuietEnd(time)
                    null -> {}
                }
                showTimePicker = false
                timePickerFor = null
            },
            onDismiss = {
                showTimePicker = false
                timePickerFor = null
            }
        )
    }
}

// ============================================
// Components
// ============================================

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isEnabled) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun TimeSelector(
    title: String,
    time: LocalTime,
    onClick: () -> Unit
) {
    val formattedTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar Horário") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private enum class TimePickerType {
    DAILY_REMINDER,
    QUIET_START,
    QUIET_END
}
