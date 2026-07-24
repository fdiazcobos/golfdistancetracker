package com.example.golfdistancetracker.wear

import android.content.Context
import com.example.golfdistancetracker.data.dao.ShotDao
import com.example.golfdistancetracker.data.entity.ShotType
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shotDao: ShotDao
) {
    private val dataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startSync() {
        scope.launch {
            shotDao.getAllShots().collectLatest { shots ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis

                val todayShots = shots.filter { it.timestamp >= startOfDay }
                val totalToday = todayShots.size
                
                val usageMap = todayShots
                    .groupBy { it.clubId }
                    .mapValues { it.value.size }

                val request = PutDataMapRequest.create("/daily_stats").apply {
                    dataMap.putInt("total_today", totalToday)
                    // Convert map to key-value pairs for DataMap
                    usageMap.forEach { (clubId, count) ->
                        dataMap.putInt("usage_$clubId", count)
                    }
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()

                dataClient.putDataItem(request)
            }
        }
    }
}
