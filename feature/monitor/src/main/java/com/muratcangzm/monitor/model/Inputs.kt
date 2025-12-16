package com.muratcangzm.monitor.model

import androidx.compose.runtime.Immutable
import com.muratcangzm.data.model.meta.PacketMeta

@Immutable
data class Inputs(
    val packets: List<PacketMeta>,
    val controls: Controls,
    val pinned: Set<Int>,
    val muted: Set<Int>
)
