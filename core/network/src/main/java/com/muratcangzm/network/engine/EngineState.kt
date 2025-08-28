package com.muratcangzm.network.engine

sealed class EngineState {
    data object Stopped : EngineState()
    data object Starting : EngineState()
    data object Running : EngineState()
    data class Failed(val cause: Throwable) : EngineState()
}