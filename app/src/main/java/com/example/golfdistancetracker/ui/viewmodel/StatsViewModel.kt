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
import java.util.Calendar
import javax.inject.Inject

data class StatsFilters(
    val selectedClubIds: Set<Long> = emptySet(),
    val shotType: ShotType? = null,
    val startDate: Long? = null,
    val includeMisshotsInDispersion: Boolean = true,
    val showHeatmap: Boolean = false
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

enum class SessionType { PLAY, PRACTICE }

data class HistorySession(
    val id: String,
    val type: SessionType,
    val date: Long,
    val title: String,
    val shotsCount: Int,
    val accuracy: Double,
    val trend: Double?, // positive = better than average
    val qualityBreakdown: QualityBreakdown
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
            val breakdown = calculateBreakdown(filteredShots)

            val avgDist = filteredShots.mapNotNull { it.distance }.average().takeIf { !it.isNaN() }
            val avgLatDev = filteredShots.mapNotNull { it.lateralDeviation }.average().takeIf { !it.isNaN() }
            val mishits = filteredShots.count { it.isMishit }
            
            val accurateShots = filteredShots.count { 
                it.quality == 2 || (it.deviation != null && Math.abs(it.deviation!!) < 0.5f) 
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

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val historySessions = combine(
        shotDao.getAllShots(),
        roundDao.getAllRounds(),
        courseDao.getAllCourses(),
        clubStats
    ) { allShots, allRounds, allCourses, stats ->
        val sessions = mutableListOf<HistorySession>()
        val courseMap = allCourses.associateBy { it.id }
        
        // Lifetime average accuracy for trend
        val totalShots = stats.sumOf { it.shots.size }
        val lifetimeAccuracy = if (totalShots > 0) stats.sumOf { it.accuracyPct * it.shots.size } / totalShots else 0.0

        // 1. Practice Sessions (Grouped by practiceSessionId or Day)
        val practiceShots = allShots.filter { it.shotType == ShotType.DRIVING_RANGE }
        val groupedPractice = practiceShots.groupBy { 
            it.practiceSessionId ?: getDayString(it.timestamp)
        }

        groupedPractice.forEach { (sid, shots) ->
            val accuracy = shots.count { it.quality == 2 }.toDouble() / shots.size
            sessions.add(HistorySession(
                id = sid,
                type = SessionType.PRACTICE,
                date = shots.first().timestamp,
                title = "Driving Range Session",
                shotsCount = shots.size,
                accuracy = accuracy,
                trend = if (lifetimeAccuracy > 0) accuracy - lifetimeAccuracy else null,
                qualityBreakdown = calculateBreakdown(shots)
            ))
        }

        // 2. Play Rounds
        allRounds.forEach { round ->
            // For now accuracy is 0 as we don't link shots to rounds yet, but we have total score
            sessions.add(HistorySession(
                id = "round_${round.id}",
                type = SessionType.PLAY,
                date = round.timestamp,
                title = courseMap[round.courseId]?.name ?: "Golf Round",
                shotsCount = round.totalScore,
                accuracy = 0.0, 
                trend = null,
                qualityBreakdown = QualityBreakdown()
            ))
        }

        sessions.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun calculateBreakdown(shots: List<Shot>): QualityBreakdown {
        val total = shots.size.toDouble()
        return if (total > 0) {
            QualityBreakdown(
                misshotPct = shots.count { it.isMishit }.toDouble() / total,
                poorPct = shots.count { !it.isMishit && it.quality == 0 }.toDouble() / total,
                goodPct = shots.count { !it.isMishit && it.quality == 1 }.toDouble() / total,
                greatPct = shots.count { !it.isMishit && it.quality == 2 }.toDouble() / total
            )
        } else QualityBreakdown()
    }

    private fun getDayString(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    fun updateShotTypeFilter(type: ShotType?) {
        _filters.update { it.copy(shotType = type) }
    }

    fun toggleMisshotsInDispersion(include: Boolean) {
        _filters.update { it.copy(includeMisshotsInDispersion = include) }
    }

    fun toggleHeatmap(show: Boolean) {
        _filters.update { it.copy(showHeatmap = show) }
    }

    fun resetAllStats() {
        viewModelScope.launch {
            shotDao.deleteAllShots()
        }
    }
}
