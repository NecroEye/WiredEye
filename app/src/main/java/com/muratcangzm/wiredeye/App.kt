package com.muratcangzm.wiredeye

import android.app.Application
import com.ReleaseTree
import com.muratcangzm.AppLogger
import com.muratcangzm.Logger
import com.muratcangzm.common.di.commonModule
import com.muratcangzm.common.di.coroutinesModule
import com.muratcangzm.data.di.dataModule
import com.muratcangzm.di.detailsModule
import com.muratcangzm.monitor.di.monitorModule
import com.muratcangzm.network.di.networkModule
import com.muratcangzm.preferences.di.prefsModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        Logger.init(object : AppLogger {
            override fun d(message: String, throwable: Throwable?) {
                if (throwable != null) Timber.d(throwable, message) else Timber.d(message)
            }

            override fun i(message: String, throwable: Throwable?) {
                if (throwable != null) Timber.i(throwable, message) else Timber.i(message)
            }

            override fun w(message: String, throwable: Throwable?) {
                if (throwable != null) Timber.w(throwable, message) else Timber.w(message)
            }

            override fun e(message: String, throwable: Throwable?) {
                if (throwable != null) Timber.e(throwable, message) else Timber.e(message)
            }
        })

        startKoin {
            androidContext(this@App)
            properties(mapOf("engine" to "stats"))
            modules(
                coroutinesModule,
                dataModule,
                networkModule,
                commonModule,
                monitorModule,
                prefsModule,
                detailsModule,
            )
        }
    }
}