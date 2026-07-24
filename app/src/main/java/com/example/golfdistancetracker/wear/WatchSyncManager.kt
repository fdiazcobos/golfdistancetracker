package com.example.golfdistancetracker.wear

import android.content.Context
import com.example.golfdistancetracker.data.dao.ClubDao
import com.example.golfdistancetracker.data.dao.ShotDao
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val dataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startSync() {
        // Sync stats
        scope.launch {
            shotDao.getAllShots().collectLatest { shots ->
                // ... same midnight logic
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

                dataClient.putDataItem(request)
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
}
