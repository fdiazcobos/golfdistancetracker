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
import javax.inject.Inject

data class DrivingRangeUiState(
    val selectedClub: Club? = null,
    val deviation: Float = 0f, // -2 to 2
    val quality: Int = 1, // 0: Malo, 1: Bien, 2: Muy Bien
    val isMishit: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class DrivingRangeViewModel @Inject constructor(
    private val clubDao: ClubDao,
    private val shotDao: ShotDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrivingRangeUiState())
    val uiState = _uiState.asStateFlow()

    val clubs = clubDao.getAllClubs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectClub(club: Club) {
        _uiState.update { it.copy(selectedClub = club) }
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
                    deviation = state.deviation,
                    quality = state.quality,
                    isMishit = state.isMishit
                )
            )
            _uiState.update { it.copy(saveSuccess = true) }
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }
}
