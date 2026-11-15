package com.campusquest.story

import android.content.Context
import android.hardware.*
import android.os.SystemClock
import android.view.Surface

/** Screen-owned sensor registration. Never substitutes a missing reading with success. */
class StorySensors(context: Context) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val kinds = listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_PROXIMITY, Sensor.TYPE_LIGHT)
    private val available = kinds.associateWith { manager.getDefaultSensor(it) }
    val hasTilt get() = available[Sensor.TYPE_ACCELEROMETER] != null
    val hasGyro get() = available[Sensor.TYPE_GYROSCOPE] != null
    val hasHeading get() = available[Sensor.TYPE_ROTATION_VECTOR] != null
    val hasProximity get() = available[Sensor.TYPE_PROXIMITY] != null
    var rotation = Surface.ROTATION_0
    val tilt = TiltControl()
    val tiltX get() = tilt.x
    val tiltY get() = tilt.y
    var heading: Float? = null; private set
    var headingReliable = true; private set
    var near: Boolean? = null; private set
    var sawFar = false; private set
    var sawNear = false; private set
    var lux: Float? = null; private set
    var turnRate: Float? = null; private set
    var lastMotion = 0L; private set
    var lastHeading = 0L; private set
    var lastTurn = 0L; private set
    var lastProximity = 0L; private set
    var relativeTurn = 0f; private set
    private var gyroTime = 0L
    private var running = false
    fun start() {
        if (running) return
        running = true
        tilt.reset()
        available.values.filterNotNull().forEach { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }
    fun stop() { manager.unregisterListener(this); running = false; gyroTime = 0; heading = null; near = null }
    fun calibrate() { tilt.calibrate(); relativeTurn = 0f }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR) headingReliable = accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE
    }
    override fun onSensorChanged(event: SensorEvent) {
        val now = SystemClock.elapsedRealtime()
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                tilt.sample(event.values[0],event.values[1],rotation,now,event.values[2])
                lastMotion = now
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastTurn = now
                turnRate = event.values[2]
                if (gyroTime != 0L) relativeTurn += Math.toDegrees((event.values[2] * ((event.timestamp-gyroTime)/1e9)).toDouble()).toFloat()
                gyroTime = event.timestamp
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                val matrix = FloatArray(9); val remap = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(matrix,event.values)
                val axes = when(rotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(matrix,axes.first,axes.second,remap)
                heading = Math.toDegrees(SensorManager.getOrientation(remap,FloatArray(3))[0].toDouble()).toFloat()
                lastHeading = now
            }
            Sensor.TYPE_PROXIMITY -> {
                near = event.values[0] < kotlin.math.min(5f,event.sensor.maximumRange)
                if (near == true) sawNear = true else sawFar = true
                lastProximity = now
            }
            Sensor.TYPE_LIGHT -> lux = event.values[0]
        }
    }
    fun summary(): String = available.entries.joinToString("\n") { (kind,sensor) ->
        val name = when(kind) { Sensor.TYPE_ACCELEROMETER -> "Tilt"; Sensor.TYPE_GYROSCOPE -> "Gyroscope"; Sensor.TYPE_ROTATION_VECTOR -> "Compass"; Sensor.TYPE_PROXIMITY -> "Cover sensor"; else -> "Light sensor" }
        "$name: ${sensor?.name ?: "unavailable — alternative controls"}"
    }
}

