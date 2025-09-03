package com.muratcangzm.network.engine

import com.muratcangzm.data.model.meta.PacketMeta
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class PacketEventBus {
    private val _events = MutableSharedFlow<PacketMeta>(
        replay = 0,
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<PacketMeta> = _events

    fun tryEmit(meta: PacketMeta) {
        _events.tryEmit(meta)
    }
}