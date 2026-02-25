package com.aireventure.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment   // ✅ REQUIRED
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
