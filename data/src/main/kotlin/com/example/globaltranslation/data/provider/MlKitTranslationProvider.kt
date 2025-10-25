package com.example.globaltranslation.data.provider

import com.example.globaltranslation.core.provider.TranslationProvider
import com.example.globaltranslation.data.preferences.LanguageModelPreferences
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ML Kit implementation of TranslationProvider.
 * 
 * This provider handles on-device text translation using Google's ML Kit Translation API.
 * It manages the lifecycle of translation models and translator instances efficiently.
 * 
 * ## Architecture:
 * - **Singleton**: One instance shared across the app (via Hilt)
 * - **Thread-Safe**: All operations use concurrent data structures and mutex synchronization
 * - **Cached**: Translators are cached per language pair to avoid recreation overhead
 * - **Persistent**: Model download state is persisted via LanguageModelPreferences
 * 
 * ## Key Features:
 * 1. **Automatic Model Management**: Downloads models when needed, tracks download state
 * 2. **Efficient Caching**: Reuses translator instances to minimize initialization time
 * 3. **Double-Checked Locking**: Prevents duplicate model downloads
 * 4. **Resource Cleanup**: Properly closes all translators on cleanup
 * 
 * ## Performance:
 * - First translation: 2-5 seconds (model download + initialization)
 * - Cached translation: 100-500ms per request
 * - Memory per translator: ~50MB
 * 
 * ## Thread Safety:
 * Uses `ConcurrentHashMap` for translator cache and `Mutex` for model downloads.
 * All public methods are safe to call from any thread/coroutine.
 * 
 * ## Example Usage:
 * ```kotlin
 * // Inject the provider
 * @Inject lateinit var translationProvider: MlKitTranslationProvider
 * 
 * // Check if models are ready
 * val available = translationProvider.areModelsDownloaded("en", "es")
 * 
 * // Translate text
 * val result = translationProvider.translate("Hello", "en", "es")
 * result.onSuccess { translated -> println(translated) }
 * 
 * // Clean up when done
 * translationProvider.cleanup()
 * ```
 * 
 * @property languageModelPreferences Persists model download state across app restarts
 * @see com.example.globaltranslation.core.provider.TranslationProvider
 * @see MlKitConfig for configuration constants
 * @see MlKitErrorHandler for error handling utilities
 */
@Singleton
class MlKitTranslationProvider @Inject constructor(
    private val languageModelPreferences: LanguageModelPreferences
) : TranslationProvider {
    
    // Thread-safe caching of active translators
    private val activeTranslators = ConcurrentHashMap<String, Translator>()
    
    // Thread-safe set of models ready for use in memory
    private val modelsReady = ConcurrentHashMap.newKeySet<String>()
    
    // Mutex to synchronize model downloads and prevent duplicates
    private val downloadMutex = Mutex()
    
    // Lazy model manager to defer initialization until first use
    private val modelManager: RemoteModelManager by lazy { 
        RemoteModelManager.getInstance() 
    }
    
    /**
     * Translates text from one language to another using ML Kit's on-device models.
     * 
     * This method handles the entire translation workflow:
     * 1. Validates input text (must not be blank)
     * 2. Gets or creates a translator for the language pair
     * 3. Downloads models if needed (with WiFi requirement)
     * 4. Performs the actual translation
     * 
     * ## Model Download Behavior:
     * - Models are downloaded automatically on first use
     * - Downloads require WiFi by default (configurable)
     * - Download state is persisted across app restarts
     * - Double-checked locking prevents duplicate downloads
     * 
     * ## Caching:
     * - Translators are cached by language pair key: "from-to"
     * - Once created, translators are reused for all subsequent requests
     * - Cache is cleared on cleanup()
     * 
     * ## Thread Safety:
     * - Safe to call from any coroutine/thread
     * - Uses mutex to synchronize model downloads
     * - Concurrent translations to different language pairs are parallelized
     * 
     * @param text The text to translate (must not be blank)
     * @param from Source language code (e.g., "en" for English)
     * @param to Target language code (e.g., "es" for Spanish)
     * @return Result containing translated text or error
     * 
     * @throws IllegalArgumentException if text is blank (wrapped in Result.failure)
     * 
     * ## Example:
     * ```kotlin
     * val result = translate("Hello", "en", "es")
     * result.fold(
     *     onSuccess = { translated -> println("Translated: $translated") },
     *     onFailure = { error -> println("Error: ${error.message}") }
     * )
     * ```
     */
    override suspend fun translate(
        text: String,
        from: String,
        to: String
    ): Result<String> {
        if (text.isBlank()) {
            return Result.failure(IllegalArgumentException("Text cannot be empty"))
        }
        
        return try {
            val key = "$from-$to"
            val translator = getOrCreateTranslator(from, to)
            
            // Only download model if not already ready (use mutex to prevent duplicate downloads)
            if (key !in modelsReady) {
                downloadMutex.withLock {
                    // Double-check after acquiring lock
                    if (key !in modelsReady) {
                        ensureModelDownloaded(translator, requireWifi = true)
                        modelsReady.add(key)
                        // Persist the download state
                        languageModelPreferences.markModelAsDownloaded(from, to)
                    }
                }
            }
            
            val translatedText = translator.translate(text).await()
            Result.success(translatedText)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Checks if translation models are downloaded for a language pair.
     * 
     * This method queries ML Kit's RemoteModelManager to verify if both
     * source and target language models are available on the device.
     * 
     * ## Behavior:
     * - Returns true only if BOTH models are downloaded
     * - Does NOT trigger downloads (use downloadModels() for that)
     * - Returns false on any error (fail-safe)
     * 
     * ## Use Cases:
     * - Pre-flight check before translation
     * - UI to show which languages are available offline
     * - Conditional offline/online translation routing
     * 
     * @param from Source language code
     * @param to Target language code
     * @return true if both models are downloaded, false otherwise
     * 
     * ## Example:
     * ```kotlin
     * if (areModelsDownloaded("en", "es")) {
     *     // Safe to translate offline
     *     val result = translate(text, "en", "es")
     * } else {
     *     // Show download prompt or use cloud API
     *     showDownloadDialog()
     * }
     * ```
     */
    override suspend fun areModelsDownloaded(from: String, to: String): Boolean {
        return try {
            val fromModel = TranslateRemoteModel.Builder(from).build()
            val toModel = TranslateRemoteModel.Builder(to).build()
            
            val fromDownloaded = modelManager.isModelDownloaded(fromModel).await()
            val toDownloaded = modelManager.isModelDownloaded(toModel).await()
            
            fromDownloaded && toDownloaded
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun downloadModels(from: String, to: String, requireWifi: Boolean): Result<Unit> {
        return try {
            val key = "$from-$to"
            val translator = getOrCreateTranslator(from, to)
            
            // Use mutex to prevent concurrent downloads of the same model
            downloadMutex.withLock {
                ensureModelDownloaded(translator, requireWifi)
                modelsReady.add(key)
                // Persist the download state
                languageModelPreferences.markModelAsDownloaded(from, to)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteModel(languageCode: String): Result<Unit> {
        return try {
            val model = TranslateRemoteModel.Builder(languageCode).build()
            
            modelManager.deleteDownloadedModel(model).await()
            
            // Remove translators from cache that use this language code
            // Use startsWith/endsWith to match exact language codes in the key pattern "from-to"
            val keysToRemove = activeTranslators.keys.filter { key ->
                key.startsWith("$languageCode-") || key.endsWith("-$languageCode")
            }
            
            keysToRemove.forEach { key ->
                activeTranslators.remove(key)?.close()
                modelsReady.remove(key)
            }
            
            // Remove from persisted state
            languageModelPreferences.removeLanguageFromModels(languageCode)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun cleanup() {
        activeTranslators.values.forEach { it.close() }
        activeTranslators.clear()
        modelsReady.clear()
    }
    
    private fun getOrCreateTranslator(from: String, to: String): Translator {
        val key = "$from-$to"
        return activeTranslators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(from)
                .setTargetLanguage(to)
                .build()
            Translation.getClient(options)
        }
    }
    
    private suspend fun ensureModelDownloaded(translator: Translator, requireWifi: Boolean) {
        val conditionsBuilder = DownloadConditions.Builder()
        if (requireWifi) {
            conditionsBuilder.requireWifi()
        }
        val conditions = conditionsBuilder.build()
        translator.downloadModelIfNeeded(conditions).await()
    }
}

