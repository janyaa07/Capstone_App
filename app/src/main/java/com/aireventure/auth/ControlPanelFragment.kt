package com.aireventure.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aireventure.auth.databinding.FragmentControlPanelBinding
import androidx.fragment.app.activityViewModels
import com.aireventure.auth.api.MLRetrofitClient
import com.aireventure.auth.api.WifiSetpointClient
import com.aireventure.auth.bluetooth.BluetoothConnection
import com.aireventure.auth.bluetooth.ConnectionMode
import com.aireventure.auth.model.MLPrediction
import com.aireventure.auth.model.MLResponse
import com.aireventure.auth.model.SetpointRequest
import com.aireventure.auth.viewmodel.SensorViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class ControlPanelFragment : Fragment() {

    private var _binding: FragmentControlPanelBinding? = null
    private val binding get() = _binding!!
    private val sensorViewModel: SensorViewModel by activityViewModels()

    @Volatile private var isReading = false

    // LinkedBlockingQueue replaces AtomicReference + sleep(10ms)
    // poll() blocks instantly until command arrives — no busy waiting
    // clear() + offer() = only latest value ever sent
    private val writeQueue = LinkedBlockingQueue<String>(1)
    private var writeThread: Thread? = null

    private val setpointHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var setpointRunnable: Runnable? = null

    // separate handler for button re-enable so it doesn't conflict with status text handler
    private val buttonHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshInterval = 1000L
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (_binding == null) return
            updateSetpointDisplay()
            binding.currentTempValue.text =
                if (BluetoothConnection.currentRoomTemp != null)
                    "${"%.1f".format(BluetoothConnection.currentRoomTemp)}°C"
                else "--"
            binding.airflowValue.text =
                if (BluetoothConnection.currentAirflow != null)
                    "${"%.1f".format(BluetoothConnection.currentAirflow)} m³/h"
                else "--"
            refreshHandler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ConnectionMode.isBluetooth) {
            setupBluetoothMode()
        } else {
            setupWifiMode()
        }

        // PLUS button
        binding.iconPlus.setOnClickListener {
            val current = BluetoothConnection.pendingSetpoint ?: BluetoothConnection.currentSetpoint
            if (current < 28f) {
                val newValue = current + 0.5f
                BluetoothConnection.pendingSetpoint = newValue
                updateSetpointDisplay()
                if (ConnectionMode.isBluetooth) {
                    disableButtons()
                    enqueueWrite(newValue)
                    scheduleSetpoint(newValue)
                } else {
                    disableButtons()
                    sendSetpointWifi(newValue)
                }
            }
        }

        // MINUS button
        binding.iconMinus.setOnClickListener {
            val current = BluetoothConnection.pendingSetpoint ?: BluetoothConnection.currentSetpoint
            if (current > 18f) {
                val newValue = current - 0.5f
                BluetoothConnection.pendingSetpoint = newValue
                updateSetpointDisplay()
                if (ConnectionMode.isBluetooth) {
                    disableButtons()
                    enqueueWrite(newValue)
                    scheduleSetpoint(newValue)
                } else {
                    disableButtons()
                    sendSetpointWifi(newValue)
                }
            }
        }
    }

    // disable both buttons while waiting for hardware ACK
    // prevents command flooding — one press one write one confirm
    private fun disableButtons() {
        if (_binding == null) return
        binding.iconPlus.isEnabled = false
        binding.iconMinus.isEnabled = false
        // safety re-enable after 15s so buttons never get permanently stuck
        buttonHandler.removeCallbacksAndMessages(null)
        buttonHandler.postDelayed({
            if (_binding == null) return@postDelayed
            binding.iconPlus.isEnabled = true
            binding.iconMinus.isEnabled = true
        }, 15000)
    }

    // re-enable buttons when hardware confirms
    private fun enableButtons() {
        if (_binding == null) return
        buttonHandler.removeCallbacksAndMessages(null)
        binding.iconPlus.isEnabled = true
        binding.iconMinus.isEnabled = true
    }

    // always replace stale command with latest — mirrors RPi deque(maxlen=1)
    private fun enqueueWrite(value: Float) {
        writeQueue.clear()
        writeQueue.offer("SET:SpRmT_C:$value\n")
    }

    private fun startWriteThread() {
        isReading = true
        writeThread = Thread {
            while (isReading) {
                try {
                    // poll() blocks up to 100ms waiting for command — no busy sleep loop
                    val command = writeQueue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                    val socket = BluetoothConnection.socket
                    if (socket != null && socket.isConnected) {
                        socket.outputStream.write(command.toByteArray())
                        socket.outputStream.flush()
                    } else {
                        // socket not ready — put command back so it is not lost
                        writeQueue.clear()
                        writeQueue.offer(command)
                        Thread.sleep(200)
                    }
                } catch (e: Exception) {
                    println("Write thread error: $e")
                    Thread.sleep(200)
                }
            }
        }.also { it.start() }
    }

    private fun scheduleSetpoint(value: Float) {
        if (_binding == null) return
        setpointRunnable?.let { setpointHandler.removeCallbacks(it) }
        binding.setpointStatusText.apply {
            text = "⏳ Sending ${"%.1f".format(value)}°C — waiting for device..."
            setTextColor(android.graphics.Color.parseColor("#E65100"))
            visibility = View.VISIBLE
        }
        setpointRunnable = Runnable {
            if (_binding == null) return@Runnable
            binding.setpointStatusText.visibility = View.GONE
        }
        setpointHandler.postDelayed(setpointRunnable!!, 15000)
    }

    // WiFi setpoint — sends to API Gateway → Lambda → DynamoDB → RPi polls
    private fun sendSetpointWifi(value: Float) {
        if (_binding != null) {
            binding.setpointStatusText.apply {
                text = "⏳ Sending ${"%.1f".format(value)}°C to cloud..."
                setTextColor(android.graphics.Color.parseColor("#E65100"))
                visibility = View.VISIBLE
            }
        }
        val request = SetpointRequest(device_id = "23", SpRmT_C = value)
        WifiSetpointClient.instance.updateSetpoint(request).enqueue(
            object : retrofit2.Callback<Void> {
                override fun onResponse(
                    call: retrofit2.Call<Void>,
                    response: retrofit2.Response<Void>
                ) {
                    activity?.runOnUiThread {
                        if (_binding == null) return@runOnUiThread
                        if (response.isSuccessful) {
                            enableButtons()
                            binding.setpointStatusText.apply {
                                text = "✓ Sent ${"%.1f".format(value)}°C to cloud"
                                setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                                visibility = View.VISIBLE
                            }
                        } else {
                            enableButtons()
                            BluetoothConnection.pendingSetpoint = null
                            updateSetpointDisplay()
                            binding.setpointStatusText.apply {
                                text = "✗ Failed to update setpoint"
                                setTextColor(android.graphics.Color.parseColor("#C62828"))
                                visibility = View.VISIBLE
                            }
                        }
                        setpointRunnable?.let { setpointHandler.removeCallbacks(it) }
                        setpointRunnable = Runnable {
                            if (_binding == null) return@Runnable
                            binding.setpointStatusText.visibility = View.GONE
                        }
                        setpointHandler.postDelayed(setpointRunnable!!, 2000)
                    }
                }
                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                    activity?.runOnUiThread {
                        if (_binding == null) return@runOnUiThread
                        enableButtons()
                        BluetoothConnection.pendingSetpoint = null
                        updateSetpointDisplay()
                        binding.setpointStatusText.apply {
                            text = "✗ Network error: ${t.message}"
                            setTextColor(android.graphics.Color.parseColor("#C62828"))
                            visibility = View.VISIBLE
                        }
                        setpointRunnable?.let { setpointHandler.removeCallbacks(it) }
                        setpointRunnable = Runnable {
                            if (_binding == null) return@Runnable
                            binding.setpointStatusText.visibility = View.GONE
                        }
                        setpointHandler.postDelayed(setpointRunnable!!, 2000)
                    }
                }
            }
        )
    }

    private fun setupWifiMode() {
        binding.deviceIdText.text = "Mode: WiFi"
        sensorViewModel.refreshAws()

        MLRetrofitClient.instance.getPredictions().enqueue(object : retrofit2.Callback<MLResponse> {
            override fun onResponse(
                call: retrofit2.Call<MLResponse>,
                response: retrofit2.Response<MLResponse>
            ) {
                val bodyString = response.body()?.body ?: return
                val type = object : TypeToken<List<MLPrediction>>() {}.type
                val list: List<MLPrediction> = Gson().fromJson(bodyString, type)
                val latest = list.firstOrNull()
                val raw = latest?.prediction ?: "--"
                val clean = raw.replace("[", "").replace("]", "").toDoubleOrNull()
                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    if (clean != null) "%.2f%%".format(clean) else "--"
                }
            }
            override fun onFailure(call: retrofit2.Call<MLResponse>, t: Throwable) {}
        })

        sensorViewModel.latest.observe(viewLifecycleOwner) { latest ->
            if (_binding == null) return@observe
            if (latest == null) return@observe
            binding.currentTempValue.text = "${"%.1f".format(latest.RmT_C.toDouble())}°C"
            binding.airflowValue.text = "${latest.AbsAirFlow_cfm} CFM"
            binding.deviceIdText.text = "Device ID: ${latest.device_id}"
            BluetoothConnection.currentSetpoint = latest.SpRmT_C.toFloat()
            val pending = BluetoothConnection.pendingSetpoint
            if (pending != null && Math.abs(latest.SpRmT_C.toFloat() - pending) < 0.1f) {
                BluetoothConnection.pendingSetpoint = null
            }
            updateSetpointDisplay()
        }
    }

    private fun setupBluetoothMode() {
        binding.deviceIdText.text = "Mode: Bluetooth"
        updateSetpointDisplay()
        startWriteThread()
        startBluetoothReading()
        refreshHandler.post(refreshRunnable)
    }

    private fun startBluetoothReading() {
        val socket = BluetoothConnection.socket ?: return
        Thread {
            try {
                val reader = socket.inputStream.bufferedReader()
                while (isReading) {
                    val line = reader.readLine() ?: break
                    val parts = line.split(";")
                    var needsUiUpdate = false

                    for (part in parts) {
                        val kv = part.split(":")
                        if (kv.size == 2) {
                            when (kv[0]) {
                                "RmT_C" -> kv[1].toFloatOrNull()?.let {
                                    BluetoothConnection.currentRoomTemp = it
                                    needsUiUpdate = true
                                }
                                "AbsAirFlow_m3h" -> kv[1].toFloatOrNull()?.let {
                                    BluetoothConnection.currentAirflow = it
                                    needsUiUpdate = true
                                }
                                "SpRmT_C" -> kv[1].toFloatOrNull()?.let { confirmedValue ->
                                    BluetoothConnection.currentSetpoint = confirmedValue
                                    needsUiUpdate = true
                                    if (BluetoothConnection.pendingSetpoint != null &&
                                        Math.abs(confirmedValue - (BluetoothConnection.pendingSetpoint ?: 0f)) < 0.1f
                                    ) {
                                        BluetoothConnection.pendingSetpoint = null
                                        showConfirmed(confirmedValue)
                                    }
                                }
                            }
                        }

                        // handle ACK:SpRmT_C:value
                        if (kv.size == 3 && kv[0] == "ACK" && kv[1] == "SpRmT_C") {
                            kv[2].toFloatOrNull()?.let { ackValue ->
                                BluetoothConnection.currentSetpoint = ackValue
                                BluetoothConnection.pendingSetpoint = null
                                needsUiUpdate = true
                                showConfirmed(ackValue)
                            }
                        }
                    }

                    if (needsUiUpdate) {
                        activity?.runOnUiThread {
                            if (_binding == null) return@runOnUiThread
                            updateSetpointDisplay()
                        }
                    }
                }
            } catch (e: Exception) {
                println("BT read error: $e")
            }
        }.start()
    }

    private fun showConfirmed(value: Float) {
        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread
            // re-enable buttons when hardware confirms
            enableButtons()
            binding.setpointStatusText.apply {
                text = "✓ Device confirmed ${"%.1f".format(value)}°C"
                setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                visibility = View.VISIBLE
            }
            setpointRunnable?.let { setpointHandler.removeCallbacks(it) }
            setpointRunnable = Runnable {
                if (_binding == null) return@Runnable
                binding.setpointStatusText.visibility = View.GONE
            }
            setpointHandler.postDelayed(setpointRunnable!!, 2000)
        }
    }

    private fun updateSetpointDisplay() {
        if (_binding == null) return
        binding.dialTempText.text = "%.1f°C".format(BluetoothConnection.currentSetpoint)
        binding.pendingSetpointText.text = "%.1f°C".format(
            BluetoothConnection.pendingSetpoint ?: BluetoothConnection.currentSetpoint
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isReading = false
        writeQueue.clear()
        writeThread = null
        refreshHandler.removeCallbacks(refreshRunnable)
        setpointRunnable?.let { setpointHandler.removeCallbacks(it) }
        buttonHandler.removeCallbacksAndMessages(null)
        _binding = null
    }
}