package com.campusquest.domain.model

enum class CheckpointActivity(val label: String) {
    TILT_MAZE("Tilt Maze"),
    QR_SCAN("QR Scan"),
    LAMP_OIL("Lamp-Oil Pouring");

    companion object {
        fun forOrder(order: Int): CheckpointActivity = entries.getOrElse(order - 1) { TILT_MAZE }

        fun fromStored(value: String?, order: Int): CheckpointActivity =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: forOrder(order)

        fun fromLabel(label: String): CheckpointActivity =
            entries.firstOrNull { it.label == label } ?: TILT_MAZE
    }
}
