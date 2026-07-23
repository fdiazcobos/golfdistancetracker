package com.example.golfdistancetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ShotType {
    FIELD, DRIVING_RANGE
}

@Entity(tableName = "shots")
data class Shot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clubId: Long,
    val holeId: Long? = null,
    val shotType: ShotType = ShotType.FIELD,
    
    // GPS Data (Optional for Practice)
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val distance: Double? = null,
    val direction: Float? = null, // Azimuth in degrees from North
    val intendedHeading: Float? = null, // What the compass showed at start
    val lateralDeviation: Double? = null, // In meters, calculated
    
    // Manual Tracking (Driving Range)
    val deviation: Float? = null, // -2 (Mucho Izq) to 2 (Mucho Der)
    val quality: Int? = null, // 0 (Malo), 1 (Bien), 2 (Muy Bien)
    val isMishit: Boolean = false,
    val notes: String? = null,
    
    val timestamp: Long = System.currentTimeMillis()
)
