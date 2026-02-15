package com.muratcangzm.monitor.di

import com.muratcangzm.data.helper.UidResolver
import com.muratcangzm.monitor.MonitorViewModel
import com.muratcangzm.network.common.EngineQualifiers
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val monitorModule = module {
    viewModel {
        MonitorViewModel(
            packetRepository = get(),
            captureEngine = get(qualifier = EngineQualifiers.Active),
            eventBus = get(),
            uidResolver = get<UidResolver>(),
        )
    }
}