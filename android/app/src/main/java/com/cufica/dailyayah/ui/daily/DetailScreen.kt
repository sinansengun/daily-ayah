package com.cufica.dailyayah.ui.daily

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cufica.dailyayah.data.DailyAyahRepository
import com.cufica.dailyayah.data.model.TafsirAyah
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import com.cufica.dailyayah.di.RepositoryEntryPoint
import com.cufica.dailyayah.ui.components.DailyAyahBackground
import com.cufica.dailyayah.ui.components.LoadingState
import com.cufica.dailyayah.ui.components.SectionHeading
import com.cufica.dailyayah.ui.theme.Newsreader

@Composable
fun DetailScreen(destination: DetailDestination, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val repository = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, RepositoryEntryPoint::class.java).repository()
    }
    val route = when (destination) {
        is DetailDestination.Tafsir -> destination.route
        is DetailDestination.Surah -> destination.route
    }
    var tafsir by remember(route) { mutableStateOf<TafsirAyah?>(null) }
    var loading by remember(route) { mutableStateOf(true) }
    LaunchedEffect(route) {
        loading = true
        tafsir = repository.loadTafsir(route.surahNumber, route.ayahNumber)
        loading = false
    }

    DailyAyahBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(
                title = if (destination is DetailDestination.Tafsir) "Ayet Tefsiri" else "Sure Bilgisi",
                onBack = onBack
            )
            when {
                loading -> LoadingState("İçerik hazırlanıyor")
                tafsir == null -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Bu içerik henüz hazır değil.", style = MaterialTheme.typography.bodyLarge)
                }
                else -> DetailContent(destination, tafsir!!)
            }
        }
    }
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun DetailContent(destination: DetailDestination, tafsir: TafsirAyah) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (destination is DetailDestination.Tafsir) "AYETİ ANLAMAK" else "SUREYE YAKINDAN BAKIŞ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text("${tafsir.surahName} Suresi", style = MaterialTheme.typography.headlineLarge)
        }
        if (destination is DetailDestination.Surah) {
            SurahMetadata(tafsir)
            tafsir.aboutText?.takeIf(String::isNotBlank)?.let {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading("Sure Hakkında")
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            tafsir.arabicText?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(20.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = Newsreader,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
            DetailTextSection("Meal", tafsir.mealText)
            DetailTextSection("Tefsir", tafsir.tafsirText)
        }
        tafsir.sourceReference?.takeIf(String::isNotBlank)?.let {
            Text(
                text = "Kaynak: $it",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SurahMetadata(tafsir: TafsirAyah) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DetailMetric("Ayet", tafsir.totalAyahCount.toString())
        tafsir.mushafOrder?.let { DetailMetric("Mushaf", it.toString()) }
        tafsir.nuzulOrder?.let { DetailMetric("Nüzul", it.toString()) }
    }
}

@Composable
private fun DetailMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun DetailTextSection(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(title)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}