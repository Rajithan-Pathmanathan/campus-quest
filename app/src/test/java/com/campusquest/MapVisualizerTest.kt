package com.campusquest

import com.campusquest.device.location.BearingCalculator
import com.campusquest.device.location.LocationState
import com.campusquest.device.location.MapGeofenceDataProvider
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.GeofenceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapVisualizerTest {

    // ==========================================
    // 1. Bearing Calculation Tests
    // ==========================================

    @Test
    fun bearingCalculator_dueNorth_returnsZeroDegrees() {
        // Point A (0.0, 0.0) to Point B (1.0, 0.0) -> Due North
        val bearing = BearingCalculator.calculateBearing(
            fromLat = 0.0,
            fromLng = 0.0,
            toLat = 1.0,
            toLng = 0.0
        )
        assertEquals(0.0f, bearing, 0.5f)
    }

    @Test
    fun bearingCalculator_dueEast_returnsNinetyDegrees() {
        // Point A (0.0, 0.0) to Point B (0.0, 1.0) -> Due East
        val bearing = BearingCalculator.calculateBearing(
            fromLat = 0.0,
            fromLng = 0.0,
            toLat = 0.0,
            toLng = 1.0
        )
        assertEquals(90.0f, bearing, 0.5f)
    }

    @Test
    fun bearingCalculator_dueSouth_returnsOneEightyDegrees() {
        val bearing = BearingCalculator.calculateBearing(
            fromLat = 1.0,
            fromLng = 0.0,
            toLat = 0.0,
            toLng = 0.0
        )
        assertEquals(180.0f, bearing, 0.5f)
    }

    @Test
    fun bearingCalculator_dueWest_returnsTwoSeventyDegrees() {
        val bearing = BearingCalculator.calculateBearing(
            fromLat = 0.0,
            fromLng = 1.0,
            toLat = 0.0,
            toLng = 0.0
        )
        assertEquals(270.0f, bearing, 0.5f)
    }

    // ==========================================
    // 2. Map Geofence Data Provider Tests
    // ==========================================

    @Test
    fun mapGeofenceDataProvider_generatesAccurateVisualState() {
        val provider = MapGeofenceDataProvider()
        val checkpoint = Checkpoint(
            id = "CP1",
            gameId = "G1",
            name = "Clock Tower",
            lat = 6.9328,
            lng = 79.8436,
            radiusM = 25.0f,
            clue = "Look up",
            lore = "Historic tower"
        )

        // Player is nearby at (6.9327, 79.8435) ~ 15m away
        val playerLocation = LocationState(
            smoothedLatitude = 6.9327,
            smoothedLongitude = 79.8435,
            accuracyMeters = 3.0f,
            servicesEnabled = true
        )

        val visualState = provider.createVisualState(checkpoint, playerLocation, GeofenceState.OUTSIDE)

        assertEquals("CP1", visualState.checkpointId)
        assertNotNull(visualState.distanceMeters)
        assertTrue("Distance should be within 25m radius", visualState.distanceMeters!! <= 25.0f)
        assertEquals(GeofenceState.INSIDE, visualState.state)
        assertNotNull(visualState.bearingDegrees)
        assertTrue(visualState.bearingDegrees!! in 0.0f..360.0f)
    }
}
