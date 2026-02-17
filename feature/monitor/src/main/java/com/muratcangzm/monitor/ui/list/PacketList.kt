package com.muratcangzm.monitor.ui.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muratcangzm.monitor.MonitorViewModel
import com.muratcangzm.monitor.model.Direction
import com.muratcangzm.monitor.ui.adapters.UiPacketItem
import com.muratcangzm.monitor.utils.isPrivateV4
import com.muratcangzm.monitor.utils.serviceName
import com.muratcangzm.shared.model.network.UiPacket
import java.util.Locale
import com.muratcangzm.resources.R as Res

@Composable
fun EmptyState(isRunning: Boolean) {
    val hintText = if (isRunning) {
        stringResource(Res.string.empty_hint_running)
    } else {
        stringResource(Res.string.empty_hint_idle)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.empty_title),
            color = Color(0xFF9EB2C0),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = hintText,
            color = Color(0xFF6E8296),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StatChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0x15FFFFFF),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    color.copy(alpha = 0.20f),
                    color.copy(alpha = 0.20f)
                )
            )
        )
    ) {
        Text(
            text = text,
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
            text = text,
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
    val valueColor = Color(0xFFB8C8D8)
    Column(
        modifier = modifier.combinedClickable(onClick = {}, onLongClick = onCopy)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color(0xFF8EA0B5),
                style = MaterialTheme.typography.labelSmall
            )
            TagChip(text = tag)
            if (!service.isNullOrBlank()) TagChip(text = service)
        }
        Text(
            text = value,
            color = valueColor,
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
    val borderBrush = remember {
        Brush.linearGradient(listOf(Color(0xFF314260), Color(0xFF243656), Color(0xFF314260)))
    }
    Surface(
        color = Color(0xFF0F1726),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderBrush),
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
                    Text(
                        text = row.app,
                        color = Color(0xFF7BD7FF),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = "${row.proto} • ${row.bytesLabel}",
                        color = Color(0xFF30E3A2),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onShareWindowJson) { Text(text = stringResource(Res.string.action_share)) }
                    TextButton(onClick = { if (uid >= 0) onUnpin(uid) }) {
                        Text(
                            text = stringResource(
                                Res.string.action_unpin
                            )
                        )
                    }
                }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PacketRow(
    modifier: Modifier = Modifier,
    row: UiPacket,
    highlighted: Boolean,
    onPin: (Int) -> Unit,
    onShareWindowJson: () -> Unit,
    onCopied: (String) -> Unit,
    monitorViewModel: MonitorViewModel,
    maxBytesInWindow: Int,
    onNavigateDetails: () -> Unit,
) {
    val borderBrush = remember {
        Brush.linearGradient(listOf(Color(0xFF1B2B45), Color(0xFF20304F), Color(0xFF1B2B45)))
    }
    val uid = row.raw.uid ?: -1
    val clipboard = LocalClipboardManager.current
    val flash by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0f,
        animationSpec = tween(300),
        label = "anomaly-flash"
    )

    val initialFrom = row.from
    val initialTo = row.to

    val fromDisplay by produceState(initialValue = initialFrom, key1 = row.key) {
        fun parseIp(s: String): String? =
            s.substringBeforeLast(':', s).takeIf { it.isNotBlank() && it != "—" && it != "-" }

        val localIp = parseIp(initialFrom)
        if (localIp != null) {
            val host = monitorViewModel.resolveHost(localIp)
            if (host != null) {
                val port = initialFrom.substringAfterLast(':', missingDelimiterValue = "")
                value = if (port.isNotEmpty()) "$host:$port" else host
            }
        }
    }

    val toDisplay by produceState(initialValue = initialTo, key1 = row.key) {
        fun parseIp(s: String): String? =
            s.substringBeforeLast(':', s).takeIf { it.isNotBlank() && it != "—" && it != "-" }

        val remoteIp = parseIp(initialTo)
        if (remoteIp != null) {
            val host = monitorViewModel.resolveHost(remoteIp)
            if (host != null) {
                val port = initialTo.substringAfterLast(':', missingDelimiterValue = "")
                value = if (port.isNotEmpty()) "$host:$port" else host
            }
        }
    }

    val asnBadgeText by produceState<String?>(initialValue = null, key1 = row.key) {
        fun parseIp(s: String): String? =
            s.substringBeforeLast(':', s).takeIf { it.isNotBlank() && it != "—" && it != "-" }

        val remoteIp = parseIp(initialTo)
        if (remoteIp != null) {
            val info = monitorViewModel.asnCountry(remoteIp)
            if (info != null) value = "AS${info.asn} • ${info.countryCode}"
        }
    }

    val onCopiedState by rememberUpdatedState(newValue = onCopied)

    val direction by remember(row.raw.protocol, row.raw.localPort, row.raw.remotePort) {
        derivedStateOf {
            when {
                row.raw.protocol.equals("AGG", true) -> Direction.MIX
                (row.raw.remotePort in 1..1024) && (row.raw.localPort !in 1..1024) -> Direction.TX
                (row.raw.localPort in 1..1024) && (row.raw.remotePort !in 1..1024) -> Direction.RX
                else -> Direction.MIX
            }
        }
    }
    val directionLabel by remember(direction) {
        derivedStateOf {
            when (direction) {
                Direction.TX -> "TX"; Direction.RX -> "RX"; Direction.MIX -> "MIX"
            }
        }
    }
    val directionColor by remember(direction) {
        derivedStateOf {
            when (direction) {
                Direction.TX -> Color(0xFFFFB86C)
                Direction.RX -> Color(0xFF7BD7FF)
                Direction.MIX -> Color(0xFFBCE784)
            }
        }
    }
    val leftPort = row.raw.localPort
    val rightPort = row.raw.remotePort
    val leftService = serviceName(leftPort)
    val rightService = serviceName(rightPort)
    val leftIsLan by remember(row.raw.localAddress) { derivedStateOf { isPrivateV4(row.raw.localAddress) } }
    val rightIsLan by remember(row.raw.remoteAddress) { derivedStateOf { isPrivateV4(row.raw.remoteAddress) } }
    val shareFraction by remember(row.raw.bytes, maxBytesInWindow) {
        derivedStateOf {
            row.raw.bytes.toFloat() / maxBytesInWindow.toFloat().coerceAtLeast(1f)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .drawWithContent {
                drawContent()
                if (flash > 0f) {
                    val strokeWidth = 2.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFF30E3A2).copy(alpha = 0.45f * flash),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
            .clickable {
                onNavigateDetails()
            },
        color = Color(0xFF0F1726),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, borderBrush),

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
                    Text(
                        text = row.app,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF7BD7FF),
                        maxLines = 3,
                        overflow = TextOverflow.Clip
                    )
                    val uidFormatted = stringResource(Res.string.uid_fmt, row.raw.uid ?: -1)
                    val packageText = row.raw.packageName ?: "—"
                    Text(
                        text = "$packageText  •  $uidFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8EA0B5),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Text(
                    text = row.time,
                    color = Color(0xFF9EB2C0),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatChip(text = row.proto, color = Color(0xFF30E3A2))
                StatChip(text = directionLabel, color = directionColor)
                StatChip(text = row.bytesLabel, color = Color(0xFF30E3A2))
                if (asnBadgeText != null) StatChip(text = asnBadgeText!!, color = Color(0xFF9EB2C0))
            }
            BytesBar(fraction = shareFraction)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                EndpointText(
                    title = stringResource(Res.string.from_label),
                    value = fromDisplay,
                    tag = if (leftIsLan) stringResource(Res.string.tag_lan) else stringResource(Res.string.tag_local),
                    service = leftService,
                    onCopy = {
                        clipboard.setText(AnnotatedString(row.from))
                        onCopiedState("From")
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "→",
                    color = Color(0xFF8EA0B5),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
                EndpointText(
                    title = stringResource(Res.string.to_label),
                    value = toDisplay,
                    tag = if (rightIsLan) stringResource(Res.string.tag_lan) else stringResource(Res.string.tag_internet),
                    service = rightService,
                    onCopy = {
                        clipboard.setText(AnnotatedString(row.to))
                        onCopiedState("To")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if (uid >= 0) onPin(uid) }) { Text(text = stringResource(Res.string.action_pin)) }
                TextButton(onClick = onShareWindowJson) { Text(text = stringResource(Res.string.action_share_json)) }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
fun PacketList(
    modifier: Modifier = Modifier,
    isRunning: Boolean,
    adapterItems: List<UiPacketItem>,
    rawItems: List<UiPacket>,
    pinnedUids: Set<Int>,
    highlightedKeys: Set<String>,
    onPin: (Int) -> Unit,
    onShareWindowJson: () -> Unit,
    onCopied: (String) -> Unit,
    monitorViewModel: MonitorViewModel,
    onNavigateDetails:(uiPacket: UiPacket) -> Unit,
) {
    val listState = rememberLazyListState()
    val isEmpty = adapterItems.isEmpty() && rawItems.isEmpty()
    if (isEmpty) {
        EmptyState(isRunning = isRunning)
        return
    }

    val latestPinnedByUid by remember(rawItems, pinnedUids) {
        derivedStateOf {
            val latestMap = HashMap<Int, UiPacket>()
            for (packet in rawItems) {
                val uid = packet.raw.uid ?: -1
                if (uid in pinnedUids && uid >= 0) {
                    val current = latestMap[uid]
                    if (current == null || packet.raw.timestamp > current.raw.timestamp) {
                        latestMap[uid] = packet
                    }
                }
            }
            latestMap.values.sortedBy { it.app.lowercase(Locale.getDefault()) }
        }
    }

    val windowMaxBytes by remember(rawItems) {
        derivedStateOf {
            rawItems.maxOfOrNull { it.raw.bytes }?.toInt() ?: 1
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (latestPinnedByUid.isNotEmpty()) {
            stickyHeader {
                Surface(
                    color = Color(0xFF0E141B),
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.section_pinned),
                            color = Color(0xFF9EB2C0),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )
                        latestPinnedByUid.forEach { row ->
                            PinnedRowCompact(
                                row = row,
                                onUnpin = { uid -> onPin(uid) },
                                onShareWindowJson = onShareWindowJson
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
        items(items = adapterItems, key = { it.id }, contentType = { "packet" }) { item ->
            PacketRow(
                row = item.model,
                highlighted = highlightedKeys.contains(item.model.key),
                onPin = onPin,
                onShareWindowJson = onShareWindowJson,
                onCopied = onCopied,
                monitorViewModel = monitorViewModel,
                maxBytesInWindow = windowMaxBytes,
                onNavigateDetails = { onNavigateDetails(item.model) },
            )
        }
    }
}