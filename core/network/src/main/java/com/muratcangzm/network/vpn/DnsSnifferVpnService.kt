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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

@SuppressLint("VpnServicePolicy")
class DnsSnifferVpnService : VpnService(), NativeTun.Listener {

    private val packetRepository: PacketRepository by inject()
    private val eventBus: PacketEventBus by inject()

    private var tunInterface: ParcelFileDescriptor? = null
    private var nativeLayerRunning = false
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            stopTun()
            return START_NOT_STICKY
        }
        if (tunInterface != null && nativeLayerRunning) return START_STICKY

        val builder = createBuilder()
        addSystemDnsServers(builder)
        if (!establishTun(builder)) return START_NOT_STICKY
        if (!startNativeLayer()) return START_NOT_STICKY

        return START_STICKY
    }

    override fun onRevoke() {
        stopSelf()
    }

    override fun onDestroy() {
        stopTun()
        super.onDestroy()
    }

    private fun createBuilder(): Builder =
        Builder()
            .setSession(sessionName)
            .setMtu(defaultMtu)
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
        tunInterface = builder.establish() ?: return false.also { stopSelf() }
        return true
    }

    private fun startNativeLayer(): Boolean {
        NativeTun.setListener(this)
        val fileDescriptor = tunInterface!!.detachFd()
        nativeLayerRunning = NativeTun.start(
            fileDescriptor,
            defaultMtu,
            64,
            128 * 1024,
            8,
            25
        )
        if (!nativeLayerRunning) {
            stopSelf()
            return false
        }
        return true
    }

    private fun stopTun() {
        runCatching { if (nativeLayerRunning) NativeTun.stop() }
        nativeLayerRunning = false
        NativeTun.setListener(null)
        runCatching { tunInterface?.close() }
        tunInterface = null
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
                val length = ((buffer[offset].toInt() and 0xFF) shl 8) or (buffer[offset + 1].toInt() and 0xFF)
                offset += 2
                remaining -= 2
                if (length <= 0 || length > remaining) return
                parseSingle(ByteBuffer.wrap(buffer, offset, length).order(ByteOrder.BIG_ENDIAN))
                offset += length
                remaining -= length
            }
        } catch (t: Throwable) {
            Log.e(tag, "native batch parse error", t)
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

    private fun handleUdp(source: InetAddress, destination: InetAddress, payload: ByteBuffer, isIpv6: Boolean) {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val header = UdpHeader.parse(payload) ?: return
        if (header.dstPort == 53 || header.srcPort == 53) {
            recordDns(source.hostAddress, destination.hostAddress, header.payload)
        }
        val uid = resolveOwnerUid(
            connectivityManager,
            17,
            InetSocketAddress(source, header.srcPort),
            InetSocketAddress(destination, header.dstPort)
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

    private fun handleTcp(source: InetAddress, destination: InetAddress, payload: ByteBuffer, isIpv6: Boolean) {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val header = TcpHeader.parse(payload) ?: return
        val uid = resolveOwnerUid(
            connectivityManager,
            6,
            InetSocketAddress(source, header.srcPort),
            InetSocketAddress(destination, header.dstPort)
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
        val event = DnsMeta(
            timestamp = System.currentTimeMillis(),
            uid = null,
            packageName = null,
            qname = question.name,
            qtype = question.type,
            server = destinationIp
        )
        ioScope.launch { runCatching { packetRepository.recordDnsEvent(event) } }
    }

    private fun emitMeta(meta: PacketMeta) {
        eventBus.tryEmit(meta)
        ioScope.launch { packetRepository.recordPacketMeta(meta) }
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

        uidCache.get(cacheKey)?.let { if (it.expireAt > now) return it.uid }

        val forward = runCatching { connectivityManager.getConnectionOwnerUid(protocol, local, remote) }.getOrNull()
        val resolved = when {
            (forward ?: -1) > 0 -> forward
            else -> {
                val reverse = runCatching { connectivityManager.getConnectionOwnerUid(protocol, remote, local) }.getOrNull()
                if ((reverse ?: -1) > 0) reverse else null
            }
        }

        uidCache.put(cacheKey, UidEntry(resolved, now + UID_TTL_MS))
        return resolved
    }

    companion object {
        const val ACTION_START = "com.muratcangzm.monitor.VPN_START"
        const val ACTION_STOP = "com.muratcangzm.monitor.VPN_STOP"
        private const val sessionName = "MetaNet VPN Sniffer"
        private const val defaultMtu = 1500
        private const val tag = "NativeTun"
    }
}

private data class TcpHeader(
    val srcPort: Int,
    val dstPort: Int,
    val headerLen: Int,
    val payload: ByteBuffer
) {
    companion object {
        fun parse(buffer: ByteBuffer): TcpHeader? {
            if (buffer.remaining() < 20) return null
            val start = buffer.position()
            val src = buffer.short.toInt() and 0xFFFF
            val dst = buffer.short.toInt() and 0xFFFF
            buffer.int
            buffer.int
            val dataOffset = (buffer.get().toInt() ushr 4) and 0x0F
            buffer.get()
            buffer.short
            buffer.short
            buffer.short
            val headerLength = dataOffset * 4
            if (headerLength < 20 || start + headerLength > buffer.limit()) {
                buffer.position(start)
                return null
            }
            val payloadSlice = buffer.duplicate().apply {
                position(start + headerLength)
                limit(buffer.limit())
            }.slice()
            buffer.position(start)
            return TcpHeader(src, dst, headerLength, payloadSlice)
        }
    }
}