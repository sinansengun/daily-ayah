package com.cufica.dailyayah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cufica.dailyayah.ui.daily.DailyAyahRoute
import com.cufica.dailyayah.ui.theme.DailyAyahTheme
import com.cufica.dailyayah.ui.zikirmatik.ZikirmatikRoute
import com.cufica.dailyayah.ui.prayer.PrayerTimesRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val opensZikirmatik = intent?.data?.host == "zikirmatik"
        setContent {
            DailyAyahTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DailyAyahApp(opensZikirmatik)
                }
            }
        }
    }
}

@Composable
private fun DailyAyahApp(opensZikirmatik: Boolean) {
    var selectedTab by rememberSaveable { mutableIntStateOf(if (opensZikirmatik) 1 else 0) }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> DailyAyahRoute()
            1 -> ZikirmatikRoute()
            2 -> PrayerTimesRoute()
        }

        NavigationBar(modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
                label = { Text("Günün Ayeti") }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Outlined.FormatListNumbered, contentDescription = null) },
                label = { Text("Zikirmatik") }
            )
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                label = { Text("Vakitler") }
            )
        }
    }
}
