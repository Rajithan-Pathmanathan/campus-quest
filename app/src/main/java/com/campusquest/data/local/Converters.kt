package com.campusquest.data.local

import androidx.room.TypeConverter
import com.campusquest.domain.model.GameStatus

class Converters {
    @TypeConverter
    fun fromGameStatus(status: GameStatus): String {
        return status.name
    }

    @TypeConverter
    fun toGameStatus(value: String): GameStatus {
        return try {
            GameStatus.valueOf(value)
        } catch (e: Exception) {
            GameStatus.DRAFT
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) emptyList() else value.split(",")
    }
}
