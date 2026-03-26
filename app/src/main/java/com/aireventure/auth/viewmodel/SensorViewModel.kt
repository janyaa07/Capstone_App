package com.aireventure.auth.viewmodel

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aireventure.auth.api.RetrofitClient
import com.aireventure.auth.model.SensorData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SensorViewModel : ViewModel() {

    private val _latest = MutableLiveData<SensorData?>()
    val latest: LiveData<SensorData?> = _latest

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshInterval = 3000L
    private var isRefreshing = false

    private val refreshRunnable = object : Runnable {
        override fun run() {
            fetchLatest()
            refreshHandler.postDelayed(this, refreshInterval)
        }
    }

    fun refreshAws() {
        if (!isRefreshing) {
            isRefreshing = true
            refreshHandler.post(refreshRunnable)
        } else {
            fetchLatest()
        }
    }

    private fun fetchLatest() {
        RetrofitClient.instance.getSensorData().enqueue(object : Callback<List<SensorData>> {
            override fun onResponse(
                call: Call<List<SensorData>>,
                response: Response<List<SensorData>>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    // ADD THIS LOG
                    Log.d("SENSOR_DATA", "Got ${body?.size} items")
                    body?.forEach {
                        Log.d("SENSOR_DATA", "ts=${it.timestamp} sp=${it.SpRmT_C}")
                    }
                    val latest = body
                        ?.maxByOrNull { item ->
                            item.timestamp
                                .replace("+00:00", "")
                                .replace("Z", "")
                                .trim()
                        }
                    Log.d("SENSOR_DATA", "Selected: ts=${latest?.timestamp} sp=${latest?.SpRmT_C}")
                    _latest.value = latest
                }
            }
            override fun onFailure(call: Call<List<SensorData>>, t: Throwable) {}
        })
    }

    override fun onCleared() {
        super.onCleared()
        isRefreshing = false
        refreshHandler.removeCallbacks(refreshRunnable)
    }
}