package com.muratcangzm.di

import com.muratcangzm.details.DetailsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val detailsModule = module {
    viewModel {
        DetailsViewModel()
    }
}