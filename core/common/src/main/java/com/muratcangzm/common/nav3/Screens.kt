package com.muratcangzm.common.nav3

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screens : NavKey {

    @Serializable
    data object WiredEyeScreen : Screens

    @Serializable
    data object DetailsScreen: Screens
}