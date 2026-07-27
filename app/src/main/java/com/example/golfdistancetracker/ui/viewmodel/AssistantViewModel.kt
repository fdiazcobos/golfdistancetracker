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

        // 2. Club Overlaps & Gapping
        analyzeClubGapping(stats, generatedTips)

        // 3. Specific Club Patterns & Miss Heat
        stats.forEach { clubStat ->
            analyzeClubPatterns(clubStat, generatedTips)
        }

        // 4. Progress Check
        // We could add logic here to compare last 10 shots vs previous shots

        generatedTips.ifEmpty { 
            listOf(CaddieTip("Steady Progress", "Continue practicing to uncover deeper insights into your game.", TipSeverity.INFO, "General")) 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun analyzePracticeVsField(stats: List<ClubStats>, tips: MutableList<CaddieTip>) {
        val allFieldShots = stats.flatMap { it.shots }.filter { it.shotType == ShotType.FIELD }
        val allPracticeShots = stats.flatMap { it.shots }.filter { it.shotType == ShotType.DRIVING_RANGE }

        if (allFieldShots.size >= 10 && allPracticeShots.size >= 10) {
            val fieldAccuracy = allFieldShots.count { it.quality == 2 || (it.lateralDeviation != null && Math.abs(it.lateralDeviation) < 10) }.toDouble() / allFieldShots.size
            val practiceAccuracy = allPracticeShots.count { it.quality == 2 || (it.deviation != null && Math.abs(it.deviation) < 0.5f) }.toDouble() / allPracticeShots.size

            if (practiceAccuracy > fieldAccuracy + 0.2) {
                tips.add(CaddieTip(
                    "Driving Range Pro", 
                    "Your practice accuracy is much higher than on the course. Focus on your pre-shot routine during practice to simulate course pressure.",
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
            if (gap < 6.0) {
                tips.add(CaddieTip(
                    "Overlap Warning: ${higher.club.name} & ${lower.club.name}", 
                    "These clubs have a gap of only ${String.format("%.1f", gap)}m. You might be carrying two clubs for the same job.",
                    TipSeverity.INFO,
                    "Bag Management"
                ))
            } else if (gap > 22.0) {
                tips.add(CaddieTip(
                    "Large Gap: ${higher.club.name} to ${lower.club.name}", 
                    "There's a ${gap.toInt()}m gap here. You might struggle to hit precise targets in between these distances.",
                    TipSeverity.WARNING,
                    "Bag Management"
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
                    "Left Bias: ${clubStat.club.name}", 
                    "Most of your misses are to the Left (Hooks/Pulls). Check your aim and ensure you're not closing the face too early.",
                    TipSeverity.WARNING,
                    "Shot Patterns"
                ))
            } else if (rightMisses.toDouble() / misses.size > 0.65) {
                tips.add(CaddieTip(
                    "Right Bias: ${clubStat.club.name}", 
                    "You have a strong tendency to miss to the Right (Slices/Pushes). Try maintaining a stronger grip or smoother release.",
                    TipSeverity.WARNING,
                    "Shot Patterns"
                ))
            }
        }

        if (clubStat.accuracyPct > 0.8) {
            tips.add(CaddieTip(
                "Sniper Status: ${clubStat.club.name}", 
                "Your consistency with this club is top-tier. Use it with confidence for narrow targets.",
                TipSeverity.SUCCESS,
                "Consistency"
            ))
        }
    }
}
