package com.aireventure.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SensorViewModel : ViewModel() {

    private val _latest = MutableLiveData<SensorData?>()
    val latest: LiveData<SensorData?> = _latest

    fun refreshAws() {
        RetrofitClient.instance.getSensorData().enqueue(object : Callback<List<SensorData>> {
            override fun onResponse(
                call: Call<List<SensorData>>,
                response: Response<List<SensorData>>
            ) {
                if (response.isSuccessful) {
                    _latest.value = response.body()?.firstOrNull()
                }
            }

            override fun onFailure(call: Call<List<SensorData>>, t: Throwable) {
                // optional: _latest.value = null
            }
        })
    }
}
