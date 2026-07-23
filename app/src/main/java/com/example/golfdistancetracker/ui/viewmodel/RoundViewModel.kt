package com.example.golfdistancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.dao.CourseDao
import com.example.golfdistancetracker.data.dao.RoundDao
import com.example.golfdistancetracker.data.entity.Course
import com.example.golfdistancetracker.data.entity.HoleScore
import com.example.golfdistancetracker.data.entity.Round
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoundUiState(
    val currentRound: Round? = null,
    val holeScores: List<HoleScore> = emptyList(),
    val courses: List<Course> = emptyList()
)

@HiltViewModel
class RoundViewModel @Inject constructor(
    private val roundDao: RoundDao,
    private val courseDao: CourseDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoundUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            courseDao.getAllCourses().collect { list ->
                _uiState.update { it.copy(courses = list) }
            }
        }
    }

    fun startNewRound(course: Course) {
        viewModelScope.launch {
            val roundId = roundDao.insertRound(Round(courseId = course.id))
            // Initialize holes based on course configuration
            for (i in 1..course.numberOfHoles) {
                roundDao.insertHoleScore(HoleScore(roundId = roundId, holeNumber = i))
            }
            loadRound(roundId)
        }
    }

    fun loadRound(roundId: Long) {
        viewModelScope.launch {
            roundDao.getHoleScoresForRound(roundId).collect { scores ->
                _uiState.update { it.copy(holeScores = scores) }
            }
        }
    }

    fun updateHoleScore(score: HoleScore) {
        viewModelScope.launch {
            roundDao.insertHoleScore(score)
            // Update total score in Round
            _uiState.value.currentRound?.let { round ->
                val total = _uiState.value.holeScores.sumOf { it.strokes }
                roundDao.updateRound(round.copy(totalScore = total))
            }
        }
    }
}
