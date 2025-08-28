package com.muratcangzm.monitor

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.monitor.common.UiPacket
import com.muratcangzm.monitor.utils.UsageAccess
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiredEye — Realtime Metadata", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    if (state.isEngineRunning) {
                        if (isStopping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp).padding(end = 10.dp),
                                color = accent, strokeWidth = 2.dp
                            )
                        } else {
                            TextButton(onClick = { isStopping = true; vm.onEvent(MonitorUiEvent.StopEngine) }) { Text("Stop") }
                        }
                    } else {
                        TextButton(onClick = {
                            if (!UsageAccess.isGranted(ctx)) {
                                startRequested = true
                                UsageAccess.openSettings(ctx)
                            } else {
                                vm.onEvent(MonitorUiEvent.StartEngine)
                            }
                        }) { Text("Start") }
                    }
                }
            )
        }
    ) { padd ->
        Box(Modifier.fillMaxSize().background(bg).padding(padd)) {
            Column(Modifier.fillMaxSize()) {
                TechStatsBar(
                    state = state,
                    onWindowChange = { vm.onEvent(MonitorUiEvent.SetWindow(it)) }
                )
                Spacer(Modifier.height(8.dp))
                FilterBar(
                    text = state.filterText,
                    minBytes = state.minBytes,
                    onText = { vm.onEvent(MonitorUiEvent.SetFilter(it)) },
                    onClear = { vm.onEvent(MonitorUiEvent.ClearFilter) },
                    onMinBytes = { vm.onEvent(MonitorUiEvent.SetMinBytes(it)) }
                )
                Spacer(Modifier.height(8.dp))
                PacketList(
                    items = state.items,
                    pinnedUids = state.pinnedUids, // <- MonitorUiState içinde Set<Int>
                    onPin = { uid -> vm.onEvent(MonitorUiEvent.TogglePin(uid)) },
                    onShareWindowJson = { shareWindowJson(ctx, state.items) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun TechStatsBar(state: MonitorUiState, onWindowChange: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StatChip("Total", humanBytes(state.totalBytes), Color(0xFF30E3A2))
            StatChip("Apps", state.uniqueApps.toString(), Color(0xFF7BD7FF))
            StatChip("PPS", String.format("%.1f", state.pps), Color(0xFFFFA6E7))
            StatChip("KB/s", String.format("%.1f", state.throughputKbs), Color(0xFFBCE784))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Window:", color = Color(0xFF9EB2C0), modifier = Modifier.alpha(0.9f))
            Segmented(options = listOf("5s" to 5_000L, "10s" to 10_000L, "30s" to 30_000L), selected = state.windowMillis, onSelect = onWindowChange)
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
        Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF0E1624)).padding(3.dp),
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        OutlinedTextField(value = text, onValueChange = onText, modifier = Modifier.fillMaxWidth(), label = { Text("Filter (app/ip/port/protocol)") }, singleLine = true, trailingIcon = { if (text.isNotBlank()) TextButton(onClick = onClear) { Text("Clear") } })
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Min bytes:", color = Color(0xFF9EB2C0), modifier = Modifier.padding(end = 8.dp))
            Slider(value = minBytes.coerceAtLeast(0).coerceAtMost(1024L * 1024L).toFloat(), onValueChange = { onMinBytes(it.toLong()) }, valueRange = 0f..(1024f * 1024f), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(humanBytes(minBytes), color = Color(0xFF7BD7FF))
        }
    }
}

@Composable
private fun PacketList(
    items: List<UiPacket>,
    pinnedUids: Set<Int>,
    onPin: (Int) -> Unit,
    onShareWindowJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Her UID için SADECE en yeni pinned öğeyi al
    val pinnedLatest = remember(items, pinnedUids) {
        val map = HashMap<Int, UiPacket>()
        for (it in items) {
            val uid = it.raw.uid ?: -1
            if (uid in pinnedUids && uid >= 0) {
                val curr = map[uid]
                if (curr == null || it.raw.timestamp > curr.raw.timestamp) map[uid] = it
            }
        }
        map.values.sortedBy { it.app.lowercase(Locale.getDefault()) }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (pinnedLatest.isNotEmpty()) {
            stickyHeader {
                // Sticky: sadece içerik kadar yükseklik; arka plan ver ki alttaki öğeler görünmesin
                Surface(
                    color = Color(0xFF0E141B),
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
                        Text(
                            "Pinned",
                            color = Color(0xFF9EB2C0),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )
                        // Kompakt satırlar: küçük kartlar, wrapContentHeight
                        pinnedLatest.forEach { r ->
                            PinnedRowCompact(
                                row = r,
                                onUnpin = { uid -> onPin(uid) },     // Toggle mantığı ViewModel’de
                                onShareWindowJson = onShareWindowJson
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // Ana akış
        items(items, key = { it.key }) { row ->
            PacketRow(
                row = row,
                onPin = onPin,
                onShareWindowJson = onShareWindowJson,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun PinnedRowCompact(
    row: UiPacket,
    onUnpin: (Int) -> Unit,
    onShareWindowJson: () -> Unit
) {
    val uid = row.raw.uid ?: -1
    val border = Brush.linearGradient(listOf(Color(0xFF314260), Color(0xFF243656), Color(0xFF314260)))

    Surface(
        color = Color(0xFF0F1726),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.app, color = Color(0xFF7BD7FF), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${row.proto} • ${row.bytesLabel}", color = Color(0xFF30E3A2), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onShareWindowJson) { Text("Share") }
                    TextButton(onClick = { if (uid >= 0) onUnpin(uid) }) { Text("Unpin") }
                }
            }
        }
    }
}

@Composable
private fun PacketRow(
    row: UiPacket,
    onPin: (Int) -> Unit,
    onShareWindowJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = Brush.linearGradient(listOf(Color(0xFF1B2B45), Color(0xFF20304F), Color(0xFF1B2B45)))
    val uid = row.raw.uid ?: -1
    val dir = inferDirection(row)

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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val dirLabel = when (dir) { Direction.TX -> "TX"; Direction.RX -> "RX"; Direction.MIX -> "MIX"; null -> "—" }
                val dirColor = when (dir) { Direction.TX -> Color(0xFFFFB86C); Direction.RX -> Color(0xFF7BD7FF); Direction.MIX -> Color(0xFFBCE784); null -> Color(0xFF9EB2C0) }
                Text(row.proto, color = Color(0xFF30E3A2), style = MaterialTheme.typography.bodyMedium)
                Text("•", color = Color(0xFF8EA0B5), style = MaterialTheme.typography.bodyMedium)
                Text(dirLabel, color = dirColor, style = MaterialTheme.typography.bodyMedium)
                Text("•", color = Color(0xFF8EA0B5), style = MaterialTheme.typography.bodyMedium)
                Text(row.bytesLabel, color = Color(0xFF30E3A2), style = MaterialTheme.typography.bodyMedium)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mono("from  ${row.from}")
                Text("→", color = Color(0xFF8EA0B5), style = MaterialTheme.typography.bodySmall)
                Mono("to  ${row.to}")
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if (uid >= 0) onPin(uid) }) { Text("Pin") }
                TextButton(onClick = onShareWindowJson) { Text("Share JSON") }
            }
        }
    }
}

private enum class Direction { TX, RX, MIX }

private fun inferDirection(row: UiPacket): Direction? {
    val m = row.raw
    if (m.protocol.equals("AGG", true)) return Direction.MIX
    val lp = m.localPort
    val rp = m.remotePort
    val localKnown = lp in 1..1024
    val remoteKnown = rp in 1..1024
    return when {
        !localKnown && remoteKnown -> Direction.TX
        localKnown && !remoteKnown -> Direction.RX
        else -> null
    }
}

@Composable
private fun Mono(text: String) {
    Text(text, color = Color(0xFFB8C8D8), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
}

private fun shareWindowJson(ctx: android.content.Context, items: List<UiPacket>) {
    val content = buildJson(items)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "WiredEye Snapshot (JSON)")
        putExtra(Intent.EXTRA_TEXT, content)
    }
    ctx.startActivity(Intent.createChooser(send, "Share snapshot"))
}

private fun buildJson(items: List<UiPacket>): String {
    val sb = StringBuilder()
    sb.append('[')
    items.forEachIndexed { i, r ->
        val m = r.raw
        sb.append("{")
            .append("\"timestamp\":").append(m.timestamp).append(',')
            .append("\"uid\":").append(m.uid ?: -1).append(',')
            .append("\"package\":\"").append((m.packageName ?: "").escapeJson()).append("\",")
            .append("\"protocol\":\"").append(m.protocol.escapeJson()).append("\",")
            .append("\"from\":\"").append(((m.localAddress ?: "") + ":" + m.localPort).escapeJson()).append("\",")
            .append("\"to\":\"").append(((m.remoteAddress ?: "") + ":" + m.remotePort).escapeJson()).append("\",")
            .append("\"bytes\":").append(m.bytes)
            .append("}")
        if (i != items.lastIndex) sb.append(',')
    }
    sb.append(']')
    return sb.toString()
}

private fun String.escapeJson(): String =
    buildString {
        for (c in this@escapeJson) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
    }

private fun humanBytes(b: Long): String {
    val u = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = b.toDouble(); var i = 0
    while (v >= 1024 && i < u.lastIndex) { v /= 1024; i++ }
    return String.format(Locale.getDefault(), "%.1f %s", v, u[i])
}