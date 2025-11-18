package com.example.globaltranslation.ui.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.globaltranslation.core.provider.TranslationProvider
import com.example.globaltranslation.data.network.NetworkMonitor
import com.example.globaltranslation.data.network.NetworkState
import com.example.globaltranslation.data.preferences.AppPreferences
import com.google.mlkit.nl.translate.TranslateLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing translation language models.
 * Migrated to use :data providers for clean architecture.
 */
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val translationProvider: TranslationProvider,
    private val appPreferences: AppPreferences,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanguageUiState())
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    // Cache for download status with timestamps to avoid redundant checks
    private val downloadStatusCache = mutableMapOf<String, Pair<Boolean, Long>>()
    private val CACHE_DURATION_MS = 30_000L // 30 seconds

    // Track active download jobs for cancellation
    private val activeDownloads = mutableMapOf<String, Job>()

    // Track pending downloads waiting for network
    private val pendingDownloads = mutableSetOf<String>()

    init {
        loadAvailableLanguages()
        loadCellularDownloadPreference()
        monitorNetworkChanges()
    }

    /**
     * Monitors network state changes and resumes pending downloads.
     */
    private fun monitorNetworkChanges() {
        viewModelScope.launch {
            networkMonitor.networkState.collect { networkState ->
                _uiState.value = _uiState.value.copy(networkState = networkState)

                // Resume pending downloads when appropriate network becomes available
                if (pendingDownloads.isNotEmpty()) {
                    val canDownload = when {
                        networkState.isWiFi() -> true
                        networkState.isCellular() && _uiState.value.allowCellularDownloads -> true
                        else -> false
                    }

                    if (canDownload) {
                        // Resume all pending downloads
                        val toResume = pendingDownloads.toList()
                        pendingDownloads.clear()
                        toResume.forEach { languageCode ->
                            retryDownload(languageCode)
                        }
                    }
                }
            }
        }
    }

    /**
     * Loads the cellular download preference.
     */
    private fun loadCellularDownloadPreference() {
        viewModelScope.launch {
            appPreferences.allowCellularDownloads.collect { allowCellular ->
                _uiState.value = _uiState.value.copy(allowCellularDownloads = allowCellular)
            }
        }
    }

    /**
     * Loads the list of available languages and checks their download status.
     */
    private fun loadAvailableLanguages() {
        val languages = getSupportedLanguages().map { lang ->
            LanguageModel(
                code = lang.code,
                name = lang.name,
                isDownloading = false,
                isDownloaded = false // Will be checked async
            )
        }

        _uiState.value = _uiState.value.copy(
            availableLanguages = languages,
            isLoading = true
        )

        // Check download status for each language pair with English
        checkDownloadStatus()
    }

    /**
     * Checks which language models are already downloaded.
     * Uses parallel coroutines for better performance and caches results.
     */
    private fun checkDownloadStatus() {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()

            // Run all download status checks in parallel
            val updatedLanguages = _uiState.value.availableLanguages.map { language ->
                async {
                    if (language.code == TranslateLanguage.ENGLISH) {
                        // English is always available
                        language.copy(isDownloaded = true)
                    } else {
                        // Check cache first
                        val cached = downloadStatusCache[language.code]
                        if (cached != null && (currentTime - cached.second) < CACHE_DURATION_MS) {
                            language.copy(isDownloaded = cached.first)
                        } else {
                            try {
                                val downloaded = translationProvider.areModelsDownloaded(
                                    TranslateLanguage.ENGLISH,
                                    language.code
                                )
                                // Update cache
                                downloadStatusCache[language.code] = downloaded to currentTime
                                language.copy(isDownloaded = downloaded)
                            } catch (e: Exception) {
                                language.copy(isDownloaded = false)
                            }
                        }
                    }
                }
            }.awaitAll()

            _uiState.value = _uiState.value.copy(
                availableLanguages = updatedLanguages,
                isLoading = false
            )
        }
    }

    /**
     * Downloads a language model with network monitoring and auto-retry.
     */
    fun downloadLanguage(languageCode: String) {
        if (languageCode == TranslateLanguage.ENGLISH) return // Already available

        // Check if language is already downloaded
        val language = _uiState.value.availableLanguages.find { it.code == languageCode }
        if (language?.isDownloaded == true) {
            // Language is already downloaded, skip download
            return
        }

        // Check if already downloading
        if (language?.isDownloading == true) {
            // Download already in progress, skip
            return
        }

        // Check network state
        val networkState = _uiState.value.networkState
        val requireWifi = !_uiState.value.allowCellularDownloads

        // Check if download can proceed
        val canDownload = when {
            !networkState.isConnected() -> false
            requireWifi && !networkState.isWiFi() -> false
            else -> true
        }

        if (!canDownload) {
            // Add to pending downloads
            pendingDownloads.add(languageCode)

            val errorMessage = when {
                !networkState.isConnected() -> "No internet connection. Will retry when connected."
                requireWifi && networkState.isCellular() -> "WiFi required. Will retry on WiFi or enable cellular downloads."
                else -> "Network unavailable. Will retry when available."
            }

            val updatedLanguages = _uiState.value.availableLanguages.map { lang ->
                if (lang.code == languageCode) {
                    lang.copy(isDownloading = false, downloadProgress = null)
                } else lang
            }

            _uiState.value = _uiState.value.copy(
                availableLanguages = updatedLanguages,
                error = errorMessage
            )
            return
        }

        // Start download
        startDownload(languageCode, requireWifi)
    }

    /**
     * Starts the actual download process with status updates.
     */
    private fun startDownload(languageCode: String, requireWifi: Boolean) {
        // Update to DOWNLOADING state with indeterminate progress
        updateLanguageStatus(languageCode, DownloadStatus.DOWNLOADING, null, downloading = true)

        val downloadJob = viewModelScope.launch {
            try {
                val result = translationProvider.downloadModels(
                    TranslateLanguage.ENGLISH,
                    languageCode,
                    requireWifi = requireWifi
                )

                result.fold(
                    onSuccess = {
                        // Invalidate cache and mark as complete
                        downloadStatusCache.remove(languageCode)
                        pendingDownloads.remove(languageCode)
                        activeDownloads.remove(languageCode)

                        val finalUpdatedLanguages = _uiState.value.availableLanguages.map { lang ->
                            if (lang.code == languageCode) {
                                lang.copy(
                                    isDownloading = false,
                                    isDownloaded = true,
                                    downloadProgress = null,
                                    downloadStatus = DownloadStatus.IDLE
                                )
                            } else lang
                        }
                        _uiState.value = _uiState.value.copy(availableLanguages = finalUpdatedLanguages)
                    },
                    onFailure = { exception ->
                        activeDownloads.remove(languageCode)

                        // Check if it's a network error - if so, add to pending
                        val isNetworkError = exception.message?.contains("network", ignoreCase = true) == true ||
                                exception.message?.contains("connection", ignoreCase = true) == true

                        if (isNetworkError) {
                            pendingDownloads.add(languageCode)
                            updateLanguageStatus(languageCode, DownloadStatus.PAUSED, null, downloading = false)
                        } else {
                            updateLanguageStatus(languageCode, DownloadStatus.FAILED, null, downloading = false)
                        }

                        val errorMessage = if (isNetworkError) {
                            "Download paused: ${exception.message}. Will retry when network is available."
                        } else {
                            "Failed to download ${getLanguageName(languageCode)}: ${exception.message}"
                        }

                        _uiState.value = _uiState.value.copy(error = errorMessage)
                    }
                )
            } catch (e: Exception) {
                activeDownloads.remove(languageCode)
                pendingDownloads.add(languageCode) // Assume network error

                updateLanguageStatus(languageCode, DownloadStatus.PAUSED, null, downloading = false)

                _uiState.value = _uiState.value.copy(
                    error = "Download error: ${e.message}. Will retry when network is available."
                )
            }
        }

        activeDownloads[languageCode] = downloadJob
    }

    /**
     * Helper to update language download status and progress.
     */
    private fun updateLanguageStatus(
        languageCode: String,
        status: DownloadStatus,
        progress: Float?,
        downloading: Boolean
    ) {
        val updatedLanguages = _uiState.value.availableLanguages.map { lang ->
            if (lang.code == languageCode) {
                lang.copy(
                    isDownloading = downloading,
                    downloadProgress = progress,
                    downloadStatus = status
                )
            } else lang
        }
        _uiState.value = _uiState.value.copy(availableLanguages = updatedLanguages)
    }

    /**
     * Retries a failed download (called when network becomes available).
     */
    private fun retryDownload(languageCode: String) {
        val requireWifi = !_uiState.value.allowCellularDownloads
        startDownload(languageCode, requireWifi)
    }

    /**
     * Cancels an active download.
     */
    fun cancelDownload(languageCode: String) {
        activeDownloads[languageCode]?.cancel()
        activeDownloads.remove(languageCode)
        pendingDownloads.remove(languageCode)

        val updatedLanguages = _uiState.value.availableLanguages.map { lang ->
            if (lang.code == languageCode) {
                lang.copy(isDownloading = false, downloadProgress = null)
            } else lang
        }
        _uiState.value = _uiState.value.copy(availableLanguages = updatedLanguages)
    }

    /**
     * Deletes a downloaded language model.
     */
    fun deleteLanguage(languageCode: String) {
        if (languageCode == TranslateLanguage.ENGLISH) return // Can't delete English

        viewModelScope.launch {
            try {
                val result = translationProvider.deleteModel(languageCode)

                result.fold(
                    onSuccess = {
                        // Invalidate cache
                        downloadStatusCache.remove(languageCode)

                        val updatedLanguages = _uiState.value.availableLanguages.map { lang ->
                            if (lang.code == languageCode) {
                                lang.copy(isDownloaded = false)
                            } else lang
                        }
                        _uiState.value = _uiState.value.copy(availableLanguages = updatedLanguages)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to delete ${getLanguageName(languageCode)}: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Delete error: ${e.message}"
                )
            }
        }
    }

    /**
     * Refreshes the download status of all languages.
     * Clears the cache to force a fresh check.
     */
    fun refreshLanguages() {
        downloadStatusCache.clear() // Clear cache to force refresh
        _uiState.value = _uiState.value.copy(isLoading = true)
        checkDownloadStatus()
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Toggles the cellular downloads setting.
     */
    fun toggleCellularDownloads() {
        val newValue = !_uiState.value.allowCellularDownloads
        _uiState.value = _uiState.value.copy(allowCellularDownloads = newValue)

        viewModelScope.launch {
            appPreferences.setAllowCellularDownloads(newValue)
        }
    }

    private fun getLanguageName(code: String): String {
        return getSupportedLanguages().find { it.code == code }?.name ?: code
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel all active downloads
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        pendingDownloads.clear()
    }
}

/**
 * UI state for the language management screen.
 */
data class LanguageUiState(
    val availableLanguages: List<LanguageModel> = emptyList(),
    val isLoading: Boolean = false,
    val allowCellularDownloads: Boolean = false,
    val networkState: NetworkState = NetworkState.Disconnected,
    val error: String? = null
)

/**
 * Data class representing a language model.
 */
data class LanguageModel(
    val code: String,
    val name: String,
    val isDownloaded: Boolean,
    val isDownloading: Boolean,
    val downloadProgress: Float? = null, // 0.0 to 1.0, null if not downloading
    val downloadStatus: DownloadStatus = DownloadStatus.IDLE
)

/**
 * Represents the status of a model download.
 */
enum class DownloadStatus {
    IDLE,           // Not downloading
    DOWNLOADING,    // Active download
    FAILED,         // Download failed
    PAUSED          // Waiting for network
}

/**
 * Data class for supported languages.
 */
data class SupportedLanguage(
    val code: String,
    val name: String
)

/**
 * Returns list of supported languages for ML Kit translation.
 */
fun getSupportedLanguages(): List<SupportedLanguage> {
    return listOf(
        SupportedLanguage(TranslateLanguage.ENGLISH, "English"),
        SupportedLanguage(TranslateLanguage.SPANISH, "Spanish"),
        SupportedLanguage(TranslateLanguage.FRENCH, "French"),
        SupportedLanguage(TranslateLanguage.GERMAN, "German"),
        SupportedLanguage(TranslateLanguage.ITALIAN, "Italian"),
        SupportedLanguage(TranslateLanguage.PORTUGUESE, "Portuguese"),
        SupportedLanguage(TranslateLanguage.CHINESE, "Chinese"),
        SupportedLanguage(TranslateLanguage.JAPANESE, "Japanese"),
        SupportedLanguage(TranslateLanguage.KOREAN, "Korean"),
        SupportedLanguage(TranslateLanguage.RUSSIAN, "Russian"),
        SupportedLanguage(TranslateLanguage.ARABIC, "Arabic"),
        SupportedLanguage(TranslateLanguage.HINDI, "Hindi"),
        SupportedLanguage(TranslateLanguage.DUTCH, "Dutch"),
        SupportedLanguage(TranslateLanguage.POLISH, "Polish"),
        SupportedLanguage(TranslateLanguage.TURKISH, "Turkish"),
        SupportedLanguage(TranslateLanguage.THAI, "Thai"),
        SupportedLanguage(TranslateLanguage.VIETNAMESE, "Vietnamese"),
        SupportedLanguage(TranslateLanguage.INDONESIAN, "Indonesian"),
        SupportedLanguage(TranslateLanguage.MALAY, "Malay"),
        SupportedLanguage(TranslateLanguage.BENGALI, "Bengali")
    )
}
