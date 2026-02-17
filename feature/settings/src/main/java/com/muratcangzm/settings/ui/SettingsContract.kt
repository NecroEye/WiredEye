package com.muratcangzm.settings.ui

import androidx.compose.runtime.Immutable
import com.muratcangzm.utils.StringUtils
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface SettingsContract {

    @Immutable
    data class State(
        val isLoading: Boolean = false,
        val versionName: String = StringUtils.EMPTY,
        val buildType: String = StringUtils.EMPTY,
        val supportEmail: String = StringUtils.EMPTY,
        val userEmail: String? = null,
        val isPremium: Boolean = false,
    )

    sealed interface Event {
        data object OpenPrivacyPolicy : Event
        data object OpenTerms : Event
        data object OpenOpenSourceLicenses : Event
        data object OpenSupport : Event
        data object OpenAccount : Event
        data object OpenPremium : Event
        data object CopySupportEmail : Event
    }

    sealed interface Effect {
        data class OpenUrl(val url: String) : Effect
        data class Snackbar(val message: String) : Effect
        data class Toast(val message: String) : Effect
        data object NavigateAccount : Effect
        data object NavigatePremium : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
