package com.example.golfdistancetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clubs")
data class Club(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "Driver", "Hibrido", "Iron", "Wedge", "Putter"
    val number: String? = null, // e.g., "3", "7", "PW"
    val brand: String? = null,
    val model: String? = null
)
