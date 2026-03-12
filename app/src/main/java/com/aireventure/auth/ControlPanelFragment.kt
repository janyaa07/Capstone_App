package com.aireventure.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aireventure.auth.databinding.FragmentControlPanelBinding
import androidx.fragment.app.activityViewModels
import kotlin.math.roundToInt

class ControlPanelFragment : Fragment() {

    private var _binding: FragmentControlPanelBinding? = null
    private val binding get() = _binding!!
    private val sensorViewModel: SensorViewModel by activityViewModels()

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

        // Fetch latest AWS data when panel opens
        sensorViewModel.refreshAws()

        // Observe AWS data
        sensorViewModel.latest.observe(viewLifecycleOwner) { latest ->
            if (latest == null) {
                binding.currentTempValue.text = "--"
                binding.airflowValue.text = "--"
                binding.deviceIdText.text = "Device ID: --"
                binding.dialTempText.text = "--"
                return@observe
            }
            // REMOVE this line: BluetoothConnection.currentSetpoint = latest.SpRmT_C.toFloat()
            // Only update display values, not the setpoint
            binding.currentTempValue.text = "${latest.RmT_C}°C"
            binding.airflowValue.text = "${latest.AbsAirFlow_cfm} CFM"
            binding.deviceIdText.text = "Device ID: ${latest.device_id}"
            updateSetpointDisplay()
        }

        // PLUS BUTTON
        binding.iconPlus.setOnClickListener {
            if (BluetoothConnection.currentSetpoint < 35f) {
                BluetoothConnection.currentSetpoint += 0.5f
                updateSetpointDisplay()
                sendSetpoint(BluetoothConnection.currentSetpoint)
            }
        }

        // MINUS BUTTON
        binding.iconMinus.setOnClickListener {
            if (BluetoothConnection.currentSetpoint > 18f) {
                BluetoothConnection.currentSetpoint -= 0.5f
                updateSetpointDisplay()
                sendSetpoint(BluetoothConnection.currentSetpoint)
            }
        }
    }

    private fun updateSetpointDisplay() {
        binding.dialTempText.text = "%.1f°C".format(BluetoothConnection.currentSetpoint)
    }

    private fun sendSetpoint(value: Float) {
        Thread {
            try {
                val command = "SET:SpRmT_C:$value\n"
                BluetoothConnection.socket?.outputStream?.write(command.toByteArray())
                BluetoothConnection.socket?.outputStream?.flush()
                println("Sent: $command")
            } catch (e: Exception) {
                println("Send failed: $e")
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}