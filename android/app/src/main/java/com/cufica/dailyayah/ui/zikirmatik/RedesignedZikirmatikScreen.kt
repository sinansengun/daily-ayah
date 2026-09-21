package com.cufica.dailyayah.ui.zikirmatik

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cufica.dailyayah.data.model.ZikirmatikState
import com.cufica.dailyayah.data.model.ZikirProfile
import com.cufica.dailyayah.ui.components.DailyAyahBackground
import com.cufica.dailyayah.ui.components.ScreenHeader
import com.cufica.dailyayah.ui.theme.DailyAyahTheme

@Composable
internal fun RedesignedZikirmatikScreen(
    state: ZikirmatikState,
    profiles: List<ZikirProfile>,
    activeProfileId: String?,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    onSave: (String, Int, Int) -> Unit,
    onSelectProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    var settingsVisible by remember { mutableStateOf(false) }
    var resetVisible by remember { mutableStateOf(false) }
    val target = state.target.coerceAtLeast(1)
    val progress = (state.countInCurrentGroup.toFloat() / target).coerceIn(0f, 1f)

    DailyAyahBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                ScreenHeader(
                    eyebrow = "Odak",
                    title = state.name,
                    subtitle = if (state.groupCount > 1) {
                        "Hedef $target · Tur ${state.currentGroup} / ${state.groupCount}"
                    } else {
                        "Hedef $target"
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { settingsVisible = true }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Zikir ayarları")
                }
            }

            Button(
                onClick = onIncrement,
                modifier = Modifier
                    .size(276.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "$state.name sayacı. Toplam ${state.count}. Artırmak için dokun."
                    },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(progress, 0f..1f) },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface,
                        strokeWidth = 9.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.count.toString(),
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${state.countInCurrentGroup} / $target",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Saymak için büyük alana dokun",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    FilledTonalIconButton(onClick = onDecrement, modifier = Modifier.size(54.dp)) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Bir azalt")
                    }
                    FilledTonalIconButton(onClick = { resetVisible = true }, modifier = Modifier.size(54.dp)) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Sayacı sıfırla")
                    }
                }
            }
        }
    }

    if (settingsVisible) {
        ZikirProfilesDialog(
            profiles = profiles,
            activeProfileId = activeProfileId,
            onDismiss = { settingsVisible = false },
            onSave = onSave,
            onSelect = onSelectProfile,
            onDelete = onDeleteProfile
        )
    }
    if (resetVisible) {
        AlertDialog(
            onDismissRequest = { resetVisible = false },
            title = { Text("Sayacı sıfırla?") },
            text = { Text("${state.name} sayımı sıfırlanacak. Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(onClick = { resetVisible = false; onReset() }) { Text("Sıfırla") }
            },
            dismissButton = {
                OutlinedButton(onClick = { resetVisible = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun ZikirProfilesDialog(
    profiles: List<ZikirProfile>,
    activeProfileId: String?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int) -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("33") }
    var groupCount by remember { mutableStateOf("1") }
    val valid = name.isNotBlank() && (target.toIntOrNull() ?: 0) > 0 && (groupCount.toIntOrNull() ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) "Yeni zikir" else "Zikirlerim") },
        text = {
            if (creating) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Zikir adı") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.filter(Char::isDigit) },
                        label = { Text("Tur hedefi") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = groupCount,
                        onValueChange = { groupCount = it.filter(Char::isDigit) },
                        label = { Text("Tur sayısı") },
                        singleLine = true
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    profiles.forEachIndexed { index, profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(profile.id)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Hedef ${profile.target} · ${profile.groupCount} tur · ${profile.count} sayım",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (profile.id == activeProfileId) {
                                Icon(Icons.Outlined.Check, contentDescription = "Aktif zikir", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { onDelete(profile.id) },
                                enabled = profiles.size > 1
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "${profile.name} zikrini sil")
                            }
                        }
                        if (index < profiles.lastIndex) HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                Button(
                    enabled = valid,
                    onClick = {
                        onSave(name.trim(), target.toInt(), groupCount.toInt())
                        onDismiss()
                    }
                ) { Text("Kaydet") }
            } else {
                Button(onClick = { creating = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("Yeni zikir", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { if (creating) creating = false else onDismiss() }) {
                Text(if (creating) "Geri" else "Kapat")
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 780)
@Composable
private fun RedesignedZikirmatikPreview() {
    DailyAyahTheme {
        RedesignedZikirmatikScreen(
            state = ZikirmatikState(name = "Subhanallah", target = 33, groupCount = 3, count = 47),
            profiles = listOf(ZikirProfile("preview", "Subhanallah", 33, 3, 47)),
            activeProfileId = "preview",
            onIncrement = {},
            onDecrement = {},
            onReset = {},
            onSave = { _, _, _ -> },
            onSelectProfile = {},
            onDeleteProfile = {}
        )
    }
}