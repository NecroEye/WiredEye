package com.muratcangzm.settings.ui

import androidx.lifecycle.ViewModel
import com.muratcangzm.model.AppBuildInfo
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val buildInfo: AppBuildInfo,
    private val privacyUrl: String,
    private val termsUrl: String,
    private val supportEmail: String
) : ViewModel(), SettingsContract.Presenter {

    private val _state = MutableStateFlow(
        SettingsContract.State(
            versionName = buildInfo.versionName,
            buildType = buildInfo.buildType,
        )
    )
    override val state: StateFlow<SettingsContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val effects: SharedFlow<SettingsContract.Effect> = _effects.asSharedFlow()

    override fun onEvent(event: SettingsContract.Event) {
        when (event) {
            SettingsContract.Event.OpenPrivacyPolicy -> _effects.tryEmit(
                SettingsContract.Effect.OpenUrl(privacyUrl)
            )

            SettingsContract.Event.OpenTerms -> _effects.tryEmit(
                SettingsContract.Effect.OpenUrl(termsUrl)
            )

            SettingsContract.Event.OpenOpenSourceLicenses -> {
                _effects.tryEmit(SettingsContract.Effect.OpenUrl("$privacyUrl#open-source"))
            }

            SettingsContract.Event.OpenSupport -> _effects.tryEmit(
                SettingsContract.Effect.OpenUrl("mailto:$supportEmail")
            )

            SettingsContract.Event.CopySupportEmail -> _effects.tryEmit(
                SettingsContract.Effect.Snackbar("Support email copied.")
            )

            SettingsContract.Event.OpenAccount -> _effects.tryEmit(SettingsContract.Effect.NavigateAccount)
            SettingsContract.Event.OpenPremium -> _effects.tryEmit(SettingsContract.Effect.NavigatePremium)
        }
    }
}
