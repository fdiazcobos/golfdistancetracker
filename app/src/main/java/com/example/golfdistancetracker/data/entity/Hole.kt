package com.example.golfdistancetracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "holes",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Hole(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val holeNumber: Int,
    val par: Int = 4,
    val distance: Int? = null,
    val notes: String? = null
)
