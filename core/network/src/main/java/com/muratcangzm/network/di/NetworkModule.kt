package com.muratcangzm.network.di

import android.app.Application
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import com.muratcangzm.common.di.DispatchersQualifiers
import com.muratcangzm.network.common.EngineQualifiers
import com.muratcangzm.network.engine.PacketCaptureEngine
import com.muratcangzm.network.engine.PacketEventBus
import com.muratcangzm.network.engine.StatsOnlyEngine
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val networkModule = module {

    single<Application> { androidContext().applicationContext as Application }

    single {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    single {
        androidContext().getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    }

    single { PacketEventBus() }

    factory<PacketCaptureEngine>(qualifier = EngineQualifiers.Active) {
        StatsOnlyEngine(
            app = get(),
            cm = get(),
            nsm = get(),
            dispatcher = get(DispatchersQualifiers.IO),
            bus = get(),
        )
    }
}
