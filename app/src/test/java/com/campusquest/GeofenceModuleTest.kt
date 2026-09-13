package com.campusquest

import android.location.Location
import com.campusquest.device.location.LocationState
import com.campusquest.device.location.geofence.BoundaryHysteresis
import com.campusquest.device.location.geofence.GeofenceKey
import com.campusquest.device.location.geofence.ScopedGeofenceFallback
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.GeofenceState
import com.campusquest.domain.model.LightSignature
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeofenceModuleTest {

    private lateinit var hysteresis: BoundaryHysteresis
    private lateinit var fallback: ScopedGeofenceFallback

    private val sampleCheckpoint = Checkpoint(
        id = "CP_BELL_TOWER",
        gameId = "GAME_HERITAGE_001",
        name = "Historic Bell Tower",
        lat = 37.4275,
        lng = -122.1697,
        radiusM = 25.0f,
        lightSignature = LightSignature(100f, 500f),
        clue = "Where shadows stretch...",
        lore = "Built in 1891.",
        order = 1
    )

    @Before
    fun setUp() {
        hysteresis = BoundaryHysteresis(hysteresisMarginMeters = 3.0f)
        fallback = ScopedGeofenceFallback(hysteresis = hysteresis)
    }

    @Test
    fun geofenceKey_buildAndParse_roundTripSuccess() {
        val gameId = "CAMPUS_QUEST_01"
        val checkpointId = "CP_QUAD_05"

        val key = GeofenceKey.build(gameId, checkpointId)
        assertEquals("CAMPUS_QUEST_01#CP_QUAD_05", key)

        val parsed = GeofenceKey.parse(key)
        assertNotNull(parsed)
        assertEquals(gameId, parsed!!.first)
        assertEquals(checkpointId, parsed.second)
    }

    @Test
    fun geofenceKey_parse_invalidFormatsReturnNull() {
        assertNull("Missing separator should return null", GeofenceKey.parse("CAMPUS_QUEST_01_CP_05"))
        assertNull("Empty key should return null", GeofenceKey.parse(""))
        assertNull("Trailing separator with empty checkpoint should return null", GeofenceKey.parse("GAME#"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun geofenceKey_build_blankGameIdThrows() {
        GeofenceKey.build("", "CP_01")
    }

    @Test
    fun boundaryHysteresis_stateTransitions_correct() {
        val radius = 25.0f // Margin = 3.0m, so exit boundary is > 28.0m

        // 1. From OUTSIDE, point at 24m triggers INSIDE
        val state1 = hysteresis.evaluate(distanceMeters = 24.0f, radiusMeters = radius, previousState = GeofenceState.OUTSIDE)
        assertEquals(GeofenceState.INSIDE, state1)

        // 2. From OUTSIDE, point at 26m stays OUTSIDE
        val state2 = hysteresis.evaluate(distanceMeters = 26.0f, radiusMeters = radius, previousState = GeofenceState.OUTSIDE)
        assertEquals(GeofenceState.OUTSIDE, state2)

        // 3. From INSIDE, point at 26m (within 28m margin) stays INSIDE
        val state3 = hysteresis.evaluate(distanceMeters = 26.0f, radiusMeters = radius, previousState = GeofenceState.INSIDE)
        assertEquals(GeofenceState.INSIDE, state3)

        // 4. From INSIDE, point at 29m (> 28m margin) exits to OUTSIDE
        val state4 = hysteresis.evaluate(distanceMeters = 29.0f, radiusMeters = radius, previousState = GeofenceState.INSIDE)
        assertEquals(GeofenceState.OUTSIDE, state4)
    }

    @Test
    fun scopedFallback_evaluatesActiveCheckpoint_producesEligibility() {
        val mockLocation = mockk<Location>()
        every { mockLocation.latitude } returns 37.42751 // ~1m away from target (37.4275, -122.1697)
        every { mockLocation.longitude } returns -122.1697
        every { mockLocation.hasAccuracy() } returns true
        every { mockLocation.accuracy } returns 4.0f

        val locationState = LocationState(
            location = mockLocation,
            smoothedLatitude = 37.42751,
            smoothedLongitude = -122.1697,
            accuracyMeters = 4.0f
        )

        val eligibility = fallback.evaluate(
            activeCheckpoint = sampleCheckpoint,
            locationState = locationState
        )

        assertEquals("GAME_HERITAGE_001", eligibility.gameId)
        assertEquals("CP_BELL_TOWER", eligibility.checkpointId)
        assertEquals(GeofenceState.INSIDE, eligibility.geofenceState)
        assertTrue(eligibility.eligibleForScan)
        assertNotNull(eligibility.distanceMeters)
        assertTrue(eligibility.distanceMeters!! < 5.0f)
    }

    @Test
    fun scopedFallback_pointOutside_isNotEligible() {
        val mockLocation = mockk<Location>()
        every { mockLocation.latitude } returns 37.4290 // ~160m away
        every { mockLocation.longitude } returns -122.1697
        every { mockLocation.hasAccuracy() } returns true
        every { mockLocation.accuracy } returns 5.0f

        val locationState = LocationState(
            location = mockLocation,
            smoothedLatitude = 37.4290,
            smoothedLongitude = -122.1697,
            accuracyMeters = 5.0f
        )

        val eligibility = fallback.evaluate(
            activeCheckpoint = sampleCheckpoint,
            locationState = locationState
        )

        assertEquals(GeofenceState.OUTSIDE, eligibility.geofenceState)
        assertFalse(eligibility.eligibleForScan)
        assertTrue(eligibility.distanceMeters!! > 100.0f)
    }
}
