package com.muratcangzm.network.model

import com.squareup.moshi.JsonClass
@JsonClass(generateAdapter = true)
data class AIResponse(
    val choices:List<AIChoice>
) {
    @JsonClass(generateAdapter = true)
    data class AIChoice(val text:String)
}