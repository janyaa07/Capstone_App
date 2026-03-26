package com.aireventure.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aireventure.auth.databinding.FragmentDataPanelBinding
import android.util.Log
import com.aireventure.auth.api.RetrofitClient
import com.aireventure.auth.bluetooth.ConnectionMode
import com.aireventure.auth.model.SensorData
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

    // REMOVED: bluetoothSocket, isReading — DataPanel no longer reads BT

    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshInterval = 5000L

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (_binding == null) return  // guard against destroyed view
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

        // Show connection mode status
        binding.liveDataText.text = if (ConnectionMode.isBluetooth) "Bluetooth Mode" else "WiFi Mode"

        refreshHandler.post(refreshRunnable)

        // REMOVED: startBluetoothReading() — ControlPanel owns the BT socket now
    }

    // REMOVED: startBluetoothReading()
    // REMOVED: parseAndDisplayBluetooth()

    private fun fetchSensorData() {
        RetrofitClient.instance.getSensorData().enqueue(object : Callback<List<SensorData>> {
            override fun onResponse(call: Call<List<SensorData>>, response: Response<List<SensorData>>) {
                if (_binding == null) return  // guard against destroyed view
                if (response.isSuccessful) {
                    val dataList = response.body()
                    if (!dataList.isNullOrEmpty()) {
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
        if (_binding == null) return
        val sortedList = dataList.sortedBy { it.timestamp }
        val latestTen = sortedList.takeLast(10)
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        latestTen.forEachIndexed { index, item ->
            entries.add(Entry(index.toFloat(), item.RmT_C.toFloat()))
            labels.add(formatTimeLabel(item.timestamp))
        }
        val dataSet = LineDataSet(entries, "Room Temp (°C)").apply {
            lineWidth = 2f; circleRadius = 4f; setDrawValues(false)
        }
        binding.temperatureChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false; legend.isEnabled = true; axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            animateX(600); invalidate()
        }
    }

    private fun renderPressureChart(dataList: List<SensorData>) {
        if (_binding == null) return
        val sortedList = dataList.sortedBy { it.timestamp }
        val latestTen = sortedList.takeLast(10)
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        latestTen.forEachIndexed { index, item ->
            entries.add(Entry(index.toFloat(), item.DeltaP_Pa.toFloat()))
            labels.add(formatTimeLabel(item.timestamp))
        }
        val dataSet = LineDataSet(entries, "Pressure (Pa)").apply {
            lineWidth = 2f; circleRadius = 4f; setDrawValues(false)
        }
        binding.pressureChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false; legend.isEnabled = true; axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            animateX(600); invalidate()
        }
    }

    private fun renderDischargeAirflowTimeChart(dataList: List<SensorData>) {
        if (_binding == null) return
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
            lineWidth = 2f; circleRadius = 4f; setDrawValues(false)
        }
        binding.dischargeAirflowChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false; legend.isEnabled = true; axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            animateX(600); invalidate()
        }
    }

    private fun renderDischargeAirflowPressureChart(dataList: List<SensorData>) {
        if (_binding == null) return
        val sortedList = dataList.sortedBy { it.timestamp }
        val latestTen = sortedList.takeLast(10)
        val entries = ArrayList<Entry>()
        latestTen.forEach { item ->
            val dischargeAirflow = calculateDischargeAirflow(item.DeltaP_Pa)
            entries.add(Entry(item.DeltaP_Pa.toFloat(), dischargeAirflow.toFloat()))
        }
        val dataSet = ScatterDataSet(entries, "Discharge Airflow").apply {
            setDrawValues(false)
            setScatterShapeSize(8f)
            setScatterShape(ScatterChart.ScatterShape.CIRCLE)
            color = android.graphics.Color.BLACK
        }
        binding.dischargeAirflowPressureChart.apply {
            data = ScatterData(dataSet)
            description.isEnabled = false; legend.isEnabled = true; axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            animateY(600); invalidate()
        }
    }

    private fun formatTimeLabel(timestamp: String): String {
        return if (timestamp.length >= 16) timestamp.substring(11, 16) else timestamp
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshHandler.removeCallbacks(refreshRunnable)
        _binding = null
        // REMOVED: isReading = false — no longer needed
    }
}