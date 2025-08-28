package com.muratcangzm.monitor.common

import com.muratcangzm.data.model.meta.PacketMeta

data class UiPacket(
    val key: String,
    val time: String,
    val app: String,
    val proto: String,
    val from: String,
    val to: String,
    val bytesLabel: String,
    val raw: PacketMeta
)
