package com.muratcangzm.monitor.model

import androidx.compose.runtime.Immutable

@Immutable
data class Stats(
    val totalAllTimeBytes: Long,
    val uniqueAppCount: Int,
    val packetsPerSecond: Double,
    val kilobytesPerSecond: Double
)