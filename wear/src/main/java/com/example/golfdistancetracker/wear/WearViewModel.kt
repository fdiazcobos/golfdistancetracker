package com.example.golfdistancetracker.wear

import android.location.Location
import android.util.Log
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

data class SyncedClub(
    val id: Long,
    val name: String,
    val type: String
)

data class WearUiState(
    val screen: WearScreen = WearScreen.MODE_SELECTION,
    val mode: WearMode = WearMode.PLAY,
    val currentClub: SyncedClub? = null,
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
    val clubUsageMap: Map<String, Int> = emptyMap(),
    val isPhoneAppActive: Boolean = false,
    val syncedClubs: List<SyncedClub> = emptyList(),
    val isSyncingShot: Boolean = false
)

@HiltViewModel
class WearViewModel @Inject constructor(
    private val swingAnalyzer: SwingAnalyzer,
    private val locationHelper: LocationHelper,
    private val dataService: WearDataService,
    private val preferenceManager: WearPreferenceManager,
    @ApplicationContext private val context: android.content.Context
) : ViewModel(), DataClient.OnDataChangedListener, CapabilityClient.OnCapabilityChangedListener, MessageClient.OnMessageReceivedListener {

    private val TAG = "WearViewModel"
    private val dataClient = Wearable.getDataClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val messageClient = Wearable.getMessageClient(context)

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState = _uiState.asStateFlow()

    init {
        dataClient.addListener(this)
        messageClient.addListener(this)
        capabilityClient.addListener(this, "golf_phone_app")
        
        checkPhoneCapability()
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
                    isUsingPhoneGps = isConnected && _uiState.value.gpsSource == "Phone",
                    isPhoneAppActive = isConnected
                ) }
                
                if (_uiState.value.screen == WearScreen.WALKING) {
                    updateWalkingDistance()
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun checkPhoneCapability() {
        viewModelScope.launch {
            try {
                val capabilityInfo = capabilityClient.getCapability("golf_phone_app", CapabilityClient.FILTER_REACHABLE).await()
                _uiState.update { it.copy(isPhoneAppActive = capabilityInfo.nodes.isNotEmpty()) }
            } catch (e: Exception) {
                Log.e(TAG, "Capability check failed", e)
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        _uiState.update { it.copy(isPhoneAppActive = capabilityInfo.nodes.isNotEmpty()) }
        if (capabilityInfo.nodes.isNotEmpty()) {
            fetchInitialData() 
        }
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            try {
                val dataItems = dataClient.dataItems.await()
                dataItems.forEach { item ->
                    when (item.uri.path) {
                        "/daily_stats" -> updateStatsFromMap(DataMapItem.fromDataItem(item).dataMap)
                        "/settings" -> updateSettingsFromMap(DataMapItem.fromDataItem(item).dataMap)
                        "/bag_sync" -> updateBagFromMap(DataMapItem.fromDataItem(item).dataMap)
                    }
                }
                dataItems.release()
            } catch (e: Exception) {
                Log.e(TAG, "Initial fetch failed", e)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                when (event.dataItem.uri.path) {
                    "/daily_stats" -> updateStatsFromMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
                    "/settings" -> updateSettingsFromMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
                    "/bag_sync" -> updateBagFromMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/shot_saved_ack") {
            Log.d(TAG, "Shot ACK received from phone")
            _uiState.update { it.copy(isSyncingShot = false, message = "Saved!") }
        }
    }

    private fun updateBagFromMap(dataMap: DataMap) {
        val count = dataMap.getInt("club_count")
        val clubs = mutableListOf<SyncedClub>()
        for (i in 0 until count) {
            clubs.add(SyncedClub(
                id = dataMap.getLong("club_id_$i"),
                name = dataMap.getString("club_name_$i") ?: "Unknown",
                type = dataMap.getString("club_type_$i") ?: "Iron"
            ))
        }
        _uiState.update { it.copy(syncedClubs = clubs) }
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
        capabilityClient.removeListener(this)
        messageClient.removeListener(this)
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

    fun selectClub(club: SyncedClub) {
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
        val club = state.currentClub ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingShot = true) }
            dataService.sendShotToPhone(
                clubId = club.id,
                clubName = club.name,
                distance = state.lastShotDistance,
                tempo = state.lastTempo,
                isPractice = false,
                direction = direction
            )
            _uiState.update { it.copy(
                screen = WearScreen.SUMMARY, 
                lastShotDirection = direction,
                dailyTotal = it.dailyTotal + 1
            ) }
        }
    }

    fun ratePracticeShot(quality: Int) {
        val state = _uiState.value
        val club = state.currentClub ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingShot = true) }
            dataService.sendShotToPhone(
                clubId = club.id,
                clubName = club.name,
                distance = null,
                tempo = state.lastTempo,
                isPractice = true,
                quality = quality
            )
            // Optimistically update counts
            val newUsage = state.clubUsageMap.toMutableMap()
            newUsage[club.name] = (newUsage[club.name] ?: 0) + 1
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
        _uiState.update { it.copy(screen = WearScreen.MODE_SELECTION, isTracking = false, currentClub = null) }
    }
}
