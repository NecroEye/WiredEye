package com.muratcangzm.common.di

import com.muratcangzm.common.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule = module {
    viewModel {
        HomeViewModel()
    }
}