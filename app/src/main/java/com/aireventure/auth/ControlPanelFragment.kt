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
    private var currentSetpoint: Int = 24


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

        // Fetch once when control panel opens (optional but useful)
        sensorViewModel.refreshAws()

        sensorViewModel.latest.observe(viewLifecycleOwner) { latest ->
            if (latest == null) {
                binding.currentTempValue.text = "--"
                binding.airflowValue.text = "--"
                binding.deviceIdText.text = "Device ID: --"
                binding.dialTempText.text = "--"
                return@observe
            }
            currentSetpoint = latest.SpRmT_C.roundToInt()
            binding.dialTempText.text = "$currentSetpoint°C"
            binding.currentTempValue.text = "${latest.RmT_C}°C"
            binding.airflowValue.text = "${latest.AbsAirFlow_cfm} CFM"
            binding.deviceIdText.text = "Device ID: ${latest.device_id}"
        }
        // PLUS BUTTON
        binding.iconPlus.setOnClickListener {
            if (currentSetpoint < 35) {  // max limit
                currentSetpoint++
                binding.dialTempText.text = "$currentSetpoint°C"
            }
        }

// MINUS BUTTON
        binding.iconMinus.setOnClickListener {
            if (currentSetpoint > 16) {  // min limit
                currentSetpoint--
                binding.dialTempText.text = "$currentSetpoint°C"
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load saved setpoint — not hardcoded 22
        updateSetpointDisplay()

        binding.btnAdd.setOnClickListener {
            BluetoothConnection.currentSetpoint += 0.5f
            updateSetpointDisplay()
            sendSetpoint(BluetoothConnection.currentSetpoint)
        }

        binding.btnMinus.setOnClickListener {
            BluetoothConnection.currentSetpoint -= 0.5f
            updateSetpointDisplay()
            sendSetpoint(BluetoothConnection.currentSetpoint)
        }
    }

    private fun updateSetpointDisplay() {
        binding.tvSetpoint.text = "%.1f°C".format(BluetoothConnection.currentSetpoint)
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
