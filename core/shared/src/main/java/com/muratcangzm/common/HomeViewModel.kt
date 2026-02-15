package com.muratcangzm.common

import androidx.lifecycle.ViewModel
import com.muratcangzm.common.nav.NavigationData
import com.muratcangzm.common.nav.Screens
import com.muratcangzm.shared.model.network.UiPacket
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class HomeViewModel: ViewModel() {
    private val _navigationEvents = MutableSharedFlow<NavigationData>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val navigationEvents: SharedFlow<NavigationData> = _navigationEvents.asSharedFlow()

    fun openDetails(uiPacket: UiPacket) {
        _navigationEvents.tryEmit(
            NavigationData(destination = Screens.DetailsScreen(uiPacket))
        )
    }

    fun openMonitorAsRoot() {
        _navigationEvents.tryEmit(
            NavigationData(
                destination = Screens.WiredEyeScreen,
                popUpTo = Screens.WiredEyeScreen,
                popUpToInclusive = true
            )
        )
    }
}