package com.muratcangzm.details.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

internal fun endpoint(address: String, port: Int): String {
    val a = address.ifBlank { "—" }
    return if (port > 0) "$a:$port" else a
}

internal fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024.0 && i < units.lastIndex) {
        v /= 1024.0
        i++
    }
    val decimals = if (i == 0) 0 else 1
    val p = if (decimals == 0) 1.0 else 10.0
    val rounded = (v * p).roundToInt() / p
    return if (decimals == 0) "${rounded.toInt()} ${units[i]}" else "$rounded ${units[i]}"
}

internal fun formatTime(epochMs: Long, formatter: DateTimeFormatter): String {
    if (epochMs <= 0L) return "—"
    return formatter.format(Instant.ofEpochMilli(epochMs))
}

internal fun defaultTimeFormatter(): DateTimeFormatter {
    return DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
}