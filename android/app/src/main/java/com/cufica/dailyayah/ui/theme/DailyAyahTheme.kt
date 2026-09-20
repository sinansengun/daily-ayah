package com.cufica.dailyayah.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DailyAyahColors = lightColorScheme()

@Composable
fun DailyAyahTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DailyAyahColors,
        content = content
    )
}