package com.muratcangzm.monitor

sealed interface MonitorUiEvent {
    data class SetFilter(val text: String) : MonitorUiEvent
    data class SetMinBytes(val value: Long) : MonitorUiEvent
    data class SetWindow(val millis: Long) : MonitorUiEvent
    data object StartEngine : MonitorUiEvent
    data object StopEngine : MonitorUiEvent
    data object ClearFilter : MonitorUiEvent
}