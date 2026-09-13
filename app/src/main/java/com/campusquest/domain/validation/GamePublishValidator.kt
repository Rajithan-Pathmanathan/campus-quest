package com.campusquest.domain.validation

import com.campusquest.domain.model.Checkpoint
import com.campusquest.domain.model.Game

/**
 * Individual validation failure item describing the invalid field and reason.
 */
data class ValidationError(
    val field: String,
    val message: String
)

/**
 * Outcome of the quest publish validation check.
 */
sealed class GameValidationResult {
    object Valid : GameValidationResult()
    data class Invalid(val errors: List<ValidationError>) : GameValidationResult() {
        val formattedSummary: String
            get() = errors.joinToString("; ") { "${it.field}: ${it.message}" }
    }
}

/**
 * Deterministic quality and integrity gate enforcing all authoring rules before a quest can be published.
 */
class GamePublishValidator {

    fun validate(game: Game, checkpoints: List<Checkpoint>): GameValidationResult {
        val errors = mutableListOf<ValidationError>()

        // 1. Game Title Rule
        if (game.title.isBlank()) {
            errors.add(ValidationError("title", "Title cannot be blank."))
        } else if (game.title.trim().length < 5) {
            errors.add(ValidationError("title", "Title must be at least 5 characters long."))
        }

        // 2. Game Description Rule
        if (game.description.isBlank()) {
            errors.add(ValidationError("description", "Description cannot be blank."))
        } else if (game.description.trim().length < 15) {
            errors.add(ValidationError("description", "Description must be at least 15 characters long."))
        }

        // 3. Minimum Checkpoints Rule
        if (checkpoints.size < 2) {
            errors.add(ValidationError("checkpoints", "A quest must contain at least 2 checkpoints (found ${checkpoints.size})."))
        }

        // 4. Sequential Ordering Rule (1..N)
        val sortedByOrder = checkpoints.sortedBy { it.order }
        val orders = sortedByOrder.map { it.order }
        val expectedOrders = (1..checkpoints.size).toList()
        if (orders != expectedOrders) {
            errors.add(ValidationError("checkpoints.order", "Checkpoint orders must be strictly sequential 1..N without duplicates or gaps (found $orders)."))
        }

        // 5. Individual Checkpoint Constraints
        checkpoints.forEachIndexed { index, cp ->
            val prefix = "checkpoint[${index + 1}] (${cp.name.ifBlank { "Unnamed" }})"

            // Name
            if (cp.name.isBlank()) {
                errors.add(ValidationError("$prefix.name", "Checkpoint name cannot be blank."))
            }

            // Clue
            if (cp.clue.isBlank()) {
                errors.add(ValidationError("$prefix.clue", "Clue riddle cannot be blank."))
            } else if (cp.clue.trim().length < 5) {
                errors.add(ValidationError("$prefix.clue", "Clue riddle must be at least 5 characters long."))
            }

            // Coordinates
            if (cp.lat < -90.0 || cp.lat > 90.0) {
                errors.add(ValidationError("$prefix.lat", "Latitude must be between -90.0 and 90.0 (found ${cp.lat})."))
            }
            if (cp.lng < -180.0 || cp.lng > 180.0) {
                errors.add(ValidationError("$prefix.lng", "Longitude must be between -180.0 and 180.0 (found ${cp.lng})."))
            }

            // Radius
            if (cp.radiusM < 5.0f) {
                errors.add(ValidationError("$prefix.radiusM", "Geofence radius must be at least 5.0m (found ${cp.radiusM}m)."))
            }

            // Light signature
            val light = cp.lightSignature
            if (light.minLux < 0.0f) {
                errors.add(ValidationError("$prefix.lightSignature", "Minimum lux cannot be negative (found ${light.minLux})."))
            }
            if (light.minLux >= light.maxLux) {
                errors.add(ValidationError("$prefix.lightSignature", "Minimum lux (${light.minLux}) must be strictly less than maximum lux (${light.maxLux})."))
            }

            // Motion Gesture
            val validGestures = setOf("SWEEP", "SHAKE", "TILT", "NONE")
            if (cp.motionType !in validGestures) {
                errors.add(ValidationError("$prefix.motionType", "Invalid motion gesture '${cp.motionType}'. Must be one of $validGestures."))
            }
        }

        return if (errors.isEmpty()) {
            GameValidationResult.Valid
        } else {
            GameValidationResult.Invalid(errors)
        }
    }
}
