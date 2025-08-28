package com.muratcangzm.network.vpn

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.muratcangzm.data.model.meta.DnsMeta
import com.muratcangzm.data.repo.packetRepo.PacketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import java.io.FileInputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class DnsSnifferVpnService : VpnService(), KoinComponent {

    private val repo: PacketRepository by inject()

    private var tun: ParcelFileDescriptor? = null
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (tun != null && job?.isActive == true) return START_STICKY

        val builder = Builder()
            .setSession(SESSION_NAME)
            .setMtu(1500)
            .addAddress("10.88.0.2", 32)
            .also { runCatching { it.addDisallowedApplication(packageName) } }

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val dnsServers = cm.getLinkProperties(cm.activeNetwork)?.dnsServers.orEmpty()
        if (dnsServers.isEmpty()) { stopSelf(); return START_NOT_STICKY }

        dnsServers.forEach { addr ->
            when (addr) {
                is Inet4Address -> builder.addRoute(addr.hostAddress, 32)
                is Inet6Address -> builder.addRoute(addr.hostAddress, 128)
            }
        }

        tun = builder.establish() ?: run { stopSelf(); return START_NOT_STICKY }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(NOTIF_CHANNEL, "Monitoring", NotificationManager.IMPORTANCE_LOW))
        val notif = NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("DNS metadata monitoring active")
            .setContentText("Only DNS headers are observed. No payload.")
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(this, NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        job = scope.launch { readLoop(tun!!) }
        return START_STICKY
    }

    override fun onRevoke() { stopSelf() }

    override fun onDestroy() {
        runBlocking { job?.cancelAndJoin() }
        tun?.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private suspend fun readLoop(pfd: ParcelFileDescriptor) {
        val input = FileInputStream(pfd.fileDescriptor)
        val buf = ByteArray(BUFFER_SIZE)
        try {
            while (currentCoroutineContext().isActive) {
                val n = input.read(buf)
                if (n <= 0) continue
                val bb = ByteBuffer.wrap(buf, 0, n)
                when (val ip = IpPacket.parse(bb)) {
                    is IpPacket.Ipv4 -> if (ip.protocol == 17 /* UDP */) handleUdp(ip)
                    else -> Unit
                }
            }
        } catch (_: Throwable) {
            stopSelf()
        }
    }

    private fun handleUdp(ip: IpPacket.Ipv4) {
        val udp = UdpHeader.parse(ip.payload) ?: return
        val isDns = udp.dstPort == 53 || udp.srcPort == 53
        if (!isDns) return

        val dns = DnsMessage.tryParse(udp.payload) ?: return
        val q = dns.questions.firstOrNull() ?: return

        val uid = tryResolveUid(ip, udp)

        val event = DnsMeta(
            timestamp = System.currentTimeMillis(),
            uid = uid,
            packageName = null,
            qname = q.name,
            qtype = q.type,
            server = (ip.dst as? Inet4Address)?.hostAddress ?: "?"
        )

        scope.launch { runCatching { repo.recordDnsEvent(event) } }
    }

    private fun tryResolveUid(ip: IpPacket.Ipv4, udp: UdpHeader.Header): Int? = runCatching {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val local = InetSocketAddress(ip.src, udp.srcPort)
        val remote = InetSocketAddress(ip.dst, udp.dstPort)
        val uid = cm.getConnectionOwnerUid(17 /* UDP */, local, remote)
        if (uid == Process.INVALID_UID) null else uid
    }.getOrNull()

    companion object {
        private const val SESSION_NAME = "MetaNet DNS Sniffer"
        private const val NOTIF_CHANNEL = "vpn_monitor"
        private const val NOTIF_ID = 42
        private const val BUFFER_SIZE = 2048
    }
}
