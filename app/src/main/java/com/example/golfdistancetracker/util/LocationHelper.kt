package com.example.golfdistancetracker.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationHelper(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        client.requestLocationUpdates(request, callback, null)
        awaitClose { client.removeLocationUpdates(callback) }
    }

    fun calculateDistance(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLon, endLat, endLon, results)
        return results[0] // distance in meters
    }

    /**
     * Calculates a coordinate given start, heading (degrees), and distance (meters).
     */
    fun destinationPoint(lat: Double, lon: Double, bearing: Float, distance: Double): Pair<Double, Double> {
        val r = 6371000.0 // Earth radius in meters
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val bearingRad = Math.toRadians(bearing.toDouble())
        val distRatio = distance / r

        val destLatRad = Math.asin(
            Math.sin(latRad) * Math.cos(distRatio) +
                    Math.cos(latRad) * Math.sin(distRatio) * Math.cos(bearingRad)
        )
        val destLonRad = lonRad + Math.atan2(
            Math.sin(bearingRad) * Math.sin(distRatio) * Math.cos(latRad),
            Math.cos(distRatio) - Math.sin(latRad) * Math.sin(destLatRad)
        )

        return Math.toDegrees(destLatRad) to Math.toDegrees(destLonRad)
    }
}
