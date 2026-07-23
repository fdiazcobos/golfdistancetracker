package com.example.golfdistancetracker.wear

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class LocationHelper(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            // High accuracy for recording the shot position
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isPhoneConnected(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.any { it.isNearby }
        } catch (e: Exception) {
            false
        }
    }
}
