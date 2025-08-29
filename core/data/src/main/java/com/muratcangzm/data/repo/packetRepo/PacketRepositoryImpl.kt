package com.muratcangzm.data.repo.packetRepo

import com.muratcangzm.data.dao.DnsEventDao
import com.muratcangzm.data.dao.PacketLogDao
import com.muratcangzm.data.model.DnsEvent
import com.muratcangzm.data.model.PacketLog
import com.muratcangzm.data.model.meta.DnsMeta
import com.muratcangzm.data.model.meta.PacketMeta
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PacketRepositoryImpl(
    private val packetDao: PacketLogDao,
    private val dnsDao: DnsEventDao,
    private val ioDispatcher: CoroutineDispatcher,
) : PacketRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val incoming = MutableSharedFlow<PacketMeta>(
        replay = 0,
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val bufferMutex = Mutex()
    private val buffer = ArrayList<PacketMeta>(256)

    init {
        scope.launch {
            while (true) {
                flush(delayMillis = 500)
            }
        }
    }

    override suspend fun recordPacketMeta(meta: PacketMeta) {
        incoming.tryEmit(meta)

        bufferMutex.withLock {
            buffer.add(meta)
            if (buffer.size >= 128) {
                flushNowLocked()
            }
        }
    }

    override suspend fun recordDnsEvent(event: DnsMeta) {
        withContext(ioDispatcher) {
            dnsDao.insert(event.toEntity())
        }
    }

    override fun liveWindow(windowMillis: Long): Flow<List<PacketMeta>> {
        val window = TimeWindow(windowMillis)
        return incoming
            .runningFold(mutableListOf<PacketMeta>()) { acc, ev ->
                acc.add(ev)
                val cutoff = ev.timestamp - window.millis
                while (acc.isNotEmpty() && acc.first().timestamp < cutoff) {
                    acc.removeAt(0)
                }
                acc
            }
            .map { it.toList() }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    private suspend fun flush(delayMillis: Long) {
        kotlinx.coroutines.delay(delayMillis)
        bufferMutex.withLock {
            if (buffer.isNotEmpty()) {
                flushNowLocked()
            }
        }
    }

    private suspend fun flushNowLocked() {
        val batch = ArrayList<PacketMeta>(buffer.size)
        batch.addAll(buffer)
        buffer.clear()

        withContext(ioDispatcher) {
            packetDao.insertAll(batch.map { it.toEntity() })
        }
    }

    private data class TimeWindow(val millis: Long)

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
}