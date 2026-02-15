package com.muratcangzm.monitor

import androidx.compose.runtime.Immutable
import com.muratcangzm.monitor.common.SpeedMode
import com.muratcangzm.monitor.common.ViewMode
import com.muratcangzm.shared.model.network.UiPacket
import com.muratcangzm.utils.StringUtils
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MonitorContract {

    @Immutable
    data class State(
        val isEngineRunning: Boolean = false,
        val items: List<UiPacket> = emptyList(),
        val filterText: String = StringUtils.EMPTY,
        val minBytes: Long = 0L,
        val windowMillis: Long = 10_000L,
        val totalBytes: Long = 0L,
        val uniqueApps: Int = 0,
        val pps: Double = 0.0,
        val throughputKbs: Double = 0.0,
        val pinnedUids: Set<Int> = emptySet(),
        val anomalyKeys: Set<String> = emptySet(),
        val speedMode: SpeedMode = SpeedMode.ECO,
        val viewMode: ViewMode = ViewMode.RAW,
    )

    sealed interface Event {
        data class SetFilter(val text: String) : Event
        data class SetMinBytes(val value: Long) : Event
        data class SetWindow(val millis: Long) : Event
        data object StartEngine : Event
        data object StopEngine : Event
        data object ClearFilter : Event
        data class TogglePin(val uid: Int) : Event
        data class ToggleMute(val uid: Int) : Event
        data class SetSpeed(val mode: SpeedMode) : Event
        data class SetViewMode(val mode: ViewMode) : Event
        data object ClearNow : Event
    }

    sealed interface Effect {
        data class Snackbar(val message: String) : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
