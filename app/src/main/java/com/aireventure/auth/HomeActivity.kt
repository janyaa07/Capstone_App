package com.aireventure.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.aireventure.auth.databinding.ActivityHomeBinding
import android.content.Intent

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default panel
        supportFragmentManager.commit {
            replace(binding.panelContainer.id, ControlPanelFragment())
        }

        // Control tab click
        binding.controlTab.setOnClickListener {
            binding.controlTab.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.dataTab.setBackgroundResource(R.drawable.bg_tab_unselected)

            supportFragmentManager.commit {
                replace(binding.panelContainer.id, ControlPanelFragment())
            }
        }

        // Data tab click
        binding.dataTab.setOnClickListener {
            binding.dataTab.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.controlTab.setBackgroundResource(R.drawable.bg_tab_unselected)

            supportFragmentManager.commit {
                replace(binding.panelContainer.id, DataPanelFragment())
            }
        }

        // Settings menu toggle
        binding.settingsIcon.setOnClickListener {
            binding.settingsMenu.visibility =
                if (binding.settingsMenu.visibility == View.VISIBLE)
                    View.GONE else View.VISIBLE
            binding.connectedDevicesButton.setOnClickListener {
                startActivity(
                    Intent(this, ConnectedDevicesActivity::class.java)
                )
            }

        }
    }
}
