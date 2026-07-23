package com.example.golfdistancetracker.data.db

import androidx.room.TypeConverter
import com.example.golfdistancetracker.data.entity.ShotType

class Converters {
    @TypeConverter
    fun fromShotType(value: ShotType): String {
        return value.name
    }

    @TypeConverter
    fun toShotType(value: String): ShotType {
        return ShotType.valueOf(value)
    }
}
