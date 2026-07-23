package com.example.golfdistancetracker.data.repository

import com.example.golfdistancetracker.data.dao.ClubDao
import com.example.golfdistancetracker.data.dao.ShotDao
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.ui.viewmodel.ClubStats
import com.example.golfdistancetracker.data.prefs.PreferenceManager
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GolfRepository @Inject constructor(
    private val clubDao: ClubDao,
    private val shotDao: ShotDao,
    private val preferenceManager: PreferenceManager
) {
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val clubStats: Flow<List<ClubStats>> = combine(
        clubDao.getAllClubs(),
        shotDao.getAllShots(),
        preferenceManager.distanceUnit
    ) { clubs, allShots, unit ->
        clubs.map { club ->
            val clubShots = allShots.filter { it.clubId == club.id }
            val avgDist = clubShots.mapNotNull { it.distance }.average().takeIf { !it.isNaN() }
            val avgLatDev = clubShots.mapNotNull { it.lateralDeviation }.average().takeIf { !it.isNaN() }
            val mishits = clubShots.count { it.isMishit }
            val accurateShots = clubShots.count { it.quality == 2 || (it.deviation != null && Math.abs(it.deviation) < 0.5f) }
            val accuracy = if (clubShots.isNotEmpty()) accurateShots.toDouble() / clubShots.size else 0.0

            ClubStats(club, avgDist, avgLatDev, accuracy, mishits, clubShots, unit)
        }
    }
}
