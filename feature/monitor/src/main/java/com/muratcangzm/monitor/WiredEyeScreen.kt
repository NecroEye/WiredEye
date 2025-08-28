package com.muratcangzm.monitor

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.monitor.common.UiPacket
import com.muratcangzm.monitor.utils.UsageAccess
import org.koin.androidx.compose.koinViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WiredEyeScreen(vm: MonitorViewModel = koinViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var startRequested by rememberSaveable { mutableStateOf(false) }
    var isStopping by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, startRequested) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && startRequested) {
                if (UsageAccess.isGranted(ctx)) vm.onEvent(MonitorUiEvent.StartEngine)
                startRequested = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(state.isEngineRunning) { if (!state.isEngineRunning) isStopping = false }

    val accent = Color(0xFF7BD7FF)
    val bg = Brush.linearGradient(listOf(Color(0xFF0E141B), Color(0xFF0B1022), Color(0xFF0E141B)))

    val startEnabled = !state.isEngineRunning && state.pps == 0.0
    val startAlpha = if (startEnabled) 1f else 0.35f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiredEye — Realtime Metadata", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    if (state.isEngineRunning) {
                        if (isStopping) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 10.dp),
                                color = accent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            TextButton(onClick = { isStopping = true; vm.onEvent(MonitorUiEvent.StopEngine) }) {
                                Text("Stop", textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        TextButton(
                            onClick = {
                                if (!UsageAccess.isGranted(ctx)) {
                                    startRequested = true
                                    UsageAccess.openSettings(ctx)
                                } else {
                                    vm.onEvent(MonitorUiEvent.StartEngine)
                                }
                            },
                            enabled = startEnabled,
                            modifier = Modifier.alpha(startAlpha)
                        ) {
                            Text("Start", textAlign = TextAlign.Center)
                        }
                    }
                }
            )
        }
    ) { padd ->
        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padd)
        ) {
            Column(Modifier.fillMaxSize()) {
                TechStatsBar(state, onWindowChange = { vm.onEvent(MonitorUiEvent.SetWindow(it)) })
                Spacer(Modifier.height(8.dp))
                FilterBar(
                    text = state.filterText,
                    minBytes = state.minBytes,
                    onText = { vm.onEvent(MonitorUiEvent.SetFilter(it)) },
                    onClear = { vm.onEvent(MonitorUiEvent.ClearFilter) },
                    onMinBytes = { vm.onEvent(MonitorUiEvent.SetMinBytes(it)) }
                )
                Spacer(Modifier.height(8.dp))
                PacketList(state.items, Modifier.weight(1f))
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun TechStatsBar(state: MonitorUiState, onWindowChange: (Long) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StatChip("Total", humanBytes(state.totalBytes), Color(0xFF30E3A2))
            StatChip("Apps", state.uniqueApps.toString(), Color(0xFF7BD7FF))
            StatChip("PPS", String.format("%.1f", state.pps), Color(0xFFFFA6E7))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Window:", color = Color(0xFF9EB2C0), modifier = Modifier.alpha(0.9f))
            Segmented(
                options = listOf("5s" to 5_000L, "10s" to 10_000L, "30s" to 30_000L),
                selected = state.windowMillis,
                onSelect = onWindowChange
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, accent: Color) {
    Surface(tonalElevation = 3.dp, color = Color(0xFF101826), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Color(0xFF9EB2C0))
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
        }
    }
}

@Composable
private fun Segmented(options: List<Pair<String, Long>>, selected: Long, onSelect: (Long) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E1624))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (label, value) ->
            val active = value == selected
            Surface(color = if (active) Color(0xFF1B2B45) else Color.Transparent, shape = RoundedCornerShape(10.dp), onClick = { onSelect(value) }) {
                Text(label, color = if (active) Color(0xFF7BD7FF) else Color(0xFF9EB2C0), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun FilterBar(text: String, minBytes: Long, onText: (String) -> Unit, onClear: () -> Unit, onMinBytes: (Long) -> Unit) {
    Column(Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Filter (app/ip/port/protocol)") },
            singleLine = true,
            trailingIcon = { if (text.isNotBlank()) TextButton(onClick = onClear) { Text("Clear") } }
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Min bytes:", color = Color(0xFF9EB2C0), modifier = Modifier.padding(end = 8.dp))
            Slider(
                value = minBytes.coerceAtLeast(0).coerceAtMost(1024L * 1024L).toFloat(),
                onValueChange = { onMinBytes(it.toLong()) },
                valueRange = 0f..(1024f * 1024f),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(humanBytes(minBytes), color = Color(0xFF7BD7FF))
        }
    }
}

@Composable
private fun PacketList(items: List<UiPacket>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(items, key = { it.key }) { row ->
            PacketRow(row = row, modifier = Modifier)
        }
    }
}

@Composable
private fun PacketRow(row: UiPacket, modifier: Modifier = Modifier) {
    val border = Brush.linearGradient(listOf(Color(0xFF1B2B45), Color(0xFF20304F), Color(0xFF1B2B45)))
    Surface(
        color = Color(0xFF0F1726),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.app, style = MaterialTheme.typography.titleMedium, color = Color(0xFF7BD7FF), maxLines = 3, overflow = TextOverflow.Ellipsis)
                    val uidText = "uid:${row.raw.uid ?: -1}"
                    val pkgText = row.raw.packageName ?: "—"
                    Text("$pkgText  •  $uidText", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8EA0B5), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(row.time, color = Color(0xFF9EB2C0), style = MaterialTheme.typography.labelLarge)
            }
            Text("${row.proto}  •  ${row.bytesLabel}", color = Color(0xFF30E3A2), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mono("from  ${row.from}")
                Text("→", color = Color(0xFF8EA0B5), style = MaterialTheme.typography.bodySmall)
                Mono("to  ${row.to}")
            }
        }
    }
}

@Composable
private fun Mono(text: String) {
    Text(text, color = Color(0xFFB8C8D8), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
}

private fun humanBytes(b: Long): String {
    val u = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = b.toDouble();
    var i = 0
    while (v >= 1024 && i < u.lastIndex) {
        v /= 1024; i++
    }
    return String.format(java.util.Locale.getDefault(), "%.1f %s", v, u[i])
}
