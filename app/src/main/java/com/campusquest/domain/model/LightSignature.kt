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
}
