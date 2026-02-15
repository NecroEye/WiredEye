package com.muratcangzm.shared.model.packet

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class DnsMeta(
    @SerialName("timestampMs") val timestampMs: Long,
    @SerialName("uid") val uid: Int,
    @SerialName("queryName") val queryName: String,
    @SerialName("queryType") val queryType: Int,
    @SerialName("serverIp") val serverIp: String
)
