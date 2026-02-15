package com.muratcangzm.data.repo.packetRepo

import com.muratcangzm.data.common.TopHost
import com.muratcangzm.data.dao.DnsEventDao
import com.muratcangzm.data.dao.PacketLogDao
import com.muratcangzm.data.model.DnsEvent
import com.muratcangzm.data.model.PacketLog
import com.muratcangzm.data.model.meta.DnsMeta
import com.muratcangzm.data.model.meta.PacketMeta
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class PacketRepositoryImpl(
    private val packetDao: PacketLogDao,
    private val dnsDao: DnsEventDao,
    private val ioDispatcher: CoroutineDispatcher,
) : PacketRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val batchMutex = Mutex()
    private val pending = ArrayList<PacketMeta>(64)

    private val batchSize = 32
    private val flushIntervalMs = 200L

    init {
        scope.launch {
            while (true) {
                delay(flushIntervalMs)
                val batch = drainPending() ?: continue
                packetDao.insertAll(batch.map { it.toEntity() })
            }
        }
    }

    override suspend fun recordPacketMeta(meta: PacketMeta) {
        val batchToInsert: List<PacketMeta>? = batchMutex.withLock {
            pending.add(meta)
            if (pending.size >= batchSize) {
                val out = pending.toList()
                pending.clear()
                out
            } else {
                null
            }
        }
        if (batchToInsert != null) {
            packetDao.insertAll(batchToInsert.map { it.toEntity() })
        }
    }

    override suspend fun recordDnsEvent(event: DnsMeta) {
        dnsDao.insert(event.toEntity())
    }

    private suspend fun drainPending(): List<PacketMeta>? = runCatching {
        batchMutex.withLock {
            if (pending.isEmpty()) return@withLock null
            val out = pending.toList()
            pending.clear()
            out
        }
    }.getOrNull()

    private val nowFlow: StateFlow<Long> =
        flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(500L)
            }
        }.stateIn(scope, SharingStarted.Eagerly, System.currentTimeMillis())

    override fun liveWindow(windowMillis: Long, limit: Int): Flow<List<PacketMeta>> = flow {
        val safeWindow = max(1_000L, windowMillis)
        while (true) {
            val now = System.currentTimeMillis()
            val from = (now - safeWindow).coerceAtLeast(0L)
            val rows = packetDao.rangeOnce(from = from, to = now, limit = limit)
            emit(rows.map { it.toMeta() })
            delay(300L)
        }
    }.distinctUntilChanged()

    override fun liveDnsWindow(windowMillis: Long, limit: Int): Flow<List<DnsMeta>> = flow {
        val safeWindow = max(1_000L, windowMillis)
        while (true) {
            val now = System.currentTimeMillis()
            val from = (now - safeWindow).coerceAtLeast(0L)
            val rows = dnsDao.rangeOnce(from = from, to = now, limit = limit)
            emit(rows.map { it.toMeta() })
            delay(500L)
        }
    }.distinctUntilChanged()

    override fun topHosts(windowMillis: Long, limit: Int): Flow<List<TopHost>> = flow {
        val safeWindow = max(1_000L, windowMillis)
        while (true) {
            val now = System.currentTimeMillis()
            val from = (now - safeWindow).coerceAtLeast(0L)
            val rows = dnsDao.topHostsOnce(from = from, to = now, limit = limit)
            emit(rows)
            delay(1_000L)
        }
    }.distinctUntilChanged()

    private fun PacketMeta.toEntity(): PacketLog = PacketLog(
        id = 0,
        timestamp = timestamp,
        uid = uid,
        packageName = packageName,
        protocol = protocol,
        localAddress = localAddress,
        localPort = localPort,
        remoteAddress = remoteAddress,
        remotePort = remotePort,
        bytes = bytes
    )

    private fun DnsMeta.toEntity(): DnsEvent = DnsEvent(
        id = 0,
        timestamp = timestamp,
        uid = uid,
        packageName = packageName,
        qname = qname,
        qtype = qtype,
        server = server
    )

    private fun PacketLog.toMeta(): PacketMeta = PacketMeta(
        timestamp = timestamp,
        uid = uid,
        packageName = packageName,
        protocol = protocol,
        localAddress = localAddress,
        localPort = localPort,
        remoteAddress = remoteAddress,
        remotePort = remotePort,
        bytes = bytes,
        tls = false,
        sni = null,
        dir = null
    )

    private fun DnsEvent.toMeta(): DnsMeta = DnsMeta(
        timestamp = timestamp,
        uid = uid,
        packageName = packageName,
        qname = qname,
        qtype = qtype,
        server = server
    )
}
