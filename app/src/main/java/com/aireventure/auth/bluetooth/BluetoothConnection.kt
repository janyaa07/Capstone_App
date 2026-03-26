package com.aireventure.auth.bluetooth

import android.bluetooth.BluetoothSocket

object BluetoothConnection {
    var socket: BluetoothSocket? = null
    var currentSetpoint: Float = 22.0f
    var currentRoomTemp: Float? = null
    var currentAirflow: Float? = null
    var pendingSetpoint: Float? = null  // ← add this
}