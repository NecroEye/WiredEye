package com.muratcangzm.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muratcangzm.data.helper.UidResolver
import com.muratcangzm.data.model.meta.PacketMeta
import com.muratcangzm.data.repo.packetRepo.PacketRepository
import com.muratcangzm.monitor.common.SpeedMode
import com.muratcangzm.monitor.common.UiPacket
import com.muratcangzm.monitor.common.ViewMode
import com.muratcangzm.network.engine.EngineState
import com.muratcangzm.network.engine.PacketCaptureEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class MonitorViewModel(
    private val repo: PacketRepository,
    private val engine: PacketCaptureEngine,
    private val resolver: UidResolver
) : ViewModel() {

    private val filterText = kotlinx.coroutines.flow.MutableStateFlow("")
    private val minBytes = kotlinx.coroutines.flow.MutableStateFlow(0L)
    private val windowMillis = kotlinx.coroutines.flow.MutableStateFlow(10_000L)
    private val clearAfter = kotlinx.coroutines.flow.MutableStateFlow(0L)
    private val speedMode = kotlinx.coroutines.flow.MutableStateFlow(SpeedMode.FAST)
    private val viewMode = kotlinx.coroutines.flow.MutableStateFlow(ViewMode.RAW)
    private val engineState: StateFlow<EngineState> = engine.state
    private val totalAllTime = kotlinx.coroutines.flow.MutableStateFlow(0L)
    private val seenApps = kotlinx.coroutines.flow.MutableStateFlow(emptySet<String>())
    private val pinnedUids = kotlinx.coroutines.flow.MutableStateFlow<Set<Int>>(emptySet())
    private val mutedUids = kotlinx.coroutines.flow.MutableStateFlow<Set<Int>>(emptySet())
    private val labelCache = ConcurrentHashMap<Int, String?>()
    private val packageCache = ConcurrentHashMap<Int, String?>()
    private data class RateEntry(var lastTs: Long, var ema: Double, var lastAlertTs: Long)
    private val rateByHost = ConcurrentHashMap<String, RateEntry>()
    private val seenHostKeys = ConcurrentHashMap<String, Long>()
    private val anomalyHighlights = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
    private val _anomalyEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val anomalyEvents: SharedFlow<String> = _anomalyEvents
    private val RATE_THRESHOLD_BPS = 128 * 1024.0
    private val NEW_HOST_BYTES_MIN = 64 * 1024L
    private val ALERT_DEBOUNCE_MS = 5_000L
    private val HIGHLIGHT_HOLD_MS = 1_600L

    @OptIn(ExperimentalCoroutinesApi::class)
    private val windowStream: Flow<List<PacketMeta>> =
        windowMillis
            .flatMapLatest { repo.liveWindow(it) }
            .buffer(Channel.BUFFERED)
            .conflate()

    init {
        viewModelScope.launch {
            engine.events.collect { meta ->
                repo.recordPacketMeta(meta)
                detectByteSpike(meta)
                detectNewHostSpike(meta)
                totalAllTime.value = totalAllTime.value + meta.bytes
                val key = appKey(meta)
                if (key.isNotBlank() && !seenApps.value.contains(key)) {
                    seenApps.value = seenApps.value + key
                }
            }
        }
    }

    private data class Controls(
        val win: Long,
        val filter: String,
        val minB: Long,
        val terms: List<String>,
        val clearAfter: Long
    )

    private fun tokenize(q: String): List<String> =
        q.lowercase(Locale.getDefault())
            .split(' ', '\t')
            .mapNotNull { t -> val s = t.trim(); if (s.isNotEmpty()) s else null }

    private val controls: StateFlow<Controls> =
        combine(windowMillis, filterText, minBytes, clearAfter) { win, filter, minB, clearTs ->
            Controls(win = win, filter = filter, minB = minB, terms = tokenize(filter), clearAfter = clearTs)
        }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                Controls(10_000L, "", 0L, emptyList(), 0L)
            )

    private val cadenceFlow: StateFlow<Long> =
        speedMode.map { mode ->
            when (mode) {
                SpeedMode.ECO   -> 500L
                SpeedMode.FAST  -> 220L
                SpeedMode.TURBO -> 100L
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 220L)

    private data class Inputs(
        val list: List<PacketMeta>,
        val c: Controls,
        val pins: Set<Int>,
        val mutes: Set<Int>
    )

    private val inputsFlow: Flow<Inputs> =
        combine(windowStream, controls, pinnedUids, mutedUids) { list, c, pins, mutes ->
            Inputs(list, c, pins, mutes)
        }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val itemsFlow: StateFlow<List<UiPacket>> =
        cadenceFlow.flatMapLatest { cadence ->
            inputsFlow.combine(viewMode) { inp, mode ->
                withContext(Dispatchers.Default) {
                    val filtered = inp.list
                        .asSequence()
                        .filter { it.bytes >= inp.c.minB && it.timestamp >= inp.c.clearAfter && matchesFilter(it, inp.c.terms) }
                        .filter { it.uid == null || !inp.mutes.contains(it.uid!!) }
                        .toList()

                    val ui: List<UiPacket> = when (mode) {
                        ViewMode.RAW        -> filtered.asReversed().map { it.toUiPacket() }
                        ViewMode.AGGREGATED -> aggregateToUi(filtered)
                    }

                    ui.sortedWith(
                        compareByDescending<UiPacket> { pkt ->
                            val uid = pkt.raw.uid
                            uid != null && inp.pins.contains(uid)
                        }.thenByDescending { it.raw.timestamp }
                    )
                }
            }
                .distinctUntilChanged()
                .sample(cadence)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private data class Stats(val total: Long, val uniqueApps: Int, val pps: Double, val kbs: Double)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val statsFlow: StateFlow<Stats> =
        cadenceFlow.flatMapLatest { cadence ->
            combine(windowStream, controls) { list, c ->
                withContext(Dispatchers.Default) {
                    val f = list.asSequence()
                        .filter { it.bytes >= c.minB && it.timestamp >= c.clearAfter && matchesFilter(it, c.terms) }
                        .toList()
                    val pps = if (c.win > 0) f.size / (c.win / 1000.0) else 0.0
                    val windowTotalBytes = f.sumOf { it.bytes }
                    val kbs = if (c.win > 0) (windowTotalBytes.toDouble() / (c.win / 1000.0)) / 1024.0 else 0.0
                    Stats(totalAllTime.value, seenApps.value.size, pps, kbs)
                }
            }.sample(cadence)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats(0L, 0, 0.0, 0.0))

    private val isRunningFlow: StateFlow<Boolean> =
        engineState.map { it is EngineState.Running }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val baseUi: Flow<MonitorUiState> =
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
        }

    val uiState: StateFlow<MonitorUiState> =
        baseUi
            .combine(speedMode) { st, sm -> st.copy(speedMode = sm) }
            .combine(viewMode) { st, vm -> st.copy(viewMode = vm) }
            .combine(anomalyHighlights) { st, hi -> st.copy(anomalyKeys = hi) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonitorUiState())

    fun onEvent(ev: MonitorUiEvent) {
        when (ev) {
            is MonitorUiEvent.SetFilter   -> filterText.value = ev.text
            is MonitorUiEvent.SetMinBytes -> minBytes.value = ev.value
            is MonitorUiEvent.SetWindow   -> windowMillis.value = ev.millis
            is MonitorUiEvent.TogglePin   -> togglePin(ev.uid)
            is MonitorUiEvent.ToggleMute  -> toggleMute(ev.uid)
            is MonitorUiEvent.SetSpeed    -> speedMode.value = ev.mode
            is MonitorUiEvent.SetViewMode -> viewMode.value = ev.mode
            MonitorUiEvent.ClearFilter    -> filterText.value = ""
            MonitorUiEvent.StartEngine    -> viewModelScope.launch { engine.start() }
            MonitorUiEvent.StopEngine     -> viewModelScope.launch { engine.stop() }
            MonitorUiEvent.ClearNow -> {
                val now = System.currentTimeMillis()
                clearAfter.value = now
                totalAllTime.value = 0L
                seenApps.value = emptySet()
                anomalyHighlights.value = emptySet()
                rateByHost.clear()
                seenHostKeys.clear()
            }
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

    private fun aggregateToUi(list: List<PacketMeta>): List<UiPacket> {
        data class Agg(var total: Long, var last: PacketMeta)
        val est = min(max(16, list.size / 4), 1_000_000)
        val map = HashMap<String, Agg>(est)
        for (m in list) {
            val key = aggKey(m)
            val a = map[key]
            if (a == null) {
                map[key] = Agg(total = m.bytes, last = m)
            } else {
                a.total += m.bytes
                if (m.timestamp > a.last.timestamp) a.last = m
            }
        }
        val out = ArrayList<UiPacket>(map.size)
        for ((k, a) in map) {
            val last = a.last
            val synthetic = last.copy(bytes = a.total, protocol = "AGG")
            val ui = synthetic.toUiPacket()
            out.add(ui.copy(key = "agg:$k", proto = "AGG", bytesLabel = humanBytes(a.total)))
        }
        return out
    }

    private fun aggKey(m: PacketMeta) =
        "${m.uid ?: -1}|${m.packageName ?: ""}|${m.protocol.uppercase()}|${m.remoteAddress}:${m.remotePort}"

    private fun appKey(m: PacketMeta): String =
        m.uid?.let { safeLabel(it) }?.takeIf { it.isNotBlank() }
            ?: m.packageName?.takeIf { it.isNotBlank() }
            ?: m.uid?.let { "uid:$it" } ?: ""

    private fun matchesFilter(m: PacketMeta, terms: List<String>): Boolean {
        if (terms.isEmpty()) return true
        val label = m.uid?.let { safeLabel(it) } ?: ""
        val pkg = m.packageName ?: m.uid?.let { safePackage(it) } ?: ""
        val haystack = buildString {
            append(label).append(' ')
            append(pkg).append(' ')
            append("uid:").append(m.uid ?: "").append(' ')
            append(m.protocol).append(' ')
            append(m.localAddress ?: "").append(' ')
            append(m.remoteAddress ?: "").append(' ')
            append(m.localPort).append(' ')
            append(m.remotePort)
        }.lowercase(Locale.getDefault())
        for (t in terms) if (!haystack.contains(t)) return false
        return true
    }

    private fun safeLabel(uid: Int): String? {
        labelCache[uid]?.let { return it }
        val v = runCatching { resolver.labelFor(uid) }.getOrNull()
        if (v != null) labelCache.putIfAbsent(uid, v)
        return v
    }

    private fun safePackage(uid: Int): String? {
        packageCache[uid]?.let { return it }
        val v = runCatching { resolver.packageFor(uid) }.getOrNull()
        if (v != null) packageCache.putIfAbsent(uid, v)
        return v
    }

    private fun detectByteSpike(m: PacketMeta) {
        val host = m.remoteAddress ?: return
        if (host == "-" || m.bytes <= 0) return
        val uidKey = m.uid ?: return
        val key = "$uidKey|$host"
        val now = m.timestamp
        val entry = rateByHost.getOrPut(key) { RateEntry(lastTs = 0L, ema = 0.0, lastAlertTs = 0L) }
        if (entry.lastTs > 0L) {
            val dtMs = (now - entry.lastTs).coerceAtLeast(1L)
            val instBps = m.bytes.toDouble() * 1000.0 / dtMs.toDouble()
            val alpha = 0.85
            entry.ema = entry.ema * alpha + instBps * (1.0 - alpha)
            if (entry.ema > RATE_THRESHOLD_BPS && (now - entry.lastAlertTs) > ALERT_DEBOUNCE_MS) {
                entry.lastAlertTs = now
                val msg = "High throughput ${humanBytesPerSec(entry.ema)} to $host"
                flagAnomaly(m, msg)
            }
        }
        entry.lastTs = now
    }

    private fun detectNewHostSpike(m: PacketMeta) {
        val host = m.remoteAddress ?: return
        if (host == "-" || m.bytes < NEW_HOST_BYTES_MIN) return
        val uidKey = m.uid ?: return
        val key = "$uidKey|$host"
        if (seenHostKeys.putIfAbsent(key, m.timestamp) == null) {
            val msg = "New host spike: $host • ${humanBytes(m.bytes)}"
            flagAnomaly(m, msg)
        }
    }

    private fun flagAnomaly(m: PacketMeta, message: String) {
        val uiKey = uiKeyOf(m)
        val cur = anomalyHighlights.value
        if (!cur.contains(uiKey)) {
            anomalyHighlights.value = cur + uiKey
            viewModelScope.launch {
                delay(HIGHLIGHT_HOLD_MS)
                anomalyHighlights.value = anomalyHighlights.value - uiKey
            }
        }
        _anomalyEvents.tryEmit(message)
    }

    private fun uiKeyOf(m: PacketMeta): String {
        val uidStr = m.uid?.toString() ?: "?"
        return "${m.timestamp}:${uidStr}:${m.bytes}:${m.localPort}:${m.remotePort}:${m.localAddress}:${m.remoteAddress}"
    }

    private fun humanBytesPerSec(bps: Double): String {
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        var v = bps
        var i = 0
        while (v >= 1024.0 && i < units.lastIndex) { v /= 1024.0; i++ }
        return String.format(java.util.Locale.getDefault(), "%.1f %s", v, units[i])
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