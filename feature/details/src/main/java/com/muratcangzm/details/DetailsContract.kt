package com.muratcangzm.details

import com.muratcangzm.common.nav.Screens
import com.muratcangzm.shared.model.UiPacket
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface DetailsContract {

    data class State(
        val uiPacket: UiPacket = UiPacket.EMPTY,
        val isRawExpanded: Boolean = false,
    )

    sealed interface Event {
        data class SetArguments(val arguments: Screens.DetailsScreen) : Event
        data object ToggleRawExpanded : Event
    }

    sealed interface Effect {
        data class Toast(val message: String) : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
