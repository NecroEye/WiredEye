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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
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

    private val pinnedUids = MutableStateFlow<Set<Int>>(emptySet())
    private val mutedUids = MutableStateFlow<Set<Int>>(emptySet())

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

    private data class Controls(val win: Long, val filter: String, val minB: Long)
    private data class Stats(val total: Long, val uniqueApps: Int, val pps: Double, val kbs: Double)

    private val controls: StateFlow<Controls> =
        combine(windowMillis, filterText, minBytes) { win, filter, minB ->
            Controls(win, filter, minB)
        }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Controls(10_000L, "", 0L))

    @OptIn(FlowPreview::class)
    private val itemsFlow: StateFlow<List<UiPacket>> =
        combine(windowStream, controls, pinnedUids, mutedUids) { list, c, pins, mutes ->
            val filtered = list.filter { it.bytes >= c.minB && matchesFilter(it, c.filter) }
                .filter { it.uid == null || !mutes.contains(it.uid!!) }

            val ui = filtered.asReversed().map { it.toUiPacket() }

            ui.sortedWith(
                compareByDescending<UiPacket> { pkt ->
                    val uid = pkt.raw.uid
                    uid != null && pins.contains(uid)
                }.thenByDescending { it.raw.timestamp }
            )
        }
            .sample(350)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val statsFlow: StateFlow<Stats> =
        combine(windowStream, controls) { list, c ->
            val f = list.filter { it.bytes >= c.minB && matchesFilter(it, c.filter) }
            val pps = if (c.win > 0) f.size / (c.win / 1000.0) else 0.0
            val windowTotalBytes = f.sumOf { it.bytes }
            val kbs = if (c.win > 0) (windowTotalBytes.toDouble() / (c.win / 1000.0)) / 1024.0 else 0.0
            Stats(totalAllTime.value, seenApps.value.size, pps, kbs)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats(0L, 0, 0.0, 0.0))

    private val isRunningFlow: StateFlow<Boolean> =
        engineState.map { it is EngineState.Running }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<MonitorUiState> =
        combine(itemsFlow, controls, isRunningFlow, statsFlow, pinnedUids) { items, c, running, s, pins ->
            MonitorUiState(
                isEngineRunning = running,
                items = items,
                filterText = c.filter,
                minBytes = c.minB,
                windowMillis = c.win,
                totalBytes = s.total,
                uniqueApps = s.uniqueApps,
                pps = s.pps,
                throughputKbs = s.kbs,
                pinnedUids = pins
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonitorUiState())

    fun onEvent(ev: MonitorUiEvent) {
        when (ev) {
            is MonitorUiEvent.SetFilter   -> filterText.value = ev.text
            is MonitorUiEvent.SetMinBytes -> minBytes.value = ev.value
            is MonitorUiEvent.SetWindow   -> windowMillis.value = ev.millis
            is MonitorUiEvent.TogglePin   -> togglePin(ev.uid)
            is MonitorUiEvent.ToggleMute  -> toggleMute(ev.uid)
            MonitorUiEvent.ClearFilter    -> filterText.value = ""
            MonitorUiEvent.StartEngine    -> viewModelScope.launch { engine.start() }
            MonitorUiEvent.StopEngine     -> viewModelScope.launch { engine.stop() }
        }
    }

    private fun togglePin(uid: Int) {
        val cur = pinnedUids.value
        pinnedUids.value = if (cur.contains(uid)) cur - uid else cur + uid
    }

    private fun toggleMute(uid: Int) {
        val cur = mutedUids.value
        mutedUids.value = if (cur.contains(uid)) cur - uid else cur + uid
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
            key = "$timestamp:$uidStr:$bytes:$localPort:$remotePort:$localAddress:$remoteAddress",
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
            21 to "FTP", 22 to "SSH", 25 to "SMTP", 53 to "DNS",
            80 to "HTTP", 123 to "NTP", 143 to "IMAP",
            443 to "HTTPS", 465 to "SMTPS", 587 to "SMTP",
            993 to "IMAPS", 995 to "POP3S", 1883 to "MQTT",
            1935 to "RTMP", 3478 to "STUN", 3479 to "STUN",
            5222 to "XMPP", 5223 to "XMPP-SSL", 5228 to "FCM",
            5353 to "mDNS", 8080 to "HTTP-ALT", 8883 to "MQTTS"
        )
    }
}
