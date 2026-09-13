package com.campusquest.domain.model

data class FusionState(
    val motionVerified: Boolean = false,
    val lightVerified: Boolean = false,
    val proximityVerified: Boolean = false,
    val fusionProgressPercent: Int = 0,
    val isFullyVerified: Boolean = false,
    val currentLux: Float = 0f,
    val currentAcceleration: Float = 0f,
    val currentDistanceCm: Float = 5f,
    val targetSignature: LightSignature? = null
)
