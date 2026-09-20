package com.cufica.dailyayah.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.cufica.dailyayah.MainActivity
import com.cufica.dailyayah.R
import com.cufica.dailyayah.data.model.DailyAyah
import com.cufica.dailyayah.data.model.ZikirmatikState

object DailyAyahWidgetUpdater {
    fun update(context: Context, ayah: DailyAyah?) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, DailyAyahWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(component)
        widgetIds.forEach { widgetId -> manager.updateAppWidget(widgetId, remoteViews(context, ayah)) }
    }

    fun remoteViews(context: Context, ayah: DailyAyah?): RemoteViews = RemoteViews(
        context.packageName,
        R.layout.widget_daily_ayah
    ).apply {
        setTextViewText(R.id.widget_daily_ayah_text, ayah?.text ?: "Günün ayeti hazırlanıyor.")
        setTextViewText(R.id.widget_daily_ayah_reference, ayah?.reference ?: "Daily Ayah")
        setOnClickPendingIntent(R.id.widget_daily_ayah_root, appIntent(context))
    }

    private fun appIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        10,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

object ZikirmatikWidgetUpdater {
    private const val PreferencesName = "zikirmatik_widget"
    private const val NameKey = "name"
    private const val TargetKey = "target"
    private const val GroupCountKey = "group_count"
    private const val CountKey = "count"

    fun update(context: Context, state: ZikirmatikState) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).edit()
            .putString(NameKey, state.name)
            .putInt(TargetKey, state.target)
            .putInt(GroupCountKey, state.groupCount)
            .putInt(CountKey, state.count)
            .apply()
        updateAll(context, state)
    }

    fun updateFromStoredState(context: Context) {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        updateAll(
            context,
            ZikirmatikState(
                name = preferences.getString(NameKey, "Subhanallah") ?: "Subhanallah",
                target = preferences.getInt(TargetKey, 33),
                groupCount = preferences.getInt(GroupCountKey, 1),
                count = preferences.getInt(CountKey, 0)
            )
        )
    }

    private fun updateAll(context: Context, state: ZikirmatikState) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, ZikirmatikWidgetProvider::class.java)
        manager.getAppWidgetIds(component).forEach { widgetId ->
            manager.updateAppWidget(widgetId, remoteViews(context, state))
        }
    }

    private fun remoteViews(context: Context, state: ZikirmatikState): RemoteViews = RemoteViews(
        context.packageName,
        R.layout.widget_zikirmatik
    ).apply {
        setTextViewText(R.id.widget_zikirmatik_count, state.count.toString())
        setTextViewText(R.id.widget_zikirmatik_name, state.name)
        setTextViewText(R.id.widget_zikirmatik_target, "Hedef: ${state.totalTarget}")
        setProgressBar(R.id.widget_zikirmatik_progress, state.totalTarget, state.count.coerceAtMost(state.totalTarget), false)
        setOnClickPendingIntent(R.id.widget_zikirmatik_root, zikirmatikIntent(context))
    }

    private fun zikirmatikIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        20,
        Intent(Intent.ACTION_VIEW, Uri.parse("dailyayah://zikirmatik"), context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}