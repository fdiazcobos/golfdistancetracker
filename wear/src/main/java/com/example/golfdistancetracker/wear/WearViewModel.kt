package com.example.golfdistancetracker.wear

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.wear.data.prefs.WearPreferenceManager
import com.google.android.gms.wearable.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    SUMMARY,
    SETTINGS
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
    val lastShotDirection: String? = null,
    val autoImpactEnabled: Boolean = true,
    val gpsSource: String = "Phone",
    val impactThreshold: Float = 35f,
    val dailyTotal: Int = 0,
    val clubUsageMap: Map<String, Int> = emptyMap()
)

@HiltViewModel
class WearViewModel @Inject constructor(
    private val swingAnalyzer: SwingAnalyzer,
    private val locationHelper: LocationHelper,
    private val dataService: WearDataService,
    private val preferenceManager: WearPreferenceManager,
    @ApplicationContext private val context: android.content.Context
) : ViewModel(), DataClient.OnDataChangedListener {

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState = _uiState.asStateFlow()

    private val dataClient = Wearable.getDataClient(context)

    init {
        dataClient.addListener(this)
        
        // Initial Fetch
        fetchInitialData()

        viewModelScope.launch {
            swingAnalyzer.events.collect { event ->
                if (_uiState.value.autoImpactEnabled) {
                    handleSwingEvent(event)
                }
            }
        }

        viewModelScope.launch {
            combine(
                preferenceManager.gpsSource,
                preferenceManager.autoImpactDetection,
                preferenceManager.impactThreshold
            ) { source, auto, threshold ->
                swingAnalyzer.updateThreshold(threshold)
                _uiState.update { it.copy(
                    gpsSource = source, 
                    autoImpactEnabled = auto,
                    impactThreshold = threshold
                ) }
            }.collect()
        }

        // Periodic status updates
        viewModelScope.launch {
            while(true) {
                val isConnected = locationHelper.isPhoneConnected()
                val loc = if (_uiState.value.isTracking) locationHelper.getCurrentLocation() else null
                
                _uiState.update { it.copy(
                    currentLocation = loc,
                    isUsingPhoneGps = isConnected && _uiState.value.gpsSource == "Phone"
                ) }
                
                if (_uiState.value.screen == WearScreen.WALKING) {
                    updateWalkingDistance()
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            try {
                val dataItems = Wearable.getDataClient(context).dataItems.await()
                dataItems.forEach { item ->
                    if (item.uri.path == "/daily_stats") {
                        updateStatsFromMap(DataMapItem.fromDataItem(item).dataMap)
                    } else if (item.uri.path == "/settings") {
                        updateSettingsFromMap(DataMapItem.fromDataItem(item).dataMap)
                    }
                }
                dataItems.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                when (event.dataItem.uri.path) {
                    "/daily_stats" -> updateStatsFromMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
                    "/settings" -> updateSettingsFromMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
                }
            }
        }
    }

    private fun updateStatsFromMap(dataMap: DataMap) {
        val total = dataMap.getInt("total_today")
        val usage = mutableMapOf<String, Int>()
        dataMap.keySet().filter { it.startsWith("usage_") }.forEach { key ->
            val clubName = key.removePrefix("usage_")
            usage[clubName] = dataMap.getInt(key)
        }
        _uiState.update { it.copy(dailyTotal = total, clubUsageMap = usage) }
    }

    private fun updateSettingsFromMap(dataMap: DataMap) {
        val threshold = dataMap.getFloat("impact_threshold")
        val auto = dataMap.getBoolean("auto_impact")
        val gps = dataMap.getString("gps_source") ?: "Phone"
        
        viewModelScope.launch {
            preferenceManager.updateImpactThreshold(threshold)
            preferenceManager.updateAutoImpact(auto)
            preferenceManager.updateGpsSource(gps)
        }
    }

    override fun onCleared() {
        dataClient.removeListener(this)
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
                val impactLoc = if (_uiState.value.mode == WearMode.PLAY) locationHelper.getCurrentLocation() else null
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
        val state = _uiState.value
        if (state.mode == WearMode.PRACTICE) {
            _uiState.update { it.copy(screen = WearScreen.PRACTICE_RATING) }
        } else {
            _uiState.update { it.copy(screen = WearScreen.WALKING, currentShotDistance = 0.0) }
            viewModelScope.launch {
                val impactLoc = locationHelper.getCurrentLocation()
                _uiState.update { it.copy(startLocation = impactLoc) }
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
            // Optimistic Update
            _uiState.update { it.copy(
                screen = WearScreen.SUMMARY, 
                lastShotDirection = direction,
                dailyTotal = it.dailyTotal + 1
            ) }
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
            // Optimistically update counts
            val newUsage = state.clubUsageMap.toMutableMap()
            newUsage[state.currentClub] = (newUsage[state.currentClub] ?: 0) + 1
            _uiState.update { it.copy(
                screen = WearScreen.READY_TO_HIT, 
                dailyTotal = it.dailyTotal + 1,
                clubUsageMap = newUsage
            ) }
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(screen = WearScreen.SETTINGS) }
    }

    fun updateAutoImpact(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.updateAutoImpact(enabled) }
    }

    fun updateImpactThreshold(value: Float) {
        viewModelScope.launch { preferenceManager.updateImpactThreshold(value) }
    }

    fun updateGpsSource(source: String) {
        viewModelScope.launch { preferenceManager.updateGpsSource(source) }
    }

    fun resetToStart() {
        swingAnalyzer.stop()
        _uiState.update { it.copy(screen = WearScreen.MODE_SELECTION, isTracking = false) }
    }
}
