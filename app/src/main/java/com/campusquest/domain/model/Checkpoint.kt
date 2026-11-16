package com.campusquest.domain.model

data class Checkpoint(
    val id: String,
    val gameId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float = 20f,
    /** Kept for stored/cloud compatibility with the old quest format; not used by The Golden Tree. */
    val lightSignature: LightSignature = LightSignature(0f, 1000f),
    val clue: String,
    val lore: String,
    val order: Int = 1,
    /** Compatibility fields from the old quest format (Room + Firestore schema); not used by The Golden Tree. */
    val motionType: String = "SWEEP",
    val rarity: String = "COMMON"
)
