package com.cufica.dailyayah.ui.prayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cufica.dailyayah.R
import com.cufica.dailyayah.data.model.PrayerTimes

@Composable
fun PrayerTimesRoute(viewModel: PrayerTimesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PrayerTimesScreen(state, viewModel::selectCity, viewModel::refresh)
}

@Composable
private fun PrayerTimesScreen(
    state: PrayerTimesUiState,
    onSelectCity: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var cityMenuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.app_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.18f),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 48.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    OutlinedButton(onClick = { cityMenuExpanded = true }) { Text(state.city) }
                    DropdownMenu(expanded = cityMenuExpanded, onDismissRequest = { cityMenuExpanded = false }) {
                        state.cities.forEach { city ->
                            DropdownMenuItem(text = { Text(city) }, onClick = { cityMenuExpanded = false; onSelectCity(city) })
                        }
                    }
                }
                IconButton(onClick = onRefresh, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Outlined.Refresh, "Yenile")
                }
            }
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                state.times != null -> PrayerTimesList(state.times)
                else -> Text(state.error ?: "Namaz vakitleri alınamadı.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun PrayerTimesList(times: PrayerTimes) {
    Text("${times.city} Namaz Vakitleri", style = MaterialTheme.typography.headlineSmall)
    Text(times.date, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        PrayerTimeRow("İmsak", times.imsak)
        PrayerTimeRow("Güneş", times.gunes)
        PrayerTimeRow("Öğle", times.ogle)
        PrayerTimeRow("İkindi", times.ikindi)
        PrayerTimeRow("Akşam", times.aksam)
        PrayerTimeRow("Yatsı", times.yatsi)
    }
    Text(times.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PrayerTimeRow(name: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}