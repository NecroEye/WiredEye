package com.muratcangzm.leaks.ui

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface LeaksContract {

    data class State(
        val isLoading: Boolean = false,
        val query: String = "",
        val selectedSeverity: Severity = Severity.All,
        val selectedTimeRange: TimeRange = TimeRange.Last24h,
    )

    sealed interface Event {
        data class SetQuery(val value: String) : Event
        data class SetSeverity(val value: Severity) : Event
        data class SetTimeRange(val value: TimeRange) : Event
        data object Refresh : Event
        data object OpenHelp : Event
    }

    sealed interface Effect {
        data class Toast(val message: String) : Effect
        data class NavigateLeakDetail(val leakId: String) : Effect
    }

    enum class Severity { All, Low, Medium, High }
    enum class TimeRange { Last1h, Last24h, Last7d }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
