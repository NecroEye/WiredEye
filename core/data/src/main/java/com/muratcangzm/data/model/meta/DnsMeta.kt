package com.muratcangzm.data.model.meta

import com.muratcangzm.data.model.DnsEvent
import kotlinx.serialization.Serializable

@Serializable
data class DnsMeta(
    val timestamp: Long,
    val uid: Int?,
    val packageName: String?,
    val qname: String,
    val qtype: String,
    val server: String
) {
    fun toEntity() = DnsEvent(0, timestamp, uid, packageName, qname, qtype, server)
}
