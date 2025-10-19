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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val packetRepository: PacketRepository,
    private val captureEngine: PacketCaptureEngine,
    private val uidResolver: UidResolver
) : ViewModel() {

    private val filterQuery = MutableStateFlow("")
    private val minimumBytes = MutableStateFlow(0L)
    private val windowDurationMillis = MutableStateFlow(10_000L)
    private val clearThresholdMillis = MutableStateFlow(0L)
    private val speed = MutableStateFlow(SpeedMode.FAST)
    private val view = MutableStateFlow(ViewMode.RAW)
    private val engineState: StateFlow<EngineState> = captureEngine.state
    private val totalBytesAllTime = MutableStateFlow(0L)
    private val pinnedUidSet = MutableStateFlow<Set<Int>>(emptySet())
    private val mutedUidSet = MutableStateFlow<Set<Int>>(emptySet())

    private val labelByUid = ConcurrentHashMap<Int, String?>()
    private val packageByUid = ConcurrentHashMap<Int, String?>()

    private data class RateSample(var lastTimestamp: Long, var exponentialMovingAverage: Double, var lastAlertTimestamp: Long)
    private val rateByUidAndHost = ConcurrentHashMap<String, RateSample>()
    private val seenUidHostKeys = ConcurrentHashMap<String, Long>()

    private val anomalyKeys = MutableStateFlow<Set<String>>(emptySet())
    private val _anomalyEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val anomalyEvents: SharedFlow<String> = _anomalyEvents

    private val rateThresholdBytesPerSecond = 128 * 1024.0
    private val newHostBytesMinimum = 64 * 1024L
    private val alertDebounceMillis = 5_000L
    private val highlightHoldMillis = 1_600L

    @OptIn(ExperimentalCoroutinesApi::class)
    private val windowPackets: StateFlow<List<PacketMeta>> =
        windowDurationMillis
            .flatMapLatest { packetRepository.liveWindow(it) }
            .buffer(Channel.BUFFERED)
            .conflate()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            captureEngine.events.collect { meta ->
                packetRepository.recordPacketMeta(meta)
                detectHighThroughput(meta)
                detectNewHost(meta)
                totalBytesAllTime.value = totalBytesAllTime.value + meta.bytes
            }
        }
    }

    private data class Controls(
        val windowMillis: Long,
        val query: String,
        val minBytes: Long,
        val terms: List<String>,
        val clearAfterMillis: Long
    )

    private fun tokenize(query: String): List<String> =
        query.lowercase(Locale.getDefault())
            .split(' ', '\t')
            .mapNotNull { token -> token.trim().ifEmpty { null } }

    private val controls: StateFlow<Controls> =
        combine(windowDurationMillis, filterQuery, minimumBytes, clearThresholdMillis) { win, q, minB, clearTs ->
            Controls(windowMillis = win, query = q, minBytes = minB, terms = tokenize(q), clearAfterMillis = clearTs)
        }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Controls(10_000L, "", 0L, emptyList(), 0L))

    private val cadenceMillis: StateFlow<Long> =
        speed.map {
            when (it) {
                SpeedMode.ECO -> 500L
                SpeedMode.FAST -> 220L
                SpeedMode.TURBO -> 100L
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 220L)

    private data class Inputs(
        val packets: List<PacketMeta>,
        val controls: Controls,
        val pinned: Set<Int>,
        val muted: Set<Int>
    )

    private val inputs: StateFlow<Inputs> =
        combine(windowPackets, controls, pinnedUidSet, mutedUidSet) { list, c, pins, mutes ->
            Inputs(list, c, pins, mutes)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Inputs(emptyList(), controls.value, emptySet(), emptySet()))

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val uiPackets: StateFlow<List<UiPacket>> =
        cadenceMillis.flatMapLatest { cadence ->
            inputs.combine(view) { input, selectedView ->
                withContext(Dispatchers.Default) {
                    val filtered = input.packets
                        .asSequence()
                        .filter { it.bytes >= input.controls.minBytes }
                        .filter { it.timestamp >= input.controls.clearAfterMillis }
                        .filter { matchesFilter(it, input.controls.terms) }
                        .filter { it.uid == null || !input.muted.contains(it.uid!!) }
                        .toList()

                    val mapped = when (selectedView) {
                        ViewMode.RAW -> filtered.asReversed().map { it.toUiPacket() }
                        ViewMode.AGGREGATED -> aggregateToUi(filtered)
                    }

                    mapped.sortedWith(
                        compareByDescending<UiPacket> { packet ->
                            val uid = packet.raw.uid
                            uid != null && input.pinned.contains(uid)
                        }.thenByDescending { it.raw.timestamp }
                    )
                }
            }.distinctUntilChanged().sample(cadence)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private data class Stats(val totalAllTimeBytes: Long, val uniqueAppCount: Int, val packetsPerSecond: Double, val kilobytesPerSecond: Double)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val stats: StateFlow<Stats> =
        cadenceMillis.flatMapLatest { cadence ->
            combine(windowPackets, controls) { list, c ->
                withContext(Dispatchers.Default) {
                    val filtered = list.asSequence()
                        .filter { it.bytes >= c.minBytes }
                        .filter { it.timestamp >= c.clearAfterMillis }
                        .filter { matchesFilter(it, c.terms) }
                        .toList()

                    val pps = if (c.windowMillis > 0) filtered.size / (c.windowMillis / 1000.0) else 0.0
                    val sumBytes = filtered.sumOf { it.bytes }
                    val kbs = if (c.windowMillis > 0) (sumBytes.toDouble() / (c.windowMillis / 1000.0)) / 1024.0 else 0.0

                    val uniqueKeys = HashSet<String>()
                    for (packet in filtered) {
                        val key = packet.packageName ?: "uid:${packet.uid ?: -1}"
                        uniqueKeys.add(key)
                    }
                    Stats(totalBytesAllTime.value, uniqueKeys.size, pps, kbs)
                }
            }.sample(cadence)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats(0L, 0, 0.0, 0.0))

    private val isRunning: StateFlow<Boolean> =
        engineState.map { it is EngineState.Running }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val baseUi: StateFlow<MonitorUiState> =
        combine(uiPackets, controls, isRunning, stats, pinnedUidSet) { items, c, running, s, pins ->
            MonitorUiState(
                isEngineRunning = running,
                items = items,
                filterText = c.query,
                minBytes = c.minBytes,
                windowMillis = c.windowMillis,
                totalBytes = s.totalAllTimeBytes,
                uniqueApps = s.uniqueAppCount,
                pps = s.packetsPerSecond,
                throughputKbs = s.kilobytesPerSecond,
                pinnedUids = pins
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonitorUiState())

    val uiState: StateFlow<MonitorUiState> =
        baseUi
            .combine(speed) { state, mode -> state.copy(speedMode = mode) }
            .combine(view) { state, selected -> state.copy(viewMode = selected) }
            .combine(anomalyKeys) { state, keys -> state.copy(anomalyKeys = keys) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonitorUiState())

    fun onEvent(event: MonitorUiEvent) {
        when (event) {
            is MonitorUiEvent.SetFilter -> filterQuery.value = event.text
            is MonitorUiEvent.SetMinBytes -> minimumBytes.value = event.value
            is MonitorUiEvent.SetWindow -> windowDurationMillis.value = event.millis
            is MonitorUiEvent.TogglePin -> togglePin(event.uid)
            is MonitorUiEvent.ToggleMute -> toggleMute(event.uid)
            is MonitorUiEvent.SetSpeed -> speed.value = event.mode
            is MonitorUiEvent.SetViewMode -> view.value = event.mode
            MonitorUiEvent.ClearFilter -> filterQuery.value = ""
            MonitorUiEvent.StartEngine -> viewModelScope.launch { captureEngine.start() }
            MonitorUiEvent.StopEngine -> viewModelScope.launch { captureEngine.stop() }
            MonitorUiEvent.ClearNow -> clearNow()
        }
    }

    private fun clearNow() {
        val now = System.currentTimeMillis()
        clearThresholdMillis.value = now
        totalBytesAllTime.value = 0L
        anomalyKeys.value = emptySet()
        rateByUidAndHost.clear()
        seenUidHostKeys.clear()
    }

    private fun togglePin(uid: Int) {
        val current = pinnedUidSet.value
        pinnedUidSet.value = if (current.contains(uid)) current - uid else current + uid
    }

    private fun toggleMute(uid: Int) {
        val current = mutedUidSet.value
        mutedUidSet.value = if (current.contains(uid)) current - uid else current + uid
    }

    private fun PacketMeta.toUiPacket(): UiPacket {
        val uidText = uid?.toString() ?: "?"
        val packageNameOrResolved = packageName ?: uid?.let { safePackage(it) } ?: ""
        val labelOrResolved = uid?.let { safeLabel(it) } ?: ""
        val appText = when {
            labelOrResolved.isNotBlank() && packageNameOrResolved.isNotBlank() -> "$labelOrResolved · $packageNameOrResolved (uid:$uidText)"
            labelOrResolved.isNotBlank() -> "$labelOrResolved (uid:$uidText)"
            packageNameOrResolved.isNotBlank() -> "$packageNameOrResolved (uid:$uidText)"
            else -> "uid:$uidText"
        }
        val local = if (localAddress == "-" || localPort <= 0) "—" else "$localAddress:$localPort"
        val remote = if (remoteAddress == "-" || remotePort <= 0) "—" else "$remoteAddress:$remotePort"
        val transport = protocol.uppercase(Locale.getDefault())
        val port = when {
            remotePort > 0 -> remotePort
            localPort > 0 -> localPort
            else -> 0
        }
        val service = SERVICE_NAMES[port]
        val protocolPretty = if (service != null && port > 0) "$transport • $service:$port" else transport
        return UiPacket(
            key = "$timestamp:$uidText:$bytes:$localPort:$remotePort:$localAddress:$remoteAddress",
            time = formatTime(timestamp),
            app = appText,
            proto = protocolPretty,
            from = local,
            to = remote,
            bytesLabel = humanBytes(bytes),
            raw = this
        )
    }

    private fun aggregateToUi(list: List<PacketMeta>): List<UiPacket> {
        data class Aggregate(var totalBytes: Long, var latest: PacketMeta)
        val capacity = min(max(16, list.size / 4), 1_000_000)
        val map = HashMap<String, Aggregate>(capacity)
        for (meta in list) {
            val key = aggregateKey(meta)
            val aggregate = map[key]
            if (aggregate == null) {
                map[key] = Aggregate(totalBytes = meta.bytes, latest = meta)
            } else {
                aggregate.totalBytes += meta.bytes
                if (meta.timestamp > aggregate.latest.timestamp) aggregate.latest = meta
            }
        }
        val output = ArrayList<UiPacket>(map.size)
        for ((key, aggregate) in map) {
            val last = aggregate.latest
            val synthetic = last.copy(bytes = aggregate.totalBytes, protocol = "AGG")
            val ui = synthetic.toUiPacket()
            output.add(ui.copy(key = "agg:$key", proto = "AGG", bytesLabel = humanBytes(aggregate.totalBytes)))
        }
        return output
    }

    private fun aggregateKey(meta: PacketMeta): String =
        "${meta.uid ?: -1}|${meta.packageName ?: ""}|${meta.protocol.uppercase()}|${meta.remoteAddress}:${meta.remotePort}"

    private fun matchesFilter(meta: PacketMeta, terms: List<String>): Boolean {
        if (terms.isEmpty()) return true
        val label = meta.uid?.let { safeLabel(it) } ?: ""
        val pkg = meta.packageName ?: meta.uid?.let { safePackage(it) } ?: ""
        val haystack = buildString {
            append(label).append(' ')
            append(pkg).append(' ')
            append("uid:").append(meta.uid ?: "").append(' ')
            append(meta.protocol).append(' ')
            append(meta.localAddress ?: "").append(' ')
            append(meta.remoteAddress ?: "").append(' ')
            append(meta.localPort).append(' ')
            append(meta.remotePort)
        }.lowercase(Locale.getDefault())
        return terms.all { haystack.contains(it) }
    }

    private fun safeLabel(uid: Int): String? {
        labelByUid[uid]?.let { return it }
        val value = runCatching { uidResolver.labelFor(uid) }.getOrNull()
        if (value != null) labelByUid.putIfAbsent(uid, value)
        return value
    }

    private fun safePackage(uid: Int): String? {
        packageByUid[uid]?.let { return it }
        val value = runCatching { uidResolver.packageFor(uid) }.getOrNull()
        if (value != null) packageByUid.putIfAbsent(uid, value)
        return value
    }

    private val dnsLock = Any()
    private val dnsCache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 512
    }

    data class AsnInfo(val asn: Int, val countryCode: String, val organization: String)
    private data class Cidr(val base: String, val prefix: Int, val info: AsnInfo)

    private val asnTable: List<Cidr> = listOf(
        Cidr("8.8.8.0", 24, AsnInfo(15169, "US", "Google")),
        Cidr("1.1.1.0", 24, AsnInfo(13335, "US", "Cloudflare")),
        Cidr("52.0.0.0", 8, AsnInfo(16509, "US", "AWS")),
        Cidr("151.101.0.0", 16, AsnInfo(54113, "US", "Fastly")),
        Cidr("23.246.0.0", 16, AsnInfo(15133, "US", "Akamai"))
    )

    suspend fun resolveHost(ip: String): String? = withContext(Dispatchers.IO) {
        synchronized(dnsLock) { dnsCache[ip] }?.let { return@withContext it }
        if (!ipPattern.matches(ip)) return@withContext null
        val host = runCatching {
            val addr = java.net.InetAddress.getByName(ip)
            val name = addr.canonicalHostName ?: addr.hostName
            if (name != ip) name else null
        }.getOrNull()
        if (host != null) synchronized(dnsLock) { dnsCache[ip] = host }
        host
    }

    fun asnCountry(ip: String): AsnInfo? {
        if (!ipv4Pattern.matches(ip)) return null
        val x = ipv4ToInt(ip)
        return asnTable.firstOrNull { ipv4InCidr(x, it) }?.info
    }

    private fun ipv4ToInt(ip: String): Int {
        val p = ip.split('.').map { it.toInt() }
        return (p[0] shl 24) or (p[1] shl 16) or (p[2] shl 8) or p[3]
    }

    private fun ipv4InCidr(addr: Int, c: Cidr): Boolean {
        val base = ipv4ToInt(c.base)
        val mask = if (c.prefix == 0) 0 else -1 shl (32 - c.prefix)
        return (addr and mask) == (base and mask)
    }

    private val ipPattern = Regex("""^(\d{1,3}\.){3}\d{1,3}$|^[0-9a-fA-F:]+$""")
    private val ipv4Pattern = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")

    private fun detectHighThroughput(meta: PacketMeta) {
        val host = meta.remoteAddress ?: return
        if (host == "-" || meta.bytes <= 0) return
        val uid = meta.uid ?: return
        val key = "$uid|$host"
        val now = meta.timestamp
        val sample = rateByUidAndHost.getOrPut(key) { RateSample(0L, 0.0, 0L) }
        if (sample.lastTimestamp > 0L) {
            val deltaMillis = (now - sample.lastTimestamp).coerceAtLeast(1L)
            val instantaneousBytesPerSecond = meta.bytes.toDouble() * 1000.0 / deltaMillis.toDouble()
            val alpha = 0.85
            sample.exponentialMovingAverage = sample.exponentialMovingAverage * alpha + instantaneousBytesPerSecond * (1.0 - alpha)
            if (sample.exponentialMovingAverage > rateThresholdBytesPerSecond && (now - sample.lastAlertTimestamp) > alertDebounceMillis) {
                sample.lastAlertTimestamp = now
                val message = "High throughput ${formatBytesPerSecond(sample.exponentialMovingAverage)} to $host"
                flagAnomaly(meta, message)
            }
        }
        sample.lastTimestamp = now
    }

    private fun detectNewHost(meta: PacketMeta) {
        val host = meta.remoteAddress ?: return
        if (host == "-" || meta.bytes < newHostBytesMinimum) return
        val uid = meta.uid ?: return
        val key = "$uid|$host"
        if (seenUidHostKeys.putIfAbsent(key, meta.timestamp) == null) {
            val message = "New host spike: $host • ${humanBytes(meta.bytes)}"
            flagAnomaly(meta, message)
        }
    }

    private fun flagAnomaly(meta: PacketMeta, message: String) {
        val key = uiKey(meta)
        val current = anomalyKeys.value
        if (!current.contains(key)) {
            anomalyKeys.value = current + key
            viewModelScope.launch {
                kotlinx.coroutines.delay(highlightHoldMillis)
                anomalyKeys.value = anomalyKeys.value - key
            }
        }
        _anomalyEvents.tryEmit(message)
    }

    private fun uiKey(meta: PacketMeta): String {
        val uidText = meta.uid?.toString() ?: "?"
        return "${meta.timestamp}:$uidText:${meta.bytes}:${meta.localPort}:${meta.remotePort}:${meta.localAddress}:${meta.remoteAddress}"
    }

    private fun formatBytesPerSecond(bytesPerSecond: Double): String {
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        var value = bytesPerSecond
        var index = 0
        while (value >= 1024.0 && index < units.lastIndex) {
            value /= 1024.0
            index++
        }
        return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
    }

    private fun humanBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
    }

    private fun formatTime(timestamp: Long): String =
        java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
            .toString()

    companion object {
        val SERVICE_NAMES = mapOf(
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
