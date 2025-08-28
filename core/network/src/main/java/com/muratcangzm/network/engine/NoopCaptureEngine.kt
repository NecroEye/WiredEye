package com.muratcangzm.network.engine

import com.muratcangzm.data.model.meta.PacketMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NoopCaptureEngine : PacketCaptureEngine {
    private val _state = MutableStateFlow<EngineState>(EngineState.Stopped)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    override val info: EngineInfo = EngineInfo(
        id = "noop",
        mode = EngineMode.StatsOnly,
        capabilities = emptySet()
    )

    override val events: Flow<PacketMeta> = kotlinx.coroutines.flow.emptyFlow()

    override suspend fun start() {
        _state.value = EngineState.Running
    }

    override suspend fun stop() {
        _state.value = EngineState.Stopped
    }
}