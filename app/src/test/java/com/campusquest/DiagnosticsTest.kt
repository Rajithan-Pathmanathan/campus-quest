package com.campusquest

import com.campusquest.device.location.LocationState
import com.campusquest.device.sensor.SensorDiagnosticsHelper
import com.campusquest.domain.model.SensorSignalType
import com.campusquest.domain.model.SensorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTest {

    private val helper = SensorDiagnosticsHelper()

    @Test
    fun allSensorsHealthy_producesZeroWarnings() {
        val sensorState = SensorState(
            currentLux = 450f,
            lightMatched = true,
            linearAccelerationMagnitude = 15.0f,
            motionDetected = true,
            isNear = true,
            availableSensors = setOf(
                SensorSignalType.GPS_LOCATION,
                SensorSignalType.AMBIENT_LIGHT,
                SensorSignalType.ACCELEROMETER_MOTION,
                SensorSignalType.PROXIMITY_GATE
            )
        )
        val locationState = LocationState(
            smoothedLatitude = 6.9,
            smoothedLongitude = 79.8,
            accuracyMeters = 4.0f,
            servicesEnabled = true
        )

        val diag = helper.createDiagnostics(sensorState, locationState)

        assertTrue(diag.lightAvailable)
        assertTrue(diag.accelerometerAvailable)
        assertTrue(diag.proximityAvailable)
        assertEquals(450f, diag.currentLux ?: 0f, 0.01f)
        assertEquals(4.0f, diag.gpsAccuracyMeters ?: 0f, 0.01f)
        assertTrue("Healthy device should have zero warnings", diag.warnings.isEmpty())
    }

    @Test
    fun missingLightSensor_andDegradedGps_producesExplicitWarnings() {
        val sensorState = SensorState(
            currentLux = null,
            lightMatched = null,
            availableSensors = setOf(
                SensorSignalType.GPS_LOCATION,
                SensorSignalType.ACCELEROMETER_MOTION,
                SensorSignalType.PROXIMITY_GATE
            )
        )
        // Degraded GPS (accuracy 25m > 15m threshold)
        val locationState = LocationState(
            smoothedLatitude = 6.9,
            smoothedLongitude = 79.8,
            accuracyMeters = 25.0f,
            servicesEnabled = true
        )

        val diag = helper.createDiagnostics(sensorState, locationState)

        assertFalse(diag.lightAvailable)
        assertTrue(diag.warnings.any { it.contains("Ambient light sensor unavailable") })
        assertTrue(diag.warnings.any { it.contains("GPS accuracy degraded") })
    }
}
