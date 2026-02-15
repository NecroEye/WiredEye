package com.muratcangzm.summary.ui

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

class SummaryViewModel(
    private val leakAnalyzerBridge: LeakAnalyzerBridge
) : ViewModel(), SummaryContract.Presenter {

    private val _state = MutableStateFlow(
        SummaryContract.State(
            isLoading = false,
            today = SummaryContract.Today(
                totalBytes = 0L,
                leakCount = 0,
                trackerCount = 0,
                topAppLabel = "—"
            ),
            lastDays = emptyList(),
            selectedWindow = SummaryContract.Window.Week
        )
    )
    override val state: StateFlow<SummaryContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SummaryContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val effects: SharedFlow<SummaryContract.Effect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            leakAnalyzerBridge.snapshot.collectLatest { snap ->
                _state.update { current ->
                    val publicDnsPct = (snap.publicDnsRatio * 100.0).roundToInt()
                    current.copy(
                        isLoading = false,
                        today = current.today.copy(
                            leakCount = snap.score,
                            trackerCount = publicDnsPct,
                            topAppLabel = snap.topDomains.firstOrNull()?.domain ?: "—"
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            leakAnalyzerBridge.emitSnapshot(force = true)
        }
    }

    override fun onEvent(event: SummaryContract.Event) {
        when (event) {
            is SummaryContract.Event.SetWindow -> {
                _state.update { it.copy(selectedWindow = event.window) }
                val windowMs = when (event.window) {
                    SummaryContract.Window.Day -> 24 * 60 * 60 * 1000L
                    SummaryContract.Window.Week -> 7 * 24 * 60 * 60 * 1000L
                    SummaryContract.Window.Month -> 30 * 24 * 60 * 60 * 1000L
                }
                leakAnalyzerBridge.setWindowMillis(windowMs)
            }

            SummaryContract.Event.Refresh -> {
                leakAnalyzerBridge.emitSnapshot(force = true)
            }

            is SummaryContract.Event.OpenDay -> {
                _effects.tryEmit(SummaryContract.Effect.NavigateDayDetail(event.dateLabel))
            }
        }
    }
}
