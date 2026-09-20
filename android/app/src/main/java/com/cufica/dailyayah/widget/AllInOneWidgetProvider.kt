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

class AllInOneWidgetProvider : AppWidgetProvider() {
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
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                RepositoryEntryPoint::class.java
            )
            val city = PrayerWidgetPreferences.selectedCity(context)
            val ayah = if (cachedOnly) entryPoint.repository().loadCachedAyah() else entryPoint.repository().loadPreferredAyah()
            val times = if (cachedOnly) entryPoint.prayerTimesRepository().loadCached(city) else entryPoint.prayerTimesRepository().load(city)
            AllInOneWidgetUpdater.update(context, ayah, times, ZikirmatikWidgetUpdater.storedState(context))
            pendingResult.finish()
        }
    }

    companion object {
        const val ActionRefreshCountdown = "com.cufica.dailyayah.widget.ALL_IN_ONE_COUNTDOWN"
        private const val ActionRefresh = "com.cufica.dailyayah.widget.ALL_IN_ONE_REFRESH"
        fun requestRefresh(context: Context, cachedOnly: Boolean = false) {
            context.sendBroadcast(Intent(context, AllInOneWidgetProvider::class.java).apply {
                action = if (cachedOnly) ActionRefreshCountdown else ActionRefresh
            })
        }
    }
}