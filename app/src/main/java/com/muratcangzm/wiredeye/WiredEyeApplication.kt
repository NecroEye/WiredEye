package com.muratcangzm.wiredeye

import android.app.Application
import com.muratcangzm.network.cursedNetworkModule
import org.koin.core.context.startKoin
import timber.log.Timber

class WiredEyeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        startKoin {
            modules(cursedNetworkModule)
        }
    }
}