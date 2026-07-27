package com.example.golfdistancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.entity.ShotType
import com.example.golfdistancetracker.data.repository.GolfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CaddieTip(
    val title: String,
    val description: String,
    val severity: TipSeverity = TipSeverity.INFO,
    val category: String = "General"
)

enum class TipSeverity { INFO, WARNING, SUCCESS }

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repository: GolfRepository
) : ViewModel() {

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val tips = repository.clubStats.map { stats ->
        val generatedTips = mutableListOf<CaddieTip>()

        if (stats.isEmpty()) {
            generatedTips.add(CaddieTip("Welcome!", "Start recording shots to receive personalized advice.", TipSeverity.INFO, "Getting Started"))
            return@map generatedTips
        }

        // 1. Performance Bias: Practice vs Field
        analyzePracticeVsField(stats, generatedTips)

        // 2. Club Overlaps
        analyzeClubGapping(stats, generatedTips)

        // 3. Specific Club Patterns
        stats.forEach { clubStat ->
            analyzeClubPatterns(clubStat, generatedTips)
        }

        generatedTips.ifEmpty { 
            listOf(CaddieTip("Steady Progress", "Continue practicing to uncover deeper insights into your game.", TipSeverity.INFO, "General")) 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun analyzePracticeVsField(stats: List<ClubStats>, tips: MutableList<CaddieTip>) {
        val allFieldShots = stats.flatMap { it.shots }.filter { it.shotType == ShotType.FIELD }
        val allPracticeShots = stats.flatMap { it.shots }.filter { it.shotType == ShotType.DRIVING_RANGE }

        if (allFieldShots.size >= 10 && allPracticeShots.size >= 10) {
            val fieldAccuracy = allFieldShots.count { it.quality == 2 || (it.lateralDeviation != null && Math.abs(it.lateralDeviation!!) < 10) }.toDouble() / allFieldShots.size
            val practiceAccuracy = allPracticeShots.count { it.quality == 2 || (it.deviation != null && Math.abs(it.deviation!!) < 0.5f) }.toDouble() / allPracticeShots.size

            if (practiceAccuracy > fieldAccuracy + 0.2) {
                tips.add(CaddieTip(
                    "Driving Range Pro", 
                    "Your practice accuracy is much higher than on the course. Try implementing a pre-shot routine to bridge the gap.",
                    TipSeverity.WARNING,
                    "Consistency"
                ))
            }
        }
    }

    private fun analyzeClubGapping(stats: List<ClubStats>, tips: MutableList<CaddieTip>) {
        val sortedStats = stats.filter { it.averageDistance != null }.sortedByDescending { it.averageDistance }
        
        sortedStats.windowed(2).forEach { (higher, lower) ->
            val gap = higher.averageDistance!! - lower.averageDistance!!
            if (gap < 5.0) {
                tips.add(CaddieTip(
                    "Redundant Clubs?", 
                    "Your ${higher.club.name} and ${lower.club.name} travel almost the same distance. You might only need one of them.",
                    TipSeverity.INFO,
                    "Bag Gapping"
                ))
            } else if (gap > 20.0) {
                tips.add(CaddieTip(
                    "Large Distance Gap", 
                    "There is a ${gap.toInt()}m gap between your ${higher.club.name} and ${lower.club.name}. Consider adding a club in between.",
                    TipSeverity.WARNING,
                    "Bag Gapping"
                ))
            }
        }
    }

    private fun analyzeClubPatterns(clubStat: ClubStats, tips: MutableList<CaddieTip>) {
        val shots = clubStat.shots
        if (shots.size < 8) return

        val misses = shots.filter { it.isMishit || (it.quality ?: 1) == 0 }
        if (misses.size >= 3) {
            val leftMisses = misses.count { (it.lateralDeviation ?: it.deviation?.toDouble() ?: 0.0) < -0.5 }
            val rightMisses = misses.count { (it.lateralDeviation ?: it.deviation?.toDouble() ?: 0.0) > 0.5 }
            
            if (leftMisses.toDouble() / misses.size > 0.65) {
                tips.add(CaddieTip(
                    "Hook Pattern: ${clubStat.club.name}", 
                    "You tend to pull this club to the left. Try checking your alignment or relaxing your grip.",
                    TipSeverity.WARNING,
                    "Shot Patterns"
                ))
            } else if (rightMisses.toDouble() / misses.size > 0.65) {
                tips.add(CaddieTip(
                    "Slice Pattern: ${clubStat.club.name}", 
                    "You have a strong tendency to miss right. Focus on closing the face through impact.",
                    TipSeverity.WARNING,
                    "Shot Patterns"
                ))
            }
        }

        if (clubStat.accuracyPct > 0.75) {
            tips.add(CaddieTip(
                "Money Club: ${clubStat.club.name}", 
                "Your accuracy with this club is outstanding. It's your safest bet for narrow fairways.",
                TipSeverity.SUCCESS,
                "Consistency"
            ))
        }
    }
}
