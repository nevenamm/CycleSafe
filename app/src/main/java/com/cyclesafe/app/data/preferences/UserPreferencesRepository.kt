package com.cyclesafe.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.dataStore

    private object Keys {
        val IS_DANGEROUS_FILTER = booleanPreferencesKey("is_dangerous_filter")
        val SEARCH_RADIUS = floatPreferencesKey("search_radius")
        val IS_TRACKING_ENABLED = booleanPreferencesKey("is_tracking_enabled")
    }

    val isDangerousFilter: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[Keys.IS_DANGEROUS_FILTER] ?: false
        }

    val searchRadius: Flow<Float> = dataStore.data
        .map { preferences ->
            preferences[Keys.SEARCH_RADIUS] ?: 2500f
        }

    val isTrackingEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[Keys.IS_TRACKING_ENABLED] ?: false
        }

    suspend fun updateIsDangerousFilter(isDangerous: Boolean) {
        dataStore.edit {
            it[Keys.IS_DANGEROUS_FILTER] = isDangerous
        }
    }

    suspend fun updateSearchRadius(radius: Float) {
        dataStore.edit {
            it[Keys.SEARCH_RADIUS] = radius
        }
    }

    suspend fun updateIsTrackingEnabled(isEnabled: Boolean) {
        dataStore.edit {
            it[Keys.IS_TRACKING_ENABLED] = isEnabled
        }
    }
}