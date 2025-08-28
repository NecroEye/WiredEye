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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
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

    // pencere listesini elde tut (tick ile tekrar kullanacağız)
    private val windowCache: StateFlow<List<PacketMeta>> =
        windowStream.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    private data class Controls(val win: Long, val filter: String, val minB: Long)
    private data class Stats(val total: Long, val uniqueApps: Int, val pps: Double)

    private val controls: StateFlow<Controls> =
        combine(windowMillis, filterText, minBytes) { win, filter, minB ->
            Controls(win, filter, minB)
        }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Controls(10_000L, "", 0L))

    private val itemsFlow: StateFlow<List<UiPacket>> =
        combine(windowCache, controls) { list, c ->
            list.filter { it.bytes >= c.minB && matchesFilter(it, c.filter) }
                .asReversed()
                .map { it.toUiPacket() }
        }
            .sample(350)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // UI'yi sürmek için düzenli tick (engine durduğunda pps'in 0'a düşmesi için)
    private fun ticker(periodMs: Long) = flow {
        while (true) {
            emit(Unit)
            delay(periodMs)
        }
    }

    private val isRunningFlow: StateFlow<Boolean> =
        engineState.map { it is EngineState.Running }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val statsFlow: StateFlow<Stats> =
        combine(windowCache, controls, isRunningFlow, ticker(250)) { list, c, _, _ ->
            val nowCutoff = System.currentTimeMillis() - c.win
            val f = list.filter {
                it.timestamp >= nowCutoff && it.bytes >= c.minB && matchesFilter(it, c.filter)
            }
            val pps = if (c.win > 0) f.size / (c.win / 1000.0) else 0.0
            Stats(totalAllTime.value, seenApps.value.size, pps)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats(0L, 0, 0.0))

    val uiState: StateFlow<MonitorUiState> =
        combine(itemsFlow, controls, isRunningFlow, statsFlow) { items, c, running, s ->
            MonitorUiState(
                isEngineRunning = running,
                items = items,
                filterText = c.filter,
                minBytes = c.minB,
                windowMillis = c.win,
                totalBytes = s.total,
                uniqueApps = s.uniqueApps,
                pps = s.pps
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonitorUiState())

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
        val pkg = packageName ?: uid?.let { safePackage(it) } ?: ""
        val label = uid?.let { safeLabel(it) } ?: ""
        val appText = when {
            label.isNotBlank() && pkg.isNotBlank() -> "$label · $pkg (uid:$uidStr)"
            label.isNotBlank() -> "$label (uid:$uidStr)"
            pkg.isNotBlank() -> "$pkg (uid:$uidStr)"
            else -> "uid:$uidStr"
        }
        val local = if (localAddress == "-" || localPort <= 0) "—" else "$localAddress:$localPort"
        val remote = if (remoteAddress == "-" || remotePort <= 0) "—" else "$remoteAddress:$remotePort"
        val transport = protocol.uppercase(Locale.getDefault())
        val port = when {
            remotePort > 0 -> remotePort
            localPort > 0 -> localPort
            else -> 0
        }
        val svc = SERVICE_NAMES[port]
        val protoPretty = if (svc != null && port > 0) "$transport • $svc:$port" else transport
        return UiPacket(
            key = "$timestamp:$uidStr:$bytes:$localPort:$remotePort",
            time = formatTime(timestamp),
            app = appText,
            proto = protoPretty,
            from = local,
            to = remote,
            bytesLabel = humanBytes(bytes),
            raw = this
        )
    }

    private fun appKey(m: PacketMeta): String =
        m.uid?.let { safeLabel(it) }?.takeIf { it.isNotBlank() }
            ?: m.packageName?.takeIf { it.isNotBlank() }
            ?: m.uid?.let { "uid:$it" } ?: ""

    private fun matchesFilter(m: PacketMeta, q: String): Boolean {
        if (q.isBlank()) return true
        val s = q.lowercase(Locale.getDefault())
        val label = m.uid?.let { safeLabel(it) } ?: ""
        val pkg = m.packageName ?: m.uid?.let { safePackage(it) } ?: ""
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

    private fun safeLabel(uid: Int): String? = runCatching { resolver.labelFor(uid) }.getOrNull()
    private fun safePackage(uid: Int): String? = runCatching { resolver.packageFor(uid) }.getOrNull()

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

    companion object {
        private val SERVICE_NAMES = mapOf(
            53 to "DNS", 80 to "HTTP", 123 to "NTP", 143 to "IMAP",
            443 to "HTTPS", 465 to "SMTPS", 587 to "SMTP", 993 to "IMAPS",
            995 to "POP3S", 1935 to "RTMP", 3478 to "STUN", 3479 to "STUN",
            5222 to "XMPP", 5223 to "XMPP-SSL", 5228 to "FCM", 8080 to "HTTP-ALT"
        )
    }
}
