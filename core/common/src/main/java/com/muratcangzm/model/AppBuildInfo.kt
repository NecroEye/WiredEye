package com.muratcangzm.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppBuildInfo(
    val versionName: String,
    val buildType: String,
)