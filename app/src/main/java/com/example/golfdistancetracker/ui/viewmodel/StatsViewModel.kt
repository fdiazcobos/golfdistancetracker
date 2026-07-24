package com.example.golfdistancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.dao.CourseDao
import com.example.golfdistancetracker.data.dao.RoundDao
import com.example.golfdistancetracker.data.dao.ShotDao
import com.example.golfdistancetracker.data.entity.*
import com.example.golfdistancetracker.data.prefs.DistanceUnit
import com.example.golfdistancetracker.data.repository.GolfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsFilters(
    val selectedClubIds: Set<Long> = emptySet(),
    val shotType: ShotType? = null,
    val startDate: Long? = null
)

data class CourseAnalytics(
    val course: Course,
    val averageScore: Double?,
    val bestScore: Int?,
    val roundsCount: Int,
    val rounds: List<Round>
)

data class QualityBreakdown(
    val misshotPct: Double = 0.0,
    val poorPct: Double = 0.0,
    val goodPct: Double = 0.0,
    val greatPct: Double = 0.0
)

data class ClubStats(
    val club: Club,
    val averageDistance: Double?,
    val avgLatDev: Double?,
    val accuracyPct: Double,
    val mishitCount: Int,
    val shots: List<Shot>,
    val unit: DistanceUnit = DistanceUnit.METERS,
    val gapToNext: Double? = null,
    val qualityBreakdown: QualityBreakdown = QualityBreakdown()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: GolfRepository,
    private val shotDao: ShotDao,
    private val roundDao: RoundDao,
    private val courseDao: CourseDao
) : ViewModel() {

    private val _filters = MutableStateFlow(StatsFilters())
    val filters = _filters.asStateFlow()

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val clubStats = combine(
        repository.clubStats,
        _filters
    ) { allStats, filters ->
        val processed = allStats.map { stat ->
            val filteredShots = stat.shots.filter { shot ->
                (filters.shotType == null || shot.shotType == filters.shotType) &&
                (filters.startDate == null || shot.timestamp >= filters.startDate)
            }
            
            val total = filteredShots.size.toDouble()
            val breakdown = if (total > 0) {
                QualityBreakdown(
                    misshotPct = filteredShots.count { it.isMishit }.toDouble() / total,
                    poorPct = filteredShots.count { !it.isMishit && it.quality == 0 }.toDouble() / total,
                    goodPct = filteredShots.count { !it.isMishit && it.quality == 1 }.toDouble() / total,
                    greatPct = filteredShots.count { !it.isMishit && it.quality == 2 }.toDouble() / total
                )
            } else QualityBreakdown()

            val avgDist = filteredShots.mapNotNull { it.distance }.average().takeIf { !it.isNaN() }
            val avgLatDev = filteredShots.mapNotNull { it.lateralDeviation }.average().takeIf { !it.isNaN() }
            val mishits = filteredShots.count { it.isMishit }
            
            val accurateShots = filteredShots.count { 
                it.quality == 2 || (it.deviation != null && Math.abs(it.deviation) < 0.5f) 
            }
            val accuracy = if (total > 0) accurateShots.toDouble() / total else 0.0

            stat.copy(
                averageDistance = avgDist,
                avgLatDev = avgLatDev,
                accuracyPct = accuracy,
                mishitCount = mishits,
                shots = filteredShots,
                qualityBreakdown = breakdown
            )
        }.sortedByDescending { it.averageDistance ?: 0.0 }

        processed.mapIndexed { index, stat ->
            val nextStat = processed.getOrNull(index + 1)
            val gap = if (stat.averageDistance != null && nextStat?.averageDistance != null) {
                stat.averageDistance - nextStat.averageDistance
            } else null
            stat.copy(gapToNext = gap)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courseStats = combine(
        courseDao.getAllCourses(),
        roundDao.getAllRounds()
    ) { courses, rounds ->
        courses.map { course ->
            val courseRounds = rounds.filter { it.courseId == course.id && it.isCompleted }
            CourseAnalytics(
                course = course,
                averageScore = courseRounds.map { it.totalScore }.average().takeIf { !it.isNaN() },
                bestScore = courseRounds.minOfOrNull { it.totalScore },
                roundsCount = courseRounds.size,
                rounds = courseRounds
            )
        }.filter { it.roundsCount > 0 }.sortedBy { it.averageScore ?: Double.MAX_VALUE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateShotTypeFilter(type: ShotType?) {
        _filters.update { it.copy(shotType = type) }
    }

    fun resetAllStats() {
        viewModelScope.launch {
            shotDao.deleteAllShots()
        }
    }
}
