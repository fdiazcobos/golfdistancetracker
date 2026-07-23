package com.example.golfdistancetracker.util

import com.example.golfdistancetracker.data.prefs.DistanceUnit
import java.util.Locale

object UnitConverter {
    private const val METERS_TO_YARDS = 1.09361

    fun formatDistance(meters: Double?, unit: DistanceUnit): String {
        if (meters == null) return "N/A"
        val value = if (unit == DistanceUnit.YARDS) meters * METERS_TO_YARDS else meters
        val suffix = if (unit == DistanceUnit.YARDS) "yd" else "m"
        return String.format(Locale.US, "%.2f %s", value, suffix)
    }
}
