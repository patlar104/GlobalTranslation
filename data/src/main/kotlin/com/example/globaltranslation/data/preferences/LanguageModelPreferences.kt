package com.example.globaltranslation.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.languageModelDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "language_models"
)

/**
 * DataStore-backed preferences for persisting downloaded language models.
 * Survives process death and app restarts.
 *
 * Battery optimizations:
 * - Efficient DataStore usage with Flow-based access
 * - Optimized edit operations with minimal writes
 * - Proper type annotations to reduce allocations
 */
@Singleton
class LanguageModelPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.languageModelDataStore
    
    companion object {
        private val DOWNLOADED_MODELS_KEY = stringSetPreferencesKey("downloaded_models")
    }
    
    /**
     * Flow of all downloaded model language pairs (e.g., "en-es", "en-fr").
     */
    val downloadedModels: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[DOWNLOADED_MODELS_KEY] ?: emptySet()
    }
    
    /**
     * Marks a language pair as downloaded.
     * @param fromLanguage Source language code
     * @param toLanguage Target language code
     */
    suspend fun markModelAsDownloaded(fromLanguage: String, toLanguage: String) {
        val key = "$fromLanguage-$toLanguage"
        dataStore.edit { preferences ->
            val current = preferences[DOWNLOADED_MODELS_KEY] ?: emptySet()
            preferences[DOWNLOADED_MODELS_KEY] = current + key
        }
    }
    
    /**
     * Removes a language code from all downloaded pairs.
     * Used when a model is deleted.
     * @param languageCode Language code to remove (e.g., "es")
     */
    suspend fun removeLanguageFromModels(languageCode: String) {
        dataStore.edit { preferences ->
            val current = preferences[DOWNLOADED_MODELS_KEY] ?: emptySet()
            val filtered = current.filterNot { pair ->
                pair.startsWith("$languageCode-") || pair.endsWith("-$languageCode")
            }.toSet()
            preferences[DOWNLOADED_MODELS_KEY] = filtered
        }
    }
    
    /**
     * Checks if a language pair is marked as downloaded.
     * @param fromLanguage Source language code
     * @param toLanguage Target language code
     * @return true if the pair is marked as downloaded
     */
    suspend fun isModelDownloaded(fromLanguage: String, toLanguage: String): Boolean {
        val key = "$fromLanguage-$toLanguage"
        val preferences = dataStore.data.first()
        val downloaded = preferences[DOWNLOADED_MODELS_KEY] ?: emptySet()
        return key in downloaded
    }
    
    /**
     * Clears all downloaded model records.
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.remove(DOWNLOADED_MODELS_KEY)
        }
    }
}

