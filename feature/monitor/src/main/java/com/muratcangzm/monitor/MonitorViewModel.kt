package com.muratcangzm.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muratcangzm.data.helper.UidResolver
import com.muratcangzm.data.model.meta.PacketMeta
import com.muratcangzm.data.repo.packetRepo.PacketRepository
import com.muratcangzm.monitor.common.UiPacket
import com.muratcangzm.network.engine.EngineState
import com.muratcangzm.network.engine.PacketCaptureEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class MonitorViewModel(
    private val repo: PacketRepository,
    private val engine: PacketCaptureEngine,
    private val resolver: UidResolver
) : ViewModel() {

    private val filterText = MutableStateFlow("")
    private val minBytes = MutableStateFlow(0L)
    private val windowMillis = MutableStateFlow(10_000L)

    private val engineState: StateFlow<EngineState> = engine.state

    private val totalAllTime = MutableStateFlow(0L)
    private val seenApps = MutableStateFlow(emptySet<String>())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val windowStream: Flow<List<PacketMeta>> =
        windowMillis.flatMapLatest { repo.liveWindow(it) }

    init {
        viewModelScope.launch {
            engine.events.collect { meta ->
                repo.recordPacketMeta(meta)
                totalAllTime.value = totalAllTime.value + meta.bytes
                val key = appKey(meta)
                if (key.isNotBlank() && !seenApps.value.contains(key)) {
                    seenApps.value = seenApps.value + key
                }
            }
        }
    }

    private data class WinCtx(
        val list: List<PacketMeta>,
        val filter: String,
        val minB: Long,
        val win: Long
    )

    private data class MetaCtx(
        val eState: EngineState,
        val totalAll: Long,
        val appsSet: Set<String>
    )

    val uiState: StateFlow<MonitorUiState> =
        combine(
            combine(windowStream, filterText, minBytes, windowMillis) { list, filter, minB, win ->
                WinCtx(list, filter, minB, win)
            },
            combine(engineState, totalAllTime, seenApps) { eState, totalAll, appsSet ->
                MetaCtx(eState, totalAll, appsSet)
            }
        ) { w, m ->
            val filtered = w.list.filter { it.bytes >= w.minB && matchesFilter(it, w.filter) }
            val items = filtered.asReversed().map { it.toUiPacket() }
            val pps = if (w.win > 0) filtered.size / (w.win / 1000.0) else 0.0
            MonitorUiState(
                isEngineRunning = m.eState is EngineState.Running,
                items = items,
                filterText = w.filter,
                minBytes = w.minB,
                windowMillis = w.win,
                totalBytes = m.totalAll,
                uniqueApps = m.appsSet.size,
                pps = pps
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MonitorUiState()
        )

    fun onEvent(ev: MonitorUiEvent) {
        when (ev) {
            is MonitorUiEvent.SetFilter   -> filterText.value = ev.text
            is MonitorUiEvent.SetMinBytes -> minBytes.value = ev.value
            is MonitorUiEvent.SetWindow   -> windowMillis.value = ev.millis
            MonitorUiEvent.ClearFilter    -> filterText.value = ""
            MonitorUiEvent.StartEngine    -> viewModelScope.launch { engine.start() }
            MonitorUiEvent.StopEngine     -> viewModelScope.launch { engine.stop() }
        }
    }

    private fun PacketMeta.toUiPacket(): UiPacket {
        val uidStr = uid?.toString() ?: "?"
        val pkg = packageName ?: (uid?.let { resolver.packageFor(it) }) ?: ""
        val label = uid?.let { resolver.labelFor(it) } ?: ""
        val appText = when {
            label.isNotBlank() && pkg.isNotBlank() -> "$label · $pkg (uid:$uidStr)"
            label.isNotBlank() -> "$label (uid:$uidStr)"
            pkg.isNotBlank() -> "$pkg (uid:$uidStr)"
            else -> "uid:$uidStr"
        }
        val from = "${localAddress}:${localPort}"
        val to = "${remoteAddress}:${remotePort}"
        val bytesLabel = humanBytes(bytes)
        val protoPretty = protocol.uppercase(Locale.getDefault())
        return UiPacket(
            key = "$timestamp:$uidStr:$bytes",
            time = formatTime(timestamp),
            app = appText,
            proto = protoPretty,
            from = from,
            to = to,
            bytesLabel = bytesLabel,
            raw = this
        )
    }

    private fun appKey(m: PacketMeta): String =
        m.uid?.let { resolver.labelFor(it) }?.takeIf { it.isNotBlank() }
            ?: m.packageName?.takeIf { it.isNotBlank() }
            ?: m.uid?.let { "uid:$it" } ?: ""

    private fun matchesFilter(m: PacketMeta, q: String): Boolean {
        if (q.isBlank()) return true
        val s = q.lowercase(Locale.getDefault())
        val label = m.uid?.let { resolver.labelFor(it) } ?: ""
        val pkg = m.packageName ?: m.uid?.let { resolver.packageFor(it) } ?: ""
        val fields = buildString {
            append(label).append(' ')
            append(pkg).append(' ')
            append("uid:").append(m.uid ?: "").append(' ')
            append(m.protocol).append(' ')
            append(m.localAddress ?: "").append(' ')
            append(m.remoteAddress ?: "").append(' ')
            append(m.localPort).append(' ')
            append(m.remotePort)
        }.lowercase(Locale.getDefault())
        return s.split(" ").filter { it.isNotBlank() }.all { term -> fields.contains(term) }
    }

    private fun humanBytes(b: Long): String {
        val u = arrayOf("B","KB","MB","GB","TB")
        var v = b.toDouble(); var i = 0
        while (v >= 1024 && i < u.lastIndex) { v /= 1024; i++ }
        return String.format(Locale.getDefault(), "%.1f %s", v, u[i])
    }

    private fun formatTime(ts: Long): String =
        java.time.Instant.ofEpochMilli(ts)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
            .toString()
}
