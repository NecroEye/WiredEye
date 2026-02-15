package com.muratcangzm.leaks.di

import com.muratcangzm.leaks.ui.LeaksViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val leaksModule = module {
    viewModel { LeaksViewModel(leakAnalyzerBridge = get()) }
}
