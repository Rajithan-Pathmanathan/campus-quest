package com.campusquest.domain.model

data class AccelerometerReading(
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat(),
    val timestamp: Long = System.currentTimeMillis()
)

data class LightReading(
    val lux: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProximityReading(
    val distanceCm: Float,
    val isNear: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class SensorReading(
    val accelerometer: AccelerometerReading? = null,
    val light: LightReading? = null,
    val proximity: ProximityReading? = null
)
