package com.campusquest.ui.map

import com.campusquest.device.location.HaversineDistanceCalculator
import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LocationState
import com.campusquest.ui.quest.NextCheckpointResolver
import kotlin.math.roundToInt

data class MapTargetState(val checkpoint: Checkpoint?, val distanceMeters: Int?) {
    companion object {
        fun from(checkpoints: List<Checkpoint>, completed: List<String>, location: LocationState): MapTargetState {
            val next = NextCheckpointResolver.next(checkpoints, completed)
            val distance = if (next != null && location.hasValidFix && location.servicesEnabled) {
                HaversineDistanceCalculator().calculateDistanceMeters(
                    location.smoothedLatitude!!, location.smoothedLongitude!!, next.lat, next.lng
                ).roundToInt()
            } else null
            return MapTargetState(next, distance)
        }
    }
}
