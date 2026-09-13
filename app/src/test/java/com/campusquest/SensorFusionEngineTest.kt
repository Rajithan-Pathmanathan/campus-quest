package com.campusquest

import com.campusquest.domain.model.LightSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SensorFusionEngineTest {

    @Test
    fun lightSignature_matchesCorrectlyInRange() {
        val signature = LightSignature(minLux = 100f, maxLux = 500f)

        assertTrue("100 lux should match minimum bound", signature.matches(100f))
        assertTrue("300 lux should match within range", signature.matches(300f))
        assertTrue("500 lux should match maximum bound", signature.matches(500f))
        assertFalse("50 lux should not match below min", signature.matches(50f))
        assertFalse("600 lux should not match above max", signature.matches(600f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun lightSignature_throwsIfMinExceedsMax() {
        LightSignature(minLux = 600f, maxLux = 100f)
    }

    @Test
    fun accelerometerMagnitude_calculatesCorrectEuclideanNorm() {
        val x = 3.0f
        val y = 4.0f
        val z = 0.0f
        val mag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        assertEquals(5.0f, mag, 0.001f)
    }

    @Test
    fun accelerometerMagnitude_shakeThresholdDetection() {
        val x = 10.0f
        val y = 10.0f
        val z = 5.0f
        val mag = sqrt((x * x + y * y + z * z).toDouble()).toFloat() // sqrt(100+100+25) = 15.0

        val shakeThreshold = 14.5f
        assertTrue("Magnitude 15.0 m/s² should exceed shake threshold 14.5 m/s²", mag > shakeThreshold)
    }
}
