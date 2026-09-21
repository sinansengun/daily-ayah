package com.cufica.dailyayah.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
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

object PrayerOnlyWidgetUpdater {
    fun update(context: Context, times: PrayerTimes?) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, PrayerOnlyWidgetProvider::class.java)
        val nextPrayer = times?.let(WidgetPrayerCountdown::nextPrayer)
        manager.getAppWidgetIds(component).forEach { widgetId ->
            val isTwoRows = manager.getAppWidgetOptions(widgetId).isTwoRowWidget()
            val opacity = PrayerWidgetPreferences.backgroundOpacity(context, widgetId)
            val lightText = opacity < 50
            val primary = if (lightText) Color.WHITE else Color.rgb(40, 48, 56)
            val secondary = if (lightText) Color.WHITE else Color.rgb(86, 97, 107)
            val layoutId = if (PrayerWidgetPreferences.textShadowEnabled(context, widgetId)) {
                R.layout.widget_prayer_only_shadow
            } else {
                R.layout.widget_prayer_only
            }
            RemoteViews(context.packageName, layoutId).apply {
                setInt(R.id.widget_prayer_only_background, "setImageAlpha", opacity * 255 / 100)
                setTextColor(R.id.widget_prayer_only_name, primary)
                setTextColor(R.id.widget_prayer_only_countdown, primary)
                setTextColor(R.id.widget_prayer_only_location, secondary)
                setTextViewTextSize(R.id.widget_prayer_only_name, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 22f else 18f)
                setTextViewTextSize(R.id.widget_prayer_only_countdown, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 28f else 23f)
                setTextViewTextSize(R.id.widget_prayer_only_location, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 12f else 10f)
                setTextViewText(R.id.widget_prayer_only_name, nextPrayer?.name ?: "Vakitler hazırlanıyor")
                setTextViewText(R.id.widget_prayer_only_countdown, nextPrayer?.let(WidgetPrayerCountdown::label) ?: "--:-- dk")
                setTextViewText(R.id.widget_prayer_only_location, times?.city.orEmpty())
                setOnClickPendingIntent(R.id.widget_prayer_only_root, mainIntent(context, 12))
                manager.updateAppWidget(widgetId, this)
            }
        }
        nextPrayer?.let {
            WidgetPrayerCountdown.schedule(
                context,
                it.dateTime,
                PrayerOnlyWidgetProvider::class.java,
                PrayerOnlyWidgetProvider.ActionRefreshCountdown,
                31
            )
        }
    }
}

object AllInOneWidgetUpdater {
    fun update(context: Context, ayah: DailyAyah?, times: PrayerTimes?, zikirmatik: ZikirmatikState) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, AllInOneWidgetProvider::class.java)
        val nextPrayer = times?.let(WidgetPrayerCountdown::nextPrayer)
        manager.getAppWidgetIds(component).forEach { widgetId ->
            val isTwoRows = manager.getAppWidgetOptions(widgetId).isTwoRowWidget()
            val opacity = PrayerWidgetPreferences.backgroundOpacity(context, widgetId)
            val lightText = opacity < 50
            val primary = if (lightText) Color.WHITE else Color.rgb(40, 48, 56)
            val secondary = if (lightText) Color.WHITE else Color.rgb(86, 97, 107)
            val layoutId = if (PrayerWidgetPreferences.textShadowEnabled(context, widgetId)) {
                R.layout.widget_all_in_one_shadow
            } else {
                R.layout.widget_all_in_one
            }
            RemoteViews(context.packageName, layoutId).apply {
                setInt(R.id.widget_all_background, "setImageAlpha", opacity * 255 / 100)
                listOf(R.id.widget_all_ayah, R.id.widget_all_reference, R.id.widget_all_prayer_name,
                    R.id.widget_all_prayer_countdown, R.id.widget_all_zikir_count, R.id.widget_all_zikir_name)
                    .forEach { setTextColor(it, primary) }
                setTextColor(R.id.widget_all_location, secondary)
                setTextColor(R.id.widget_all_zikir_target, secondary)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setColorStateList(
                        R.id.widget_all_zikir_progress,
                        "setProgressTintList",
                        ColorStateList.valueOf(primary)
                    )
                    setColorStateList(
                        R.id.widget_all_zikir_progress,
                        "setProgressBackgroundTintList",
                        ColorStateList.valueOf(primary.withAlpha(70))
                    )
                }
                setInt(R.id.widget_all_ayah, "setMaxLines", if (isTwoRows) 9 else 4)
                setTextViewTextSize(R.id.widget_all_ayah, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 16f else 11f)
                setTextViewTextSize(R.id.widget_all_reference, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 13f else 10f)
                setTextViewTextSize(R.id.widget_all_prayer_name, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 23f else 16f)
                setTextViewTextSize(R.id.widget_all_prayer_countdown, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 26f else 17f)
                setTextViewTextSize(R.id.widget_all_location, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 12f else 9f)
                setTextViewTextSize(R.id.widget_all_zikir_count, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 38f else 25f)
                setTextViewTextSize(R.id.widget_all_zikir_name, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 14f else 10f)
                setTextViewTextSize(R.id.widget_all_zikir_target, TypedValue.COMPLEX_UNIT_SP, if (isTwoRows) 12f else 10f)
                setViewVisibility(R.id.widget_all_zikir_target, if (isTwoRows) View.VISIBLE else View.GONE)
                setTextViewText(R.id.widget_all_ayah, ayah?.text ?: "Günün ayeti hazırlanıyor.")
                setTextViewText(R.id.widget_all_reference, ayah?.reference ?: "Daily Ayah")
                setTextViewText(R.id.widget_all_prayer_name, nextPrayer?.name ?: "Vakitler hazırlanıyor")
                setTextViewText(R.id.widget_all_prayer_countdown, nextPrayer?.let(WidgetPrayerCountdown::label) ?: "--:-- dk")
                setTextViewText(R.id.widget_all_location, times?.city.orEmpty())
                setTextViewText(R.id.widget_all_zikir_count, zikirmatik.count.toString())
                setTextViewText(R.id.widget_all_zikir_name, zikirmatik.name)
                setTextViewText(R.id.widget_all_zikir_target, "Hedef: ${zikirmatik.totalTarget}")
                setProgressBar(R.id.widget_all_zikir_progress, zikirmatik.totalTarget, zikirmatik.count.coerceAtMost(zikirmatik.totalTarget), false)
                setOnClickPendingIntent(R.id.widget_all_root, mainIntent(context, 13))
                manager.updateAppWidget(widgetId, this)
            }
        }
        nextPrayer?.let {
            WidgetPrayerCountdown.schedule(
                context,
                it.dateTime,
                AllInOneWidgetProvider::class.java,
                AllInOneWidgetProvider.ActionRefreshCountdown,
                32
            )
        }
    }
}

private object WidgetPrayerCountdown {
    data class NextPrayer(val name: String, val dateTime: ZonedDateTime)

    fun nextPrayer(times: PrayerTimes): NextPrayer {
        val zone = ZoneId.of(times.timeZone)
        val now = ZonedDateTime.now(zone)
        val date = maxOf(LocalDate.parse(times.date), now.toLocalDate())
        val prayers = listOf(
            "Güneş" to times.gunes,
            "Öğle" to times.ogle,
            "İkindi" to times.ikindi,
            "Akşam" to times.aksam,
            "Yatsı" to times.yatsi
        ).map { (name, time) -> NextPrayer(name, date.atTime(LocalTime.parse(time)).atZone(zone)) }
        return prayers.firstOrNull { it.dateTime.isAfter(now) }
            ?: NextPrayer("Güneş", date.plusDays(1).atTime(LocalTime.parse(times.gunes)).atZone(zone))
    }

    fun label(nextPrayer: NextPrayer): String {
        val minutes = Duration.between(ZonedDateTime.now(nextPrayer.dateTime.zone), nextPrayer.dateTime)
            .toMinutes()
            .coerceAtLeast(0)
        return "%d:%02d dk".format(minutes / 60, minutes % 60)
    }

    fun schedule(context: Context, prayerDateTime: ZonedDateTime, receiver: Class<*>, action: String, requestCode: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, receiver).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val now = ZonedDateTime.now(prayerDateTime.zone)
        val refreshAt = minOf(now.withSecond(0).withNano(0).plusMinutes(1), prayerDateTime)
        context.getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            refreshAt.toInstant().toEpochMilli(),
            pendingIntent
        )
    }
}

private fun mainIntent(context: Context, requestCode: Int): PendingIntent = PendingIntent.getActivity(
    context,
    requestCode,
    Intent(context, MainActivity::class.java),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)