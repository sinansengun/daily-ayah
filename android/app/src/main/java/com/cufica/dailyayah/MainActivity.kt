package com.cufica.dailyayah

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cufica.dailyayah.ui.daily.DailyAyahRoute
import com.cufica.dailyayah.ui.daily.DetailDestination
import com.cufica.dailyayah.ui.daily.DetailScreen
import com.cufica.dailyayah.ui.theme.DailyAyahTheme
import com.cufica.dailyayah.ui.zikirmatik.ZikirmatikRoute
import com.cufica.dailyayah.ui.prayer.PrayerTimesRoute
import com.cufica.dailyayah.ui.settings.NotificationSettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var notificationPermissionGranted by mutableStateOf(false)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationPermissionGranted = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationPermissionGranted = notificationPermissionGranted()
        requestNotificationPermission()
        val opensZikirmatik = intent?.data?.host == "zikirmatik"
        setContent {
            DailyAyahTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DailyAyahApp(
                        opensZikirmatik,
                        notificationPermissionGranted,
                        ::requestNotificationPermission
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun notificationPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun DailyAyahApp(
    opensZikirmatik: Boolean,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(if (opensZikirmatik) 1 else 0) }
    var detailDestination by remember { mutableStateOf<DetailDestination?>(null) }
    var settingsVisible by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = settingsVisible || selectedTab != 0) {
        if (settingsVisible) settingsVisible = false else selectedTab = 0
    }

    Scaffold(
        bottomBar = {
            if (detailDestination == null && !settingsVisible) NavigationBar {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
                label = { Text("Günün Ayeti") },
                colors = NavigationBarItemDefaults.colors(indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Outlined.FormatListNumbered, contentDescription = null) },
                label = { Text("Zikir") },
                colors = NavigationBarItemDefaults.colors(indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
            )
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                label = { Text("Vakitler") },
                colors = NavigationBarItemDefaults.colors(indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
            )
            }
        }
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            when {
                detailDestination != null -> DetailScreen(
                    detailDestination!!,
                    onBack = { detailDestination = null }
                )
                settingsVisible -> NotificationSettingsScreen(
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestNotificationPermission = onRequestNotificationPermission
                )
                else -> when (selectedTab) {
                    0 -> DailyAyahRoute(
                        onOpenDetail = { detailDestination = it },
                        onOpenSettings = { settingsVisible = true }
                    )
                    1 -> ZikirmatikRoute()
                    else -> PrayerTimesRoute()
                }
            }
        }
    }
}
