package com.muratcangzm.common.nav

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {

    @Serializable
    data object WiredEyeScreen : Screen
}