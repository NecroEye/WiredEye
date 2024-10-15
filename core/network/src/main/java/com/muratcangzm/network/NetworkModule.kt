package com.muratcangzm.network

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val cursedNetworkModule = module {

    single {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                var request = chain.request().newBuilder()
                    .addHeader("Authorization", "")
                    .build()

                //manipulate the request body to tweak the prompt
                request = request.newBuilder()
                    .method(request.method, request.body)
                    .build()

                chain.proceed(request)
            }
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                // Intercept and modify the response if necessary
                val responseBody = response.body.string()
                if (responseBody.contains("warning")) {

                    // Kill it before passing it to the app
                    val sanitizedResponse = responseBody.replace("warning", "")

                    return@addInterceptor response.newBuilder()
                        .body(sanitizedResponse.toResponseBody(response.body.contentType()))
                        .build()
                }
                response
            }.build()
    }

    single {
        Moshi.Builder().build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/v1/")
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }

    single {
        get<Retrofit>().create(OpenAIService::class.java)
    }
}