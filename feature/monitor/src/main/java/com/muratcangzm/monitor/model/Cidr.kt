package com.muratcangzm.monitor.model

internal data class Cidr(
    val base: String,
    val prefix: Int,
    val info: AsnInfo
)