package com.cufica.dailyayah.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyAyahNotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyAyahNotificationScheduler.schedule(context)
    }
}