package com.cufica.dailyayah.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.cufica.dailyayah.notification.DailyAyahNotificationPreferences
import com.cufica.dailyayah.notification.DailyAyahNotificationScheduler
import java.time.LocalTime
import java.util.Locale

@Composable
fun NotificationSettingsScreen(
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(DailyAyahNotificationPreferences.isEnabled(context)) }
    var deliveryTime by remember { mutableStateOf(DailyAyahNotificationPreferences.time(context)) }
    val notificationsEnabled = notificationPermissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Günün ayeti bildirimleri",
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PreferenceRow(
            title = "Günlük bildirim",
            subtitle = if (enabled) "Her gün ${deliveryTime.label()}" else "Kapalı",
            trailing = {
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        enabled = checked
                        DailyAyahNotificationPreferences.setEnabled(context, checked)
                        if (checked) DailyAyahNotificationScheduler.schedule(context)
                        else DailyAyahNotificationScheduler.cancel(context)
                    }
                )
            },
            onClick = { }
        )
        HorizontalDivider()
        PreferenceRow(
            title = "Bildirim saati",
            subtitle = deliveryTime.label(),
            enabled = enabled,
            trailing = { Text(deliveryTime.label(), color = MaterialTheme.colorScheme.primary) },
            onClick = {
                showTimePicker(context, deliveryTime) { selectedTime ->
                    deliveryTime = selectedTime
                    DailyAyahNotificationPreferences.setTime(context, selectedTime)
                    DailyAyahNotificationScheduler.schedule(context)
                }
            }
        )
        HorizontalDivider()
        PreferenceRow(
            title = "Bildirim izni",
            subtitle = if (notificationsEnabled) "İzin verildi" else "Bildirimler kapalı",
            trailing = {
                Text(
                    if (notificationsEnabled) "Açık" else "Yönet",
                    color = MaterialTheme.colorScheme.primary
                )
            },
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted) {
                    onRequestNotificationPermission()
                } else {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    })
                }
            }
        )
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}

private fun showTimePicker(context: Context, time: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
        time.hour,
        time.minute,
        true
    ).show()
}

private fun LocalTime.label(): String = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)