package com.aireventure.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aireventure.auth.databinding.ActivityConnectedDevicesBinding

class ConnectedDevicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectedDevicesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectedDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}
