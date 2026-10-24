package com.cufica.dailyayah.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.cufica.dailyayah.di.RepositoryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyAyahOnlyWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ActionRefresh) refresh(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        refresh(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { PrayerWidgetPreferences.removeWidget(context, it) }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshPendingIntent(context))
    }

    private fun refresh(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val repository = EntryPointAccessors.fromApplication(
                context.applicationContext,
                RepositoryEntryPoint::class.java
            ).repository()
            val ayah = repository.loadAyahForWidget()
            DailyAyahOnlyWidgetUpdater.update(context, ayah)
            scheduleNextRefresh(context, isToday = ayah?.publishedDateTR == todayTR())
            pendingResult.finish()
        }
    }

    // Next TR day shortly after the backend refresh, or a retry if today's ayah isn't available yet.
    private fun scheduleNextRefresh(context: Context, isToday: Boolean) {
        val zone = ZoneId.of("Europe/Istanbul")
        val now = ZonedDateTime.now(zone)
        val next = if (isToday) {
            now.toLocalDate().plusDays(1).atTime(0, 5).atZone(zone)
        } else {
            now.plusMinutes(15)
        }
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC,
            next.toInstant().toEpochMilli(),
            refreshPendingIntent(context)
        )
    }

    private fun todayTR(): String = LocalDate.now(ZoneId.of("Europe/Istanbul")).toString()

    private fun refreshPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        33,
        Intent(context, DailyAyahOnlyWidgetProvider::class.java).apply { action = ActionRefresh },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val ActionRefresh = "com.cufica.dailyayah.widget.AYAH_ONLY_REFRESH"
        fun requestRefresh(context: Context) {
            context.sendBroadcast(Intent(context, DailyAyahOnlyWidgetProvider::class.java).apply {
                action = ActionRefresh
            })
        }
    }
}