package com.muratcangzm.summary.di

import com.muratcangzm.summary.ui.SummaryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val summaryModule = module {
    viewModel { SummaryViewModel(leakAnalyzerBridge = get()) }
}
