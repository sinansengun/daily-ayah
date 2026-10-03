package com.cufica.dailyayah.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object DailyAyahNotificationScheduler {
    private const val RequestCode = 100
    const val ActionShowDailyAyah = "com.cufica.dailyayah.action.SHOW_DAILY_AYAH"

    fun schedule(context: Context) {
        if (!DailyAyahNotificationPreferences.isEnabled(context)) {
            cancel(context)
            return
        }

        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var nextDelivery = now.toLocalDate().atTime(DailyAyahNotificationPreferences.time(context))
        if (!nextDelivery.isAfter(now)) {
            nextDelivery = nextDelivery.plusDays(1)
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent(context)
        val triggerAtMillis = nextDelivery.atZone(zone).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        RequestCode,
        Intent(context, DailyAyahNotificationReceiver::class.java).setAction(ActionShowDailyAyah),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}