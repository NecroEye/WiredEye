package com.muratcangzm.monitor

import com.muratcangzm.monitor.common.UiPacket

data class MonitorUiState(
    val isEngineRunning: Boolean = false,
    val items: List<UiPacket> = emptyList(),
    val filterText: String = "",
    val minBytes: Long = 0,
    val windowMillis: Long = 10_000,
    val totalBytes: Long = 0,
    val uniqueApps: Int = 0,
    val pps: Double = 0.0,
    val throughputKbs: Double = 0.0,
    val pinnedUids: Set<Int> = emptySet()
)
