package com.muratcangzm.details

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DetailsViewModel : ViewModel(), DetailsContract.Presenter {

    private val _state = MutableStateFlow(DetailsContract.State())
    override val state: StateFlow<DetailsContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<DetailsContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val effects: SharedFlow<DetailsContract.Effect> = _effects.asSharedFlow()

    override fun onEvent(event: DetailsContract.Event) {
        when (event) {
            is DetailsContract.Event.SetArguments -> {
                _state.update { it.copy(uiPacket = event.arguments.uiPacket) }
            }

            DetailsContract.Event.ToggleRawExpanded -> {
                _state.update { it.copy(isRawExpanded = !it.isRawExpanded) }
            }
        }
    }
}
