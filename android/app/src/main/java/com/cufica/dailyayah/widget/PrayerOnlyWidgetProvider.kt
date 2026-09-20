package com.cufica.dailyayah.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.cufica.dailyayah.di.RepositoryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerOnlyWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ActionRefresh -> refresh(context)
            ActionRefreshCountdown -> refresh(context, cachedOnly = true)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = refresh(context)

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) = refresh(context)

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { PrayerWidgetPreferences.removeWidget(context, it) }
    }

    private fun refresh(context: Context, cachedOnly: Boolean = false) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val repository = EntryPointAccessors.fromApplication(
                context.applicationContext,
                RepositoryEntryPoint::class.java
            ).prayerTimesRepository()
            val city = PrayerWidgetPreferences.selectedCity(context)
            PrayerOnlyWidgetUpdater.update(context, if (cachedOnly) repository.loadCached(city) else repository.load(city))
            pendingResult.finish()
        }
    }

    companion object {
        const val ActionRefreshCountdown = "com.cufica.dailyayah.widget.PRAYER_ONLY_COUNTDOWN"
        private const val ActionRefresh = "com.cufica.dailyayah.widget.PRAYER_ONLY_REFRESH"
        fun requestRefresh(context: Context) {
            context.sendBroadcast(Intent(context, PrayerOnlyWidgetProvider::class.java).apply {
                action = ActionRefresh
            })
        }
    }
}