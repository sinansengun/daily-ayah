package com.cufica.dailyayah.widget

import android.app.PendingIntent
import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.cufica.dailyayah.MainActivity
import com.cufica.dailyayah.R
import com.cufica.dailyayah.data.model.DailyAyah
import com.cufica.dailyayah.data.model.PrayerTimes
import com.cufica.dailyayah.data.model.ZikirmatikState
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object DailyAyahWidgetUpdater {
    fun update(context: Context, ayah: DailyAyah?, prayerTimes: PrayerTimes? = null) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, DailyAyahWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(component)
        val nextPrayer = prayerTimes?.let(::nextPrayer)
        widgetIds.forEach { widgetId ->
            manager.updateAppWidget(widgetId, remoteViews(context, widgetId, ayah, nextPrayer, prayerTimes?.city))
        }
        nextPrayer?.let { scheduleCountdownRefresh(context, it.dateTime) }
    }

    private fun remoteViews(
        context: Context,
        widgetId: Int,
        ayah: DailyAyah?,
        nextPrayer: NextPrayer?,
        city: String?
    ): RemoteViews = RemoteViews(
        context.packageName,
        if (PrayerWidgetPreferences.textShadowEnabled(context, widgetId)) {
            R.layout.widget_daily_ayah_shadow
        } else {
            R.layout.widget_daily_ayah
        }
    ).apply {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
        val isTwoRows = options.isTwoRowWidget()
        val backgroundOpacity = PrayerWidgetPreferences.backgroundOpacity(context, widgetId)
        setInt(
            R.id.widget_daily_ayah_background,
            "setImageAlpha",
            backgroundOpacity * 255 / 100
        )
        val usesDarkText = PrayerWidgetPreferences.darkTextEnabled(context)
        val primaryTextColor = if (usesDarkText) Color.rgb(40, 48, 56) else Color.WHITE
        val secondaryTextColor = if (usesDarkText) Color.rgb(86, 97, 107) else Color.WHITE
        setTextColor(R.id.widget_daily_ayah_text, primaryTextColor)
        setTextColor(R.id.widget_daily_ayah_reference, primaryTextColor)
        setTextColor(R.id.widget_next_prayer_name, primaryTextColor)
        setTextColor(R.id.widget_next_prayer_countdown, secondaryTextColor)
        setTextColor(R.id.widget_prayer_location, secondaryTextColor)
        setInt(R.id.widget_daily_ayah_text, "setMaxLines", if (isTwoRows) 9 else 4)
        setTextViewTextSize(
            R.id.widget_daily_ayah_text,
            TypedValue.COMPLEX_UNIT_SP,
            if (isTwoRows) 14f else 11f
        )
        setTextViewTextSize(
            R.id.widget_daily_ayah_reference,
            TypedValue.COMPLEX_UNIT_SP,
            if (isTwoRows) 12f else 11f
        )
        setTextViewTextSize(
            R.id.widget_next_prayer_name,
            TypedValue.COMPLEX_UNIT_SP,
            if (isTwoRows) 20f else 15f
        )
        setTextViewTextSize(
            R.id.widget_next_prayer_countdown,
            TypedValue.COMPLEX_UNIT_SP,
            if (isTwoRows) 22f else 18f
        )
        setTextViewTextSize(
            R.id.widget_prayer_location,
            TypedValue.COMPLEX_UNIT_SP,
            if (isTwoRows) 11f else 10f
        )
        setTextViewText(R.id.widget_daily_ayah_text, ayah?.text ?: "Günün ayeti hazırlanıyor.")
        setTextViewText(R.id.widget_daily_ayah_reference, ayah?.reference ?: "Daily Ayah")
        if (nextPrayer == null) {
            setTextViewText(R.id.widget_next_prayer_name, "Vakitler hazırlanıyor")
            setTextViewText(R.id.widget_next_prayer_countdown, "--:-- dk")
        } else {
            val remainingMinutes = Duration.between(
                ZonedDateTime.now(nextPrayer.dateTime.zone),
                nextPrayer.dateTime
            ).toMinutes().coerceAtLeast(0)
            setTextViewText(R.id.widget_next_prayer_name, nextPrayer.name)
            setTextViewText(
                R.id.widget_next_prayer_countdown,
                if (isTwoRows) {
                    "%d:%02d dk".format(remainingMinutes / 60, remainingMinutes % 60)
                } else {
                    compactCountdownLabel(remainingMinutes)
                }
            )
        }
        setTextViewText(R.id.widget_prayer_location, city.orEmpty())
        setOnClickPendingIntent(R.id.widget_daily_ayah_root, appIntent(context))
    }

    private fun nextPrayer(times: PrayerTimes): NextPrayer {
        val zone = ZoneId.of(times.timeZone)
        val now = ZonedDateTime.now(zone)
        val date = maxOf(LocalDate.parse(times.date), now.toLocalDate())
        val prayers = listOf(
            "İmsak" to times.imsak,
            "Güneş" to times.gunes,
            "Öğle" to times.ogle,
            "İkindi" to times.ikindi,
            "Akşam" to times.aksam,
            "Yatsı" to times.yatsi
        ).map { (name, time) -> NextPrayer(name, date.atTime(LocalTime.parse(time)).atZone(zone)) }

        return prayers.firstOrNull { it.dateTime.isAfter(now) }
            ?: NextPrayer("İmsak", date.plusDays(1).atTime(LocalTime.parse(times.imsak)).atZone(zone))
    }

    private fun scheduleCountdownRefresh(context: Context, prayerDateTime: ZonedDateTime) {
        val intent = Intent(context, DailyAyahWidgetProvider::class.java).apply {
            action = DailyAyahWidgetProvider.ActionRefreshCountdown
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            30,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val now = ZonedDateTime.now(prayerDateTime.zone)
        val nextMinute = now.withSecond(0).withNano(0).plusMinutes(1)
        val refreshAt = minOf(nextMinute, prayerDateTime)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            refreshAt.toInstant().toEpochMilli(),
            pendingIntent
        )
    }

    private data class NextPrayer(val name: String, val dateTime: ZonedDateTime)

    private fun compactCountdownLabel(remainingMinutes: Long): String {
        val hours = remainingMinutes / 60
        val minutes = remainingMinutes % 60
        return if (hours == 0L) "${minutes}dk" else "${hours}s ${minutes}dk"
    }

    private fun appIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        10,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

object DailyAyahOnlyWidgetUpdater {
    fun update(context: Context, ayah: DailyAyah?) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, DailyAyahOnlyWidgetProvider::class.java)
        manager.getAppWidgetIds(component).forEach { widgetId ->
            val options = manager.getAppWidgetOptions(widgetId)
            val isTwoRows = options.isTwoRowWidget()
            val backgroundOpacity = PrayerWidgetPreferences.backgroundOpacity(context, widgetId)
            val usesDarkText = PrayerWidgetPreferences.darkTextEnabled(context)
            val primaryTextColor = if (usesDarkText) Color.rgb(40, 48, 56) else Color.WHITE
            val layoutId = if (PrayerWidgetPreferences.textShadowEnabled(context, widgetId)) {
                R.layout.widget_daily_ayah_only_shadow
            } else {
                R.layout.widget_daily_ayah_only
            }
            RemoteViews(context.packageName, layoutId).apply {
                setInt(R.id.widget_ayah_only_background, "setImageAlpha", backgroundOpacity * 255 / 100)
                setTextColor(R.id.widget_ayah_only_text, primaryTextColor)
                setTextColor(R.id.widget_ayah_only_reference, primaryTextColor)
                setInt(R.id.widget_ayah_only_text, "setMaxLines", if (isTwoRows) 9 else 3)
                setTextViewTextSize(
                    R.id.widget_ayah_only_text,
                    TypedValue.COMPLEX_UNIT_SP,
                    if (isTwoRows) 16f else 14f
                )
                setTextViewTextSize(
                    R.id.widget_ayah_only_reference,
                    TypedValue.COMPLEX_UNIT_SP,
                    if (isTwoRows) 13f else 11f
                )
                setTextViewText(R.id.widget_ayah_only_text, ayah?.text ?: "Günün ayeti hazırlanıyor.")
                setTextViewText(R.id.widget_ayah_only_reference, ayah?.reference ?: "Daily Ayah")
                setOnClickPendingIntent(R.id.widget_ayah_only_root, appIntent(context))
                manager.updateAppWidget(widgetId, this)
            }
        }
    }

    private fun appIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        11,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

object PrayerWidgetPreferences {
    private const val PreferencesName = "prayer_widget"
    private const val CityKey = "city"
    private const val BackgroundOpacityKey = "background_opacity"
    private const val TextShadowKey = "text_shadow"
    private const val DarkTextKey = "dark_text"

    fun selectedCity(context: Context): String = context
        .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getString(CityKey, "Istanbul") ?: "Istanbul"

    fun setSelectedCity(context: Context, city: String) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(CityKey, city)
            .apply()
    }

    fun backgroundOpacity(context: Context, widgetId: Int): Int {
        val storedOpacity = context
            .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .getInt(BackgroundOpacityKey, 100)
        return ((storedOpacity + 12) / 25 * 25).coerceIn(0, 100)
    }

    fun setBackgroundOpacity(context: Context, widgetId: Int, opacity: Int) {
        val steppedOpacity = ((opacity + 12) / 25 * 25).coerceIn(0, 100)
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putInt(BackgroundOpacityKey, steppedOpacity)
            .apply()
    }

    fun textShadowEnabled(context: Context, widgetId: Int): Boolean = context
        .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getBoolean(TextShadowKey, false)

    fun setTextShadowEnabled(context: Context, widgetId: Int, enabled: Boolean) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(TextShadowKey, enabled)
            .apply()
    }

    fun darkTextEnabled(context: Context): Boolean = context
        .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getBoolean(DarkTextKey, true)

    fun setDarkTextEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(DarkTextKey, enabled)
            .apply()
    }

    fun removeWidget(context: Context, widgetId: Int) {
        // Widget appearance is shared by every widget instance.
    }
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
        AllInOneWidgetProvider.requestRefresh(context, cachedOnly = true)
    }

    fun updateFromStoredState(context: Context) {
        updateAll(context, storedState(context))
    }

    fun storedState(context: Context): ZikirmatikState {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        return ZikirmatikState(
            name = preferences.getString(NameKey, "Subhanallah") ?: "Subhanallah",
            target = preferences.getInt(TargetKey, 33),
            groupCount = preferences.getInt(GroupCountKey, 1),
            count = preferences.getInt(CountKey, 0)
        )
    }

    private fun updateAll(context: Context, state: ZikirmatikState) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, ZikirmatikWidgetProvider::class.java)
        manager.getAppWidgetIds(component).forEach { widgetId ->
            manager.updateAppWidget(widgetId, remoteViews(context, widgetId, state))
        }
    }

    private fun remoteViews(context: Context, widgetId: Int, state: ZikirmatikState): RemoteViews = RemoteViews(
        context.packageName,
        if (PrayerWidgetPreferences.textShadowEnabled(context, widgetId)) {
            R.layout.widget_zikirmatik_shadow
        } else {
            R.layout.widget_zikirmatik
        }
    ).apply {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
        val isLarge = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) >= 100 ||
            options.isTwoRowWidget()
        val backgroundOpacity = PrayerWidgetPreferences.backgroundOpacity(context, widgetId)
        setInt(R.id.widget_zikirmatik_background, "setImageAlpha", backgroundOpacity * 255 / 100)
        val usesDarkText = PrayerWidgetPreferences.darkTextEnabled(context)
        val primaryTextColor = if (usesDarkText) Color.rgb(40, 48, 56) else Color.WHITE
        val secondaryTextColor = if (usesDarkText) Color.rgb(86, 97, 107) else Color.WHITE
        setTextColor(R.id.widget_zikirmatik_count, primaryTextColor)
        setTextColor(R.id.widget_zikirmatik_name, primaryTextColor)
        setTextColor(R.id.widget_zikirmatik_target, secondaryTextColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setColorStateList(
                R.id.widget_zikirmatik_progress,
                "setProgressTintList",
                ColorStateList.valueOf(primaryTextColor)
            )
            setColorStateList(
                R.id.widget_zikirmatik_progress,
                "setProgressBackgroundTintList",
                ColorStateList.valueOf(primaryTextColor.withAlpha(70))
            )
        }
        setTextViewTextSize(
            R.id.widget_zikirmatik_count,
            TypedValue.COMPLEX_UNIT_SP,
            if (isLarge) 34f else 28f
        )
        setTextViewTextSize(
            R.id.widget_zikirmatik_name,
            TypedValue.COMPLEX_UNIT_SP,
            if (isLarge) 14f else 11f
        )
        setTextViewTextSize(
            R.id.widget_zikirmatik_target,
            TypedValue.COMPLEX_UNIT_SP,
            if (isLarge) 12f else 10f
        )
        setViewVisibility(R.id.widget_zikirmatik_target, if (isLarge) View.VISIBLE else View.GONE)
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

internal fun Bundle.isTwoRowWidget(): Boolean = maxOf(
    getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
    getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
) >= 150

internal fun Int.withAlpha(alpha: Int): Int = Color.argb(
    alpha,
    Color.red(this),
    Color.green(this),
    Color.blue(this)
)
