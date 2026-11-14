package com.campusquest.ui.creator.validation

/**
 * PLACEHOLDER limits. Replace with the team's agreed values
 * from SHARED_CONTRACTS once confirmed.
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

    val RARITIES = listOf("COMMON", "RARE", "EPIC", "LEGENDARY")
}