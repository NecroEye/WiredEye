package com.muratcangzm.shared.model.packet

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class PacketMeta(
    @SerialName("timestampMs") val timestampMs: Long,
    @SerialName("uid") val uid: Int,
    @SerialName("proto") val proto: Int,
    @SerialName("srcIp") val srcIp: String,
    @SerialName("dstIp") val dstIp: String,
    @SerialName("srcPort") val srcPort: Int,
    @SerialName("dstPort") val dstPort: Int,
    @SerialName("bytes") val bytes: Long
)
