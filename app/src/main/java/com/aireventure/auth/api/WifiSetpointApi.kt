package com.aireventure.auth.api

import com.aireventure.auth.model.SetpointRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface WifiSetpointApi {
    @POST("setpoint")
    @Headers("Content-Type: application/json")
    fun updateSetpoint(@Body body: SetpointRequest): Call<Void>
}