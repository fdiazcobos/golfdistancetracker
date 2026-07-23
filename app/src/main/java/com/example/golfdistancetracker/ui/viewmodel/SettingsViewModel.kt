package com.example.golfdistancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.prefs.DistanceUnit
import com.example.golfdistancetracker.data.prefs.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val distanceUnit = preferenceManager.distanceUnit.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DistanceUnit.METERS
    )

    fun updateUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            preferenceManager.updateDistanceUnit(unit)
        }
    }
}
