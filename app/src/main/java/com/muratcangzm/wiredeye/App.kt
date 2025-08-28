package com.muratcangzm.wiredeye

import android.app.Application
import com.muratcangzm.common.di.coroutinesModule
import com.muratcangzm.data.di.dataModule
import com.muratcangzm.monitor.di.monitorModule
import com.muratcangzm.network.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            properties(mapOf("engine" to "stats"))
            modules(
                coroutinesModule,
                dataModule,
                networkModule,
                monitorModule,
            )
        }
    }
}