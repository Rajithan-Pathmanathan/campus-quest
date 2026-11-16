package com.campusquest.domain.quest

import com.campusquest.domain.model.LightSignature
import com.campusquest.domain.model.MotionType
import com.campusquest.domain.model.TrialStatus
import kotlin.math.roundToInt

/**
 * Turns raw sensor values into what the player sees and feels. Players never see percentages,
 * lux or accuracy numbers — only rounded distance, warmth words, a heartbeat and story hints.
 */

/** Rings around an ember, from far away to "the relic appears". */
enum class QuestZone { FAR, OUTER, MIDDLE, RELIC }

object QuestZones {
    const val OUTER_RING_M = 200f
    const val MIDDLE_RING_M = 100f
    /** The relic pops up within this distance... */
    const val RELIC_RADIUS_M = 10f
    /** ...but only if GPS is at least this accurate (otherwise players "arrive" by GPS noise). */
    const val RELIC_MAX_ACCURACY_M = 15f
    /** Once shown, the relic stays until the player is this far away (stops flicker at the edge). */
    const val RELIC_EXIT_M = 20f
}

/**
 * Next zone, with hysteresis for the relic ring: the relic appears within RELIC_RADIUS_M (with good
 * accuracy) and only disappears again beyond RELIC_EXIT_M. Pass FAR as [previous] for a new target.
 */
fun nextQuestZone(previous: QuestZone, distanceM: Float?, accuracyM: Float?): QuestZone {
    if (distanceM == null) return QuestZone.FAR
    val accurate = accuracyM != null && accuracyM <= QuestZones.RELIC_MAX_ACCURACY_M
    return when {
        previous == QuestZone.RELIC && distanceM <= QuestZones.RELIC_EXIT_M -> QuestZone.RELIC
        distanceM <= QuestZones.RELIC_RADIUS_M && accurate -> QuestZone.RELIC
        distanceM <= QuestZones.MIDDLE_RING_M -> QuestZone.MIDDLE
        distanceM <= QuestZones.OUTER_RING_M -> QuestZone.OUTER
        else -> QuestZone.FAR
    }
}

/** What kind of light an ember needs, derived from its light signature. */
enum class LightMode {
    BRIGHT, SHADE, DARK;

    companion object {
        fun of(signature: LightSignature): LightMode = when {
            signature.maxLux <= 100f -> DARK
            signature.minLux >= 10_000f -> BRIGHT
            else -> SHADE
        }
    }
}

/** Flame size shown instead of a fusion percentage. */
enum class EmberGlow(val label: String) {
    COLD("The ember is cold…"),
    FAINT("A faint glow…"),
    FLICKERING("The ember flickers…"),
    STEADY("The ember burns steadily"),
    BLAZING("The ember is blazing!")
}

object QuestCues {

    /** Rounded distance; below 20 m GPS jitter makes exact metres misleading, so use words. */
    fun distanceText(distanceM: Float): String = when {
        distanceM >= 1000f -> "~${"%.1f".format(distanceM / 1000f)} km"
        distanceM >= 100f -> "~${((distanceM / 10f).roundToInt() * 10)} m"
        distanceM > 20f -> "~${((distanceM / 5f).roundToInt() * 5)} m"
        else -> "Very close…"
    }

    fun warmth(distanceM: Float): String = when {
        distanceM > 300f -> "Freezing"
        distanceM > QuestZones.OUTER_RING_M -> "Cold"
        distanceM > QuestZones.MIDDLE_RING_M -> "Warm"
        distanceM > QuestZones.RELIC_EXIT_M -> "Hot"
        else -> "Burning!"
    }

    /** Heartbeat vibration interval: slow at the outer ring, fast near the relic, none when far. */
    fun heartbeatIntervalMs(zone: QuestZone, distanceM: Float): Long? {
        if (zone == QuestZone.FAR) return null
        val t = ((distanceM - QuestZones.RELIC_RADIUS_M) /
            (QuestZones.OUTER_RING_M - QuestZones.RELIC_RADIUS_M)).coerceIn(0f, 1f)
        return (300 + t * 1200).toLong() // 300 ms near the relic … 1500 ms at 200 m
    }

    fun zoneLine(zone: QuestZone): String = when (zone) {
        QuestZone.FAR -> "The ember is far… follow the warmth."
        QuestZone.OUTER -> "The Resonator hears a heartbeat!"
        QuestZone.MIDDLE -> "You're close — protect the ember on the way in."
        QuestZone.RELIC -> "✨ The ember appears!"
    }

    /** Story hint for the light the player is standing in. */
    fun lightHint(signature: LightSignature, lux: Float?): String {
        if (lux == null) return "The ember waits…"
        if (signature.matches(lux)) return "The ember glows — this light is just right."
        val tooBright = lux > signature.maxLux
        return when (LightMode.of(signature)) {
            LightMode.BRIGHT -> "Too dark… the ember is cold. Step into the sunlight."
            LightMode.SHADE -> if (tooBright) "Too much sun! The ember is drowning in light. Find shade."
            else "Too dark here… find softer daylight."
            LightMode.DARK -> "It's afraid of the light… cover it, or find somewhere dark."
        }
    }

    fun glow(progressPercent: Int): EmberGlow = when {
        progressPercent >= 85 -> EmberGlow.BLAZING
        progressPercent >= 65 -> EmberGlow.STEADY
        progressPercent >= 40 -> EmberGlow.FLICKERING
        progressPercent >= 15 -> EmberGlow.FAINT
        else -> EmberGlow.COLD
    }

    /** The chapter trial, in story words. */
    fun trialHint(motionType: MotionType, status: TrialStatus?): String {
        if (status?.completed == true) return when (motionType) {
            MotionType.SHAKE -> "The ember is awake!"
            MotionType.TILT -> "The holy water is safe."
            MotionType.SWEEP -> "The cobwebs are gone."
        }
        return when (motionType) {
            MotionType.SHAKE -> when (val left = (status?.remaining ?: 3)) {
                3 -> "It's falling asleep — shake the Resonator to wake it!"
                1 -> "One more shake!"
                else -> "$left more shakes!"
            }
            MotionType.SWEEP -> when (val left = (status?.remaining ?: 3)) {
                3 -> "Cobwebs block the way — sweep them aside!"
                1 -> "One more sweep!"
                else -> "$left more sweeps!"
            }
            MotionType.TILT -> when {
                status == null || !status.started -> "Hold the jar of holy water level. Don't spill a drop!"
                status.justSpilled -> "Spilled! Steady… start again."
                else -> "Steady… keep it level."
            }
        }
    }

    fun shieldHint(near: Boolean): String =
        if (near) "The ember is safe in your hands." else "Cup the ember — hold the Resonator close (or press and hold here)."
}
