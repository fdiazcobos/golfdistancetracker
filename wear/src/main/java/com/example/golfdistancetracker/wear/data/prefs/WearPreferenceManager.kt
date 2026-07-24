package com.example.golfdistancetracker.wear.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("wear_settings")

@Singleton
class WearPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val GPS_SOURCE_KEY = stringPreferencesKey("gps_source")
    private val AUTO_IMPACT_KEY = booleanPreferencesKey("auto_impact_detection")

    val gpsSource: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GPS_SOURCE_KEY] ?: "Phone"
    }

    val autoImpactDetection: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_IMPACT_KEY] ?: true
    }

    suspend fun updateGpsSource(source: String) {
        context.dataStore.edit { prefs -> prefs[GPS_SOURCE_KEY] = source }
    }

    suspend fun updateAutoImpact(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[AUTO_IMPACT_KEY] = enabled }
    }
}
