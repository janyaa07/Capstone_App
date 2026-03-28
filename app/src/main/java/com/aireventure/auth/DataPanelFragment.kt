package com.aireventure.auth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aireventure.auth.databinding.FragmentDataPanelBinding
import android.util.Log
import com.aireventure.auth.api.RetrofitClient
import com.aireventure.auth.model.SensorData
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.sqrt
import com.aireventure.auth.api.MLRetrofitClient
import com.aireventure.auth.model.MLPrediction
import com.aireventure.auth.model.MLResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataPanelFragment : Fragment() {

    private var _binding: FragmentDataPanelBinding? = null
    private val binding get() = _binding!!

    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshInterval = 5000L

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (_binding == null) return
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
        refreshHandler.post(refreshRunnable)
        setupDropdowns()
        fetchMlPrediction()
    }

    private fun fetchSensorData() {
        RetrofitClient.instance.getSensorData().enqueue(object : Callback<List<SensorData>> {
            override fun onResponse(call: Call<List<SensorData>>, response: Response<List<SensorData>>) {
                if (_binding == null) return
                if (response.isSuccessful) {
                    val dataList = response.body()
                    if (!dataList.isNullOrEmpty()) {
                        // Update current damper opening from latest sensor reading
                        val latest = dataList.sortedBy { it.timestamp }.lastOrNull()
                        latest?.let {
                            activity?.runOnUiThread {
                                if (_binding == null) return@runOnUiThread
                                binding.currentDamperValue.text = "%.1f".format(it.RelPosDmp)
                            }
                        }
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

    // ── Shared chart style helper ─────────────────────────────────────────────

    private fun styleLineChart(
        chart: com.github.mikephil.charting.charts.LineChart,
        labels: List<String>
    ) {
        chart.apply {
            description.isEnabled = false
            legend.apply {
                isEnabled = true
                textColor = Color.parseColor("#888888")
                textSize = 11f
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
            }
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)
            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
                gridLineWidth = 0.8f
                axisLineColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#888888")
                textSize = 10f
                labelRotationAngle = -30f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
                gridLineWidth = 0.8f
                axisLineColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#888888")
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) = "%.1f".format(value)
                }
            }

            setExtraOffsets(8f, 12f, 8f, 16f)
        }
    }

    private fun styleDataSet(dataSet: LineDataSet, lineColor: Int, label: String) {
        dataSet.apply {
            this.label = label
            color = lineColor
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(lineColor)
            circleHoleColor = Color.WHITE
            circleHoleRadius = 2f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = lineColor
            fillAlpha = 25
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }
    }

    // ── Chart renderers ───────────────────────────────────────────────────────

    private fun renderTemperatureChart(dataList: List<SensorData>) {
        if (_binding == null) return
        val sorted = dataList.sortedBy { it.timestamp }.takeLast(10)
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        sorted.forEachIndexed { i, item ->
            entries.add(Entry(i.toFloat(), item.RmT_C.toFloat()))
            labels.add(formatTimeLabel(item.timestamp))
        }
        val dataSet = LineDataSet(entries, "Room Temp (°C)")
        styleDataSet(dataSet, Color.parseColor("#FBC30E"), "Room Temp (°C)")

        binding.temperatureChart.apply {
            data = LineData(dataSet)
            styleLineChart(this, labels)
            animateX(600)
            invalidate()
        }
    }

    private fun renderPressureChart(dataList: List<SensorData>) {
        if (_binding == null) return
        val sorted = dataList.sortedBy { it.timestamp }.takeLast(10)
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        sorted.forEachIndexed { i, item ->
            entries.add(Entry(i.toFloat(), item.DeltaP_Pa.toFloat()))
            labels.add(formatTimeLabel(item.timestamp))
        }
        val dataSet = LineDataSet(entries, "Pressure (Pa)")
        styleDataSet(dataSet, Color.parseColor("#0284C7"), "Pressure (Pa)")

        binding.pressureChart.apply {
            data = LineData(dataSet)
            styleLineChart(this, labels)
            animateX(600)
            invalidate()
        }
    }

    private fun renderDischargeAirflowTimeChart(dataList: List<SensorData>) {
        if (_binding == null) return
        val sorted = dataList.sortedBy { it.timestamp }.takeLast(10)
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        sorted.forEachIndexed { i, item ->
            val airflow = calculateDischargeAirflow(item.DeltaP_Pa)
            entries.add(Entry(i.toFloat(), airflow.toFloat()))
            labels.add(formatTimeLabel(item.timestamp))
        }
        val dataSet = LineDataSet(entries, "Discharge Airflow")
        styleDataSet(dataSet, Color.parseColor("#16A34A"), "Discharge Airflow")

        binding.dischargeAirflowChart.apply {
            data = LineData(dataSet)
            styleLineChart(this, labels)
            animateX(600)
            invalidate()
        }
    }

    private fun renderDischargeAirflowPressureChart(dataList: List<SensorData>) {
        if (_binding == null) return
        val sorted = dataList.sortedBy { it.timestamp }.takeLast(10)
        val entries = ArrayList<Entry>()
        sorted.forEach { item ->
            val airflow = calculateDischargeAirflow(item.DeltaP_Pa)
            entries.add(Entry(item.DeltaP_Pa.toFloat(), airflow.toFloat()))
        }
        val dataSet = ScatterDataSet(entries, "Airflow vs Pressure").apply {
            setDrawValues(false)
            setScatterShapeSize(10f)
            setScatterShape(ScatterChart.ScatterShape.CIRCLE)
            color = Color.parseColor("#9333EA")
        }

        binding.dischargeAirflowPressureChart.apply {
            data = ScatterData(dataSet)
            description.isEnabled = false
            legend.apply {
                isEnabled = true
                textColor = Color.parseColor("#888888")
                textSize = 11f
            }
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)
            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
                gridLineWidth = 0.8f
                axisLineColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#888888")
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) = "%.1f Pa".format(value)
                }
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
                gridLineWidth = 0.8f
                axisLineColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#888888")
                textSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) = "%.3f".format(value)
                }
            }
            setExtraOffsets(8f, 12f, 8f, 16f)
            animateY(600)
            invalidate()
        }
    }

    private fun formatTimeLabel(timestamp: String): String {
        return if (timestamp.length >= 16) timestamp.substring(11, 16) else timestamp
    }

    private fun setupDropdowns() {
        binding.temperatureRow.setOnClickListener {
            toggleSection(binding.temperatureChart, binding.temperatureChevron)
        }
        binding.pressureRow.setOnClickListener {
            toggleSection(binding.pressureChart, binding.pressureChevron)
        }
        binding.dischargeAirflowTimeRow.setOnClickListener {
            toggleSection(binding.dischargeAirflowChart, binding.dischargeAirflowTimeChevron)
        }
        binding.dischargeAirflowPressureRow.setOnClickListener {
            toggleSection(binding.dischargeAirflowPressureChart, binding.dischargeAirflowPressureChevron)
        }
    }

    private fun toggleSection(content: View, arrow: View) {
        val isOpening = content.visibility != View.VISIBLE
        content.visibility = if (isOpening) View.VISIBLE else View.GONE
        arrow.animate().rotation(if (isOpening) 180f else 0f).setDuration(200).start()
    }

    private fun fetchMlPrediction() {
        binding.mlPredictionValue.text = "..."

        MLRetrofitClient.instance.getPredictions().enqueue(object : retrofit2.Callback<MLResponse> {
            override fun onResponse(
                call: retrofit2.Call<MLResponse>,
                response: retrofit2.Response<MLResponse>
            ) {
                val bodyString = response.body()?.body ?: run {
                    activity?.runOnUiThread {
                        if (_binding == null) return@runOnUiThread
                        binding.mlPredictionValue.text = "—"
                    }
                    return
                }
                val type = object : TypeToken<List<MLPrediction>>() {}.type
                val list: List<MLPrediction> = Gson().fromJson(bodyString, type)
                val latest = list.firstOrNull()
                val raw = latest?.prediction ?: "--"
                val clean = raw.replace("[", "").replace("]", "").toDoubleOrNull()
                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.mlPredictionValue.text =
                        if (clean != null) "%.2f".format(clean) else "—"
                }
            }

            override fun onFailure(call: retrofit2.Call<MLResponse>, t: Throwable) {
                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.mlPredictionValue.text = "—"
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshHandler.removeCallbacks(refreshRunnable)
        _binding = null
    }
}