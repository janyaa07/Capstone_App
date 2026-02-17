package com.aireventure.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aireventure.auth.databinding.ActivityConverterBinding
import android.content.Intent
import android.os.Handler
import android.os.Looper

class ConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConverterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConverterBinding.inflate(layoutInflater)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }, 3500)

        setContentView(binding.root)
    }

}
