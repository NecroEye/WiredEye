package com.muratcangzm.monitor.model

import androidx.compose.runtime.Immutable

@Immutable
data class Controls(
    val windowMillis: Long,
    val query: String,
    val minBytes: Long,
    val terms: List<String>,
    val clearAfterMillis: Long
)
