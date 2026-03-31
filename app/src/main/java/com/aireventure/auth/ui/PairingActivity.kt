package com.aireventure.auth.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.aireventure.auth.bluetooth.BluetoothConnection
import com.aireventure.auth.bluetooth.ConnectionMode
import com.aireventure.auth.databinding.ActivityPairingBinding
import java.util.UUID
import com.aireventure.auth.ui.HomeActivity

class PairingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPairingBinding
    private var bluetoothSocket: BluetoothSocket? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val MY_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Bluetooth button
        binding.bluetoothButton.setOnClickListener {
            connectToPi()
        }

        // WiFi button
        binding.wifiButton.setOnClickListener {
            BluetoothConnection.socket = null
            ConnectionMode.isBluetooth = false  // ← add this
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun connectToPi() {




        if (!checkBluetoothPermission()) return

        if (bluetoothAdapter == null) {
            binding.statusText.text = "Bluetooth not supported"
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        val piDevice = bluetoothAdapter!!.bondedDevices.firstOrNull {
            it.name.contains("raspberry", true)
        }

        if (piDevice == null) {
            binding.statusText.text = "Pi not paired"
            return
        }

        Thread {
            try {

                bluetoothSocket?.close()
                bluetoothAdapter!!.cancelDiscovery()

                val method = piDevice.javaClass.getMethod(
                    "createRfcommSocket",
                    Int::class.javaPrimitiveType
                )

                bluetoothSocket = method.invoke(piDevice, 1) as BluetoothSocket
                bluetoothSocket!!.connect()

                // Save socket globally
                BluetoothConnection.socket = bluetoothSocket
                ConnectionMode.isBluetooth = true

                runOnUiThread {
                    binding.statusText.text = "Connected"

                    // Go to your existing activity that holds DataPanelFragment
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    binding.statusText.text = "Disconnected"
                }
            }
        }.start()
    }

    private fun checkBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            val notGranted = permissions.any {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }

            if (notGranted) {
                ActivityCompat.requestPermissions(this, permissions, 1)
                return false
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // DO NOT close socket here
        // It is now used by DataPanelFragment
    }
}