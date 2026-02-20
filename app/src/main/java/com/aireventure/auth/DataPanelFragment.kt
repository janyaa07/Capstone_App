package com.aireventure.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aireventure.auth.databinding.FragmentDataPanelBinding
import android.bluetooth.BluetoothSocket


import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DataPanelFragment : Fragment() {

    private var _binding: FragmentDataPanelBinding? = null
    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshInterval = 5000L // 5 seconds
    private val binding get() = _binding!!

    private val refreshRunnable = object : Runnable {
        override fun run() {
            fetchSensorData()
            refreshHandler.postDelayed(this, refreshInterval)
        }
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var isReading = true

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

        // Start AWS auto refresh
        refreshHandler.post(refreshRunnable)

        // Bluetooth
        bluetoothSocket = BluetoothConnection.socket
        startBluetoothReading()
    }

    // ---------------- BLUETOOTH SECTION ----------------

    private fun startBluetoothReading() {

        if (bluetoothSocket == null) {
            binding.liveDataText.text = "Bluetooth Not Connected"
            return
        }

        Thread {
            try {
                val reader = bluetoothSocket!!.inputStream.bufferedReader()

                while (isReading) {
                    val line = reader.readLine()

                    if (line != null) {

                        activity?.runOnUiThread {

                            binding.liveDataText.text = line

                            parseAndDisplayBluetooth(line)
                        }
                    }
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    binding.liveDataText.text = "Bluetooth Disconnected"
                }
            }
        }.start()
    }

    private fun parseAndDisplayBluetooth(data: String) {

        val parts = data.split(";")

        for (part in parts) {
            val keyValue = part.split(":")
            if (keyValue.size == 2) {

                when (keyValue[0]) {

                    "AbsAirFlow_m3h" -> {
                        binding.btAirFlowText.text =
                            "BT Air Flow: ${keyValue[1]} m³/h"
                    }

                    "RmT_C" -> {
                        binding.btRoomTempText.text =
                            "BT Room Temp: ${keyValue[1]} °C"
                    }

                    "SpRmT_C" -> {
                        binding.setpointText.text =
                            "BT Setpoint: ${keyValue[1]} °C"
                    }
                }
            }
        }
    }

    // ---------------- AWS SECTION ----------------

    private fun fetchSensorData() {

        RetrofitClient.instance.getSensorData().enqueue(object :
            Callback<List<SensorData>> {

            override fun onResponse(
                call: Call<List<SensorData>>,
                response: Response<List<SensorData>>
            ) {
                if (response.isSuccessful) {

                    val dataList = response.body()

                    if (!dataList.isNullOrEmpty()) {

                        val latestData = dataList[0]

                        // Adjust these field names EXACTLY to match your SensorData class

                        binding.awsAirFlowText.text =
                            "AWS Air Flow: ${latestData.AbsAirFlow_cfm} m³/h"

                        binding.awsRoomTempText.text =
                            "AWS Room Temp: ${latestData.RmT_C} °C"

                        binding.awsSetpointText.text =
                            "AWS Setpoint: ${latestData.SpRmT_C} °C"

                        Log.d("AWS_DATA", "Displayed: $latestData")
                    }

                } else {
                    Log.e("AWS_ERROR", "Code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<SensorData>>, t: Throwable) {
                Log.e("AWS_ERROR", "Failure: ${t.message}")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()

        isReading = false
        refreshHandler.removeCallbacks(refreshRunnable)

        _binding = null
    }
}
