package com.campusquest

import com.campusquest.device.sensor.ProximityGateDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProximityGateTest {

    private lateinit var detector: ProximityGateDetector

    @Before
    fun setUp() {
        detector = ProximityGateDetector()
    }

    @Test
    fun binaryProximitySensor_zeroDistanceIsNear() {
        // Typical binary sensor: 0.0 = near, 5.0 (maxRange) = far
        val isNear = detector.processProximity(distance = 0.0f, maximumRange = 5.0f)

        assertTrue(isNear)
        assertTrue(detector.isNear)
        assertEquals(0.0f, detector.reportedDistanceCm, 0.01f)
    }

    @Test
    fun binaryProximitySensor_maxRangeIsFar() {
        val isNear = detector.processProximity(distance = 5.0f, maximumRange = 5.0f)

        assertFalse(isNear)
        assertFalse(detector.isNear)
    }

    @Test
    fun continuousProximitySensor_valuesLessThanMaxRangeAreNear() {
        // Continuous sensor with max range 8cm: 2.5cm reported
        val isNear = detector.processProximity(distance = 2.5f, maximumRange = 8.0f)

        assertTrue(isNear)
    }

    @Test
    fun reset_clearsNearState() {
        detector.processProximity(distance = 0.0f, maximumRange = 5.0f)
        assertTrue(detector.isNear)

        detector.reset()
        assertFalse(detector.isNear)
    }
}
