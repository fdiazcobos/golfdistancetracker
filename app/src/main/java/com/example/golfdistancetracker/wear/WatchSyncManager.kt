package com.example.golfdistancetracker.wear

import android.content.Context
import android.util.Log
import com.example.golfdistancetracker.data.dao.ClubDao
import com.example.golfdistancetracker.data.dao.ShotDao
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shotDao: ShotDao,
    private val clubDao: ClubDao,
    private val preferenceManager: com.example.golfdistancetracker.data.prefs.PreferenceManager
) {
    private val TAG = "WatchSyncManager"
    private val dataClient = Wearable.getDataClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isWatchConnected = MutableStateFlow(false)
    val isWatchConnected = _isWatchConnected.asStateFlow()

    fun startSync() {
        Log.d(TAG, "Starting WatchSyncManager")
        
        // Connectivity Monitor
        scope.launch {
            while(true) {
                try {
                    val nodes = nodeClient.connectedNodes.await()
                    _isWatchConnected.value = nodes.isNotEmpty()
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking nodes", e)
                }
                kotlinx.coroutines.delay(5000)
            }
        }

        // Sync stats automatically on DB change
        scope.launch {
            shotDao.getAllShots().collectLatest { shots ->
                pushStats(shots)
            }
        }

        // Sync Bag automatically on change
        scope.launch {
            clubDao.getAllClubs().collectLatest { clubs ->
                Log.d(TAG, "Bag changed, pushing to watch")
                pushBag(clubs)
            }
        }

        // Sync settings
        scope.launch {
            combine(
                preferenceManager.impactThreshold,
                preferenceManager.autoImpactDetection,
                preferenceManager.gpsSource
            ) { threshold, auto, gps ->
                val request = PutDataMapRequest.create("/settings").apply {
                    dataMap.putFloat("impact_threshold", threshold)
                    dataMap.putBoolean("auto_impact", auto)
                    dataMap.putString("gps_source", gps)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()
                dataClient.putDataItem(request)
            }.collect()
        }
    }

    fun forceSync() {
        scope.launch {
            val shots = shotDao.getAllShots().first()
            pushStats(shots)
            val clubs = clubDao.getAllClubs().first()
            pushBag(clubs)
        }
    }

    private suspend fun pushBag(clubs: List<com.example.golfdistancetracker.data.entity.Club>) {
        val request = PutDataMapRequest.create("/bag_sync").apply {
            dataMap.putInt("club_count", clubs.size)
            clubs.forEachIndexed { index, club ->
                dataMap.putLong("club_id_$index", club.id)
                dataMap.putString("club_name_$index", club.name)
                dataMap.putString("club_type_$index", club.type)
            }
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        
        try {
            dataClient.putDataItem(request).await()
            Log.d(TAG, "Bag pushed successfully: ${clubs.size} clubs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push bag", e)
        }
    }

    private suspend fun pushStats(shots: List<com.example.golfdistancetracker.data.entity.Shot>) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        val todayShots = shots.filter { it.timestamp >= startOfDay }
        
        val allClubs = clubDao.getAllClubs().first()
        val clubMap = allClubs.associateBy { it.id }

        val usageByName = todayShots
            .groupBy { it.clubId }
            .mapNotNull { (id, groupedShots) -> 
                clubMap[id]?.name?.let { it to groupedShots.size } 
            }.toMap()

        val request = PutDataMapRequest.create("/daily_stats").apply {
            dataMap.putInt("total_today", todayShots.size)
            usageByName.forEach { (name, count) ->
                dataMap.putInt("usage_$name", count)
            }
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        try {
            dataClient.putDataItem(request).await()
            Log.d(TAG, "Stats pushed successfully: Total=${todayShots.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push stats", e)
        }
    }
}
