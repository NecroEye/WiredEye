package com.muratcangzm.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    // Controls
    private val filterText = MutableStateFlow("")
    private val minBytes = MutableStateFlow(0L)
    private val windowMillis = MutableStateFlow(10_000L)

    // Engine state (read-only)
    private val engineState: StateFlow<EngineState> = engine.state

    // Stream: switch map repo.liveWindow(windowMillis)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val windowStream: Flow<List<PacketMeta>> =
        windowMillis.flatMapLatest { repo.liveWindow(it) }

    init {
        viewModelScope.launch {
            engine.events.collect { meta -> repo.recordPacketMeta(meta) }
        }
    }

    // Compose-friendly state
    val uiState: StateFlow<MonitorUiState> =
        combine(
            windowStream,
            filterText,
            minBytes,
            windowMillis,
            engineState
        ) { list, filter, minB, win, eState ->
            val filtered = list.filter { m ->
                (m.bytes >= minB) && matchesFilter(m, filter)
            }

            val items = filtered.asReversed()
                .map { m -> m.toUiPacket() }

            val totalBytes = filtered.sumOf { it.bytes }
            val uniqueApps = filtered.map { it.packageName ?: "uid:${it.uid}" }.toSet().size
            val pps = if (win > 0) filtered.size / (win / 1000.0) else 0.0

            MonitorUiState(
                isEngineRunning = eState is EngineState.Running,
                items = items,
                filterText = filter,
                minBytes = minB,
                windowMillis = win,
                totalBytes = totalBytes,
                uniqueApps = uniqueApps,
                pps = pps
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MonitorUiState()
        )

    fun onEvent(ev: MonitorUiEvent) {
        when (ev) {
            is MonitorUiEvent.SetFilter -> filterText.value = ev.text
            is MonitorUiEvent.SetMinBytes -> minBytes.value = ev.value
            is MonitorUiEvent.SetWindow -> windowMillis.value = ev.millis
            MonitorUiEvent.ClearFilter -> filterText.value = ""
            MonitorUiEvent.StartEngine -> viewModelScope.launch { engine.start() }
            MonitorUiEvent.StopEngine -> viewModelScope.launch { engine.stop() }
        }
    }

    // --- Helpers ---

    private fun PacketMeta.toUiPacket(): UiPacket {
        val key = "${timestamp}:${uid}:${bytes}"
        val time = formatTime(timestamp)
        val app = packageName ?: "uid:${uid ?: -1}"
        val proto = protocol
        val from = "${localAddress}:${localPort}"
        val to = "${remoteAddress}:${remotePort}"
        val bytesLabel = humanBytes(bytes)
        return UiPacket(key, time, app, proto, from, to, bytesLabel, this)
    }

    private fun matchesFilter(m: PacketMeta, q: String): Boolean {
        if (q.isBlank()) return true
        val s = q.lowercase(Locale.getDefault())
        val fields = listOfNotNull(
            m.packageName,
            m.protocol,
            m.localAddress,
            m.remoteAddress,
            m.localPort.toString(),
            m.remotePort.toString()
        ).joinToString(" ").lowercase(Locale.getDefault())
        return s.split(" ").all { term -> fields.contains(term) }
    }

    private fun humanBytes(b: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var v = b.toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024; i++
        }
        return String.format(Locale.getDefault(), "%.1f %s", v, units[i])
    }

    private fun formatTime(ts: Long): String {
        val d = java.time.Instant.ofEpochMilli(ts)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
        return d.toString()
    }
}