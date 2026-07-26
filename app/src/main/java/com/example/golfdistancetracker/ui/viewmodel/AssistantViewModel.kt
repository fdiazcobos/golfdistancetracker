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
    val severity: TipSeverity = TipSeverity.INFO
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
            generatedTips.add(CaddieTip("Welcome!", "Start recording shots to receive personalized advice."))
            return@map generatedTips
        }

        stats.forEach { clubStat ->
            val shots = clubStat.shots
            if (shots.size >= 5) {
                // Direction Pattern Analysis
                val misses = shots.filter { it.isMishit || (it.quality ?: 1) == 0 }
                if (misses.isNotEmpty()) {
                    val leftMisses = misses.count { (it.lateralDeviation ?: it.deviation?.toDouble() ?: 0.0) < -0.5 }
                    val rightMisses = misses.count { (it.lateralDeviation ?: it.deviation?.toDouble() ?: 0.0) > 0.5 }
                    val totalMisses = misses.size

                    if (leftMisses.toDouble() / totalMisses > 0.6) {
                        generatedTips.add(CaddieTip(
                            "Hook Alert: ${clubStat.club.name}", 
                            "Most of your misses are to the Left. Check your grip pressure.",
                            TipSeverity.WARNING
                        ))
                    } else if (rightMisses.toDouble() / totalMisses > 0.6) {
                        generatedTips.add(CaddieTip(
                            "Slice Alert: ${clubStat.club.name}", 
                            "You tend to miss to the Right. Try closing the face at impact.",
                            TipSeverity.WARNING
                        ))
                    }
                }

                // Consistency Check
                if (clubStat.accuracyPct > 0.8) {
                    generatedTips.add(CaddieTip(
                        "Sniper Mode: ${clubStat.club.name}", 
                        "Your consistency is excellent! You can trust this club for tight targets.",
                        TipSeverity.SUCCESS
                    ))
                }
            }
        }

        // Gapping Advice
        val largeGaps = stats.filter { (it.gapToNext ?: 0.0) > 18.0 }
        if (largeGaps.isNotEmpty()) {
            generatedTips.add(CaddieTip(
                "Bag Gap Detected", 
                "There is a large distance gap after your ${largeGaps.first().club.name}. Consider adding a club.",
                TipSeverity.INFO
            ))
        }

        generatedTips.ifEmpty { 
            listOf(CaddieTip("Keep it up!", "Continue practicing to uncover deeper insights into your game.")) 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
