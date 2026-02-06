package com.muratcangzm.leaks.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LeaksViewModel : ViewModel(), LeaksContract.Presenter {

    private val _state = MutableStateFlow(LeaksContract.State())
    override val state: StateFlow<LeaksContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LeaksContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val effects: SharedFlow<LeaksContract.Effect> = _effects.asSharedFlow()

    override fun onEvent(event: LeaksContract.Event) {
        when (event) {
            is LeaksContract.Event.SetQuery ->
                _state.update { it.copy(query = event.value) }

            is LeaksContract.Event.SetSeverity ->
                _state.update { it.copy(selectedSeverity = event.value) }

            is LeaksContract.Event.SetTimeRange ->
                _state.update { it.copy(selectedTimeRange = event.value) }

            LeaksContract.Event.Refresh -> {
                // şimdilik placeholder
                _effects.tryEmit(LeaksContract.Effect.Toast("Refreshing…"))
            }

            LeaksContract.Event.OpenHelp -> {
                _effects.tryEmit(LeaksContract.Effect.Toast("Privacy Leak Detector: coming soon"))
            }
        }
    }
}
