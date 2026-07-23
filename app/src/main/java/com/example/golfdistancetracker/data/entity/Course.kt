package com.example.golfdistancetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String? = null,
    val numberOfHoles: Int = 18, // 6, 9, 18
    val isPar3: Boolean = false
)
