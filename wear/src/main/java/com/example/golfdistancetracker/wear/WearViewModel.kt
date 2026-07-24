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

enum class WearScreen {
    MODE_SELECTION,
    CLUB_SELECTION,
    READY_TO_HIT,
    WALKING,
    DIRECTION_INPUT,
    PRACTICE_RATING,
    SUMMARY
}

data class WearUiState(
    val screen: WearScreen = WearScreen.MODE_SELECTION,
    val mode: WearMode = WearMode.PLAY,
    val currentClub: String = "7 Iron",
    val isTracking: Boolean = false,
    val lastTempo: String? = null,
    val currentShotDistance: Double? = null,
    val lastShotDistance: Double? = null,
    val message: String = "Ready",
    val currentLocation: Location? = null,
    val startLocation: Location? = null,
    val isUsingPhoneGps: Boolean = false,
    val lastShotDirection: String? = null
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

        // Periodic status updates
        viewModelScope.launch {
            while(true) {
                val isConnected = locationHelper.isPhoneConnected()
                val loc = if (_uiState.value.isTracking) locationHelper.getCurrentLocation() else null
                
                _uiState.update { it.copy(
                    currentLocation = loc,
                    isUsingPhoneGps = isConnected
                ) }
                
                if (_uiState.value.screen == WearScreen.WALKING) {
                    updateWalkingDistance()
                }
                kotlinx.coroutines.delay(2000)
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
                    _uiState.update { it.copy(
                        screen = WearScreen.WALKING,
                        startLocation = impactLoc,
                        currentShotDistance = 0.0,
                        lastTempo = tempoText
                    ) }
                } else {
                    _uiState.update { it.copy(
                        screen = WearScreen.PRACTICE_RATING,
                        lastTempo = tempoText
                    ) }
                }
            }
        }
    }

    fun selectMode(mode: WearMode) {
        _uiState.update { it.copy(mode = mode, screen = WearScreen.CLUB_SELECTION) }
    }

    fun selectClub(club: String) {
        _uiState.update { it.copy(currentClub = club, screen = WearScreen.READY_TO_HIT) }
        swingAnalyzer.start()
        _uiState.update { it.copy(isTracking = true) }
    }

    fun manualMarkShot() {
        viewModelScope.launch {
            val impactLoc = locationHelper.getCurrentLocation()
            if (_uiState.value.mode == WearMode.PLAY) {
                _uiState.update { it.copy(
                    screen = WearScreen.WALKING,
                    startLocation = impactLoc,
                    currentShotDistance = 0.0
                ) }
            }
        }
    }

    fun reachedBall() {
        _uiState.update { it.copy(
            screen = WearScreen.DIRECTION_INPUT,
            lastShotDistance = it.currentShotDistance
        ) }
    }

    fun selectDirection(direction: String) {
        val state = _uiState.value
        viewModelScope.launch {
            dataService.sendShotToPhone(
                clubName = state.currentClub,
                distance = state.lastShotDistance,
                tempo = state.lastTempo,
                isPractice = false,
                direction = direction
            )
            _uiState.update { it.copy(screen = WearScreen.SUMMARY, lastShotDirection = direction) }
        }
    }

    fun ratePracticeShot(quality: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            dataService.sendShotToPhone(
                clubName = state.currentClub,
                distance = null,
                tempo = state.lastTempo,
                isPractice = true,
                quality = quality
            )
            _uiState.update { it.copy(screen = WearScreen.READY_TO_HIT) }
        }
    }

    fun resetToStart() {
        swingAnalyzer.stop()
        _uiState.update { WearUiState() }
    }
}
