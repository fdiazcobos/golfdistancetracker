package com.example.golfdistancetracker.util

import com.example.golfdistancetracker.data.entity.Club

enum class SG_Category { TEE, APPROACH, ARG, PUTT }

object StrokesGainedEngine {

    /**
     * Standardized Expected Strokes to Hole Out (Benchmark: Scratch Player)
     * Key: Distance in Meters, Value: Expected Strokes
     */
    private val scratchBenchmark = mapOf(
        450.0 to 4.3,
        400.0 to 4.1,
        350.0 to 3.9,
        300.0 to 3.7,
        250.0 to 3.5,
        200.0 to 3.2,
        180.0 to 3.1,
        150.0 to 3.0,
        120.0 to 2.9,
        100.0 to 2.8,
        80.0 to 2.7,
        50.0 to 2.5,
        30.0 to 2.4, // Around the Green boundary
        20.0 to 2.3,
        10.0 to 2.1,
        5.0 to 1.8,  // Putting boundary
        3.0 to 1.4,
        1.5 to 1.1,
        0.5 to 1.02
    )

    /**
     * Returns expected strokes for a given distance by interpolating benchmark data.
     */
    fun getExpectedStrokes(distanceMeters: Double): Double {
        if (distanceMeters <= 0) return 0.0
        
        val distances = scratchBenchmark.keys.sorted()
        
        // Lower bound
        if (distanceMeters <= distances.first()) return scratchBenchmark[distances.first()]!!
        // Upper bound
        if (distanceMeters >= distances.last()) return scratchBenchmark[distances.last()]!!
        
        // Find interval for interpolation
        var lower = distances.first()
        var upper = distances.last()
        
        for (i in 0 until distances.size - 1) {
            if (distanceMeters >= distances[i] && distanceMeters <= distances[i+1]) {
                lower = distances[i]
                upper = distances[i+1]
                break
            }
        }
        
        val valLower = scratchBenchmark[lower]!!
        val valUpper = scratchBenchmark[upper]!!
        
        // Linear interpolation
        val weight = (distanceMeters - lower) / (upper - lower)
        return valLower + weight * (valUpper - valLower)
    }

    /**
     * Calculates SG for a single shot.
     */
    fun calculateShotSG(startDist: Double, endDist: Double): Double {
        val expStart = getExpectedStrokes(startDist)
        val expEnd = getExpectedStrokes(endDist)
        return expStart - expEnd - 1.0
    }

    /**
     * Categorizes a shot into SG silos.
     */
    fun getCategory(distanceMeters: Double, club: Club?): SG_Category {
        return when {
            club?.type == "Putter" -> SG_Category.PUTT
            distanceMeters <= 30.0 -> SG_Category.ARG
            club?.type == "Driver" || (club?.name?.contains("Madera", ignoreCase = true) == true && distanceMeters > 200) -> SG_Category.TEE
            else -> SG_Category.APPROACH
        }
    }
}
