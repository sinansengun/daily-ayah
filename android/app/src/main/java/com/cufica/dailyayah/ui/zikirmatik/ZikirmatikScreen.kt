package com.cufica.dailyayah.ui.zikirmatik

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cufica.dailyayah.R
import com.cufica.dailyayah.data.model.ZikirmatikState

@Composable
fun ZikirmatikRoute(viewModel: ZikirmatikViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    RedesignedZikirmatikScreen(
        state = state,
        profiles = profiles,
        activeProfileId = activeProfileId,
        onIncrement = viewModel::increment,
        onDecrement = viewModel::decrement,
        onReset = viewModel::reset,
        onSave = viewModel::save,
        onSelectProfile = viewModel::selectProfile,
        onDeleteProfile = viewModel::deleteProfile
    )
}

@Composable
private fun ZikirmatikScreen(
    state: ZikirmatikState,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    onSave: (String, Int, Int) -> Unit
) {
    var settingsVisible by remember { mutableStateOf(false) }
    var resetVisible by remember { mutableStateOf(false) }
    val progress = state.countInCurrentGroup.toFloat() / state.target.toFloat()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.app_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.18f),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.name, style = MaterialTheme.typography.headlineSmall)
                    Text("Hedef ${state.target}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.groupCount > 1) Text("Tur ${state.currentGroup} / ${state.groupCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { settingsVisible = true }) { Icon(Icons.Outlined.Settings, "Zikir ayarları") }
            }
            Button(
                onClick = onIncrement,
                modifier = Modifier.size(280.dp),
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 7.dp, strokeCap = StrokeCap.Round)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.count}", fontSize = 72.sp, fontWeight = FontWeight.Light)
                        Text("${state.countInCurrentGroup} / ${state.target}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                FloatingActionButton(onClick = onDecrement, containerColor = MaterialTheme.colorScheme.secondaryContainer) { Icon(Icons.Outlined.Remove, "Geri al") }
                FloatingActionButton(onClick = { resetVisible = true }, containerColor = MaterialTheme.colorScheme.secondaryContainer) { Icon(Icons.Outlined.Refresh, "Sayacı sıfırla") }
            }
        }
    }
    if (settingsVisible) ZikirmatikSettings(state, { settingsVisible = false }, onSave)
    if (resetVisible) AlertDialog(
        onDismissRequest = { resetVisible = false },
        title = { Text("Sayacı sıfırla?") },
        text = { Text("${state.name} sayımı sıfırlanacak.") },
        confirmButton = { Button(onClick = { resetVisible = false; onReset() }) { Text("Sıfırla") } },
        dismissButton = { Button(onClick = { resetVisible = false }) { Text("Vazgeç") } }
    )
}

@Composable
private fun ZikirmatikSettings(state: ZikirmatikState, onDismiss: () -> Unit, onSave: (String, Int, Int) -> Unit) {
    var name by remember { mutableStateOf(state.name) }
    var target by remember { mutableStateOf(state.target.toString()) }
    var groupCount by remember { mutableStateOf(state.groupCount.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zikir ayarları") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Zikir adı") })
                OutlinedTextField(target, { target = it.filter(Char::isDigit) }, label = { Text("Hedef") })
                OutlinedTextField(groupCount, { groupCount = it.filter(Char::isDigit) }, label = { Text("Tur sayısı") })
            }
        },
        confirmButton = { Button(onClick = { onSave(name, target.toIntOrNull() ?: 33, groupCount.toIntOrNull() ?: 1); onDismiss() }) { Text("Kaydet") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Vazgeç") } }
    )
}