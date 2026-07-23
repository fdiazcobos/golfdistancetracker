package com.example.golfdistancetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rounds")
data class Round(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val totalScore: Int = 0,
    val totalPutts: Int = 0,
    val handicapDifferential: Double? = null
)
