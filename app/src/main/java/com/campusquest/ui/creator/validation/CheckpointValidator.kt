package com.campusquest.ui.creator.validation

object CheckpointValidator {

    fun validateName(input: String): String? {
        val name = input.trim()
        if (name.isEmpty()) return "Name is required"
        if (name.length > CreatorLimits.NAME_MAX_LENGTH)
            return "Name must be at most ${CreatorLimits.NAME_MAX_LENGTH} characters"
        return null
    }

    fun validateLatitude(input: String): String? {
        val value = input.trim().toDoubleOrNull()
        if (value == null || !value.isFinite()) return "Latitude must be a number"
        if (value < -90.0 || value > 90.0) return "Latitude must be between -90 and 90"
        return null
    }

    fun validateLongitude(input: String): String? {
        val value = input.trim().toDoubleOrNull()
        if (value == null || !value.isFinite()) return "Longitude must be a number"
        if (value < -180.0 || value > 180.0) return "Longitude must be between -180 and 180"
        return null
    }

    fun validateRadius(input: String): String? {
        val value = input.trim().toFloatOrNull()
        if (value == null || !value.isFinite()) return "Radius must be a number"
        if (value <= 0f) return "Radius must be positive"
        if (value < CreatorLimits.MIN_RADIUS_M) return "Radius must be at least ${CreatorLimits.MIN_RADIUS_M.toInt()} m"
        if (value > CreatorLimits.MAX_RADIUS_M) return "Radius must be at most ${CreatorLimits.MAX_RADIUS_M.toInt()} m"
        return null
    }

    /** Must pass BEFORE you create a LightSignature(minLux, maxLux). */
    fun validateLux(minInput: String, maxInput: String): String? {
        val min = minInput.trim().toFloatOrNull()
        val max = maxInput.trim().toFloatOrNull()
        if (min == null || !min.isFinite()) return "Minimum lux must be a number"
        if (max == null || !max.isFinite()) return "Maximum lux must be a number"
        if (min < 0f) return "Minimum lux cannot be negative"
        if (max < 0f) return "Maximum lux cannot be negative"
        if (max > CreatorLimits.MAX_LUX) return "Maximum lux is too large"
        if (min > max) return "Minimum lux cannot exceed maximum lux"
        return null
    }

    fun validateClue(input: String, forPublish: Boolean): String? {
        val clue = input.trim()
        if (forPublish && clue.isEmpty()) return "Clue is required to publish"
        if (clue.length > CreatorLimits.CLUE_MAX_LENGTH)
            return "Clue must be at most ${CreatorLimits.CLUE_MAX_LENGTH} characters"
        return null
    }

    fun validateRarity(input: String): String? =
        if (input in CreatorLimits.RARITIES) null else "Choose a valid rarity"
}