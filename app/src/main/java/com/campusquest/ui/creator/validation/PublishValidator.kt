package com.campusquest.ui.creator.validation

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus

object PublishValidator {

    /** Returns readable problems. An empty list means the game can be published. */
    fun validate(game: Game?, checkpoints: List<Checkpoint>): List<String> {
        val problems = mutableListOf<String>()
        if (game == null) return listOf("This game could not be found. Save the draft first.")

        GameValidator.validateTitle(game.title)?.let { problems += it }
        GameValidator.validateDescription(game.description, forPublish = true)?.let { problems += it }

        if (checkpoints.size != CreatorLimits.MAX_CHECKPOINTS) {
            problems += "The Golden Tree needs exactly ${CreatorLimits.MAX_CHECKPOINTS} stops (key, instruction card, golden water). This quest has ${checkpoints.size}."
        }

        val sorted = checkpoints.sortedBy { it.order }
        sorted.forEachIndexed { index, cp ->
            val label = "Checkpoint ${index + 1} (${cp.name.ifBlank { "unnamed" }})"
            CheckpointValidator.validateName(cp.name)?.let { problems += "$label: $it" }
            CheckpointValidator.validateLatitude(cp.lat.toString())?.let { problems += "$label: $it" }
            CheckpointValidator.validateLongitude(cp.lng.toString())?.let { problems += "$label: $it" }
            CheckpointValidator.validateRadius(cp.radiusM.toString())?.let { problems += "$label: $it" }
            CheckpointValidator.validateLux(
                cp.lightSignature.minLux.toString(), cp.lightSignature.maxLux.toString()
            )?.let { problems += "$label: $it" }
            CheckpointValidator.validateClue(cp.clue, forPublish = true)?.let { problems += "$label: $it" }
            CheckpointValidator.validateRarity(cp.rarity)?.let { problems += "$label: $it" }
            if (cp.order < 1) problems += "$label: Order must be 1 or more"
        }

        val duplicateOrders = checkpoints.groupBy { it.order }.filter { it.value.size > 1 }.keys
        duplicateOrders.forEach { problems += "More than one checkpoint has order $it." }
        if (checkpoints.size == CreatorLimits.MAX_CHECKPOINTS && checkpoints.map { it.order }.toSet() != setOf(1, 2, 3)) {
            problems += "Number the stops 1, 2 and 3 (use the arrows to reorder)."
        }

        return problems
    }
}