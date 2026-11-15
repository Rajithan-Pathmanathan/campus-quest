package com.campusquest.ui.quest

object TrailGuidance {
    fun relativeAngle(bearing: Float, heading: Float): Float = ((bearing - heading + 540f) % 360f) - 180f
    fun heat(distance: Float): Float = (1f - distance.coerceAtLeast(0f) / 300f).coerceIn(0f, 1f)
    fun warmth(distance: Float): String = when {
        distance <= 10 -> "VERY HOT"
        distance <= 30 -> "HOT"
        distance <= 100 -> "WARM"
        distance <= 200 -> "COOL"
        else -> "COLD"
    }
}
