package com.muratcangzm.network.engine

data class EngineInfo(
    val id: String,
    val mode: EngineMode,
    val capabilities: Set<Capability>
)

enum class EngineMode {
    StatsOnly,
    DnsOnly,
    TunForwarder
}

enum class Capability {
    UID_RESOLUTION,
    DNS_QNAME,
    REMOTE_ENDPOINTS
}

data class PacketCaptureConfig(
    val pollIntervalMs: Long = 1_000,
    val mtu: Int? = null
)

val EngineState.isRunning: Boolean
    get() = this is EngineState.Running