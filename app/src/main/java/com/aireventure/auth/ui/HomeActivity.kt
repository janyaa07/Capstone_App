package com.aireventure.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.aireventure.auth.AuthFiles.AuthActivity
import com.aireventure.auth.ControlPanelFragment
import com.aireventure.auth.DataPanelFragment
import com.aireventure.auth.ui.PairingActivity
import com.aireventure.auth.R
import com.aireventure.auth.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    // FIX: keep single instances so BT threads don't stack
    private val controlFragment = ControlPanelFragment()
    private val dataFragment = DataPanelFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default panel
        supportFragmentManager.commit {
            replace(binding.panelContainer.id, controlFragment)
        }
        binding.controlTab.setBackgroundResource(R.drawable.bg_tab_selected)
        binding.dataTab.setBackgroundResource(R.drawable.bg_tab_unselected)

        // Control tab click
        binding.controlTab.setOnClickListener {
            binding.controlTab.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.dataTab.setBackgroundResource(R.drawable.bg_tab_unselected)
            binding.settingsMenu.visibility = View.GONE

            // FIX: reuse same instance, don't create new one
            supportFragmentManager.commit {
                replace(binding.panelContainer.id, controlFragment)
            }
        }

        // Data tab click
        binding.dataTab.setOnClickListener {
            binding.dataTab.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.controlTab.setBackgroundResource(R.drawable.bg_tab_unselected)
            binding.settingsMenu.visibility = View.GONE

            // FIX: reuse same instance
            supportFragmentManager.commit {
                replace(binding.panelContainer.id, dataFragment)
            }
        }



        // Settings menu toggle
        binding.settingsIcon.setOnClickListener {
            binding.settingsMenu.visibility =
                if (binding.settingsMenu.visibility == View.VISIBLE)
                    View.GONE else View.VISIBLE
        }

// Add this inside your settings menu buttons
        binding.WifiBluetoothButton.setOnClickListener {
            binding.settingsMenu.visibility = View.GONE
            startActivity(Intent(this, PairingActivity::class.java))
        }

        binding.SignOut.setOnClickListener {
            binding.settingsMenu.visibility = View.GONE
            startActivity(Intent(this, AuthActivity::class.java))
        }
    }
}