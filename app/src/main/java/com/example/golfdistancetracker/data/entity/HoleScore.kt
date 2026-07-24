package com.example.golfdistancetracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "hole_scores",
    foreignKeys = [
        ForeignKey(
            entity = Round::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HoleScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundId: Long,
    val playerId: Long, // Link to who made this score
    val holeNumber: Int,
    val strokes: Int = 0,
    val putts: Int = 0,
    val fairwayHit: Boolean = true,
    val sandSaves: Int = 0,
    val penalties: Int = 0
)
