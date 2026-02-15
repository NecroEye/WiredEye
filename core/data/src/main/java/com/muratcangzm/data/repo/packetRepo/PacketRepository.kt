package com.muratcangzm.data.repo.packetRepo

import com.muratcangzm.data.common.TopHost
import com.muratcangzm.data.model.meta.DnsMeta
import com.muratcangzm.data.model.meta.PacketMeta
import kotlinx.coroutines.flow.Flow

interface PacketRepository {
    suspend fun recordPacketMeta(meta: PacketMeta)
    suspend fun recordDnsEvent(event: DnsMeta)

    fun liveWindow(windowMillis: Long = 10_000L, limit: Int = 2_000): Flow<List<PacketMeta>>
    fun liveDnsWindow(windowMillis: Long = 600_000L, limit: Int = 5_000): Flow<List<DnsMeta>>

    fun topHosts(windowMillis: Long = 600_000L, limit: Int = 12): Flow<List<TopHost>>
}
