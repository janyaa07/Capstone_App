package com.aireventure.auth.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WifiSetpointClient {
    // ← put your API Gateway URL here
    private const val BASE_URL = "https://yqv85v1a8k.execute-api.ap-southeast-2.amazonaws.com/prod/"


    val instance: WifiSetpointApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WifiSetpointApi::class.java)  // ← must be WifiSetpointApi not ApiService
    }
}