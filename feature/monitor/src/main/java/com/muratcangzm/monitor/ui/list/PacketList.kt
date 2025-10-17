package com.muratcangzm.monitor.ui.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muratcangzm.monitor.MonitorViewModel
import com.muratcangzm.monitor.common.UiPacket
import com.muratcangzm.monitor.model.Direction
import com.muratcangzm.monitor.ui.adapters.UiPacketItem
import com.muratcangzm.monitor.utils.isPrivateV4
import com.muratcangzm.monitor.utils.serviceName
import java.util.Locale

@Composable
fun EmptyState(isRunning: Boolean) {
    val hint = if (isRunning) "Listening for packets…" else "Tap Start to begin capture"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("No packets yet", color = Color(0xFF9EB2C0), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(hint, color = Color(0xFF6E8296), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StatChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x15FFFFFF),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f))
    ) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun BytesBar(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF121A2A))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(Color(0xFF2CD4C5), Color(0xFF7BD7FF))))
        )
    }
}

@Composable
fun TagChip(text: String) {
    Surface(shape = RoundedCornerShape(6.dp), color = Color(0x2230E3A2)) {
        Text(
            text,
            color = Color(0xFF30E3A2),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EndpointText(
    title: String,
    value: String,
    tag: String,
    service: String?,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = Color(0xFFB8C8D8)
    Column(
        modifier = modifier.combinedClickable(onClick = {}, onLongClick = onCopy)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color(0xFF8EA0B5), style = MaterialTheme.typography.labelSmall)
            TagChip(tag)
            if (!service.isNullOrBlank()) TagChip(service)
        }
        Text(
            value,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun PinnedRowCompact(
    row: UiPacket,
    onUnpin: (Int) -> Unit,
    onShareWindowJson: () -> Unit
) {
    val uid = row.raw.uid ?: -1
    val border = remember { Brush.linearGradient(listOf(Color(0xFF314260), Color(0xFF243656), Color(0xFF314260))) }
    Surface(
        color = Color(0xFF0F1726),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(row.app, color = Color(0xFF7BD7FF), style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Clip)
                    Text("${row.proto} • ${row.bytesLabel}", color = Color(0xFF30E3A2), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Clip)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onShareWindowJson) { Text("Share") }
                    TextButton(onClick = { if (uid >= 0) onUnpin(uid) }) { Text("Unpin") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PacketRow(
    row: UiPacket,
    highlighted: Boolean,
    onPin: (Int) -> Unit,
    onShareWindowJson: () -> Unit,
    onCopied: (String) -> Unit,
    vm: MonitorViewModel,
    maxBytesInWindow: Int,
    modifier: Modifier = Modifier
) {
    val border = remember { Brush.linearGradient(listOf(Color(0xFF1B2B45), Color(0xFF20304F), Color(0xFF1B2B45))) }
    val uid = row.raw.uid ?: -1
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val flash by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0f,
        animationSpec = tween(300),
        label = "anomaly-flash"
    )
    val fromInit = row.from
    val toInit = row.to
    var fromDisp by remember(row.key) { mutableStateOf(fromInit) }
    var toDisp by remember(row.key) { mutableStateOf(toInit) }
    var asnText by remember(row.key) { mutableStateOf<String?>(null) }
    LaunchedEffect(row.key) {
        fun parseIp(s: String): String? =
            s.substringBeforeLast(':', s).takeIf { it.isNotBlank() && it != "—" && it != "-" }
        val remoteIp = parseIp(toInit)
        val localIp = parseIp(fromInit)
        if (remoteIp != null) {
            vm.resolveHost(remoteIp)?.let { host ->
                val port = toInit.substringAfterLast(':', missingDelimiterValue = "")
                toDisp = if (port.isNotEmpty()) "$host:$port" else host
            }
            vm.asnCountry(remoteIp)?.let { info ->
                asnText = "AS${info.asn} • ${info.cc}"
            }
        }
        if (localIp != null) {
            vm.resolveHost(localIp)?.let { host ->
                val port = fromInit.substringAfterLast(':', missingDelimiterValue = "")
                fromDisp = if (port.isNotEmpty()) "$host:$port" else host
            }
        }
    }
    val dir = when {
        row.raw.protocol.equals("AGG", true) -> Direction.MIX
        (row.raw.remotePort in 1..1024) && (row.raw.localPort !in 1..1024) -> Direction.TX
        (row.raw.localPort in 1..1024) && (row.raw.remotePort !in 1..1024) -> Direction.RX
        else -> Direction.MIX
    }
    val dirLabel = when (dir) { Direction.TX -> "TX"; Direction.RX -> "RX"; Direction.MIX -> "MIX" }
    val dirColor = when (dir) { Direction.TX -> Color(0xFFFFB86C); Direction.RX -> Color(0xFF7BD7FF); Direction.MIX -> Color(0xFFBCE784) }
    val lPort = row.raw.localPort
    val rPort = row.raw.remotePort
    val lSvc = serviceName(lPort)
    val rSvc = serviceName(rPort)
    val leftIsLan = isPrivateV4(row.raw.localAddress ?: "")
    val rightIsLan = isPrivateV4(row.raw.remoteAddress ?: "")
    val share = remember(row.raw.bytes, maxBytesInWindow) { row.raw.bytes.toFloat() / maxBytesInWindow.toFloat().coerceAtLeast(1f) }
    Surface(
        color = Color(0xFF0F1726),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                if (flash > 0f) {
                    val stroke = 2.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFF30E3A2).copy(alpha = 0.45f * flash),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(width = stroke)
                    )
                }
            }
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(row.app, style = MaterialTheme.typography.titleMedium, color = Color(0xFF7BD7FF), maxLines = 3, overflow = TextOverflow.Clip)
                    val uidText = "uid:${row.raw.uid ?: -1}"
                    val pkgText = row.raw.packageName ?: "—"
                    Text("$pkgText  •  $uidText", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8EA0B5), maxLines = 1, overflow = TextOverflow.Clip)
                }
                Text(row.time, color = Color(0xFF9EB2C0), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                StatChip(text = row.proto, color = Color(0xFF30E3A2))
                StatChip(text = dirLabel, color = dirColor)
                StatChip(text = row.bytesLabel, color = Color(0xFF30E3A2))
                if (asnText != null) StatChip(text = asnText!!, color = Color(0xFF9EB2C0))
            }
            BytesBar(fraction = share)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                EndpointText(
                    title = "from",
                    value = fromDisp,
                    tag = if (leftIsLan) "LAN" else "LOCAL",
                    service = lSvc,
                    onCopy = {
                        clipboard.setText(AnnotatedString(row.from))
                        onCopied("From")
                    },
                    modifier = Modifier.weight(1f)
                )
                Text("→", color = Color(0xFF8EA0B5), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                EndpointText(
                    title = "to",
                    value = toDisp,
                    tag = if (rightIsLan) "LAN" else "INTERNET",
                    service = rSvc,
                    onCopy = {
                        clipboard.setText(AnnotatedString(row.to))
                        onCopied("To")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if (uid >= 0) onPin(uid) }) { Text("Pin") }
                TextButton(onClick = onShareWindowJson) { Text("Share JSON") }
            }
        }
    }
}

@Composable
fun PacketList(
    isRunning: Boolean,
    adapterItems: List<UiPacketItem>,
    rawItems: List<UiPacket>,
    pinnedUids: Set<Int>,
    highlightedKeys: Set<String>,
    onPin: (Int) -> Unit,
    onShareWindowJson: () -> Unit,
    onCopied: (String) -> Unit,
    vm: MonitorViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isEmpty = adapterItems.isEmpty() && rawItems.isEmpty()
    if (isEmpty) {
        EmptyState(isRunning = isRunning)
        return
    }
    val pinnedLatest = remember(rawItems, pinnedUids) {
        val map = HashMap<Int, UiPacket>()
        for (it in rawItems) {
            val uid = it.raw.uid ?: -1
            if (uid in pinnedUids && uid >= 0) {
                val curr = map[uid]
                if (curr == null || it.raw.timestamp > curr.raw.timestamp) map[uid] = it
            }
        }
        map.values.sortedBy { it.app.lowercase(Locale.getDefault()) }
    }
    val windowMaxBytes = remember(rawItems) { rawItems.maxOfOrNull { it.raw.bytes }?.toInt() ?: 1 }
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (pinnedLatest.isNotEmpty()) {
            stickyHeader {
                Surface(color = Color(0xFF0E141B), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 6.dp)
                    ) {
                        Text(
                            "Pinned",
                            color = Color(0xFF9EB2C0),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )
                        pinnedLatest.forEach { r ->
                            PinnedRowCompact(row = r, onUnpin = { uid -> onPin(uid) }, onShareWindowJson = onShareWindowJson)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
        items(items = adapterItems, key = { it.id }) { item ->
            PacketRow(
                row = item.model,
                highlighted = highlightedKeys.contains(item.model.key),
                onPin = onPin,
                onShareWindowJson = onShareWindowJson,
                onCopied = onCopied,
                vm = vm,
                maxBytesInWindow = windowMaxBytes
            )
        }
    }
}