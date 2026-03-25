package com.aireventure.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aireventure.auth.databinding.FragmentDataPanelBinding
import android.bluetooth.BluetoothSocket


import android.util.Log
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.sqrt


class DataPanelFragment : Fragment() {

    private var _binding: FragmentDataPanelBinding? = null
    private val binding get() = _binding!!
    private var currentSetpoint = BluetoothConnection.currentSetpoint
    private var userIsChanging = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var setpointJob: Runnable? = null

    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshInterval = 5000L
    private var bluetoothSocket: BluetoothSocket? = null
    private var isReading = true

    private val refreshRunnable = object : Runnable {
        override fun run() {
            fetchSensorData()
            refreshHandler.postDelayed(this, refreshInterval)
        }
    }

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

        //currentSetpoint = BluetoothConnection.currentSetpoint
        //binding.tvCurrentSetpoint.text = "Set To: ${"%.1f".format(currentSetpoint)}°C"

        refreshHandler.post(refreshRunnable)
        bluetoothSocket = BluetoothConnection.socket
        startBluetoothReading()

        //binding.btnAdd.setOnClickListener {
            //currentSetpoint += 0.5f
            //BluetoothConnection.currentSetpoint = currentSetpoint
            //userIsChanging = true
            //binding.tvCurrentSetpoint.text = "Set To: ${"%.1f".format(currentSetpoint)}°C"
            //setpointJob?.let { handler.removeCallbacks(it) }
            //setpointJob = Runnable {
                //sendSetpoint(currentSetpoint)
                //userIsChanging = false
            //}
            //handler.postDelayed(setpointJob!!, 1000)
        //}

        //binding.btnMinus.setOnClickListener {
            //currentSetpoint -= 0.5f
            //BluetoothConnection.currentSetpoint = currentSetpoint
            //userIsChanging = true
            //binding.tvCurrentSetpoint.text = "Set To: ${"%.1f".format(currentSetpoint)}°C"
            //setpointJob?.let { handler.removeCallbacks(it) }
            //setpointJob = Runnable {
                //sendSetpoint(currentSetpoint)
                //userIsChanging = false
            //}
            //handler.postDelayed(setpointJob!!, 1000)
        //}
    }

    //private fun sendSetpoint(value: Float) {
        //Thread {
            //try {
                //val command = "SET:SpRmT_C:$value\n"
                //BluetoothConnection.socket?.outputStream?.write(command.toByteArray())
                //BluetoothConnection.socket?.outputStream?.flush()
                //Log.d("BT_SEND", "Sent: $command")
            //} catch (e: Exception) {
                //Log.e("BT_SEND", "Failed: $e")
            //}
        //}.start()
    //}

    private fun startBluetoothReading() {
        if (bluetoothSocket == null) {
            //binding.liveDataText.text = "Bluetooth Not Connected"
            return
        }
        Thread {
            try {
                val reader = bluetoothSocket!!.inputStream.bufferedReader()
                while (isReading) {
                    val line = reader.readLine()
                    if (line != null) {
                        activity?.runOnUiThread {
                            //binding.liveDataText.text = line
                            parseAndDisplayBluetooth(line)
                        }
                    }
                }
            } catch (e: Exception) {
                //activity?.runOnUiThread {
                    //binding.liveDataText.text = "Bluetooth Disconnected"
                    Log.e("BT_READ", "Bluetooth Disconnected: ${e.message}")
                //}
            }
        }.start()
    }

    private fun parseAndDisplayBluetooth(data: String) {
        val parts = data.split(";")
        for (part in parts) {
            val keyValue = part.split(":")
            if (keyValue.size == 2) {
                when (keyValue[0]) {
                    //"AbsAirFlow_m3h" -> {
                        //binding.btAirFlowText.text = "BT Air Flow: ${keyValue[1]} m³/h"
                    //}
                    //"RmT_C" -> {
                        //binding.btRoomTempText.text = "BT Room Temp: ${keyValue[1]} °C"
                    //}
                    "SpRmT_C" -> {
                        //binding.setpointText.text = "BT Setpoint: ${keyValue[1]} °C"
                        if (!userIsChanging) {
                            keyValue[1].toFloatOrNull()?.let { currentSetpoint = it
                                BluetoothConnection.currentSetpoint = it }
                        }
                    }
                }
            }
        }
    }

    private fun fetchSensorData() {
        RetrofitClient.instance.getSensorData().enqueue(object : Callback<List<SensorData>> {
            override fun onResponse(call: Call<List<SensorData>>, response: Response<List<SensorData>>) {
                if (response.isSuccessful) {
                    val dataList = response.body()
                    if (!dataList.isNullOrEmpty()) {
                        //val latestData = dataList[0]
                        //binding.awsAirFlowText.text = "AWS Air Flow: ${latestData.AbsAirFlow_cfm} m³/h"
                        //binding.awsRoomTempText.text = "AWS Room Temp: ${latestData.RmT_C} °C"
                        //binding.awsSetpointText.text = "AWS Setpoint: ${latestData.SpRmT_C} °C"
                        renderTemperatureChart(dataList)
                        renderPressureChart(dataList)
                        renderDischargeAirflowTimeChart(dataList)
                        renderDischargeAirflowPressureChart(dataList)
                        Log.d("AWS_DATA", "Chart updated with ${dataList.size} points")
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

    private fun calculateDischargeAirflow(deltaPa: Double): Double {
        if (deltaPa <= 0.0) return 0.0
        return Math.PI * sqrt((deltaPa * 2.0) / 1.225) * (0.099 * 0.099)
    }

    private fun renderTemperatureChart(dataList: List<SensorData>) {
        val sortedList = dataList.sortedBy { it.timestamp }
        val latestTen = sortedList.takeLast(10)

        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        latestTen.forEachIndexed { index, item ->
            entries.add(Entry(index.toFloat(), item.RmT_C.toFloat()))
            labels.add(formatTimeLabel(item.timestamp))
        }

        val dataSet = LineDataSet(entries, "Room Temp (°C)").apply {
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
        }

        binding.temperatureChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = true
            axisRight.isEnabled = false

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)

            animateX(600)
            invalidate()
        }
    }

    private fun renderPressureChart(dataList: List<SensorData>) {
        val sortedList = dataList.sortedBy { it.timestamp }
        val latestTen = sortedList.takeLast(10)

        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        latestTen.forEachIndexed { index, item ->
            entries.add(Entry(index.toFloat(), item.DeltaP_Pa.toFloat()))
            labels.add(formatTimeLabel(item.timestamp))
        }

        val dataSet = LineDataSet(entries, "Pressure (Pa)").apply {
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
        }

        binding.pressureChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = true
            axisRight.isEnabled = false

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)

            animateX(600)
            invalidate()
        }
    }

    private fun renderDischargeAirflowTimeChart(dataList: List<SensorData>) {
        val sortedList = dataList.sortedBy { it.timestamp }
        val latestTen = sortedList.takeLast(10)

        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        latestTen.forEachIndexed { index, item ->
            val dischargeAirflow = calculateDischargeAirflow(item.DeltaP_Pa)
            entries.add(Entry(index.toFloat(), dischargeAirflow.toFloat()))
            labels.add(formatTimeLabel(item.timestamp))
        }

        val dataSet = LineDataSet(entries, "Discharge Airflow").apply {
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
        }

        binding.dischargeAirflowChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = true
            axisRight.isEnabled = false

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)

            animateX(600)
            invalidate()
        }
    }

    private fun renderDischargeAirflowPressureChart(dataList: List<SensorData>) {
        val sortedList = dataList.sortedBy { it.timestamp }
        val latestTen = sortedList.takeLast(10)

        val entries = ArrayList<Entry>()

        latestTen.forEach { item ->
            val dischargeAirflow = calculateDischargeAirflow(item.DeltaP_Pa)
            entries.add(
                Entry(
                    item.DeltaP_Pa.toFloat(),
                    dischargeAirflow.toFloat()
                )
            )
        }

        val dataSet = ScatterDataSet(entries, "Discharge Airflow").apply {
            setDrawValues(false)
            setScatterShapeSize(8f)
            setScatterShape(ScatterChart.ScatterShape.CIRCLE)
            color = android.graphics.Color.BLACK
        }

        binding.dischargeAirflowPressureChart.apply {
            data = ScatterData(dataSet)
            description.isEnabled = false
            legend.isEnabled = true
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM

            animateY(600)
            invalidate()
        }
    }

    private fun formatTimeLabel(timestamp: String): String {
        return if (timestamp.length >= 16) {
            timestamp.substring(11, 16)
        } else {
            timestamp
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isReading = false
        refreshHandler.removeCallbacks(refreshRunnable)
        handler.removeCallbacks(setpointJob ?: Runnable {})
        _binding = null
    }
}