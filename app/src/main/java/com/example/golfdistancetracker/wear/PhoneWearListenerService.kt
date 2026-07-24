package com.example.golfdistancetracker.wear

import android.util.Log
import com.example.golfdistancetracker.data.dao.ClubDao
import com.example.golfdistancetracker.data.dao.ShotDao
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.data.entity.ShotType
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class PhoneWearListenerService : WearableListenerService() {

    private val TAG = "PhoneWearListener"

    @Inject
    lateinit var shotDao: ShotDao

    @Inject
    lateinit var clubDao: ClubDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received: ${messageEvent.path}")
        if (messageEvent.path == "/shot_detected") {
            val payload = String(messageEvent.data)
            handleShotPayload(payload, messageEvent.sourceNodeId)
        }
    }

    private fun handleShotPayload(payload: String, nodeId: String) {
        Log.d(TAG, "Payload: $payload")
        // Payload format: CLUB_ID:$id|DIST:$distance|TEMPO:$tempo|PRACTICE:$isPractice|DIR:$dir|QUAL:$qual
        val parts = payload.split("|").associate { 
            val pair = it.split(":")
            pair[0] to pair.getOrNull(1)
        }

        val clubId = parts["CLUB_ID"]?.toLongOrNull()
        val distance = parts["DIST"]?.toDoubleOrNull()
        val tempo = parts["TEMPO"]
        val isPractice = parts["PRACTICE"]?.toBoolean() ?: false
        val directionStr = parts["DIR"]
        val qualityVal = parts["QUAL"]?.toIntOrNull()

        scope.launch {
            if (clubId != null) {
                // Convert string direction (Left, Straight, Right) to float deviation
                val deviation = when(directionStr) {
                    "Left" -> -1.5f
                    "Right" -> 1.5f
                    "Straight" -> 0f
                    else -> null
                }

                shotDao.insertShot(
                    Shot(
                        clubId = clubId,
                        shotType = if (isPractice) ShotType.DRIVING_RANGE else ShotType.FIELD,
                        distance = distance,
                        notes = "Tempo: $tempo (Watch)",
                        quality = qualityVal ?: 1,
                        deviation = deviation
                    )
                )
                
                Log.d(TAG, "Shot saved successfully for clubId: $clubId")
                
                // Send confirmation back to watch
                try {
                    Wearable.getMessageClient(this@PhoneWearListenerService)
                        .sendMessage(nodeId, "/shot_saved_ack", "OK".toByteArray())
                        .await()
                    Log.d(TAG, "ACK sent to watch")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send ACK", e)
                }
            } else {
                Log.e(TAG, "Received shot with null clubId")
            }
        }
    }
}
