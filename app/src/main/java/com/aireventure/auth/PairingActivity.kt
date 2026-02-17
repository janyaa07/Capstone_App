package com.aireventure.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aireventure.auth.databinding.ActivityPairingBinding

class PairingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPairingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bluetoothButton.setOnClickListener {
            startActivity(Intent(this, ConverterActivity::class.java))
        }
    }
}
