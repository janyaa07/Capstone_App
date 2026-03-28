package com.aireventure.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.aireventure.auth.AuthFiles.AuthActivity
import com.aireventure.auth.ControlPanelFragment
import com.aireventure.auth.DataPanelFragment
import com.aireventure.auth.bluetooth.ConnectionMode
import com.aireventure.auth.R
import com.aireventure.auth.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val controlFragment = ControlPanelFragment()
    private val dataFragment = DataPanelFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Connection badge ──────────────────────────────────────────────────
        // Show Wi-Fi or Bluetooth label based on current mode
        binding.connLabel.text = if (ConnectionMode.isBluetooth) "Bluetooth" else "Wi-Fi"

        // ── Default panel ─────────────────────────────────────────────────────
        supportFragmentManager.commit {
            replace(binding.panelContainer.id, controlFragment)
        }
        setActiveTab(isControl = true)

        // ── Control tab ───────────────────────────────────────────────────────
        binding.controlTab.setOnClickListener {
            binding.settingsMenu.visibility = View.GONE
            supportFragmentManager.commit {
                replace(binding.panelContainer.id, controlFragment)
            }
            setActiveTab(isControl = true)
        }

        // ── Data tab ──────────────────────────────────────────────────────────
        binding.dataTab.setOnClickListener {
            binding.settingsMenu.visibility = View.GONE
            supportFragmentManager.commit {
                replace(binding.panelContainer.id, dataFragment)
            }
            setActiveTab(isControl = false)
        }

        // ── Settings toggle ───────────────────────────────────────────────────
        binding.settingsIcon.setOnClickListener {
            binding.settingsMenu.visibility =
                if (binding.settingsMenu.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.WifiBluetoothButton.setOnClickListener {
            binding.settingsMenu.visibility = View.GONE
            startActivity(Intent(this, PairingActivity::class.java))
        }

        binding.SignOut.setOnClickListener {
            binding.settingsMenu.visibility = View.GONE
            startActivity(Intent(this, AuthActivity::class.java))
        }
    }

    /**
     * Updates the tab bar visual state.
     * Active tab: yellow indicator bar + yellow label.
     * Inactive tab: transparent indicator + grey label.
     */
    private fun setActiveTab(isControl: Boolean) {
        // Indicator bars
        binding.controlTabIndicator.setBackgroundColor(
            if (isControl) android.graphics.Color.parseColor("#FBC30E")
            else android.graphics.Color.TRANSPARENT
        )
        binding.dataTabIndicator.setBackgroundColor(
            if (!isControl) android.graphics.Color.parseColor("#FBC30E")
            else android.graphics.Color.TRANSPARENT
        )

        // Label colours
        binding.controlTabLabel.setTextColor(
            android.graphics.Color.parseColor(if (isControl) "#B8930A" else "#AAAAAA")
        )
        binding.dataTabLabel.setTextColor(
            android.graphics.Color.parseColor(if (!isControl) "#B8930A" else "#AAAAAA")
        )

        // Icon tint (optional — tint yellow when active, grey when inactive)
        val activeColor = android.graphics.Color.parseColor("#FBC30E")
        val inactiveColor = android.graphics.Color.parseColor("#AAAAAA")
        binding.controlTabIcon.setColorFilter(if (isControl) activeColor else inactiveColor)
        binding.dataTabIcon.setColorFilter(if (!isControl) activeColor else inactiveColor)
    }
}