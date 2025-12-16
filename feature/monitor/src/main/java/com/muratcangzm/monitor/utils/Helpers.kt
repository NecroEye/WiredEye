package com.muratcangzm.monitor.utils

import java.util.Locale
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.material3.SnackbarHostState
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import com.muratcangzm.monitor.MonitorViewModel
import com.muratcangzm.shared.model.UiPacket
import com.muratcangzm.utils.StringUtils

fun humanBytes(b: Long): String {
    val u = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = b.toDouble()
    var i = 0
    while (v >= 1024 && i < u.lastIndex) {
        v /= 1024
        i++
    }
    return String.format(Locale.getDefault(), "%.1f %s", v, u[i])
}

fun shareWindowJson(ctx: Context, items: List<UiPacket>, snackbarHost: SnackbarHostState, scope: CoroutineScope) {
    runCatching {
        val json = buildJson(items)
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "wiredeye_${System.currentTimeMillis()}.json")
        file.writeText(json)
        val authority = ctx.applicationContext.packageName + ".fileprovider"
        val uri = FileProvider.getUriForFile(ctx, authority, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(ctx.contentResolver, file.name, uri)
        }
        val resInfo = ctx.packageManager.queryIntentActivities(send, PackageManager.MATCH_DEFAULT_ONLY)
        for (ri in resInfo) ctx.grantUriPermission(ri.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(send, "Share snapshot"))
    }.onFailure {
        scope.launch { snackbarHost.showSnackbar("Sharing failed") }
        runCatching {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, buildJson(items))
            }
            ctx.startActivity(Intent.createChooser(fallback, "Share snapshot"))
        }
    }
}

private fun buildJson(items: List<UiPacket>): String {
    val sb = StringBuilder(items.size * 128)
    sb.append('[')
    items.forEachIndexed { i, r ->
        val m = r.raw
        sb.append("{")
            .append("\"timestamp\":").append(m.timestamp).append(',')
            .append("\"uid\":").append(m.uid ?: -1).append(',')
            .append("\"package\":\"").append((m.packageName ?: StringUtils.EMPTY).escapeJson()).append("\",")
            .append("\"protocol\":\"").append(m.protocol.escapeJson()).append("\",")
            .append("\"from\":\"").append((m.localAddress + ":" + m.localPort).escapeJson()).append("\",")
            .append("\"to\":\"").append((m.remoteAddress + ":" + m.remotePort).escapeJson()).append("\",")
            .append("\"bytes\":").append(m.bytes)
            .append("}")
        if (i != items.lastIndex) sb.append(',')
    }
    sb.append(']')
    return sb.toString()
}

private fun String.escapeJson(): String = buildString {
    for (c in this@escapeJson) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}

fun serviceName(port: Int): String? = if (port == 0) null else MonitorViewModel.SERVICE_NAMES[port]

fun isPrivateV4(ip: String): Boolean {
    if (!ip.contains('.')) return false
    val p = ip.split('.').mapNotNull { it.toIntOrNull() }
    if (p.size != 4) return false
    val a = p[0]
    val b = p[1]
    return (a == 10) || (a == 172 && b in 16..31) || (a == 192 && b == 168)
}