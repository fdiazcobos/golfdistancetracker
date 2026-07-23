package com.example.golfdistancetracker.wear

import com.example.golfdistancetracker.data.dao.ClubDao
import com.example.golfdistancetracker.data.dao.ShotDao
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.data.entity.ShotType
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhoneWearListenerService : WearableListenerService() {

    @Inject
    lateinit var shotDao: ShotDao

    @Inject
    lateinit var clubDao: ClubDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/shot_detected") {
            val payload = String(messageEvent.data)
            handleShotPayload(payload)
        }
    }

    private fun handleShotPayload(payload: String) {
        // Payload format: CLUB:$clubName|DIST:$distance|TEMPO:$tempo|PRACTICE:$isPractice
        val parts = payload.split("|").associate { 
            val pair = it.split(":")
            pair[0] to pair.getOrNull(1)
        }

        val clubName = parts["CLUB"] ?: "Unknown"
        val distance = parts["DIST"]?.toDoubleOrNull()
        val tempo = parts["TEMPO"]
        val isPractice = parts["PRACTICE"]?.toBoolean() ?: false

        scope.launch {
            val club = clubDao.getAllClubs().first().find { it.name == clubName }
            if (club != null) {
                shotDao.insertShot(
                    Shot(
                        clubId = club.id,
                        shotType = if (isPractice) ShotType.DRIVING_RANGE else ShotType.FIELD,
                        distance = distance,
                        notes = "Tempo: $tempo (Watch)",
                        quality = 1 // Default to "Bien"
                    )
                )
            }
        }
    }
}
