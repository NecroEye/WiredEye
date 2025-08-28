package com.muratcangzm.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

object DispatchersQualifiers {
    val IO = named("dispatcher_io")
    val Default = named("dispatcher_default")
    val Main = named("dispatcher_main")
}

val coroutinesModule = module {
    single<CoroutineDispatcher>(DispatchersQualifiers.IO) { Dispatchers.IO }
    single<CoroutineDispatcher>(DispatchersQualifiers.Default) { Dispatchers.Default }
    single<CoroutineDispatcher>(DispatchersQualifiers.Main) { Dispatchers.Main }
}