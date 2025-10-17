// core/network/src/main/java/com/muratcangzm/network/vpn/DnsSnifferVpnService.kt
package com.muratcangzm.network.vpn

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresPermission
import com.muratcangzm.core.NativeTun
import com.muratcangzm.data.model.meta.DnsMeta
import com.muratcangzm.data.model.meta.PacketMeta
import com.muratcangzm.data.repo.packetRepo.PacketRepository
import com.muratcangzm.network.common.FlowKey
import com.muratcangzm.network.common.UID_TTL_MS
import com.muratcangzm.network.common.UidEntry
import com.muratcangzm.network.common.uidCache
import com.muratcangzm.network.engine.PacketEventBus
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

@SuppressLint("VpnServicePolicy")
class DnsSnifferVpnService : VpnService(), NativeTun.Listener {

    private val repo: PacketRepository by inject()
    private val bus: PacketEventBus by inject()

    private var tun: ParcelFileDescriptor? = null
    private var nativeRunning = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); stopVpn(); return START_NOT_STICKY }
        if (tun != null && nativeRunning) return START_STICKY

        val builder = Builder()
            .setSession(SESSION_NAME)
            .setMtu(MTU)
            .addAddress("10.88.0.2", 32)
            .addAddress("fd00:88::2", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .also { runCatching { it.addDisallowedApplication(packageName) } }

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.getLinkProperties(cm.activeNetwork)?.dnsServers.orEmpty()
            .forEach { it.hostAddress?.let(builder::addDnsServer) }

        tun = builder.establish() ?: run { stopSelf(); return START_NOT_STICKY }

        NativeTun.setListener(this)
        val fd = tun!!.detachFd()
        nativeRunning = NativeTun.start(
            fd,
            MTU,
            64,             // maxBatch
            128 * 1024,     // maxBatchBytes
            8,              // flushTimeoutMs
            25              // readTimeoutMs
        )
        if (!nativeRunning) { stopSelf(); return START_NOT_STICKY }

        return START_STICKY
    }

    override fun onRevoke() {
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun stopVpn() {
        runCatching { if (nativeRunning) NativeTun.stop() }
        nativeRunning = false
        NativeTun.setListener(null)
        runCatching { tun?.close() }
        tun = null
    }

    // ---------------- NativeTun.Listener ----------------

    override fun onNativeBatch(buf: ByteArray, validBytes: Int, packetCount: Int) {
        try {
            if (packetCount <= 0) {
                parseSingle(ByteBuffer.wrap(buf, 0, validBytes).order(ByteOrder.BIG_ENDIAN))
            } else {
                var off = 0
                var left = validBytes
                repeat(packetCount) {
                    if (left < 2) return
                    val len =
                        ((buf[off].toInt() and 0xFF) shl 8) or (buf[off + 1].toInt() and 0xFF)
                    off += 2; left -= 2
                    if (len <= 0 || len > left) return
                    parseSingle(ByteBuffer.wrap(buf, off, len).order(ByteOrder.BIG_ENDIAN))
                    off += len; left -= len
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onNativeBatch parse error", t)
        }
    }

    private fun parseSingle(bb: ByteBuffer) {
        when (val ip = IpPacket.parse(bb)) {
            is IpPacket.Ipv4 -> when (ip.protocol) {
                17 -> handleUdp(ip.src, ip.dst, ip.payload, isV6 = false)
                6  -> handleTcp(ip.src, ip.dst, ip.payload, isV6 = false)
                else -> Unit
            }
            is IpPacket.Ipv6 -> when (ip.nextHeader) {
                17 -> handleUdp(ip.src, ip.dst, ip.payload, isV6 = true)
                6  -> handleTcp(ip.src, ip.dst, ip.payload, isV6 = true)
                else -> Unit
            }
            else -> Unit
        }
    }

    // ---------------- UDP/TCP ----------------

    private fun handleUdp(src: InetAddress, dst: InetAddress, payload: ByteBuffer, isV6: Boolean) {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val udp = UdpHeader.parse(payload) ?: return
        if (udp.dstPort == 53 || udp.srcPort == 53) {
            recordDns(src.hostAddress, dst.hostAddress, udp.payload)
        }
        val uid = tryResolveUid(
            cm, 17,
            InetSocketAddress(src, udp.srcPort),
            InetSocketAddress(dst, udp.dstPort)
        )
        emitMeta(
            PacketMeta(
                timestamp = System.currentTimeMillis(),
                uid = uid,
                packageName = null,
                protocol = if (isV6) "UDP6" else "UDP",
                localAddress = src.hostAddress,
                localPort = udp.srcPort,
                remoteAddress = dst.hostAddress,
                remotePort = udp.dstPort,
                bytes = udp.length.toLong()
            )
        )
    }

    private fun handleTcp(src: InetAddress, dst: InetAddress, payload: ByteBuffer, isV6: Boolean) {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val tcp = TcpHeader.parse(payload) ?: return
        val uid = tryResolveUid(
            cm, 6,
            InetSocketAddress(src, tcp.srcPort),
            InetSocketAddress(dst, tcp.dstPort)
        )
        emitMeta(
            PacketMeta(
                timestamp = System.currentTimeMillis(),
                uid = uid,
                packageName = null,
                protocol = if (isV6) "TCP6" else "TCP",
                localAddress = src.hostAddress,
                localPort = tcp.srcPort,
                remoteAddress = dst.hostAddress,
                remotePort = tcp.dstPort,
                bytes = (tcp.headerLen + tcp.payload.remaining()).toLong()
            )
        )
    }

    // ---------------- DNS ----------------

    private fun recordDns(
        @Suppress("UNUSED_PARAMETER") srcIp: String,
        dstIp: String,
        payload: ByteBuffer
    ) {
        val msg = DnsMessage.tryParse(payload.duplicate().order(ByteOrder.BIG_ENDIAN)) ?: return
        val q = msg.questions.firstOrNull() ?: return
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

    // ---------------- helpers ----------------

    private fun emitMeta(meta: PacketMeta) {
        bus.tryEmit(meta)
        scope.launch { repo.recordPacketMeta(meta) }
    }

    private fun tryResolveUid(
        cm: ConnectivityManager,
        proto: Int,
        local: InetSocketAddress,
        remote: InetSocketAddress
    ): Int? {
        val now = System.currentTimeMillis()
        val key = FlowKey(
            proto,
            local.address.hostAddress.orEmpty(), local.port,
            remote.address.hostAddress.orEmpty(), remote.port,
        ).norm()
        uidCache.get(key)?.let { if (it.expireAt > now) return it.uid }

        val u1 = runCatching { cm.getConnectionOwnerUid(proto, local, remote) }.getOrNull()
        val uid = when {
            (u1 ?: -1) > 0 -> u1
            else -> {
                val u2 =
                    runCatching { cm.getConnectionOwnerUid(proto, remote, local) }.getOrNull()
                if ((u2 ?: -1) > 0) u2 else null
            }
        }
        uidCache.put(key, UidEntry(uid, now + UID_TTL_MS))
        return uid
    }

    companion object {
        const val ACTION_START = "com.muratcangzm.monitor.VPN_START"
        const val ACTION_STOP  = "com.muratcangzm.monitor.VPN_STOP"
        private const val SESSION_NAME = "MetaNet VPN Sniffer"
        private const val MTU = 1500
        private const val TAG = "NativeTun"
        private const val BUFFER_SIZE = 65535
    }
}

/* ===================== TCP parser (private, tek dosyada) ===================== */
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
            if (hdrLen < 20 || pos0 + hdrLen > bb.limit()) {
                bb.position(pos0); return null
            }
            val payload = bb.duplicate().apply {
                position(pos0 + hdrLen); limit(bb.limit())
            }.slice()
            bb.position(pos0)
            return TcpHeader(src, dst, hdrLen, payload)
        }
    }
}