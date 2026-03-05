package com.aireventure.auth

import android.bluetooth.BluetoothSocket

object BluetoothConnection {
    var socket: BluetoothSocket? = null
    var currentSetpoint: Float = 22.0f
}