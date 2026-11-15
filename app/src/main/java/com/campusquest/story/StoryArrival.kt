package com.campusquest.story

/** A task opens only after recent accurate fixes remain inside its entry radius. */
class StoryArrival(private val radius: Float = 15f) {
    private val hold = ContinuousHold(3000)
    var arrived = false; private set
    fun update(distance: Float?, accuracy: Float?, fixAgeMs: Long, active: Boolean, now: Long): Boolean {
        val usable = active && distance != null && distance.isFinite() && accuracy != null && accuracy.isFinite() && accuracy <= 20f && fixAgeMs in 0..15000
        if (!usable || distance!! > radius + 10f) { reset(); return false }
        if (!arrived && hold.update(distance <= radius, now) >= 1f) arrived = true
        return arrived
    }
    fun reset() { arrived = false; hold.reset() }
}
