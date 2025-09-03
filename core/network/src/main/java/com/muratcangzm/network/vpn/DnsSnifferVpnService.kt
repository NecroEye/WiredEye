package com.muratcangzm.network.vpn

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.muratcangzm.data.model.meta.DnsMeta
import com.muratcangzm.data.model.meta.PacketMeta
import com.muratcangzm.data.repo.packetRepo.PacketRepository
import com.muratcangzm.network.engine.PacketEventBus
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DnsSnifferVpnService : VpnService(), KoinComponent {

    private val repo: PacketRepository by inject()
    private val bus: PacketEventBus by inject()

    private var tun: ParcelFileDescriptor? = null
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                stopVpn()
                return START_NOT_STICKY
            }
        }
        if (tun != null && job?.isActive == true) return START_STICKY

        // ---- VPN Builder
        val builder = Builder()
            .setSession(SESSION_NAME)
            .setMtu(1500)
            // TUN’a yerel adresler (v4 + v6)
            .addAddress("10.88.0.2", 32)
            .addAddress("fd00:88::2", 128)
            // Tüm trafiği TUN’a yönlendir
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            // Kendi uygulamamızı hariç tut (loop engelleme)
            .also { runCatching { it.addDisallowedApplication(packageName) } }

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val dnsServers = cm.getLinkProperties(cm.activeNetwork)?.dnsServers.orEmpty()
        dnsServers.forEach { builder.addDnsServer(it.hostAddress) }

        tun = builder.establish() ?: run { stopSelf(); return START_NOT_STICKY }

        // ---- Foreground notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(NOTIF_CHANNEL, "Monitoring", NotificationManager.IMPORTANCE_LOW)
        )
        val notif = NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("VPN metadata monitoring active")
            .setContentText("IP/TCP/UDP headers; DNS parsed (no payload).")
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notif,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )

        job = scope.launch { readLoop(tun!!) }
        return START_STICKY
    }

    override fun onRevoke() { stopSelf() }


    private fun emitMeta(meta: PacketMeta) {
        Log.d("WireLog","emitMeta -> ${meta.protocol} ${meta.bytes}")
        bus.tryEmit(meta)                         // <— kritik hat
        scope.launch { repo.recordPacketMeta(meta) }
    }

    override fun onDestroy() {
        runBlocking { job?.cancelAndJoin() }
        tun?.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIF_ID)
        super.onDestroy()
    }

    // --------------------------------------------------------------------

    private suspend fun readLoop(pfd: ParcelFileDescriptor) {
        val input = FileInputStream(pfd.fileDescriptor)
        val buf = ByteArray(BUFFER_SIZE)
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        try {
            while (currentCoroutineContext().isActive) {
                val n = input.read(buf)
                if (n <= 0) continue

                // Android VpnService: veri doğrudan IP ile başlar (tun_pi yok)
                val ipBuf = ByteBuffer.wrap(buf, 0, n).order(ByteOrder.BIG_ENDIAN)

                // Teşhis için: 0x45 -> IPv4, 0x60 -> IPv6
                val first = ipBuf.get(0).toInt() and 0xFF
                Log.d("AppLog", "pkt n=$n first=0x${first.toString(16)}")

                when (val ip = IpPacket.parse(ipBuf)) {
                    is IpPacket.Ipv4 -> when (ip.protocol) {
                        17 -> handleUdpV4(cm, ip)
                        6  -> handleTcpV4(cm, ip)
                    }
                    is IpPacket.Ipv6 -> when (ip.nextHeader) {
                        17 -> handleUdpV6(cm, ip)
                        6  -> handleTcpV6(cm, ip)
                    }
                    else -> Unit
                }
            }
        } catch (t: Throwable) {
            Log.e("AppLog", "readLoop error", t)
            stopSelf()
        }
    }

    /* ---------------- UDP/TCP handlers ---------------- */

    private fun handleUdpV4(cm: ConnectivityManager, ip: IpPacket.Ipv4) {
        val udp = UdpHeader.parse(ip.payload) ?: return
        Log.d("AppLog","UDP4 ${ip.src.hostAddress}:${udp.srcPort} -> ${ip.dst.hostAddress}:${udp.dstPort} len=${udp.length}")
        if (udp.dstPort == 53 || udp.srcPort == 53) {
            recordDns(ip.src.hostAddress, ip.dst.hostAddress, udp.payload)
        }
        val uid = tryResolveUid(
            cm, 17,
            InetSocketAddress(ip.src, udp.srcPort),
            InetSocketAddress(ip.dst, udp.dstPort)
        )
        emitMeta(
            PacketMeta(
                timestamp = System.currentTimeMillis(),
                uid = uid,
                packageName = null,
                protocol = "UDP",
                localAddress = ip.src.hostAddress,
                localPort = udp.srcPort,
                remoteAddress = ip.dst.hostAddress,
                remotePort = udp.dstPort,
                bytes = udp.length.toLong()
            )
        )
    }

    private fun handleTcpV4(cm: ConnectivityManager, ip: IpPacket.Ipv4) {
        val tcp = TcpHeader.parse(ip.payload) ?: return
        val uid = tryResolveUid(
            cm, 6,
            InetSocketAddress(ip.src, tcp.srcPort),
            InetSocketAddress(ip.dst, tcp.dstPort)
        )
        emitMeta(
            PacketMeta(
                timestamp = System.currentTimeMillis(),
                uid = uid,
                packageName = null,
                protocol = "TCP",
                localAddress = ip.src.hostAddress,
                localPort = tcp.srcPort,
                remoteAddress = ip.dst.hostAddress,
                remotePort = tcp.dstPort,
                bytes = (tcp.headerLen + tcp.payload.remaining()).toLong()
            )
        )
    }

    private fun handleUdpV6(cm: ConnectivityManager, ip: IpPacket.Ipv6) {
        val udp = UdpHeader.parse(ip.payload) ?: return
        if (udp.dstPort == 53 || udp.srcPort == 53) {
            recordDns(ip.src.hostAddress, ip.dst.hostAddress, udp.payload)
        }
        val uid = tryResolveUid(
            cm, 17,
            InetSocketAddress(ip.src, udp.srcPort),
            InetSocketAddress(ip.dst, udp.dstPort)
        )
        emitMeta(
            PacketMeta(
                timestamp = System.currentTimeMillis(),
                uid = uid,
                packageName = null,
                protocol = "UDP6",
                localAddress = ip.src.hostAddress,
                localPort = udp.srcPort,
                remoteAddress = ip.dst.hostAddress,
                remotePort = udp.dstPort,
                bytes = udp.length.toLong()
            )
        )
    }

    private fun handleTcpV6(cm: ConnectivityManager, ip: IpPacket.Ipv6) {
        val tcp = TcpHeader.parse(ip.payload) ?: return
        val uid = tryResolveUid(
            cm, 6,
            InetSocketAddress(ip.src, tcp.srcPort),
            InetSocketAddress(ip.dst, tcp.dstPort)
        )
        emitMeta(
            PacketMeta(
                timestamp = System.currentTimeMillis(),
                uid = uid,
                packageName = null,
                protocol = "TCP6",
                localAddress = ip.src.hostAddress,
                localPort = tcp.srcPort,
                remoteAddress = ip.dst.hostAddress,
                remotePort = tcp.dstPort,
                bytes = (tcp.headerLen + tcp.payload.remaining()).toLong()
            )
        )
    }

    /* ---------------- DNS ---------------- */

    private fun recordDns(srcIp: String, dstIp: String, payload: ByteBuffer) {
        val dns = DnsMessage.tryParse(payload.duplicate().order(ByteOrder.BIG_ENDIAN)) ?: return
        val q = dns.questions.firstOrNull() ?: return
        val event = DnsMeta(
            timestamp = System.currentTimeMillis(),
            uid = null,
            packageName = null,
            qname = q.name,
            qtype = q.type,
            server = dstIp
        )
        scope.launch { runCatching { repo.recordDnsEvent(event) } }
    }

    /* ---------------- helpers ---------------- */
    
    private fun stopVpn() {
        runCatching { job?.cancel() }
        job = null

        runCatching { tun?.close() }
        tun = null

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIF_ID)
    }

    private fun tryResolveUid(
        cm: ConnectivityManager,
        proto: Int,
        local: InetSocketAddress,
        remote: InetSocketAddress
    ): Int? = runCatching {
        val uid = cm.getConnectionOwnerUid(proto, local, remote)
        if (uid <= 0) null else uid
    }.getOrNull()

//    private fun emitMeta(meta: PacketMeta) {
//        val json = Json.encodeToString(meta)
//        // Android 13+’ta implicit broadcast kaçmasın diye paketi hedefliyoruz
//        sendBroadcast(
//            Intent(ACTION_EVENT)
//                .setPackage(packageName)
//                .putExtra(EXTRA_JSON, json)
//        )
//    }

    companion object {
        const val ACTION_START = "com.muratcangzm.monitor.VPN_START"
        const val ACTION_STOP  = "com.muratcangzm.monitor.VPN_STOP"
        const val ACTION_EVENT = "com.muratcangzm.monitor.VPN_EVENT"
        const val EXTRA_JSON   = "json"

        private const val SESSION_NAME = "MetaNet VPN Sniffer"
        private const val NOTIF_CHANNEL = "vpn_monitor"
        private const val NOTIF_ID = 1001
        private const val BUFFER_SIZE = 65535
    }
}

/* ===================== TCP parser ===================== */

private data class TcpHeader(
    val srcPort: Int,
    val dstPort: Int,
    val headerLen: Int,
    val payload: ByteBuffer
) {
    companion object {
        fun parse(bb: ByteBuffer): TcpHeader? {
            if (bb.remaining() < 20) return null
            val pos0 = bb.position()
            val src = bb.short.toInt() and 0xFFFF
            val dst = bb.short.toInt() and 0xFFFF
            bb.int   // seq
            bb.int   // ack
            val dataOff = (bb.get().toInt() ushr 4) and 0x0F
            bb.get() // flags
            bb.short // window
            bb.short // checksum
            bb.short // urg ptr
            val hdrLen = dataOff * 4
            if (hdrLen < 20 || pos0 + hdrLen > bb.limit()) { bb.position(pos0); return null }
            val payload = bb.duplicate().apply {
                position(pos0 + hdrLen); limit(bb.limit())
            }.slice()
            bb.position(pos0)
            return TcpHeader(src, dst, hdrLen, payload)
        }
    }
}