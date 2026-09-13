package com.campusquest.ui.creator

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.LightSignature

/**
 * Ambient light sensitivity presets for easy waypoint configuration.
 */
enum class LightPreset(
    val title: String,
    val minLux: Float,
    val maxLux: Float
) {
    DIM_SHADOW("🌙 Dim / Shadow", 0f, 100f),
    INDOOR_LAB("🏢 Indoor Lab", 100f, 500f),
    DAYLIGHT("☀️ Daylight", 500f, 2000f),
    HIGH_NOON("🔆 Direct Sun", 2000f, 10000f),
    CUSTOM("⚙️ Custom Range", 0f, 1000f)
}

/**
 * UI State for the Checkpoint Editor and Sensor Constraints configurator.
 */
data class CheckpointEditorUiState(
    val isLoading: Boolean = false,
    val gameId: String = "",
    val checkpointId: String? = null,
    val order: Int = 1,
    val name: String = "",
    val clue: String = "",
    val lore: String = "",
    val lat: Double = 6.9360,
    val lng: Double = 79.8436,
    val radiusM: Float = 25f,
    val minLux: Float = 200f,
    val maxLux: Float = 800f,
    val selectedPreset: LightPreset = LightPreset.INDOOR_LAB,
    val motionType: String = "SWEEP",
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val isEditMode: Boolean
        get() = !checkpointId.isNullOrBlank()

    val isValid: Boolean
        get() = name.isNotBlank() && clue.isNotBlank() && radiusM >= 5f && minLux < maxLux

    val validationMessage: String?
        get() = when {
            name.isBlank() -> "Checkpoint name is required."
            clue.isBlank() -> "Clue riddle is required."
            radiusM < 5f -> "Geofence radius must be at least 5 meters."
            minLux >= maxLux -> "Minimum light lux must be less than maximum lux."
            else -> null
        }

    fun toCheckpoint(): Checkpoint {
        val targetId = if (!checkpointId.isNullOrBlank()) checkpointId else "CP_${System.currentTimeMillis()}"
        return Checkpoint(
            id = targetId,
            gameId = gameId,
            name = name.trim(),
            lat = lat,
            lng = lng,
            radiusM = radiusM,
            lightSignature = LightSignature(minLux, maxLux),
            clue = clue.trim(),
            lore = lore.trim(),
            order = order,
            motionType = motionType
        )
    }
}
