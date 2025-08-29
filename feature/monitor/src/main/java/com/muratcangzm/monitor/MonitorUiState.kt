package com.muratcangzm.monitor

import com.muratcangzm.monitor.common.SpeedMode
import com.muratcangzm.monitor.common.UiPacket
import com.muratcangzm.monitor.common.ViewMode

data class MonitorUiState(
    val isEngineRunning: Boolean = false,
    val items: List<UiPacket> = emptyList(),
    val filterText: String = "",
    val minBytes: Long = 0L,
    val windowMillis: Long = 10_000L,
    val totalBytes: Long = 0L,
    val uniqueApps: Int = 0,
    val pps: Double = 0.0,
    val throughputKbs: Double = 0.0,
    val pinnedUids: Set<Int> = emptySet(),
    val speedMode: SpeedMode = SpeedMode.ECO,
    val viewMode: ViewMode = ViewMode.RAW,
)