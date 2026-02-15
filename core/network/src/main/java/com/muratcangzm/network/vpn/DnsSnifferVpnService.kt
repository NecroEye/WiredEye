package com.muratcangzm.network.vpn

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.muratcangzm.core.NativeTun
import com.muratcangzm.core.leak.LeakAnalyzerBridge
import com.muratcangzm.data.model.meta.DnsMeta
import com.muratcangzm.data.model.meta.PacketMeta
import com.muratcangzm.data.repo.packetRepo.PacketRepository
import com.muratcangzm.network.common.FlowKey
import com.muratcangzm.network.common.UID_TTL_MS
import com.muratcangzm.network.common.UidEntry
import com.muratcangzm.network.common.uidCache
import com.muratcangzm.network.engine.PacketEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

@SuppressLint("VpnServicePolicy")
class DnsSnifferVpnService : VpnService(), NativeTun.Listener {

    private val packetRepository: PacketRepository by inject()
    private val eventBus: PacketEventBus by inject()
    private val leakAnalyzerBridge: LeakAnalyzerBridge by inject()

    private var tunInterface: ParcelFileDescriptor? = null
    private var nativeLayerRunning: Boolean = false
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastNotifUpdate: Long = 0L
    private var totalBytes: Long = 0L
    private var lastStatsWindowStart: Long = 0L
    private var windowBytes: Long = 0L
    private var windowPackets: Long = 0L

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTun()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        if (tunInterface != null && nativeLayerRunning) {
            updateNotification(
                totalBytes = totalBytes,
                kbs = currentKbs(),
                pps = currentPps(),
                mode = "LIVE"
            )
            return START_STICKY
        }

        ensureNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                content = "Starting…",
                ongoing = true
            )
        )

        val builder = createBuilder()
        addSystemDnsServers(builder)

        if (!establishTun(builder)) {
            stopTun()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!startNativeLayer()) {
            stopTun()
            stopSelf()
            return START_NOT_STICKY
        }

        resetWindow()
        updateNotification(
            totalBytes = totalBytes,
            kbs = currentKbs(),
            pps = currentPps(),
            mode = "LIVE"
        )

        return START_STICKY
    }

    override fun onRevoke() {
        stopTun()
        stopSelf()
    }

    override fun onDestroy() {
        stopTun()
        super.onDestroy()
    }

    private fun createBuilder(): Builder =
        Builder()
            .setSession(SESSION_NAME)
            .setMtu(DEFAULT_MTU)
            .addAddress("10.88.0.2", 32)
            .addAddress("fd00:88::2", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .also { runCatching { it.addDisallowedApplication(packageName) } }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun addSystemDnsServers(builder: Builder) {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val properties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
        properties?.dnsServers.orEmpty().forEach { server ->
            server.hostAddress?.let(builder::addDnsServer)
        }
    }

    private fun establishTun(builder: Builder): Boolean {
        tunInterface = builder.establish()
        return tunInterface != null
    }

    private fun startNativeLayer(): Boolean {
        NativeTun.setListener(this)
        val fd = tunInterface?.detachFd() ?: return false
        nativeLayerRunning = NativeTun.start(
            fd,
            DEFAULT_MTU,
            64,
            128 * 1024,
            8,
            25
        )
        if (!nativeLayerRunning) NativeTun.setListener(null)
        return nativeLayerRunning
    }

    private fun stopTun() {
        runCatching { if (nativeLayerRunning) NativeTun.stop() }
        nativeLayerRunning = false
        NativeTun.setListener(null)
        runCatching { tunInterface?.close() }
        tunInterface = null
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
    }

    override fun onNativeBatch(buffer: ByteArray, validBytes: Int, packetCount: Int) {
        try {
            if (packetCount <= 0) {
                parseSingle(ByteBuffer.wrap(buffer, 0, validBytes).order(ByteOrder.BIG_ENDIAN))
                return
            }
            var offset = 0
            var remaining = validBytes
            repeat(packetCount) {
                if (remaining < 2) return
                val length =
                    ((buffer[offset].toInt() and 0xFF) shl 8) or (buffer[offset + 1].toInt() and 0xFF)
                offset += 2
                remaining -= 2
                if (length <= 0 || length > remaining) return
                parseSingle(ByteBuffer.wrap(buffer, offset, length).order(ByteOrder.BIG_ENDIAN))
                offset += length
                remaining -= length
            }
        } catch (t: Throwable) {
            Log.e(TAG, "native batch parse error", t)
        }
    }

    private fun parseSingle(byteBuffer: ByteBuffer) {
        when (val ip = IpPacket.parse(byteBuffer)) {
            is IpPacket.Ipv4 -> when (ip.protocol) {
                17 -> handleUdp(ip.src, ip.dst, ip.payload, false)
                6 -> handleTcp(ip.src, ip.dst, ip.payload, false)
            }

            is IpPacket.Ipv6 -> when (ip.nextHeader) {
                17 -> handleUdp(ip.src, ip.dst, ip.payload, true)
                6 -> handleTcp(ip.src, ip.dst, ip.payload, true)
            }

            null -> Unit
        }
    }

    private fun handleUdp(
        source: InetAddress,
        destination: InetAddress,
        payload: ByteBuffer,
        isIpv6: Boolean
    ) {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val header = UdpHeader.parse(payload) ?: return

        if (header.dstPort == 53 || header.srcPort == 53) {
            recordDns(source.hostAddress, destination.hostAddress, header.payload)
        }

        val uid = resolveOwnerUid(
            connectivityManager = connectivityManager,
            protocol = 17,
            local = InetSocketAddress(source, header.srcPort),
            remote = InetSocketAddress(destination, header.dstPort)
        )

        emitMeta(
            PacketMeta(
                timestamp = System.currentTimeMillis(),
                uid = uid,
                packageName = null,
                protocol = if (isIpv6) "UDP6" else "UDP",
                localAddress = source.hostAddress,
                localPort = header.srcPort,
                remoteAddress = destination.hostAddress,
                remotePort = header.dstPort,
                bytes = header.length.toLong()
            )
        )
    }

    private fun handleTcp(
        source: InetAddress,
        destination: InetAddress,
        payload: ByteBuffer,
        isIpv6: Boolean
    ) {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val header = TcpHeader.parse(payload) ?: return

        val uid = resolveOwnerUid(
            connectivityManager = connectivityManager,
            protocol = 6,
            local = InetSocketAddress(source, header.srcPort),
            remote = InetSocketAddress(destination, header.dstPort)
        )

        emitMeta(
            PacketMeta(
                timestamp = System.currentTimeMillis(),
                uid = uid,
                packageName = null,
                protocol = if (isIpv6) "TCP6" else "TCP",
                localAddress = source.hostAddress,
                localPort = header.srcPort,
                remoteAddress = destination.hostAddress,
                remotePort = header.dstPort,
                bytes = (header.headerLen + header.payload.remaining()).toLong()
            )
        )
    }

    private fun recordDns(sourceIp: String, destinationIp: String, payload: ByteBuffer) {
        val message = DnsMessage.tryParse(payload.duplicate().order(ByteOrder.BIG_ENDIAN)) ?: return
        val question = message.questions.firstOrNull() ?: return

        val ts = System.currentTimeMillis()
        leakAnalyzerBridge.onDns(
            timestampMillis = ts,
            userIdentifier = -1,
            queryName = question.name,
            queryType = dnsTypeToInt(question.type),
            serverIp = destinationIp
        )

        val event = DnsMeta(
            timestamp = ts,
            uid = null,
            packageName = null,
            qname = question.name,
            qtype = question.type,
            server = destinationIp
        )
        ioScope.launch { runCatching { packetRepository.recordDnsEvent(event) } }
    }

    private fun dnsTypeToInt(type: String): Int {
        return when (type.trim().uppercase()) {
            "A" -> 1
            "NS" -> 2
            "CNAME" -> 5
            "SOA" -> 6
            "PTR" -> 12
            "MX" -> 15
            "TXT" -> 16
            "AAAA" -> 28
            "SRV" -> 33
            "OPT" -> 41
            "HTTPS" -> 65
            else -> type.filter { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    private fun emitMeta(meta: PacketMeta) {
        eventBus.tryEmit(meta)
        ioScope.launch { packetRepository.recordPacketMeta(meta) }

        totalBytes += meta.bytes
        windowBytes += meta.bytes
        windowPackets += 1

        updateNotification(
            totalBytes = totalBytes,
            kbs = currentKbs(),
            pps = currentPps(),
            mode = "LIVE"
        )
    }

    private fun resolveOwnerUid(
        connectivityManager: ConnectivityManager,
        protocol: Int,
        local: InetSocketAddress,
        remote: InetSocketAddress
    ): Int? {
        val now = System.currentTimeMillis()
        val cacheKey = FlowKey(
            protocol,
            local.address.hostAddress.orEmpty(),
            local.port,
            remote.address.hostAddress.orEmpty(),
            remote.port
        ).norm()

        uidCache.get(cacheKey)?.let { entry ->
            if (entry.expireAt > now) return entry.uid
        }

        val forward = runCatching {
            connectivityManager.getConnectionOwnerUid(protocol, local, remote)
        }.getOrNull()

        val resolved = when {
            (forward ?: -1) > 0 -> forward
            else -> {
                val reverse = runCatching {
                    connectivityManager.getConnectionOwnerUid(protocol, remote, local)
                }.getOrNull()
                if ((reverse ?: -1) > 0) reverse else null
            }
        }

        uidCache.put(cacheKey, UidEntry(resolved, now + UID_TTL_MS))
        return resolved
    }

    private fun ensureNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun stopPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, DnsSnifferVpnService::class.java).apply {
            action = ACTION_STOP
            setPackage(packageName)
        }
        val flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getForegroundService(this, 1001, stopIntent, flags)
    }

    private fun updateNotification(
        totalBytes: Long,
        kbs: Double,
        pps: Double,
        mode: String
    ) {
        val now = System.currentTimeMillis()
        if (now - lastNotifUpdate < NOTIF_INTERVAL_MS) return
        lastNotifUpdate = now

        val content =
            "Total ${humanBytes(totalBytes)} • ${format1(kbs)} KB/s • ${format1(pps)} pps • $mode"

        val openIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val openPendingIntent =
            if (openIntent != null) {
                PendingIntent.getActivity(
                    this,
                    1000,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                null
            }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Wiredeye monitoring")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent())
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(content: String, ongoing: Boolean): android.app.Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val contentPi = if (launchIntent != null) {
            PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Wiredeye monitoring")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent())

        if (contentPi != null) builder.setContentIntent(contentPi)

        return builder.build()
    }

    private fun resetWindow() {
        lastStatsWindowStart = System.currentTimeMillis()
        windowBytes = 0L
        windowPackets = 0L
    }

    private fun currentKbs(): Double {
        val now = System.currentTimeMillis()
        val elapsedMs = (now - lastStatsWindowStart).coerceAtLeast(1L)
        val seconds = elapsedMs / 1000.0
        val bytesPerSecond = windowBytes / seconds
        if (elapsedMs >= STATS_WINDOW_RESET_MS) resetWindow()
        return bytesPerSecond / 1024.0
    }

    private fun currentPps(): Double {
        val now = System.currentTimeMillis()
        val elapsedMs = (now - lastStatsWindowStart).coerceAtLeast(1L)
        val seconds = elapsedMs / 1000.0
        val packetsPerSecond = windowPackets / seconds
        if (elapsedMs >= STATS_WINDOW_RESET_MS) resetWindow()
        return packetsPerSecond
    }

    private fun format1(v: Double): String = String.format(Locale.getDefault(), "%.1f", v)

    private fun humanBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024.0 && index < units.lastIndex) {
            value /= 1024.0
            index++
        }
        return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
    }

    companion object {
        const val ACTION_START = "com.muratcangzm.monitor.VPN_START"
        const val ACTION_STOP = "com.muratcangzm.monitor.VPN_STOP"

        private const val SESSION_NAME = "MetaNet VPN Sniffer"
        private const val DEFAULT_MTU = 1500
        private const val TAG = "NativeTun"

        private const val NOTIFICATION_CHANNEL_ID = "wired_eye_monitoring"
        private const val NOTIFICATION_ID = 9001

        private const val NOTIF_INTERVAL_MS = 1000L
        private const val STATS_WINDOW_RESET_MS = 10_000L
    }
}
