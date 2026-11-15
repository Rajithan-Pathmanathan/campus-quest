package com.campusquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncOperationType {
    CREATE_GAME,
    CREATE_CHECKPOINT,
    UPDATE_CHECKPOINT,
    DELETE_CHECKPOINT,
    PUBLISH_GAME,
    JOIN_GAME,
    RECORD_DISCOVERY,
    UPDATE_PROGRESS
}

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val syncId: Long = 0,
    val operationType: SyncOperationType,
    val entityId: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
