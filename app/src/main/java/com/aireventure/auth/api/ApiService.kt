package com.aireventure.auth.api

import com.aireventure.auth.model.SensorData
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET(".")
    fun getSensorData(): Call<List<SensorData>>
}