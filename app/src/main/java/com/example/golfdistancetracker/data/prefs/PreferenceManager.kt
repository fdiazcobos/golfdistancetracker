package com.example.golfdistancetracker.data.prefs

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

private val Context.dataStore by preferencesDataStore("settings")

enum class DistanceUnit {
    METERS, YARDS
}

enum class ThemePreference {
    SYSTEM, LIGHT, DARK
}

enum class LanguagePreference {
    AUTO, ENGLISH, SPANISH
}

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val UNIT_KEY = stringPreferencesKey("distance_unit")
    private val THEME_KEY = stringPreferencesKey("theme_preference")
    private val LANG_KEY = stringPreferencesKey("language_preference")
    private val HANDICAP_KEY = stringPreferencesKey("handicap_profile")
    private val API_KEY = stringPreferencesKey("weather_api_key")
    private val GPS_SOURCE_KEY = stringPreferencesKey("gps_source")
    private val AUTO_IMPACT_KEY = booleanPreferencesKey("auto_impact_detection")

    val handicap: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[HANDICAP_KEY] ?: ""
    }

    val gpsSource: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GPS_SOURCE_KEY] ?: "Phone" // "Phone" or "Watch"
    }

    val autoImpactDetection: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_IMPACT_KEY] ?: true
    }

    val weatherApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[API_KEY] ?: "66ab1d65e5e84b84739962a81ff95f1a"
    }

    suspend fun updateHandicap(value: String) {
        context.dataStore.edit { prefs -> prefs[HANDICAP_KEY] = value }
    }

    suspend fun updateGpsSource(source: String) {
        context.dataStore.edit { prefs -> prefs[GPS_SOURCE_KEY] = source }
    }

    suspend fun updateAutoImpact(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[AUTO_IMPACT_KEY] = enabled }
    }

    suspend fun updateWeatherApiKey(value: String) {
        context.dataStore.edit { prefs -> prefs[API_KEY] = value }
    }

    val distanceUnit: Flow<DistanceUnit> = context.dataStore.data.map { prefs ->
        val unitName = prefs[UNIT_KEY] ?: DistanceUnit.METERS.name
        DistanceUnit.valueOf(unitName)
    }

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        val themeName = prefs[THEME_KEY] ?: ThemePreference.SYSTEM.name
        ThemePreference.valueOf(themeName)
    }

    val languagePreference: Flow<LanguagePreference> = context.dataStore.data.map { prefs ->
        val langName = prefs[LANG_KEY] ?: LanguagePreference.AUTO.name
        LanguagePreference.valueOf(langName)
    }

    suspend fun updateDistanceUnit(unit: DistanceUnit) {
        context.dataStore.edit { prefs -> prefs[UNIT_KEY] = unit.name }
    }

    suspend fun updateTheme(theme: ThemePreference) {
        context.dataStore.edit { prefs -> prefs[THEME_KEY] = theme.name }
    }

    suspend fun updateLanguage(lang: LanguagePreference) {
        context.dataStore.edit { prefs -> prefs[LANG_KEY] = lang.name }
    }
}
