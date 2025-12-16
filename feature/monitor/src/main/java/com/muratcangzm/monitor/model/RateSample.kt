package com.muratcangzm.monitor.model

internal data class RateSample(
    var lastTimestamp: Long,
    var exponentialMovingAverage: Double,
    var lastAlertTimestamp: Long
)