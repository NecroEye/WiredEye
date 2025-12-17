package com.muratcangzm.monitor

import androidx.compose.runtime.Immutable
import com.muratcangzm.monitor.common.SpeedMode
import com.muratcangzm.monitor.common.ViewMode
import com.muratcangzm.shared.model.UiPacket
import com.muratcangzm.utils.StringUtils

@Immutable
data class MonitorUiState(
    val isEngineRunning: Boolean = false,
    val items: List<UiPacket> = emptyList(),
    val filterText: String = StringUtils.EMPTY,
    val minBytes: Long = 0L,
    val windowMillis: Long = 10_000L,
    val totalBytes: Long = 0L,
    val uniqueApps: Int = 0,
    val pps: Double = 0.0,
    val throughputKbs: Double = 0.0,
    val pinnedUids: Set<Int> = emptySet(),
    val anomalyKeys: Set<String> = emptySet(),
    val speedMode: SpeedMode = SpeedMode.ECO,
    val viewMode: ViewMode = ViewMode.RAW,
)