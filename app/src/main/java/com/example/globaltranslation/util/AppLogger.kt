package com.example.globaltranslation.util

import android.util.Log

/**
 * Centralized logging utility for the GlobalTranslation app.
 * Automatically disables debug/verbose logs in release builds for security and performance.
 *
 * Usage:
 * ```kotlin
 * AppLogger.d("MyTag", "Debug message")
 * AppLogger.e("MyTag", "Error message", exception)
 * AppLogger.i("MyTag", "Info message")
 * ```
 */
object AppLogger {

    private const val DEFAULT_TAG = "GlobalTranslation"

    // Use reflection to safely check BuildConfig.DEBUG to avoid build-time dependency
    private val isDebugBuild: Boolean by lazy {
        try {
            val buildConfigClass = Class.forName("com.example.globaltranslation.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            debugField.getBoolean(null)
        } catch (e: Exception) {
            // Default to false (production mode) if BuildConfig not available
            false
        }
    }

    /**
     * Log verbose message (only in debug builds)
     */
    fun v(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugBuild) {
            Log.v(tag, message)
        }
    }

    /**
     * Log debug message (only in debug builds)
     */
    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugBuild) {
            Log.d(tag, message)
        }
    }

    /**
     * Log info message
     */
    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
    }

    /**
     * Log warning message
     */
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    /**
     * Log error message
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * Log "What a Terrible Failure" - for errors that should never happen
     */
    fun wtf(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.wtf(tag, message, throwable)
        } else {
            Log.wtf(tag, message)
        }
    }

    /**
     * Helper to log provider errors consistently
     */
    fun logProviderError(providerName: String, operation: String, error: Throwable) {
        e("$providerName:$operation", "Operation failed", error)
    }

    /**
     * Helper to log ViewModel errors consistently
     */
    fun logViewModelError(viewModelName: String, action: String, error: Throwable) {
        e("$viewModelName:$action", "Action failed", error)
    }
}
