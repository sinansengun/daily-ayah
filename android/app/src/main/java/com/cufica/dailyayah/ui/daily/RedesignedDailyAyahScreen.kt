package com.cufica.dailyayah.ui.daily

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cufica.dailyayah.data.model.DailyAyah
import com.cufica.dailyayah.ui.components.DailyAyahBackground
import com.cufica.dailyayah.ui.components.ErrorState
import com.cufica.dailyayah.ui.components.LoadingState
import com.cufica.dailyayah.ui.components.ScreenHeader
import com.cufica.dailyayah.ui.components.SectionHeading
import com.cufica.dailyayah.ui.theme.DailyAyahTheme
import com.cufica.dailyayah.ui.theme.Newsreader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun RedesignedDailyAyahScreen(
    uiState: DailyAyahUiState,
    onRefresh: () -> Unit,
    onOpenDetail: (DetailDestination) -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    DailyAyahBackground {
        when {
            uiState.isLoading && uiState.ayah == null -> LoadingState("Günün içeriği hazırlanıyor")
            uiState.ayah != null -> RedesignedDailyContent(uiState, onRefresh, onOpenDetail, onOpenSettings)
            else -> ErrorState(uiState.errorMessage.orEmpty(), onRefresh)
        }
    }
}

@Composable
private fun RedesignedDailyContent(
    uiState: DailyAyahUiState,
    onRefresh: () -> Unit,
    onOpenDetail: (DetailDestination) -> Unit,
    onOpenSettings: () -> Unit
) {
    val items = remember(uiState.ayah, uiState.history) {
        listOfNotNull(uiState.ayah)
            .plus(uiState.history.filter { it.publishedDateTR != uiState.ayah?.publishedDateTR })
            .take(15)
    }
    var selectedIndex by rememberSaveable(items.firstOrNull()?.publishedDateTR) { mutableIntStateOf(0) }
    val ayah = items[selectedIndex.coerceIn(0, items.lastIndex)]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ScreenHeader(
            eyebrow = "Günün seçkisi",
            title = "Günün Ayeti",
            subtitle = redesignFormattedDate(ayah.publishedDateTR),
            onSettings = onOpenSettings
        )

        AnimatedContent(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(selectedIndex, items.size) {
                    var horizontalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount -> horizontalDrag += dragAmount },
                        onDragEnd = {
                            when {
                                horizontalDrag <= -80f && selectedIndex < items.lastIndex -> selectedIndex += 1
                                horizontalDrag >= 80f && selectedIndex > 0 -> selectedIndex -= 1
                            }
                            horizontalDrag = 0f
                        },
                        onDragCancel = { horizontalDrag = 0f }
                    )
                },
            targetState = ayah,
            transitionSpec = {
                val older = targetState.publishedDateTR < initialState.publishedDateTR
                val direction = if (older) 1 else -1
                (slideInHorizontally(tween(240)) { direction * it / 5 } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(200)) { -direction * it / 5 } + fadeOut(tween(150)))
            },
            label = "redesigned-daily-ayah"
        ) { displayedAyah ->
            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                AyahReadingSurface(
                    ayah = displayedAyah,
                    onTafsir = displayedAyah.routeOrNull()?.let { route -> { onOpenDetail(DetailDestination.Tafsir(route)) } },
                    onSurah = displayedAyah.routeOrNull()?.let { route -> { onOpenDetail(DetailDestination.Surah(route)) } }
                )
                displayedAyah.hadithText?.takeIf(String::isNotBlank)?.let { text ->
                    ReadingSection("Günün Hadisi", text, displayedAyah.hadithReference)
                }
                displayedAyah.duaText?.takeIf(String::isNotBlank)?.let { text ->
                    ReadingSection("Günün Duası", text, displayedAyah.duaReference)
                }
            }
        }

        RedesignedDayNavigation(
            date = redesignFormattedDate(ayah.publishedDateTR),
            canShowNewer = selectedIndex > 0,
            canShowOlder = selectedIndex < items.lastIndex,
            onShowNewer = { selectedIndex -= 1 },
            onShowOlder = { selectedIndex += 1 },
            onShowToday = { selectedIndex = 0 }
        )
    }
}

@Composable
private fun AyahReadingSurface(
    ayah: DailyAyah,
    onTafsir: (() -> Unit)?,
    onSurah: (() -> Unit)?
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var menuExpanded by remember { mutableStateOf(false) }
    val shareText = listOf("Günün Ayeti", ayah.text, ayah.reference).joinToString("\n\n")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f))
            .clickable(enabled = onTafsir != null) { onTafsir?.invoke() }
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("AYET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(ayah.reference, style = MaterialTheme.typography.titleLarge)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Ayet işlemleri")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    onTafsir?.let { action ->
                        DropdownMenuItem(
                            text = { Text("Ayet tefsiri") },
                            leadingIcon = { Icon(Icons.Outlined.MenuBook, null) },
                            onClick = { menuExpanded = false; action() }
                        )
                    }
                    onSurah?.let { action ->
                        DropdownMenuItem(
                            text = { Text("Sure bilgisi") },
                            leadingIcon = { Icon(Icons.Outlined.Info, null) },
                            onClick = { menuExpanded = false; action() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Kopyala") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                        onClick = { menuExpanded = false; clipboard.setText(AnnotatedString(shareText)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Paylaş") },
                        leadingIcon = { Icon(Icons.Outlined.Share, null) },
                        onClick = {
                            menuExpanded = false
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }, "Paylaş"))
                        }
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
        Text(
            text = ayah.text,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = Newsreader,
                fontWeight = FontWeight.Normal,
                lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * 1.08f
            )
        )
        Text(
            text = "Kaynak: ${ayah.source}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadingSection(title: String, text: String, reference: String?) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(title)
        Text(text, style = MaterialTheme.typography.bodyLarge)
        reference?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun RedesignedDayNavigation(
    date: String,
    canShowNewer: Boolean,
    canShowOlder: Boolean,
    onShowNewer: () -> Unit,
    onShowOlder: () -> Unit,
    onShowToday: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onShowNewer, enabled = canShowNewer) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Daha yeni gün")
            }
            Text(date, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            IconButton(onClick = onShowOlder, enabled = canShowOlder) {
                Icon(Icons.Outlined.ArrowForward, contentDescription = "Daha eski gün")
            }
        }
        if (canShowNewer) {
            Text(
                "Bugüne dön",
                modifier = Modifier.clickable(onClick = onShowToday).padding(12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun redesignFormattedDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale("tr", "TR")))
}.getOrDefault(value)

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun DailyAyahPreview() {
    DailyAyahTheme {
        RedesignedDailyAyahScreen(
            uiState = DailyAyahUiState(
                isLoading = false,
                ayah = DailyAyah(
                    text = "Şüphesiz güçlükle beraber bir kolaylık vardır.",
                    reference = "İnşirah Suresi, 5",
                    surahNumber = 94,
                    ayahNumber = 5,
                    hadithText = "Kolaylaştırınız, güçleştirmeyiniz; müjdeleyiniz, nefret ettirmeyiniz.",
                    hadithReference = "Buhârî, İlim, 11",
                    duaText = "Rabbimiz, bize dünyada da ahirette de iyilik ver.",
                    duaReference = "Bakara Suresi, 201",
                    source = "Diyanet İşleri Başkanlığı",
                    publishedDateTR = "2026-09-21",
                    fetchedAt = "2026-09-21T08:00:00Z",
                    hash = "preview"
                )
            ),
            onRefresh = {},
            onOpenDetail = {},
            onOpenSettings = {}
        )
    }
}