package com.muratcangzm.summary.ui

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface SummaryContract {

    data class State(
        val isLoading: Boolean = false,
        val today: Today = Today(),
        val lastDays: List<DayItem> = emptyList(),
        val selectedWindow: Window = Window.Week,
    )

    data class Today(
        val totalBytes: Long = 0L,
        val leakCount: Int = 0,
        val trackerCount: Int = 0,
        val topAppLabel: String = "—",
    )

    data class DayItem(
        val dateLabel: String,
        val totalBytes: Long,
        val leakCount: Int,
    )

    enum class Window { Day, Week, Month }

    sealed interface Event {
        data class SetWindow(val window: Window) : Event
        data object Refresh : Event
        data class OpenDay(val dateLabel: String) : Event
    }

    sealed interface Effect {
        data class Toast(val message: String) : Effect
        data class NavigateDayDetail(val dateLabel: String) : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
