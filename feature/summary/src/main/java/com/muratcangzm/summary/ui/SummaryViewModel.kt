package com.muratcangzm.summary.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SummaryViewModel : ViewModel(), SummaryContract.Presenter {

    private val _state = MutableStateFlow(
        SummaryContract.State(
            isLoading = false,
            today = SummaryContract.Today(
                totalBytes = 0L,
                leakCount = 0,
                trackerCount = 0,
                topAppLabel = "—"
            ),
            lastDays = buildFakeDays(),
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

    override fun onEvent(event: SummaryContract.Event) {
        when (event) {
            is SummaryContract.Event.SetWindow -> _state.update { it.copy(selectedWindow = event.window) }

            SummaryContract.Event.Refresh -> {
                _effects.tryEmit(SummaryContract.Effect.Toast("Refreshing…"))
                // ileride repo’dan gerçek veriyi çekip state’i set edeceğiz
            }

            is SummaryContract.Event.OpenDay -> {
                _effects.tryEmit(SummaryContract.Effect.NavigateDayDetail(event.dateLabel))
            }
        }
    }

    private fun buildFakeDays(): List<SummaryContract.DayItem> {
        // Skeleton hissi için “günler listesi” var ama sayılar 0
        return listOf(
            SummaryContract.DayItem("Mon", 0L, 0),
            SummaryContract.DayItem("Tue", 0L, 0),
            SummaryContract.DayItem("Wed", 0L, 0),
            SummaryContract.DayItem("Thu", 0L, 0),
            SummaryContract.DayItem("Fri", 0L, 0),
            SummaryContract.DayItem("Sat", 0L, 0),
            SummaryContract.DayItem("Sun", 0L, 0),
        )
    }
}
