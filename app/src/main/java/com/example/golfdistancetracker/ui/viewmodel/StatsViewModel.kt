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
    val mishitPct: Double = 0.0,
    val toppedPct: Double = 0.0,
    val fatPct: Double = 0.0,
    val cleanPct: Double = 0.0,
    val purePct: Double = 0.0
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
    val qualityBreakdown: QualityBreakdown,
    val originalId: Long? = null // Round ID if PLAY
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
            val mishits = filteredShots.count { it.isMishit || it.quality == 0 }
            
            val accurateShots = filteredShots.count { 
                it.quality == 4 || it.quality == 3 || (it.deviation != null && Math.abs(it.deviation) < 0.5f) 
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
        
        val totalShotsCount = stats.sumOf { it.shots.size }
        val lifetimeAccuracy = if (totalShotsCount > 0) stats.sumOf { it.accuracyPct * it.shots.size } / totalShotsCount else 0.0

        // 1. Practice Sessions (Grouped strictly by day to unify phone and watch)
        val practiceShots = allShots.filter { it.shotType == ShotType.DRIVING_RANGE }
        val groupedByDay = practiceShots.groupBy { getDayKey(it.timestamp) }

        groupedByDay.forEach { (dayKey, shots) ->
            val accuracy = shots.count { it.quality == 4 || it.quality == 3 }.toDouble() / shots.size
            sessions.add(HistorySession(
                id = "practice_$dayKey",
                type = SessionType.PRACTICE,
                date = shots.first().timestamp,
                title = "Daily Practice Summary",
                shotsCount = shots.size,
                accuracy = accuracy,
                trend = if (lifetimeAccuracy > 0) accuracy - lifetimeAccuracy else null,
                qualityBreakdown = calculateBreakdown(shots)
            ))
        }

        // 2. Play Rounds
        allRounds.forEach { round ->
            sessions.add(HistorySession(
                id = "round_${round.id}",
                type = SessionType.PLAY,
                date = round.timestamp,
                title = courseMap[round.courseId]?.name ?: "Golf Round",
                shotsCount = round.totalScore,
                accuracy = 0.0, 
                trend = null,
                qualityBreakdown = QualityBreakdown(),
                originalId = round.id
            ))
        }

        sessions.filter { it.shotsCount > 0 }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun calculateBreakdown(shots: List<Shot>): QualityBreakdown {
        val total = shots.size.toDouble()
        return if (total > 0) {
            QualityBreakdown(
                mishitPct = shots.count { it.isMishit || it.quality == 0 }.toDouble() / total,
                toppedPct = shots.count { !it.isMishit && it.quality == 1 }.toDouble() / total,
                fatPct = shots.count { !it.isMishit && it.quality == 2 }.toDouble() / total,
                cleanPct = shots.count { !it.isMishit && it.quality == 3 }.toDouble() / total,
                purePct = shots.count { !it.isMishit && it.quality == 4 }.toDouble() / total
            )
        } else QualityBreakdown()
    }

    private fun getDayKey(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    fun deleteSession(session: HistorySession) {
        viewModelScope.launch {
            if (session.type == SessionType.PLAY) {
                session.originalId?.let { roundDao.deleteRoundById(it) }
            } else {
                // Delete all practice shots for that day
                val cal = Calendar.getInstance().apply { timeInMillis = session.date }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val end = cal.timeInMillis - 1
                
                shotDao.deleteShotsInRange(ShotType.DRIVING_RANGE, start, end)
            }
        }
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
