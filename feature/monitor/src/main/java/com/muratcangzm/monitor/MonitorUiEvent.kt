package com.muratcangzm.monitor

import com.muratcangzm.monitor.common.SpeedMode
import com.muratcangzm.monitor.common.ViewMode

sealed interface MonitorUiEvent {
    data class SetFilter(val text: String) : MonitorUiEvent
    data class SetMinBytes(val value: Long) : MonitorUiEvent
    data class SetWindow(val millis: Long) : MonitorUiEvent
    data object StartEngine : MonitorUiEvent
    data object StopEngine : MonitorUiEvent
    data object ClearFilter : MonitorUiEvent
    data class TogglePin(val uid: Int) : MonitorUiEvent
    data class ToggleMute(val uid: Int) : MonitorUiEvent
    data class SetSpeed(val mode: SpeedMode) : MonitorUiEvent
    data class SetViewMode(val mode: ViewMode) : MonitorUiEvent
}