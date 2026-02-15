package com.muratcangzm.leaks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muratcangzm.core.leak.LeakAnalyzerBridge
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LeaksViewModel(
    private val leakAnalyzerBridge: LeakAnalyzerBridge
) : ViewModel(), LeaksContract.Presenter {

    private val _state = MutableStateFlow(LeaksContract.State())
    override val state: StateFlow<LeaksContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LeaksContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val effects: SharedFlow<LeaksContract.Effect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            leakAnalyzerBridge.snapshot.collectLatest { snap ->
                _state.update { current ->
                    val q = current.query.trim()
                    val domains = snap.topDomains.filterQuery(q) { it.domain }
                    val servers = snap.topServers.filterQuery(q) { it.ip }

                    current.copy(
                        isLoading = false,
                        score = snap.score,
                        totalQueries = snap.totalQueries,
                        uniqueDomains = snap.uniqueDomains,
                        publicDnsRatio = snap.publicDnsRatio,
                        suspiciousEntropyQueries = snap.suspiciousEntropyQueries,
                        burstQueries = snap.burstQueries,
                        topDomains = domains,
                        topServers = servers
                    )
                }
            }
        }

        viewModelScope.launch {
            leakAnalyzerBridge.emitSnapshot(force = true)
        }
    }

    override fun onEvent(event: LeaksContract.Event) {
        when (event) {
            is LeaksContract.Event.SetQuery -> {
                _state.update { it.copy(query = event.value) }
                leakAnalyzerBridge.emitSnapshot(force = true)
            }

            is LeaksContract.Event.SetSeverity -> {
                _state.update { it.copy(selectedSeverity = event.value) }
                leakAnalyzerBridge.emitSnapshot(force = true)
            }

            is LeaksContract.Event.SetTimeRange -> {
                _state.update { it.copy(selectedTimeRange = event.value) }
                leakAnalyzerBridge.setWindowMillis(event.value.windowMillis)
            }

            LeaksContract.Event.Refresh -> {
                leakAnalyzerBridge.emitSnapshot(force = true)
            }

            LeaksContract.Event.OpenHelp -> {
                val ratio = (_state.value.publicDnsRatio * 100.0).roundToInt()
                _effects.tryEmit(
                    LeaksContract.Effect.Toast(
                        "Score=${_state.value.score} | Total=${_state.value.totalQueries} | PublicDNS=$ratio%"
                    )
                )
            }
        }
    }

    private inline fun <T> List<T>.filterQuery(query: String, crossinline key: (T) -> String): List<T> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return this
        return filter { key(it).lowercase().contains(q) }
    }
}
