package com.muratcangzm.wiredeye.di

import com.muratcangzm.model.AppBuildInfo
import com.muratcangzm.settings.ui.SettingsViewModel
import com.muratcangzm.wiredeye.BuildConfig
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsAppModule = module {

    single {
        AppBuildInfo(
            versionName = BuildConfig.VERSION_NAME,
            buildType = BuildConfig.BUILD_TYPE
        )
    }

    viewModel {
        SettingsViewModel(
            buildInfo = get(),
            privacyUrl = "https://yourdomain.com/privacy",
            termsUrl = "https://yourdomain.com/terms",
            supportEmail = "support@yourdomain.com"
        )
    }
}