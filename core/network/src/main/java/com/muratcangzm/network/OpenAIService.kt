package com.muratcangzm.network

import com.muratcangzm.network.model.AIRequest
import com.muratcangzm.network.model.AIResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAIService {

    @POST("completions")
    suspend fun generateResponse(
        @Body request: AIRequest
    ): AIResponse
}