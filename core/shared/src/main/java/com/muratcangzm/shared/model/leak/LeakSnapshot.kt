package com.muratcangzm.shared.model.leak

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeakSnapshot(
    @SerialName("score") val score: Int = 0,
    @SerialName("windowMs") val windowMs: Long = 600_000L,
    @SerialName("nowMs") val nowMs: Long = 0L,
    @SerialName("totalQueries") val totalQueries: Long = 0L,
    @SerialName("uniqueDomains") val uniqueDomains: Int = 0,
    @SerialName("publicDnsRatio") val publicDnsRatio: Double = 0.0,
    @SerialName("publicDnsQueries") val publicDnsQueries: Long = 0L,
    @SerialName("suspiciousEntropyQueries") val suspiciousEntropyQueries: Long = 0L,
    @SerialName("burstQueries") val burstQueries: Long = 0L,
    @SerialName("topDomains") val topDomains: List<TopDomain> = emptyList(),
    @SerialName("topServers") val topServers: List<TopServer> = emptyList()
)

@Serializable
data class TopDomain(
    @SerialName("domain") val domain: String,
    @SerialName("count") val count: Long,
    @SerialName("entropySuspicious") val entropySuspicious: Long,
    @SerialName("burst") val burst: Long
)

@Serializable
data class TopServer(
    @SerialName("ip") val ip: String,
    @SerialName("count") val count: Long,
    @SerialName("publicCount") val publicCount: Long
)
