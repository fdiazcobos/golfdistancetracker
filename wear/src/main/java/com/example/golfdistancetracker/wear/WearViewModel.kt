package com.example.golfdistancetracker.wear

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WearMode {
    PLAY, PRACTICE
}

data class WearUiState(
    val mode: WearMode = WearMode.PLAY,
    val currentClub: String = "Driver",
    val isTracking: Boolean = false,
    val lastTempo: String? = null,
    val currentShotDistance: Double? = null,
    val message: String = "Ready",
    val currentLocation: Location? = null,
    val startLocation: Location? = null,
    val isUsingPhoneGps: Boolean = false
)

@HiltViewModel
class WearViewModel @Inject constructor(
    private val swingAnalyzer: SwingAnalyzer,
    private val locationHelper: LocationHelper,
    private val dataService: WearDataService
) : ViewModel() {

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            swingAnalyzer.events.collect { event ->
                handleSwingEvent(event)
            }
        }

        // Periodic location and status updates
        viewModelScope.launch {
            while(true) {
                val isConnected = locationHelper.isPhoneConnected()
                
                if (_uiState.value.isTracking) {
                    val loc = locationHelper.getCurrentLocation()
                    _uiState.update { it.copy(
                        currentLocation = loc,
                        isUsingPhoneGps = isConnected
                    ) }
                    if (_uiState.value.mode == WearMode.PLAY) {
                        updateWalkingDistance()
                    }
                } else {
                    _uiState.update { it.copy(isUsingPhoneGps = isConnected) }
                }
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    private fun updateWalkingDistance() {
        val start = _uiState.value.startLocation
        val current = _uiState.value.currentLocation
        if (start != null && current != null) {
            val dist = start.distanceTo(current).toDouble()
            _uiState.update { it.copy(currentShotDistance = dist) }
        }
    }

    private fun handleSwingEvent(event: SwingEvent) {
        if (event.type == EventType.IMPACT) {
            viewModelScope.launch {
                val impactLoc = locationHelper.getCurrentLocation()
                val tempoText = String.format("%.1f:1", event.tempoRatio ?: 0.0)
                
                if (_uiState.value.mode == WearMode.PLAY) {
                    val start = _uiState.value.startLocation
                    if (start != null && impactLoc != null) {
                        val finalDist = start.distanceTo(impactLoc).toDouble()
                        dataService.sendShotToPhone(
                            clubName = _uiState.value.currentClub,
                            distance = finalDist,
                            tempo = tempoText,
                            isPractice = false
                        )
                    }
                    
                    _uiState.update { it.copy(
                        startLocation = impactLoc,
                        currentShotDistance = 0.0,
                        message = "New Shot!",
                        lastTempo = tempoText
                    ) }
                } else {
                    dataService.sendShotToPhone(
                        clubName = _uiState.value.currentClub,
                        distance = null,
                        tempo = tempoText,
                        isPractice = true
                    )
                    _uiState.update { it.copy(
                        message = "IMPACT!",
                        lastTempo = tempoText
                    ) }
                }
            }
        }
    }

    fun setMode(mode: WearMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun startSession() {
        swingAnalyzer.start()
        _uiState.update { it.copy(isTracking = true, message = "Tracking Active") }
    }

    fun stopSession() {
        swingAnalyzer.stop()
        _uiState.update { it.copy(isTracking = false, startLocation = null, currentShotDistance = null, lastTempo = null) }
    }

    fun selectClub(club: String) {
        _uiState.update { it.copy(currentClub = club) }
    }
}
