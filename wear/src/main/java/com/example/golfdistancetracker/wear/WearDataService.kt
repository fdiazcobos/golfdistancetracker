package com.example.golfdistancetracker.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearDataService @Inject constructor(
    private val context: Context
) {
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    suspend fun sendShotToPhone(
        clubName: String,
        distance: Double?,
        tempo: String?,
        isPractice: Boolean,
        direction: String? = null,
        quality: Int? = null
    ) {
        val nodes = nodeClient.connectedNodes.await()
        val payload = buildString {
            append("CLUB:$clubName|")
            append("DIST:${distance ?: "null"}|")
            append("TEMPO:${tempo ?: "null"}|")
            append("PRACTICE:$isPractice|")
            append("DIR:${direction ?: "null"}|")
            append("QUAL:${quality ?: "null"}")
        }
        
        for (node in nodes) {
            messageClient.sendMessage(node.id, "/shot_detected", payload.toByteArray()).await()
        }
    }
}
