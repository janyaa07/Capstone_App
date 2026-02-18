package com.aireventure.auth

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET(".")
    fun getSensorData(): Call<List<SensorData>>
}
