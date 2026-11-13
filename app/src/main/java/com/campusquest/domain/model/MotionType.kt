package com.campusquest.domain.model

/**
 * Supported physical motion gesture types configured per Checkpoint.
 */
enum class MotionType {
    SWEEP,
    TILT,
    SHAKE;

    companion object {
        /** Safe mapping from the Checkpoint.motionType string (Firestore/creator). Unknown → SWEEP. */
        fun fromString(value: String?): MotionType =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: SWEEP
    }
}
