package com.muratcangzm.monitor.model

import com.muratcangzm.data.model.meta.PacketMeta

data class Aggregate(
    var totalBytes: Long,
    var latest: PacketMeta
)
