package com.muratcangzm.data.common

import androidx.room.ColumnInfo

data class TopHost(
    @ColumnInfo(name = "host")
    val host: String,
    @ColumnInfo(name = "c")
    val c: Long
)
