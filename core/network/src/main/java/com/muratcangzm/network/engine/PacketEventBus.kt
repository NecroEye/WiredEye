package com.muratcangzm.network.engine

import android.util.Log
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
        val ok = _events.tryEmit(meta)
        Log.d("WE_FLOW", "EVENTBUS tryEmit ok=$ok bytes=${meta.bytes} uid=${meta.uid}")
    }
}