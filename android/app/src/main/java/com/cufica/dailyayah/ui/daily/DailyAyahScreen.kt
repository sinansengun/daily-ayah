package com.cufica.dailyayah.ui.daily

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cufica.dailyayah.data.model.DailyAyah
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DailyAyahRoute(viewModel: DailyAyahViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DailyAyahScreen(uiState = uiState, onRefresh = viewModel::refresh)
}

@Composable
fun DailyAyahScreen(uiState: DailyAyahUiState, onRefresh: () -> Unit) {
    when {
        uiState.isLoading && uiState.ayah == null -> LoadingContent()
        uiState.ayah != null -> DailyContent(uiState = uiState, onRefresh = onRefresh)
        else -> ErrorContent(message = uiState.errorMessage.orEmpty(), onRefresh = onRefresh)
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DailyContent(uiState: DailyAyahUiState, onRefresh: () -> Unit) {
    val items = remember(uiState.ayah, uiState.history) {
        listOfNotNull(uiState.ayah).plus(uiState.history.filter { it.publishedDateTR != uiState.ayah?.publishedDateTR }).take(15)
    }
    var selectedIndex by rememberSaveable(items.firstOrNull()?.publishedDateTR) { mutableIntStateOf(0) }
    val ayah = items[selectedIndex.coerceIn(0, items.lastIndex)]
    var detail by remember { mutableStateOf<DetailDestination?>(null) }

    detail?.let { destination ->
        DetailScreen(destination = destination, onBack = { detail = null })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = androidx.compose.ui.res.painterResource(com.cufica.dailyayah.R.drawable.app_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.18f),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 48.dp, end = 20.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedContent(
                targetState = ayah,
                transitionSpec = {
                    val movingToOlderDay = targetState.publishedDateTR < initialState.publishedDateTR
                    if (movingToOlderDay) {
                        (slideInHorizontally(animationSpec = tween(240)) { it / 5 } + fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(200)) { -it / 5 } + fadeOut(tween(150)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(240)) { -it / 5 } + fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(200)) { it / 5 } + fadeOut(tween(150)))
                    }
                },
                label = "daily-ayah-transition"
            ) { displayedAyah ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formattedDate(displayedAyah.publishedDateTR),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Yenile")
                        }
                    }
                    ContentCard(
                        title = "Günün Ayeti",
                        text = displayedAyah.text,
                        reference = displayedAyah.reference,
                        transparent = true,
                        onTafsir = displayedAyah.routeOrNull()?.let { route -> { detail = DetailDestination.Tafsir(route) } },
                        onSurah = displayedAyah.routeOrNull()?.let { route -> { detail = DetailDestination.Surah(route) } }
                    )
                    displayedAyah.hadithText?.takeIf(String::isNotBlank)?.let { text ->
                        ContentCard("Günün Hadisi", text, displayedAyah.hadithReference, transparent = true)
                    }
                    displayedAyah.duaText?.takeIf(String::isNotBlank)?.let { text ->
                        ContentCard("Günün Duası", text, displayedAyah.duaReference, transparent = true)
                    }
                }
            }
            DayNavigation(
                formattedDate = formattedDate(ayah.publishedDateTR),
                canShowNewer = selectedIndex > 0,
                canShowOlder = selectedIndex < items.lastIndex,
                onShowNewer = { selectedIndex -= 1 },
                onShowOlder = { selectedIndex += 1 },
                onShowToday = { selectedIndex = 0 }
            )
        }
    }
}

@Composable
private fun DayNavigation(
    formattedDate: String,
    canShowNewer: Boolean,
    canShowOlder: Boolean,
    onShowNewer: () -> Unit,
    onShowOlder: () -> Unit,
    onShowToday: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedIconButton(onClick = onShowNewer, enabled = canShowNewer) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Daha yeni gün")
            }
            Text(formattedDate, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            OutlinedIconButton(onClick = onShowOlder, enabled = canShowOlder) {
                Icon(Icons.Outlined.ArrowForward, contentDescription = "Daha eski gün")
            }
        }
        if (canShowNewer) {
            Text(
                "Bugüne dön",
                modifier = Modifier.padding(top = 10.dp).clickable(onClick = onShowToday),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ContentCard(
    title: String,
    text: String,
    reference: String?,
    transparent: Boolean = false,
    onTafsir: (() -> Unit)? = null,
    onSurah: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var menuExpanded by remember { mutableStateOf(false) }
    val shareText = listOf(title, text, reference.orEmpty()).filter(String::isNotBlank).joinToString("\n\n")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (transparent) {
            CardDefaults.cardColors(containerColor = Color.Transparent)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onTafsir != null) { onTafsir?.invoke() }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    reference?.takeIf(String::isNotBlank)?.let {
                        Text(it, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Aksiyonlar")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        onTafsir?.let { action ->
                            DropdownMenuItem(text = { Text("Ayet tefsiri") }, leadingIcon = { Icon(Icons.Outlined.MenuBook, null) }, onClick = { menuExpanded = false; action() })
                        }
                        onSurah?.let { action ->
                            DropdownMenuItem(text = { Text("Sure bilgisi") }, leadingIcon = { Icon(Icons.Outlined.Info, null) }, onClick = { menuExpanded = false; action() })
                        }
                        DropdownMenuItem(text = { Text("Kopyala") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { menuExpanded = false; clipboard.setText(AnnotatedString(shareText)) })
                        DropdownMenuItem(
                            text = { Text("Paylaş") },
                            leadingIcon = { Icon(Icons.Outlined.Share, null) },
                            onClick = {
                            menuExpanded = false
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }, "Paylaş"))
                            }
                        )
                    }
                }
            }
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onRefresh) {
            Text("Tekrar dene")
        }
    }
}

data class AyahRoute(val surahNumber: Int, val ayahNumber: Int, val reference: String)

sealed interface DetailDestination {
    data class Tafsir(val route: AyahRoute) : DetailDestination
    data class Surah(val route: AyahRoute) : DetailDestination
}

fun DailyAyah.routeOrNull(): AyahRoute? =
    if (surahNumber != null && ayahNumber != null) AyahRoute(surahNumber, ayahNumber, reference) else null

private fun formattedDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMMM", Locale("tr", "TR")))
}.getOrDefault(value)