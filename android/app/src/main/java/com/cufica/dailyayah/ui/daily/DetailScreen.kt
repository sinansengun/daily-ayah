package com.cufica.dailyayah.ui.daily

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cufica.dailyayah.data.DailyAyahRepository
import com.cufica.dailyayah.data.model.TafsirAyah
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import com.cufica.dailyayah.di.RepositoryEntryPoint

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

    Column(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Geri") }
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        } else if (tafsir == null) {
            Text("Bu içerik henüz hazır değil.", modifier = Modifier.padding(24.dp))
        } else {
            DetailContent(destination, tafsir!!)
        }
    }
}

@Composable
private fun DetailContent(destination: DetailDestination, tafsir: TafsirAyah) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(if (destination is DetailDestination.Tafsir) "Tefsir" else "Sure Bilgisi", style = MaterialTheme.typography.headlineSmall)
        Text("${tafsir.surahName} Suresi", style = MaterialTheme.typography.titleLarge)
        if (destination is DetailDestination.Surah) {
            Text("${tafsir.totalAyahCount} ayet", style = MaterialTheme.typography.bodyLarge)
            tafsir.mushafOrder?.let { Text("Mushaf sırası: $it") }
            tafsir.nuzulOrder?.let { Text("Nüzul sırası: $it") }
            tafsir.aboutText?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        } else {
            tafsir.arabicText?.let { Text(it, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.titleLarge) }
            Text("Meal", style = MaterialTheme.typography.titleMedium)
            Text(tafsir.mealText, style = MaterialTheme.typography.bodyLarge)
            Text("Tefsir", style = MaterialTheme.typography.titleMedium)
            Text(tafsir.tafsirText, style = MaterialTheme.typography.bodyLarge)
        }
        tafsir.sourceReference?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
    }
}