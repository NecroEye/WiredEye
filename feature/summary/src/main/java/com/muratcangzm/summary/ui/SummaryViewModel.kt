package com.muratcangzm.summary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muratcangzm.core.leak.LeakAnalyzerBridge
import com.muratcangzm.data.dao.DnsEventDao
import com.muratcangzm.data.dao.PacketLogDao
import com.muratcangzm.data.dao.TopAppByBytesRow
import com.muratcangzm.data.helper.UidResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SummaryViewModel(
    private val leakAnalyzerBridge: LeakAnalyzerBridge,
    private val packetLogDao: PacketLogDao,
    private val dnsEventDao: DnsEventDao,
    private val uidResolver: UidResolver
) : ViewModel(), SummaryContract.Presenter {

    private val selectedWindow = MutableStateFlow(SummaryContract.Window.Week)

    private var refreshStartedAtMs: Long = 0L
    private val minShimmerMs = 320L
    private val isRefreshing = MutableStateFlow(false)
    private var refreshToken: Long = 0L

    private val nowMillisFlow: StateFlow<Long> =
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(System.currentTimeMillis())
                kotlinx.coroutines.delay(750L)
            }
        }.stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            System.currentTimeMillis()
        )

    private val windowMillisFlow: StateFlow<Long> =
        selectedWindow
            .map { window ->
                when (window) {
                    SummaryContract.Window.Day -> 24 * 60 * 60 * 1000L
                    SummaryContract.Window.Week -> 7 * 24 * 60 * 60 * 1000L
                    SummaryContract.Window.Month -> 30 * 24 * 60 * 60 * 1000L
                }
            }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                7 * 24 * 60 * 60 * 1000L
            )

    private val rangeFlow: StateFlow<Pair<Long, Long>> =
        combine(nowMillisFlow, windowMillisFlow) { nowMillis, windowMillis ->
            val fromMillis = (nowMillis - windowMillis).coerceAtLeast(0L)
            Pair(fromMillis, nowMillis)
        }.distinctUntilChanged()
            .stateIn(
                viewModelScope,
                kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                Pair(0L, System.currentTimeMillis())
            )

    private val todayBytesFlow: StateFlow<Long> =
        rangeFlow
            .flatMapLatestCompat { (fromMillis, toMillis) ->
                packetLogDao.totalBytesBetweenFlow(fromMillis, toMillis)
            }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                0L
            )

    private val topAppFlow: StateFlow<TopAppByBytesRow?> =
        rangeFlow
            .flatMapLatestCompat { (fromMillis, toMillis) ->
                packetLogDao.topAppByBytesFlow(fromMillis, toMillis)
            }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                null
            )

    private val lastDaysFlow: StateFlow<List<SummaryContract.DayItem>> =
        rangeFlow
            .flatMapLatestCompat { (fromMillis, toMillis) ->
                combine(
                    packetLogDao.bytesByDayFlow(fromMillis, toMillis, limit = 14),
                    dnsEventDao.countByDayFlow(fromMillis, toMillis, limit = 14)
                ) { packetRows, dnsRows ->
                    val bytesByDay = packetRows.associateBy({ it.dateLabel }, { it.totalBytes })
                    val leaksByDay = dnsRows.associateBy({ it.dateLabel }, { it.leakCount })
                    val dateLabels = (bytesByDay.keys + leaksByDay.keys).distinct().sortedDescending()
                    dateLabels.map { dateLabel ->
                        SummaryContract.DayItem(
                            dateLabel = dateLabel,
                            totalBytes = bytesByDay[dateLabel] ?: 0L,
                            leakCount = leaksByDay[dateLabel] ?: 0
                        )
                    }
                }
            }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    private val _state = MutableStateFlow(SummaryContract.State())
    override val state: StateFlow<SummaryContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SummaryContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val effects: SharedFlow<SummaryContract.Effect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            leakAnalyzerBridge.snapshot.collect { snapshot ->
                val publicDnsPercent = (snapshot.publicDnsRatio * 100.0).roundToInt()
                _state.update { current ->
                    current.copy(
                        isLoading = current.isLoading,
                        today = current.today.copy(
                            leakCount = snapshot.score,
                            trackerCount = publicDnsPercent,
                            topAppLabel = snapshot.topDomains.firstOrNull()?.domain ?: "—"
                        )
                    )
                }

                if (refreshStartedAtMs != 0L) {
                    val elapsed = System.currentTimeMillis() - refreshStartedAtMs
                    if (elapsed < minShimmerMs) delay(minShimmerMs - elapsed)
                    refreshStartedAtMs = 0L
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }

        viewModelScope.launch {
            combine(
                todayBytesFlow,
                topAppFlow,
                lastDaysFlow,
                selectedWindow,
                isRefreshing
            ) { totalBytes, topAppRow, lastDays, selected, refreshing ->
                val resolvedTopAppLabel = resolveTopAppLabel(topAppRow)
                SummaryContract.State(
                    isLoading = refreshing,
                    today = SummaryContract.Today(
                        totalBytes = totalBytes,
                        leakCount = state.value.today.leakCount,
                        trackerCount = state.value.today.trackerCount,
                        topAppLabel = resolvedTopAppLabel.ifBlank { state.value.today.topAppLabel }
                    ),
                    lastDays = lastDays,
                    selectedWindow = selected
                )
            }.collect { newState ->
                _state.value = newState
            }
        }

        viewModelScope.launch {
            leakAnalyzerBridge.emitSnapshot(force = true)
        }
    }

    override fun onEvent(event: SummaryContract.Event) {
        when (event) {
            is SummaryContract.Event.SetWindow -> {
                selectedWindow.value = event.window
                leakAnalyzerBridge.setWindowMillis(windowMillisFlow.value)
            }

            SummaryContract.Event.Refresh -> {
                val token = System.nanoTime()
                refreshToken = token

                isRefreshing.value = true
                _effects.tryEmit(SummaryContract.Effect.Snackbar("Syncing metrics…"))

                leakAnalyzerBridge.emitSnapshot(force = true)

                viewModelScope.launch {
                    delay(minShimmerMs)
                    if (refreshToken == token) {
                        isRefreshing.value = false
                    }
                }
            }

            is SummaryContract.Event.OpenDay -> {
                _effects.tryEmit(SummaryContract.Effect.NavigateDayDetail(event.dateLabel))
            }
        }
    }

    private fun resolveTopAppLabel(row: TopAppByBytesRow?): String {
        val uid = row?.uid
        val packageName = row?.packageName
        if (uid == null && packageName.isNullOrBlank()) return ""
        val label = if (uid != null) runCatching { uidResolver.labelFor(uid) }.getOrNull() else null
        return when {
            !label.isNullOrBlank() && !packageName.isNullOrBlank() -> "$label · $packageName"
            !label.isNullOrBlank() -> label
            !packageName.isNullOrBlank() -> packageName
            uid != null -> "uid:$uid"
            else -> ""
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private inline fun <T, R> StateFlow<T>.flatMapLatestCompat(
    crossinline transform: suspend (T) -> Flow<R>
): Flow<R> = this.flatMapLatest { value -> transform(value) }
