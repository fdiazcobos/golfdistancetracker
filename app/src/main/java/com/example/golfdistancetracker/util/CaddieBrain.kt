package com.example.golfdistancetracker.util

import com.example.golfdistancetracker.data.entity.Club
import com.example.golfdistancetracker.ui.viewmodel.ClubStats

object CaddieBrain {

    fun recommendClub(targetMeters: Double, clubStats: List<ClubStats>): Club? {
        if (clubStats.isEmpty()) return null
        
        return clubStats
            .filter { it.averageDistance != null }
            .minByOrNull { Math.abs(it.averageDistance!! - targetMeters) }
            ?.club
    }

    /**
     * Calculates the "Plays Like" distance based on wind and temperature.
     * @param baseDistance Meters to target
     * @param windSpeedMps Wind speed in meters per second
     * @param windDeg Wind direction in degrees (0 = North)
     * @param shotHeading User's heading in degrees (0 = North)
     * @param tempCelsius Current temperature in Celsius
     */
    fun calculatePlaysLikeDistance(
        baseDistance: Double,
        windSpeedMps: Double,
        windDeg: Int,
        shotHeading: Float,
        tempCelsius: Double? = 20.0
    ): Double {
        val windSpeedKmH = windSpeedMps * 3.6
        
        // Decompose wind into Parallel (Head/Tail)
        // Cos(0) = 1 (Tailwind), Cos(180) = -1 (Headwind)
        // Note: windDeg is where wind COMES FROM. 
        // If windDeg = 0 (North) and shotHeading = 0 (North), it's a HEADWIND.
        // So angleDiff = 0 -> Cos(0) = 1 -> Headwind
        val relativeWindAngle = Math.toRadians((windDeg - shotHeading).toDouble())
        val headwindComponent = windSpeedKmH * Math.cos(relativeWindAngle)
        
        // 1. Wind Adjustment
        // Professional heuristic: 
        // Headwind: adds ~1.5% distance per 5 km/h (increases with spin)
        // Tailwind: subtracts ~0.8% distance per 5 km/h
        var windAdj = 1.0
        if (headwindComponent > 0) { // Coming towards us
            windAdj += (headwindComponent / 5.0) * 0.015
        } else { // Behind us
            windAdj -= (Math.abs(headwindComponent) / 5.0) * 0.008
        }

        // 2. Temperature Adjustment (Air Density)
        // Heuristic: ~1% distance change per 10°C (18°F) deviation from 20°C (68°F)
        val temp = tempCelsius ?: 20.0
        val tempDiff = 20.0 - temp
        val tempAdj = 1.0 + (tempDiff / 10.0) * 0.01

        return baseDistance * windAdj * tempAdj
    }

    fun calculateHandicapIndex(differentials: List<Double>): Double? {
        if (differentials.isEmpty()) return null
        
        val count = differentials.size
        val numToUse = when {
            count >= 20 -> 8
            count >= 15 -> 6
            count >= 12 -> 4
            count >= 9 -> 3
            count >= 7 -> 2
            else -> 1
        }
        
        return differentials.sorted().take(numToUse).average()
    }
}
