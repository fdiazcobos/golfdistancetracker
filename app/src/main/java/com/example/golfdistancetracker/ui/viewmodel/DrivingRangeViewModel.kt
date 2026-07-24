package com.example.golfdistancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.dao.ClubDao
import com.example.golfdistancetracker.data.dao.ShotDao
import com.example.golfdistancetracker.data.entity.Club
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.data.entity.ShotType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class DrivingRangeUiState(
    val selectedClub: Club? = null,
    val deviation: Float = 0f,
    val quality: Int = 1,
    val isMishit: Boolean = false,
    val saveSuccess: Boolean = false,
    val dailyTotalShots: Int = 0,
    val clubUsageToday: Map<Long, Int> = emptyMap(),
    val currentSessionId: String = UUID.randomUUID().toString()
)

@HiltViewModel
class DrivingRangeViewModel @Inject constructor(
    private val clubDao: ClubDao,
    private val shotDao: ShotDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrivingRangeUiState())
    val uiState = _uiState.asStateFlow()

    val clubs = clubDao.getAllClubs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            shotDao.getAllShots().collect { shots ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis

                val todayShots = shots.filter { it.timestamp >= startOfDay && it.shotType == ShotType.DRIVING_RANGE }
                
                _uiState.update { it.copy(
                    dailyTotalShots = todayShots.size,
                    clubUsageToday = todayShots.groupBy { it.clubId }.mapValues { it.value.size }
                ) }
            }
        }
    }

    fun selectClub(club: Club) {
        val isPutter = club.type == "Putter"
        _uiState.update { it.copy(
            selectedClub = club,
            quality = if (isPutter) 0 else 1,
            deviation = 0f,
            isMishit = false
        ) }
    }

    fun updateDeviation(value: Float) {
        _uiState.update { it.copy(deviation = value) }
    }

    fun updateQuality(value: Int) {
        _uiState.update { it.copy(quality = value) }
    }

    fun toggleMishit(value: Boolean) {
        _uiState.update { it.copy(isMishit = value) }
    }

    fun saveShot() {
        val state = _uiState.value
        val club = state.selectedClub ?: return

        viewModelScope.launch {
            shotDao.insertShot(
                Shot(
                    clubId = club.id,
                    shotType = ShotType.DRIVING_RANGE,
                    practiceSessionId = state.currentSessionId,
                    deviation = state.deviation,
                    quality = state.quality,
                    isMishit = state.isMishit
                )
            )
            
            val isPutter = club.type == "Putter"
            _uiState.update { it.copy(
                saveSuccess = true,
                deviation = 0f,
                quality = if (isPutter) 0 else 1,
                isMishit = false
            ) }
            
            kotlinx.coroutines.delay(1500)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }
}
