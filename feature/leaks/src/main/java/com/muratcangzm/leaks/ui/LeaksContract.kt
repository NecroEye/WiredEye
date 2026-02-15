package com.muratcangzm.leaks.ui

import com.muratcangzm.shared.model.leak.TopDomain
import com.muratcangzm.shared.model.leak.TopServer
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface LeaksContract {

    data class State(
        val isLoading: Boolean = false,
        val query: String = "",
        val selectedSeverity: Severity = Severity.All,
        val selectedTimeRange: TimeRange = TimeRange.Last24h,
        val score: Int = 0,
        val totalQueries: Long = 0L,
        val uniqueDomains: Int = 0,
        val publicDnsRatio: Double = 0.0,
        val suspiciousEntropyQueries: Long = 0L,
        val burstQueries: Long = 0L,
        val topDomains: List<TopDomain> = emptyList(),
        val topServers: List<TopServer> = emptyList()
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

    enum class TimeRange(val windowMillis: Long) {
        Last1h(60 * 60 * 1000L),
        Last24h(24 * 60 * 60 * 1000L),
        Last7d(7 * 24 * 60 * 60 * 1000L)
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
