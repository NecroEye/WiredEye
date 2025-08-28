package com.muratcangzm.data.model.meta

import com.muratcangzm.data.model.PacketLog
import kotlinx.serialization.Serializable

@Serializable
data class PacketMeta(
    val timestamp: Long,
    val uid: Int?,
    val packageName: String?,
    val protocol: String,
    val localAddress: String,
    val localPort: Int,
    val remoteAddress: String,
    val remotePort: Int,
    val bytes: Long
) {
    fun toEntity() = PacketLog(
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
}
