package com.cufica.dailyayah.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.cufica.dailyayah.di.RepositoryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyAyahWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ActionRefreshPrayer -> refresh(context)
            ActionRefreshCountdown -> refresh(context, cachedOnly = true)
        }
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

    private fun refresh(context: Context, cachedOnly: Boolean = false) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                RepositoryEntryPoint::class.java
            )
            val city = PrayerWidgetPreferences.selectedCity(context)
            val ayah = if (cachedOnly) {
                entryPoint.repository().loadCachedAyah()
            } else {
                entryPoint.repository().loadPreferredAyah()
            }
            val prayerTimes = if (cachedOnly) {
                entryPoint.prayerTimesRepository().loadCached(city)
            } else {
                entryPoint.prayerTimesRepository().load(city)
            }
            DailyAyahWidgetUpdater.update(context, ayah, prayerTimes)
            pendingResult.finish()
        }
    }

    companion object {
        const val ActionRefreshPrayer = "com.cufica.dailyayah.widget.REFRESH_PRAYER"
        const val ActionRefreshCountdown = "com.cufica.dailyayah.widget.REFRESH_COUNTDOWN"

        fun requestRefresh(context: Context) {
            context.sendBroadcast(android.content.Intent(context, DailyAyahWidgetProvider::class.java).apply {
                action = ActionRefreshPrayer
            })
        }
    }
}