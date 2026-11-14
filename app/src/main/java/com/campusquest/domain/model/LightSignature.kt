package com.campusquest.domain.model

data class LightSignature(
    val minLux: Float,
    val maxLux: Float
) {
    init {
        require(minLux >= 0f) { "minLux must be non-negative" }
        require(maxLux >= 0f) { "maxLux must be non-negative" }
        require(minLux <= maxLux) { "minLux ($minLux) cannot exceed maxLux ($maxLux)" }
    }

    fun matches(currentLux: Float): Boolean {
        return currentLux in minLux..maxLux
    }

    /**
     * Continuous light-match score in [0,1]:
     *   1.0 inside [minLux, maxLux],
     *   falling linearly to 0.0 at [tolerance] lux outside the range.
     * tolerance = 25% of the range width, but never less than [MIN_TOLERANCE_LUX].
     */
    fun score(currentLux: Float): Float {
        if (matches(currentLux)) return 1.0f
        val tolerance = maxOf((maxLux - minLux) * 0.25f, MIN_TOLERANCE_LUX)
        val outside = if (currentLux < minLux) minLux - currentLux else currentLux - maxLux
        return (1.0f - outside / tolerance).coerceIn(0.0f, 1.0f)
    }

    companion object {
        const val MIN_TOLERANCE_LUX = 20f
    }
}
