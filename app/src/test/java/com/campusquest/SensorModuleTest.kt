package com.campusquest

import com.campusquest.device.sensor.light.LightSensorDetector
import com.campusquest.device.sensor.motion.GravityFilter
import com.campusquest.device.sensor.motion.ShakeDetector
import com.campusquest.device.sensor.motion.SweepDetector
import com.campusquest.device.sensor.motion.TiltDetector
import com.campusquest.domain.model.LightSignature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorModuleTest {

    // ==========================================
    // 1. Light Sensor State Machine & Debounce Tests
    // ==========================================

    @Test
    fun lightSensor_candidateTransition_requiresStabilityWindow() {
        val detector = LightSensorDetector(candidateStabilityWindowMs = 500L)
        val signature = LightSignature(minLux = 200f, maxLux = 600f)

        // Initial state: OUTSIDE
        assertEquals(LightSensorDetector.LightState.OUTSIDE, detector.state)
        assertFalse(detector.isLightMatched)

        // Reading enters range at t = 1000ms -> transitions to CANDIDATE
        detector.evaluateLux(lux = 350f, targetSignature = signature, timestamp = 1000L)
        assertEquals(LightSensorDetector.LightState.CANDIDATE, detector.state)
        assertFalse("At entry, candidate is not yet matched", detector.isLightMatched)

        // Reading remains in range at t = 1200ms (200ms elapsed < 500ms) -> remains CANDIDATE
        detector.evaluateLux(lux = 400f, targetSignature = signature, timestamp = 1200L)
        assertEquals(LightSensorDetector.LightState.CANDIDATE, detector.state)
        assertFalse(detector.isLightMatched)

        // Reading remains stable at t = 1500ms (500ms elapsed) -> transitions to MATCHED
        detector.evaluateLux(lux = 420f, targetSignature = signature, timestamp = 1500L)
        assertEquals(LightSensorDetector.LightState.MATCHED, detector.state)
        assertTrue(detector.isLightMatched)
    }

    @Test
    fun lightSensor_candidateFlap_resetsToOutsideIfExitsBeforeStability() {
        val detector = LightSensorDetector(candidateStabilityWindowMs = 500L)
        val signature = LightSignature(minLux = 200f, maxLux = 600f)

        // Enters candidate state at t = 1000ms
        detector.evaluateLux(lux = 350f, targetSignature = signature, timestamp = 1000L)
        assertEquals(LightSensorDetector.LightState.CANDIDATE, detector.state)

        // Flaps outside range at t = 1200ms (lux drops to 50f)
        detector.evaluateLux(lux = 50f, targetSignature = signature, timestamp = 1200L)
        assertEquals(LightSensorDetector.LightState.OUTSIDE, detector.state)
        assertFalse(detector.isLightMatched)

        // Re-enters at t = 1300ms -> starts new candidate window
        detector.evaluateLux(lux = 350f, targetSignature = signature, timestamp = 1300L)
        assertEquals(LightSensorDetector.LightState.CANDIDATE, detector.state)

        // At t = 1600ms (only 300ms since re-entry) -> still CANDIDATE
        detector.evaluateLux(lux = 350f, targetSignature = signature, timestamp = 1600L)
        assertEquals(LightSensorDetector.LightState.CANDIDATE, detector.state)

        // At t = 1800ms (500ms since re-entry) -> MATCHED
        detector.evaluateLux(lux = 350f, targetSignature = signature, timestamp = 1800L)
        assertEquals(LightSensorDetector.LightState.MATCHED, detector.state)
        assertTrue(detector.isLightMatched)
    }

    // ==========================================
    // 2. Gravity Filter & Shake Detector Tests
    // ==========================================

    @Test
    fun gravityFilter_isolatesConstantGravityFromDynamicMotion() {
        val filter = GravityFilter(alpha = 0.8f)

        // Resting phone (Z = 9.8)
        val (linear1, grav1) = filter.filter(0.0f, 0.0f, 9.8f)
        assertEquals(0.0f, linear1.dx, 0.01f)
        assertEquals(0.0f, linear1.dy, 0.01f)
        assertEquals(0.0f, linear1.dz, 0.01f)
        assertEquals(9.8f, grav1.gz, 0.01f)

        // Dynamic shake impulse along X axis
        val (linearShake, _) = filter.filter(15.0f, 0.0f, 9.8f)
        assertTrue("Linear acceleration magnitude should reflect dynamic impulse", linearShake.magnitude > 10.0f)
    }

    @Test
    fun shakeDetector_triggersOnlyWhenLinearAccelerationExceedsThreshold() {
        val detector = ShakeDetector(threshold = 14.5f)
        val filter = GravityFilter(alpha = 0.8f)

        // Initial resting position (flat on table)
        filter.filter(0.0f, 0.0f, 9.8f)

        // Gentle movement: raw X = 5.0 (linear magnitude = 5.0 - 1.0 = 4.0 < 14.5)
        val (linearGentle, gravGentle) = filter.filter(5.0f, 0.0f, 9.8f)
        detector.process(linearGentle, gravGentle)
        assertFalse("Gentle movement should not trigger shake", detector.isVerified)

        // Vigorous shake: raw X = 20.0 (linear magnitude = 20.0 - (0.8*1.0 + 0.2*20) = 15.2 > 14.5)
        val (linearShake, gravShake) = filter.filter(20.0f, 0.0f, 9.8f)
        detector.process(linearShake, gravShake)
        assertTrue("Impulse exceeding 14.5 m/s² must trigger shake", detector.isVerified)
    }

    // ==========================================
    // 3. Sweep Detector State Machine & Timeout Tests
    // ==========================================

    @Test
    fun sweepDetector_reversalWithinWindow_triggersSweepDetected() {
        val detector = SweepDetector(accelerationThreshold = 6.0f, timeWindowMs = 1500L)
        val grav = GravityFilter.GravityVector(0f, 0f, 9.8f)

        // 1. Initial positive movement along X (dx = +8.0 m/s²) at t = 1000ms
        val linearPos = GravityFilter.LinearAcceleration(dx = 8.0f, dy = 0.0f, dz = 0.0f, magnitude = 8.0f)
        detector.process(linearPos, grav, timestamp = 1000L)
        assertEquals(SweepDetector.SweepState.MOVING_POSITIVE, detector.currentState)
        assertFalse(detector.isVerified)

        // 2. Direction reversal along X (dx = -8.0 m/s²) at t = 1500ms (500ms later < 1500ms window)
        val linearNeg = GravityFilter.LinearAcceleration(dx = -8.0f, dy = 0.0f, dz = 0.0f, magnitude = 8.0f)
        detector.process(linearNeg, grav, timestamp = 1500L)
        assertEquals(SweepDetector.SweepState.SWEEP_DETECTED, detector.currentState)
        assertTrue(detector.isVerified)
    }

    @Test
    fun sweepDetector_timesOutIfReversalNotCompletedWithinWindow() {
        val detector = SweepDetector(accelerationThreshold = 6.0f, timeWindowMs = 1500L)
        val grav = GravityFilter.GravityVector(0f, 0f, 9.8f)

        // Positive movement at t = 1000ms
        val linearPos = GravityFilter.LinearAcceleration(dx = 8.0f, dy = 0.0f, dz = 0.0f, magnitude = 8.0f)
        detector.process(linearPos, grav, timestamp = 1000L)
        assertEquals(SweepDetector.SweepState.MOVING_POSITIVE, detector.currentState)

        // No reversal for 2000ms (t = 3100ms) -> timeout resets to IDLE
        val linearIdle = GravityFilter.LinearAcceleration(dx = 0.0f, dy = 0.0f, dz = 0.0f, magnitude = 0.0f)
        detector.process(linearIdle, grav, timestamp = 3100L)
        assertEquals(SweepDetector.SweepState.IDLE, detector.currentState)
        assertFalse(detector.isVerified)
    }

    // ==========================================
    // 4. Tilt Detector Tests
    // ==========================================

    @Test
    fun tiltDetector_detectsSignificantGravityVectorShift() {
        val detector = TiltDetector(deltaZThreshold = 5.0f, angleDegreesThreshold = 30.0f)
        val dummyLinear = GravityFilter.LinearAcceleration(0f, 0f, 0f, 0f)

        // Initial resting position: flat on table (gz = 9.8, gx = 0, gy = 0)
        val gravFlat = GravityFilter.GravityVector(0f, 0f, 9.8f)
        detector.process(dummyLinear, gravFlat)
        assertFalse(detector.isVerified)

        // Slight tilt: gz = 8.5 (deltaZ = 1.3 < 5.0)
        val gravSlight = GravityFilter.GravityVector(3.0f, 0f, 8.5f)
        detector.process(dummyLinear, gravSlight)
        assertFalse(detector.isVerified)

        // Significant vertical tilt: gz = 2.0 (deltaZ = 7.8 > 5.0)
        val gravTilted = GravityFilter.GravityVector(0f, 9.0f, 2.0f)
        detector.process(dummyLinear, gravTilted)
        assertTrue(detector.isVerified)
    }
}
