package com.campusquest.ui.quest

import com.campusquest.domain.model.Checkpoint

/**
 * Game-specific progression (M4 workplan §36-38, §90). The next chapter comes from the game's own
 * checkpoint order and the player's progress, never from hard-coded checkpoint ids.
 */
object NextCheckpointResolver {

    /** First checkpoint, by `order`, that the player has not found yet; null when the quest is complete. */
    fun next(checkpoints: List<Checkpoint>, completedIds: Collection<String>): Checkpoint? =
        checkpoints.sortedBy { it.order }.firstOrNull { it.id !in completedIds }

    /** True when [current] is the only checkpoint the player still has to find. */
    fun isFinal(checkpoints: List<Checkpoint>, completedIds: Collection<String>, current: Checkpoint): Boolean =
        checkpoints.none { it.id != current.id && it.id !in completedIds }
}
