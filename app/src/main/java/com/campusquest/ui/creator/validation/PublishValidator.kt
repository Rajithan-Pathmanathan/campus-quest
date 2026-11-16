package com.campusquest.ui.creator.validation

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.CheckpointActivity
import com.campusquest.domain.model.Game
import com.campusquest.domain.model.GameStatus

object PublishValidator {

    /** Returns readable problems. An empty list means the game can be published. */
    fun validate(game: Game?, checkpoints: List<Checkpoint>): List<String> {
        val problems = mutableListOf<String>()
        if (game == null) return listOf("This game could not be found. Save the draft first.")
        if (game.status != GameStatus.DRAFT) problems += "Only draft games can be published."

        GameValidator.validateTitle(game.title)?.let { problems += it }
        GameValidator.validateDescription(game.description, forPublish = true)?.let { problems += it }

        if (checkpoints.size != 3) {
            problems += "This story needs exactly 3 checkpoints."
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
            if (cp.lore.isBlank()) problems += "$label: Lore is required to publish"
            CheckpointValidator.validateRarity(cp.rarity)?.let { problems += "$label: $it" }
            if (cp.order < 1) problems += "$label: Order must be 1 or more"
            val expected = CheckpointActivity.forOrder(index + 1)
            if (CheckpointActivity.fromStored(cp.motionType, cp.order) != expected) {
                problems += "$label: Choose ${expected.label} for chapter ${index + 1}"
            }
        }

        val duplicateOrders = checkpoints.groupBy { it.order }.filter { it.value.size > 1 }.keys
        duplicateOrders.forEach { problems += "More than one checkpoint has order $it." }

        return problems
    }
}
