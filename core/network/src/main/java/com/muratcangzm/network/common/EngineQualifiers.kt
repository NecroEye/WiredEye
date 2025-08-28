package com.muratcangzm.network.common

import org.koin.core.qualifier.named

object EngineQualifiers {
    val Active = named("engine_active")
    val StatsOnly = named("engine_stats_only")
    val DnsOnly = named("engine_dns_only")
}