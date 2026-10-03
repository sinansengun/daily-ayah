package com.cufica.dailyayah.ui.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cufica.dailyayah.data.model.PrayerTimes
import com.cufica.dailyayah.ui.components.DailyAyahBackground
import com.cufica.dailyayah.ui.components.ErrorState
import com.cufica.dailyayah.ui.components.LoadingState
import com.cufica.dailyayah.ui.components.ScreenHeader
import com.cufica.dailyayah.ui.theme.DailyAyahTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun RedesignedPrayerTimesScreen(
    state: PrayerTimesUiState,
    onRefresh: () -> Unit
) {
    DailyAyahBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            ScreenHeader(
                eyebrow = "Günlük düzen",
                title = "Namaz Vakitleri",
                subtitle = state.times?.date?.let(::prayerTimesFormattedDate),
                onRefresh = onRefresh
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.MyLocation,
                    contentDescription = "Canlı konum",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(state.city, style = MaterialTheme.typography.titleMedium)
            }

            when {
                state.isLoading && state.times == null -> LoadingState("Vakitler hesaplanıyor")
                state.times != null -> RedesignedPrayerTimesList(state.times)
                else -> ErrorState(state.error ?: "Namaz vakitleri alınamadı.", onRefresh)
            }
        }
    }
}

@Composable
private fun RedesignedPrayerTimesList(times: PrayerTimes) {
    val prayers = listOf(
        "İmsak" to times.imsak,
        "Güneş" to times.gunes,
        "Öğle" to times.ogle,
        "İkindi" to times.ikindi,
        "Akşam" to times.aksam,
        "Yatsı" to times.yatsi
    )
    val nextPrayer = remember(prayers) { redesignedNextPrayer(prayers) }

    Column(modifier = Modifier.fillMaxWidth()) {
        prayers.forEachIndexed { index, prayer ->
            RedesignedPrayerTimeRow(prayer.first, prayer.second, prayer.first == nextPrayer)
            if (index < prayers.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Text(
            text = "Kaynak: ${times.source}",
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RedesignedPrayerTimeRow(name: String, value: String, isNext: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isNext) Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 14.dp)
                else Modifier.padding(horizontal = 14.dp)
            )
            .padding(vertical = 17.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            if (isNext) {
                Text(
                    "Sıradaki vakit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isNext) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun redesignedNextPrayer(prayers: List<Pair<String, String>>): String {
    val formatter = DateTimeFormatter.ofPattern("H:mm")
    val now = LocalTime.now(ZoneId.of("Europe/Istanbul"))
    return prayers.firstOrNull { (_, value) ->
        runCatching { LocalTime.parse(value.trim(), formatter).isAfter(now) }.getOrDefault(false)
    }?.first ?: prayers.first().first
}

private fun prayerTimesFormattedDate(value: String): String = runCatching {
    LocalDate.parse(value, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        .format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale("tr", "TR")))
}.recoverCatching {
    LocalDate.parse(value)
        .format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale("tr", "TR")))
}.recoverCatching {
    LocalDate.parse(value, DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr", "TR")))
        .format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale("tr", "TR")))
}.getOrDefault(value)

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun RedesignedPrayerTimesPreview() {
    DailyAyahTheme {
        RedesignedPrayerTimesScreen(
            state = PrayerTimesUiState(
                city = "İstanbul",
                isLoading = false,
                times = PrayerTimes(
                    city = "İstanbul",
                    country = "Türkiye",
                    date = "21 Eylül 2026",
                    timeZone = "Europe/Istanbul",
                    imsak = "05:12",
                    gunes = "06:38",
                    ogle = "13:02",
                    ikindi = "16:31",
                    aksam = "19:17",
                    yatsi = "20:38",
                    source = "Diyanet İşleri Başkanlığı",
                    fetchedAt = "2026-09-21T08:00:00Z"
                )
            ),
            onRefresh = {}
        )
    }
}