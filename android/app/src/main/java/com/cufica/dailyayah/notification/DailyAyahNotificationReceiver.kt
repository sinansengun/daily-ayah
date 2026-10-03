package com.cufica.dailyayah.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cufica.dailyayah.MainActivity
import com.cufica.dailyayah.R
import com.cufica.dailyayah.data.DailyAyahRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DailyAyahNotificationReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: DailyAyahRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DailyAyahNotificationScheduler.ActionShowDailyAyah) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ayah = repository.loadPreferredAyah() ?: repository.loadCachedAyah()
                DailyAyahNotificationPublisher.show(
                    context,
                    ayah?.text ?: "Günün ayeti hazırlanıyor.",
                    ayah?.reference ?: "Daily Ayah"
                )
            } finally {
                DailyAyahNotificationScheduler.schedule(context)
                pendingResult.finish()
            }
        }
    }

}

object DailyAyahNotificationPublisher {
    private const val ChannelId = "daily_ayah"
    private const val NotificationId = 10

    fun show(context: Context, text: String, reference: String) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(ChannelId, "Günün Ayeti", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val notificationManager = NotificationManagerCompat.from(context)
        val openAppIntent = PendingIntent.getActivity(
            context,
            101,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Günün Ayeti")
            .setContentText(reference)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NotificationId, notification)
    }
}