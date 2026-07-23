package com.example.golfdistancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.entity.Club
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.data.entity.ShotType
import com.example.golfdistancetracker.data.prefs.DistanceUnit
import com.example.golfdistancetracker.data.repository.GolfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class StatsFilters(
    val selectedClubIds: Set<Long> = emptySet(),
    val shotType: ShotType? = null,
    val startDate: Long? = null
)

data class ClubStats(
    val club: Club,
    val averageDistance: Double?,
    val avgLatDev: Double?,
    val accuracyPct: Double,
    val mishitCount: Int,
    val shots: List<Shot>,
    val unit: DistanceUnit = DistanceUnit.METERS,
    val gapToNext: Double? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: GolfRepository
) : ViewModel() {

    private val _filters = MutableStateFlow(StatsFilters())
    val filters = _filters.asStateFlow()

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val stats = combine(
        repository.clubStats,
        _filters
    ) { allStats, filters ->
        val processed = allStats.map { stat ->
            val filteredShots = stat.shots.filter { shot ->
                (filters.shotType == null || shot.shotType == filters.shotType) &&
                (filters.startDate == null || shot.timestamp >= filters.startDate)
            }
            
            val avgDist = filteredShots.mapNotNull { it.distance }.average().takeIf { !it.isNaN() }
            val avgLatDev = filteredShots.mapNotNull { it.lateralDeviation }.average().takeIf { !it.isNaN() }
            val mishits = filteredShots.count { it.isMishit }
            
            val accurateShots = filteredShots.count { 
                it.quality == 2 || (it.deviation != null && Math.abs(it.deviation) < 0.5f) 
            }
            val accuracy = if (filteredShots.isNotEmpty()) accurateShots.toDouble() / filteredShots.size else 0.0

            stat.copy(
                averageDistance = avgDist,
                avgLatDev = avgLatDev,
                accuracyPct = accuracy,
                mishitCount = mishits,
                shots = filteredShots
            )
        }.sortedByDescending { it.averageDistance ?: 0.0 }

        // Calculate Gaps
        processed.mapIndexed { index, stat ->
            val nextStat = processed.getOrNull(index + 1)
            val gap = if (stat.averageDistance != null && nextStat?.averageDistance != null) {
                stat.averageDistance - nextStat.averageDistance
            } else null
            stat.copy(gapToNext = gap)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateShotTypeFilter(type: ShotType?) {
        _filters.update { it.copy(shotType = type) }
    }
}
