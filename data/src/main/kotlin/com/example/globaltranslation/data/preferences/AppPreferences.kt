package com.example.globaltranslation.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

/**
 * DataStore-backed app preferences for settings and version tracking.
 * Handles app update detection and migration.
 * Open for testing - allows fake implementations to override behavior.
 *
 * Battery optimizations:
 * - Uses DataStore Flow for reactive, efficient preference access
 * - No polling or repeated disk reads
 * - Efficient Flow-based reactivity minimizes unnecessary operations
 */
@Singleton
open class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.appPreferencesDataStore
    
    companion object {
        private val LAST_VERSION_CODE_KEY = intPreferencesKey("last_version_code")
        private val ALLOW_CELLULAR_DOWNLOADS_KEY = booleanPreferencesKey("allow_cellular_downloads")
        private val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
    }
    
    /**
     * Flow of the last known app version code.
     */
    open val lastVersionCode: Flow<Int> = dataStore.data.map { preferences ->
        preferences[LAST_VERSION_CODE_KEY] ?: 0
    }
    
    /**
     * Flow of cellular download preference.
     */
    open val allowCellularDownloads: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ALLOW_CELLULAR_DOWNLOADS_KEY] ?: false
    }
    
    /**
     * Flow of first launch state.
     */
    open val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH_KEY] ?: true
    }
    
    /**
     * Updates the stored version code to the current version.
     * @param versionCode Current app version code
     */
    open suspend fun updateVersionCode(versionCode: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_VERSION_CODE_KEY] = versionCode
        }
    }
    
    /**
     * Sets the cellular download preference.
     * @param allow Whether to allow downloads on cellular network
     */
    open suspend fun setAllowCellularDownloads(allow: Boolean) {
        dataStore.edit { preferences ->
            preferences[ALLOW_CELLULAR_DOWNLOADS_KEY] = allow
        }
    }
    
    /**
     * Marks that the app has been launched (no longer first launch).
     */
    open suspend fun markLaunched() {
        dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = false
        }
    }
    
    /**
     * Checks if the app has been updated since the last launch.
     * @param currentVersionCode Current app version code
     * @return true if app has been updated
     */
    open suspend fun hasAppBeenUpdated(currentVersionCode: Int): Boolean {
        val lastVersion: Int = lastVersionCode.first()
        return lastVersion != 0 && lastVersion != currentVersionCode
    }
    
    /**
     * Clears all app preferences (for debugging/testing).
     */
    open suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

