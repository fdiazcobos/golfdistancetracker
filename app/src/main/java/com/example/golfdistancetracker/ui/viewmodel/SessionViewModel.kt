package com.example.golfdistancetracker.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.dao.ClubDao
import com.example.golfdistancetracker.data.dao.ShotDao
import com.example.golfdistancetracker.data.entity.Club
import com.example.golfdistancetracker.data.entity.Shot
import com.example.golfdistancetracker.data.entity.ShotType
import com.example.golfdistancetracker.data.network.WeatherService
import com.example.golfdistancetracker.data.prefs.DistanceUnit
import com.example.golfdistancetracker.data.prefs.PreferenceManager
import com.example.golfdistancetracker.data.repository.GolfRepository
import com.example.golfdistancetracker.util.CaddieBrain
import com.example.golfdistancetracker.util.CompassHelper
import com.example.golfdistancetracker.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeatherInfo(
    val temp: Double,
    val windSpeed: Double,
    val windDeg: Int
)

data class SessionUiState(
    val selectedClub: Club? = null,
    val currentPosition: Location? = null,
    val currentHeading: Float = 0f,
    val intendedHeading: Float = 0f,
    val startLocation: Location? = null,
    val lastShotDistance: Double? = null,
    val lastShotLatDev: Double? = null,
    val isGpsReady: Boolean = false,
    val distanceUnit: DistanceUnit = DistanceUnit.METERS,
    val weather: WeatherInfo? = null,
    val targetDistanceMeters: Double? = null,
    val playsLikeDistance: Double? = null,
    val recommendedClub: Club? = null,
    val clubUsage: Map<Long, Int> = emptyMap()
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val clubDao: ClubDao,
    private val shotDao: ShotDao,
    private val locationHelper: LocationHelper,
    private val compassHelper: CompassHelper,
    private val preferenceManager: PreferenceManager,
    private val weatherService: WeatherService,
    private val repository: GolfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState = _uiState.asStateFlow()

    val clubs = clubDao.getAllClubs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            shotDao.getAllShots().collect { shots ->
                val today = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                val usage = shots
                    .filter { it.timestamp > today && it.shotType == ShotType.FIELD }
                    .groupBy { it.clubId }
                    .mapValues { it.value.size }
                _uiState.update { it.copy(clubUsage = usage) }
            }
        }
        viewModelScope.launch {
            locationHelper.getLocationUpdates().collect { location ->
                _uiState.update { it.copy(
                    currentPosition = location,
                    isGpsReady = location.accuracy < 20
                ) }
                fetchWeather(location.latitude, location.longitude)
            }
        }
        viewModelScope.launch {
            compassHelper.getHeadingUpdates().collect { heading ->
                _uiState.update { it.copy(currentHeading = heading) }
                updateCaddieInsights()
            }
        }
        viewModelScope.launch {
            preferenceManager.distanceUnit.collect { unit ->
                _uiState.update { it.copy(distanceUnit = unit) }
            }
        }
    }

    private suspend fun fetchWeather(lat: Double, lon: Double) {
        try {
            val key = preferenceManager.weatherApiKey.first()
            val response = weatherService.getWeather(lat, lon, key)
            _uiState.update { it.copy(
                weather = WeatherInfo(response.main.temp, response.wind.speed, response.wind.deg)
            ) }
            updateCaddieInsights()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    private fun updateCaddieInsights() {
        val state = _uiState.value
        val target = state.targetDistanceMeters ?: return
        val weather = state.weather
        
        val playsLike = if (weather != null) {
            CaddieBrain.calculatePlaysLikeDistance(
                target,
                weather.windSpeed,
                weather.windDeg,
                state.currentHeading
            )
        } else target

        viewModelScope.launch {
            val stats = repository.clubStats.first()
            val recommendation = CaddieBrain.recommendClub(playsLike, stats)
            _uiState.update { it.copy(playsLikeDistance = playsLike, recommendedClub = recommendation) }
        }
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun updateTargetDistance(dist: Double?) {
        _uiState.update { it.copy(targetDistanceMeters = dist) }
        updateCaddieInsights()
    }

    fun selectClub(club: Club) {
        _uiState.update { it.copy(selectedClub = club, startLocation = null, lastShotDistance = null) }
    }

    fun resetSession() {
        _uiState.update { it.copy(selectedClub = null, startLocation = null, targetDistanceMeters = null, playsLikeDistance = null) }
    }

    fun markStart() {
        val current = _uiState.value.currentPosition
        if (current != null && _uiState.value.isGpsReady) {
            _uiState.update { it.copy(
                startLocation = current, 
                lastShotDistance = null,
                intendedHeading = _uiState.value.currentHeading
            ) }
        }
    }

    fun markEnd() {
        val club = _uiState.value.selectedClub
        val start = _uiState.value.startLocation
        val end = _uiState.value.currentPosition
        val intended = _uiState.value.intendedHeading
        
        if (club != null && start != null && end != null) {
            val distance = start.distanceTo(end).toDouble()
            val actualBearing = start.bearingTo(end)
            
            val normActual = (actualBearing + 360) % 360
            val normIntended = (intended + 360) % 360
            
            val angleDiff = Math.toRadians((normActual - normIntended).toDouble())
            val latDev = distance * Math.sin(angleDiff)

            viewModelScope.launch {
                shotDao.insertShot(
                    Shot(
                        clubId = club.id,
                        shotType = ShotType.FIELD,
                        startLatitude = start.latitude,
                        startLongitude = start.longitude,
                        endLatitude = end.latitude,
                        endLongitude = end.longitude,
                        distance = distance,
                        direction = normActual.toFloat(),
                        intendedHeading = normIntended,
                        lateralDeviation = latDev
                    )
                )
            }
            _uiState.update { it.copy(
                lastShotDistance = distance, 
                lastShotLatDev = latDev, 
                startLocation = null,
                targetDistanceMeters = null,
                playsLikeDistance = null
            ) }
        }
    }
}
