package com.aireventure.auth.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL =
        "https://2rdlkwrx7f.execute-api.ap-southeast-2.amazonaws.com/deploy/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

object MLRetrofitClient {
    private const val BASE_URL =
        "https://okniqync5g.execute-api.ap-southeast-2.amazonaws.com/prod/"

    val instance: MLApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MLApiService::class.java)
    }
}
