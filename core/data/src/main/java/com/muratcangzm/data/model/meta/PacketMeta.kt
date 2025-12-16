package com.muratcangzm.data.model.meta

import androidx.compose.runtime.Immutable
import com.muratcangzm.data.model.PacketLog
import com.muratcangzm.utils.StringUtils
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class PacketMeta(
    val timestamp: Long = 0L,
    val uid: Int? = 0,
    val packageName: String? = StringUtils.EMPTY,
    val protocol: String = StringUtils.EMPTY,
    val localAddress: String = StringUtils.EMPTY,
    val localPort: Int = 0,
    val remoteAddress: String = StringUtils.EMPTY,
    val remotePort: Int = 0,
    val bytes: Long = 0,
    val tls: Boolean = false,
    val sni: String? = null,
    val dir: String? = null
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
