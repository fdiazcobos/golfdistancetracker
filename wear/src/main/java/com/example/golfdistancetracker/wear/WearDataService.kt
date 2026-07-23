package com.example.golfdistancetracker.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearDataService @Inject constructor(
    private val context: Context
) {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    suspend fun sendShotToPhone(
        clubName: String,
        distance: Double?,
        tempo: String?,
        isPractice: Boolean
    ) {
        val nodes = nodeClient.connectedNodes.await()
        val payload = "CLUB:$clubName|DIST:$distance|TEMPO:$tempo|PRACTICE:$isPractice"
        
        for (node in nodes) {
            messageClient.sendMessage(node.id, "/shot_detected", payload.toByteArray()).await()
        }
    }
}
