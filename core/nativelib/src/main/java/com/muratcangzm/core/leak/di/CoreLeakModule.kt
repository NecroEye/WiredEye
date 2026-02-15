package com.muratcangzm.core.leak.di

import com.muratcangzm.core.leak.LeakAnalyzerBridge
import com.muratcangzm.core.leak.LeakAnalyzerBridgeImpl
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val coreLeakModule = module {
    single<LeakAnalyzerBridge> { LeakAnalyzerBridgeImpl(dispatcher = Dispatchers.Default) }
}
