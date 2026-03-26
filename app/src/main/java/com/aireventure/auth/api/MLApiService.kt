package com.aireventure.auth.api

import com.aireventure.auth.model.MLResponse
import retrofit2.Call
import retrofit2.http.GET

interface MLApiService {
    @GET("ml-predictions")
    fun getPredictions(): Call<MLResponse>  // Change this line
}