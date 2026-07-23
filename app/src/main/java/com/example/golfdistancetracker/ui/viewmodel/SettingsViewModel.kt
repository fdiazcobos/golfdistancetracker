package com.example.golfdistancetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.golfdistancetracker.data.prefs.*
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

    val themePreference = preferenceManager.themePreference.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemePreference.SYSTEM
    )

    val languagePreference = preferenceManager.languagePreference.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LanguagePreference.AUTO
    )

    fun updateUnit(unit: DistanceUnit) {
        viewModelScope.launch { preferenceManager.updateDistanceUnit(unit) }
    }

    fun updateTheme(theme: ThemePreference) {
        viewModelScope.launch { preferenceManager.updateTheme(theme) }
    }

    fun updateLanguage(lang: LanguagePreference) {
        viewModelScope.launch { preferenceManager.updateLanguage(lang) }
    }
}
