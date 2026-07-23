package com.example.golfdistancetracker.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

enum class DistanceUnit {
    METERS, YARDS
}

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val UNIT_KEY = stringPreferencesKey("distance_unit")

    val distanceUnit: Flow<DistanceUnit> = context.dataStore.data.map { prefs ->
        val unitName = prefs[UNIT_KEY] ?: DistanceUnit.METERS.name
        DistanceUnit.valueOf(unitName)
    }

    suspend fun updateDistanceUnit(unit: DistanceUnit) {
        context.dataStore.edit { prefs ->
            prefs[UNIT_KEY] = unit.name
        }
    }
}
