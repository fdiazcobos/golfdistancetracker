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
import java.util.UUID
import javax.inject.Inject

data class DrivingRangeUiState(
    val selectedClub: Club? = null,
    val deviation: Float = 0f, // -2 to 2
    val quality: Int = 1, // 0: Malo, 1: Bien, 2: Muy Bien
    val isMishit: Boolean = false,
    val saveSuccess: Boolean = false,
    val sessionShotCount: Int = 0,
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

    fun selectClub(club: Club) {
        val isPutter = club.type == "Putter"
        _uiState.update { it.copy(
            selectedClub = club,
            quality = if (isPutter) 0 else 1, // "Bueno" for putter is index 0 in my list
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
            
            // Auto-reset UI state but keep the club
            val isPutter = club.type == "Putter"
            _uiState.update { it.copy(
                saveSuccess = true,
                sessionShotCount = it.sessionShotCount + 1,
                deviation = 0f,
                quality = if (isPutter) 0 else 1,
                isMishit = false
            ) }
            
            kotlinx.coroutines.delay(1500)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }
    
    fun startNewSession() {
        _uiState.update { DrivingRangeUiState() }
    }
}
