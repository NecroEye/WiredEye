package com.muratcangzm.network.di

import android.app.Application
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import com.muratcangzm.common.di.DispatchersQualifiers
import com.muratcangzm.network.engine.PacketCaptureEngine
import com.muratcangzm.network.engine.StatsOnlyEngine
import com.muratcangzm.network.vpn.DnsVpnController
import com.muratcangzm.network.vpn.DnsVpnControllerImpl
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

object EngineQualifiers {
    val Active = named("engine_active")
    val StatsOnly = named("engine_stats_only")
    val DnsOnly = named("engine_dns_only")
}

val networkModule = module {

    single { androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    single { androidContext().getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager }

    single<DnsVpnController> { DnsVpnControllerImpl(androidContext()) }

    single<PacketCaptureEngine>(EngineQualifiers.StatsOnly) {
        val appCtx = androidContext().applicationContext as Application
        val cm = get<ConnectivityManager>()
        val nsm = get<NetworkStatsManager>()
        val defaultDispatcher = get<CoroutineDispatcher>(DispatchersQualifiers.Default)
        StatsOnlyEngine(appCtx, cm, nsm, defaultDispatcher) // info/state içeride yönetiliyor
    }

    single<PacketCaptureEngine>(EngineQualifiers.Active) {
        when (getKoin().getProperty<String>("engine", "stats")) {
            "dns" -> getOrNull<PacketCaptureEngine>(EngineQualifiers.DnsOnly)
                ?: get(EngineQualifiers.StatsOnly)
            else -> get(EngineQualifiers.StatsOnly)
        }
    }
}
