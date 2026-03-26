package com.aireventure.auth.model

data class SensorData(
    val AbsAirFlow_cfm: Double,
    val SpRmT_UnitSel: Double,
    val SpRmT_C: Double,
    val RmT_C: Double,
    val timestamp: String,
    val AbsAirFlow_m3h: Double,
    val device_id: String,
    val RelPosDmp: Double,
    val DeltaP_Pa: Double
)