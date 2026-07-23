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
     * Calculates the "Plays Like" distance based on wind.
     * @param baseDistance Meters to target
     * @param windSpeedMps Wind speed in meters per second
     * @param windDeg Wind direction in degrees (0 = North)
     * @param shotHeading User's heading in degrees (0 = North)
     */
    fun calculatePlaysLikeDistance(
        baseDistance: Double,
        windSpeedMps: Double,
        windDeg: Int,
        shotHeading: Float
    ): Double {
        val windSpeedKmH = windSpeedMps * 3.6
        val angleDiffRad = Math.toRadians((windDeg - shotHeading).toDouble())
        
        // Parallel component of wind (negative for headwind if we define 0 as following)
        // Cos(0) = 1 (Tailwind), Cos(180) = -1 (Headwind)
        val parallelWind = windSpeedKmH * Math.cos(angleDiffRad)
        
        // Basic heuristic: 
        // Headwind adds ~1% per 1.5 km/h
        // Tailwind subtracts ~0.5% per 1.5 km/h
        val adjFactor = if (parallelWind < 0) { // Headwind
            1.0 + (Math.abs(parallelWind) / 1.5) * 0.01
        } else { // Tailwind
            1.0 - (parallelWind / 1.5) * 0.005
        }
        
        return baseDistance * adjFactor
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
