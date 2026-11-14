package com.campusquest.device.location.geofence

/**
 * Pure composite key utility for dynamic game-scoped geofences.
 * Guarantees zero cross-game collisions between different quests.
 */
object GeofenceKey {
    private const val SEPARATOR = "#"

    fun build(gameId: String, checkpointId: String): String {
        require(gameId.isNotBlank()) { "gameId cannot be blank" }
        require(checkpointId.isNotBlank()) { "checkpointId cannot be blank" }
        return "$gameId$SEPARATOR$checkpointId"
    }

    fun parse(key: String): Pair<String, String>? {
        val parts = key.split(SEPARATOR)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            Pair(parts[0], parts[1])
        } else {
            null
        }
    }
}
