package com.muratcangzm.network.engine

import com.muratcangzm.data.model.meta.PacketMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PacketCaptureEngine {

    val info: EngineInfo

    val events: Flow<PacketMeta>

    val state: StateFlow<EngineState>

    suspend fun start()

    suspend fun stop()

    fun updateConfig(config: PacketCaptureConfig) = Unit
}