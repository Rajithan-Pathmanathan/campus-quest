package com.campusquest.ui.creator.validation

/**
 * Limits for creator input. Radius: the relic appears within 10 m in-app, and geofence alerts
 * use at least 100 m, so 5-500 m covers every useful checkpoint.
 */
object CreatorLimits {
    const val TITLE_MAX_LENGTH = 60
    const val DESCRIPTION_MAX_LENGTH = 500
    const val NAME_MAX_LENGTH = 60
    const val CLUE_MAX_LENGTH = 300
    const val LORE_MAX_LENGTH = 1000

    const val MIN_RADIUS_M = 5f
    const val MAX_RADIUS_M = 500f
    const val MAX_LUX = 100_000f

    /** A quest needs at least one place to visit. */
    const val MIN_CHECKPOINTS_TO_PUBLISH = 1

    val RARITIES = listOf("COMMON", "RARE", "EPIC", "LEGENDARY")
}