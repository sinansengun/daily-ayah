package com.cufica.dailyayah.notification

import android.content.Context
import java.time.LocalTime

object DailyAyahNotificationPreferences {
    private const val PreferencesName = "daily_ayah_notification"
    private const val EnabledKey = "enabled"
    private const val HourKey = "hour"
    private const val MinuteKey = "minute"

    fun isEnabled(context: Context): Boolean = preferences(context).getBoolean(EnabledKey, true)

    fun time(context: Context): LocalTime = LocalTime.of(
        preferences(context).getInt(HourKey, 10).coerceIn(0, 23),
        preferences(context).getInt(MinuteKey, 0).coerceIn(0, 59)
    )

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(EnabledKey, enabled).apply()
    }

    fun setTime(context: Context, time: LocalTime) {
        preferences(context).edit()
            .putInt(HourKey, time.hour)
            .putInt(MinuteKey, time.minute)
            .apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )
}