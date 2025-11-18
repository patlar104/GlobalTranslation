package com.example.globaltranslation.data.migration

import android.content.Context
import android.util.Log
import com.example.globaltranslation.data.BuildConfig
import com.example.globaltranslation.data.local.ConversationDatabase
import com.example.globaltranslation.data.preferences.AppPreferences
import com.example.globaltranslation.data.preferences.LanguageModelPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app version updates and data migration.
 * Handles cache clearing, database migration, and preference updates on version changes.
 */
@Singleton
class AppMigrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val languageModelPreferences: LanguageModelPreferences,
    private val conversationDatabase: ConversationDatabase
) {
    companion object {
        private const val TAG = "AppMigrationManager"
    }
    
    /**
     * Checks if the app has been updated and performs necessary migrations.
     * Should be called on app startup before any other operations.
     * 
     * @param currentVersionCode Current app version code from BuildConfig
     */
    suspend fun handleAppUpdate(currentVersionCode: Int) {
        val isFirstLaunch = appPreferences.isFirstLaunch.first()
        val lastVersionCode = appPreferences.lastVersionCode.first()
        
        when {
            isFirstLaunch -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "First app launch (version $currentVersionCode)")
                // Initialize app for first time
                handleFirstLaunch(currentVersionCode)
            }
            lastVersionCode < currentVersionCode -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "App updated from version $lastVersionCode to $currentVersionCode")
                // Handle upgrade
                handleAppUpgrade(lastVersionCode, currentVersionCode)
            }
            lastVersionCode > currentVersionCode -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "App downgraded from version $lastVersionCode to $currentVersionCode")
                // Handle downgrade (rare, but possible during development)
                handleAppDowngrade(lastVersionCode, currentVersionCode)
            }
            else -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "App version unchanged ($currentVersionCode)")
                // No migration needed
            }
        }
        
        // Always update the stored version code
        appPreferences.updateVersionCode(currentVersionCode)
    }
    
    /**
     * Handles first app launch initialization.
     */
    private suspend fun handleFirstLaunch(versionCode: Int) {
        try {
            // Mark as launched
            appPreferences.markLaunched()

            // Initialize default preferences
            appPreferences.setAllowCellularDownloads(false)

            if (BuildConfig.DEBUG) Log.d(TAG, "First launch initialization complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during first launch initialization", e)
        }
    }
    
    /**
     * Handles app upgrade migration.
     */
    private suspend fun handleAppUpgrade(fromVersion: Int, toVersion: Int) {
        try {
            if (BuildConfig.DEBUG) Log.d(TAG, "Performing upgrade migration from v$fromVersion to v$toVersion")

            // Example version-specific migrations:
            // if (fromVersion < 2 && toVersion >= 2) {
            //     // Migration for version 2
            //     migrateToVersion2()
            // }

            // For now, we validate existing data but don't clear it
            // This is safer than destructive migration
            validateExistingData()

            if (BuildConfig.DEBUG) Log.d(TAG, "Upgrade migration complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during app upgrade migration", e)
            // On migration error, you might want to clear corrupted data
            // clearCorruptedData()
        }
    }
    
    /**
     * Handles app downgrade (usually during development).
     */
    private suspend fun handleAppDowngrade(fromVersion: Int, toVersion: Int) {
        try {
            if (BuildConfig.DEBUG) Log.d(TAG, "App downgraded - clearing potentially incompatible data")

            // Downgrade is risky - clear cached data to prevent crashes
            clearAppCache()

            if (BuildConfig.DEBUG) Log.d(TAG, "Downgrade cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during app downgrade", e)
        }
    }
    
    /**
     * Validates existing data for corruption.
     */
    private suspend fun validateExistingData() {
        try {
            // Test database access by attempting to read conversations
            val dao = conversationDatabase.conversationDao()
            val conversations = dao.getAllConversations()
            // Just accessing the Flow validates the database structure
            conversations.first()
            if (BuildConfig.DEBUG) Log.d(TAG, "Database validation passed")
        } catch (e: Exception) {
            Log.e(TAG, "Database validation failed - may need rebuild", e)
            // Database might be corrupted - the fallbackToDestructiveMigrationOnDowngrade will handle it
        }
    }
    
    /**
     * Clears app cache while preserving important user data.
     * Called on downgrade or when corruption is detected.
     */
    private suspend fun clearAppCache() {
        try {
            // Clear language model download status cache
            // (actual models stay on disk, just the status tracking is cleared)
            // languageModelPreferences.clearAll()
            // Uncomment if you want to clear model tracking on downgrade

            // Clear app cache directory
            context.cacheDir.deleteRecursively()

            if (BuildConfig.DEBUG) Log.d(TAG, "App cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing app cache", e)
        }
    }
    
    /**
     * Clears all app data (destructive - use with caution).
     * Only call this if corruption is detected and cannot be recovered.
     */
    suspend fun clearAllAppData() {
        try {
            Log.w(TAG, "Clearing all app data - this is destructive!")

            // Clear all preferences
            appPreferences.clearAll()
            languageModelPreferences.clearAll()

            // Clear database
            conversationDatabase.clearAllTables()

            // Clear cache
            context.cacheDir.deleteRecursively()

            if (BuildConfig.DEBUG) Log.d(TAG, "All app data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing app data", e)
        }
    }
}

