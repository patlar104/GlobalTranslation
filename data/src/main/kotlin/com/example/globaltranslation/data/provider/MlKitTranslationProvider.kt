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
 * Handles model downloading, translation operations, and resource management.
 * Thread-safe with persisted model state.
 * 
 * Battery optimizations:
 * - Lazy model manager initialization
 * - Efficient translator caching with automatic cleanup
 * - Optimized mutex usage for download synchronization
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

