package com.muratcangzm.shared.model.leak

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
@Immutable
internal data class LeakSnapshotDto(
    val windowMs: Long = 600_000,
    val nowMs: Long = 0,
    val score: Int = 0,
    val totalQueries: Long = 0,
    val uniqueDomains: Int = 0,
    val publicDnsRatio: Double = 0.0,
    val publicDnsQueries: Long = 0,
    val suspiciousEntropyQueries: Long = 0,
    val burstQueries: Long = 0,
    val topDomains: List<LeakTopDomainDto> = emptyList(),
    val topServers: List<LeakTopServerDto> = emptyList()
)

@Serializable
internal data class LeakTopDomainDto(
    val domain: String,
    val count: Long,
    val entropySuspicious: Long,
    val burst: Long
)

@Serializable
internal data class LeakTopServerDto(
    val ip: String,
    val count: Long,
    @SerialName("publicCount") val publicCount: Long
)

internal object LeakSnapshotJson {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun decode(raw: String): LeakSnapshot {
        if (raw.isBlank()) return LeakSnapshot()
        val dto = runCatching { json.decodeFromString(LeakSnapshotDto.serializer(), raw) }.getOrNull()
            ?: return LeakSnapshot()
        return LeakSnapshot(
            score = dto.score,
            windowMs = dto.windowMs,
            nowMs = dto.nowMs,
            totalQueries = dto.totalQueries,
            uniqueDomains = dto.uniqueDomains,
            publicDnsRatio = dto.publicDnsRatio,
            publicDnsQueries = dto.publicDnsQueries,
            suspiciousEntropyQueries = dto.suspiciousEntropyQueries,
            burstQueries = dto.burstQueries,
            topDomains = dto.topDomains.map {
                TopDomain(
                    domain = it.domain,
                    count = it.count,
                    entropySuspicious = it.entropySuspicious,
                    burst = it.burst
                )
            },
            topServers = dto.topServers.map {
                TopServer(
                    ip = it.ip,
                    count = it.count,
                    publicCount = it.publicCount
                )
            }
        )
    }
}
