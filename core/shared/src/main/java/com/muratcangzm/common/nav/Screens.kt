    package com.muratcangzm.common.nav

    import com.muratcangzm.shared.model.UiPacket
    import kotlinx.serialization.Serializable

    @Serializable
    sealed interface Screens  {

        @Serializable
        data object Root: Screens

        @Serializable
        data object WiredEyeScreen : Screens

        @Serializable
        data object LeaksScreen : Screens

        @Serializable
        data object  SummaryScreen: Screens

        @Serializable
        data class DetailsScreen(val uiPacket: UiPacket): Screens
    }