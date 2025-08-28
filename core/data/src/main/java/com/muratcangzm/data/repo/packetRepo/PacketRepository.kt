package com.muratcangzm.data.repo.packetRepo

import com.muratcangzm.data.model.meta.DnsMeta
import com.muratcangzm.data.model.meta.PacketMeta
import kotlinx.coroutines.flow.Flow

interface PacketRepository {
    suspend fun recordPacketMeta(meta: PacketMeta)
    suspend fun recordDnsEvent(event: DnsMeta)
    fun liveWindow(windowMillis: Long = 10_000): Flow<List<PacketMeta>>
}