package com.aireventure.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment   // ✅ REQUIRED
import com.aireventure.auth.databinding.FragmentDataPanelBinding


import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DataPanelFragment : Fragment() {

    private var _binding: FragmentDataPanelBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchSensorData()
    }

    private fun fetchSensorData() {
        RetrofitClient.instance.getSensorData().enqueue(object :
            Callback<List<SensorData>> {

            override fun onResponse(
                call: Call<List<SensorData>>,
                response: Response<List<SensorData>>
            ) {
                if (response.isSuccessful) {
                    val data = response.body()
                    Log.d("AWS_DATA", "Received: $data")
                } else {
                    Log.e("AWS_ERROR", "Code: ${response.code()}")
                    Log.e("AWS_ERROR", "Error Body: ${response.errorBody()?.string()}")
                }
            }


            override fun onFailure(call: Call<List<SensorData>>, t: Throwable) {
                Log.e("AWS_ERROR", "Failure: ${t.message}")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

