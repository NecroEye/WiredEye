package com.muratcangzm.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AIRequest(
    val prompt:String,
    val max_tokens:Int = 150,
    val temperature:Float = 0.7f
)