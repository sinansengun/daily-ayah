package com.cufica.dailyayah.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cufica.dailyayah.R
import com.cufica.dailyayah.ui.theme.DailyAyahTheme
import kotlin.math.roundToInt

class DailyAyahWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val providerClassName = appWidgetManager
            .getAppWidgetInfo(widgetId)
            ?.provider
            ?.className
            .orEmpty()
        val isZikirmatik = providerClassName.endsWith("ZikirmatikWidgetProvider")

        setContent {
            DailyAyahTheme {
                var opacity by remember {
                    mutableFloatStateOf(PrayerWidgetPreferences.backgroundOpacity(this, widgetId).toFloat())
                }
                var textShadowEnabled by remember {
                    mutableStateOf(PrayerWidgetPreferences.textShadowEnabled(this, widgetId))
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (isZikirmatik) "Zikirmatik arka planı" else "Widget arka planı",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Arka plan yoğunluğu")
                        Spacer(modifier = Modifier.height(12.dp))
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0f..100f,
                            steps = 3
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Şeffaf")
                            Text("%${opacity.roundToInt()}")
                            Text("Opak")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (opacity < 50f) {
                                "Yazı rengi: Açık"
                            } else {
                                "Yazı rengi: Koyu"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Metin gölgesi")
                            Switch(
                                checked = textShadowEnabled,
                                onCheckedChange = { textShadowEnabled = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                PrayerWidgetPreferences.setBackgroundOpacity(
                                    this@DailyAyahWidgetConfigActivity,
                                    widgetId,
                                    opacity.roundToInt()
                                )
                                PrayerWidgetPreferences.setTextShadowEnabled(
                                    this@DailyAyahWidgetConfigActivity,
                                    widgetId,
                                    textShadowEnabled
                                )
                                publishInitialRemoteViews(
                                    appWidgetManager,
                                    widgetId,
                                    providerClassName,
                                    opacity.roundToInt(),
                                    textShadowEnabled
                                )
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                )
                                when {
                                    providerClassName.endsWith("ZikirmatikWidgetProvider") ->
                                        ZikirmatikWidgetUpdater.updateFromStoredState(this@DailyAyahWidgetConfigActivity)
                                    providerClassName.endsWith("DailyAyahOnlyWidgetProvider") ->
                                        DailyAyahOnlyWidgetProvider.requestRefresh(this@DailyAyahWidgetConfigActivity)
                                    providerClassName.endsWith("PrayerOnlyWidgetProvider") ->
                                        PrayerOnlyWidgetProvider.requestRefresh(this@DailyAyahWidgetConfigActivity)
                                    providerClassName.endsWith("AllInOneWidgetProvider") ->
                                        AllInOneWidgetProvider.requestRefresh(this@DailyAyahWidgetConfigActivity)
                                    else -> DailyAyahWidgetProvider.requestRefresh(this@DailyAyahWidgetConfigActivity)
                                }
                                finish()
                            }
                        ) {
                            Text("Uygula")
                        }
                    }
                }
            }
        }
    }

    private fun publishInitialRemoteViews(
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        providerClassName: String,
        opacity: Int,
        textShadowEnabled: Boolean
    ) {
        val (layoutId, backgroundId) = when {
            providerClassName.endsWith("ZikirmatikWidgetProvider") ->
                (if (textShadowEnabled) R.layout.widget_zikirmatik_shadow else R.layout.widget_zikirmatik) to
                    R.id.widget_zikirmatik_background
            providerClassName.endsWith("DailyAyahOnlyWidgetProvider") ->
                (if (textShadowEnabled) R.layout.widget_daily_ayah_only_shadow else R.layout.widget_daily_ayah_only) to
                    R.id.widget_ayah_only_background
            providerClassName.endsWith("PrayerOnlyWidgetProvider") ->
                (if (textShadowEnabled) R.layout.widget_prayer_only_shadow else R.layout.widget_prayer_only) to
                    R.id.widget_prayer_only_background
            providerClassName.endsWith("AllInOneWidgetProvider") ->
                (if (textShadowEnabled) R.layout.widget_all_in_one_shadow else R.layout.widget_all_in_one) to
                    R.id.widget_all_background
            else ->
                (if (textShadowEnabled) R.layout.widget_daily_ayah_shadow else R.layout.widget_daily_ayah) to
                    R.id.widget_daily_ayah_background
        }
        appWidgetManager.updateAppWidget(
            widgetId,
            RemoteViews(packageName, layoutId).apply {
                setInt(backgroundId, "setImageAlpha", opacity * 255 / 100)
            }
        )
    }
}