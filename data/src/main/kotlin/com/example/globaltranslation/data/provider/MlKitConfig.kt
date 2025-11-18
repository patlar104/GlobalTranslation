package com.example.globaltranslation.data.provider

/**
 * ML Kit Provider Configuration and Constants.
 * 
 * This object centralizes configuration for all ML Kit providers in the application,
 * ensuring consistency and making it easier to tune performance parameters.
 * 
 * ## Design Principles:
 * - Centralized configuration for easy maintenance
 * - Well-documented default values
 * - Performance tuning based on device capabilities
 * 
 * ## Usage:
 * ```kotlin
 * val timeout = MlKitConfig.DEFAULT_OPERATION_TIMEOUT_MS
 * val maxRetries = MlKitConfig.MAX_RETRY_ATTEMPTS
 * ```
 */
object MlKitConfig {
    
    /**
     * Default timeout for ML Kit operations (in milliseconds).
     * This prevents operations from hanging indefinitely.
     */
    const val DEFAULT_OPERATION_TIMEOUT_MS = 30_000L
    
    /**
     * Maximum number of retry attempts for failed operations.
     * Helps handle transient network or model loading failures.
     */
    const val MAX_RETRY_ATTEMPTS = 3
    
    /**
     * Delay between retry attempts (in milliseconds).
     * Uses exponential backoff: delay * (2 ^ attempt)
     */
    const val RETRY_DELAY_MS = 1_000L
    
    /**
     * Maximum number of cached translators.
     * Prevents unbounded memory growth while maintaining performance.
     */
    const val MAX_CACHED_TRANSLATORS = 10
    
    /**
     * Maximum number of cached text recognizers.
     * Limited by the number of supported language scripts.
     */
    const val MAX_CACHED_RECOGNIZERS = 5
    
    /**
     * Minimum text length for translation (characters).
     * Prevents wasting resources on trivial inputs.
     */
    const val MIN_TEXT_LENGTH = 1
    
    /**
     * Maximum text length for single translation (characters).
     * ML Kit has limits on input size; this prevents errors.
     */
    const val MAX_TEXT_LENGTH = 5_000
    
    /**
     * Maximum number of text blocks to process in parallel.
     * Prevents excessive coroutine creation and memory usage.
     */
    const val MAX_PARALLEL_TRANSLATIONS = 20
    
    /**
     * Confidence threshold for text recognition (0.0 to 1.0).
     * Text blocks below this threshold may be filtered out.
     */
    const val MIN_RECOGNITION_CONFIDENCE = 0.5f
    
    /**
     * Whether to enable detailed logging for debugging.
     * Should be false in production to avoid performance impact.
     */
    const val ENABLE_DEBUG_LOGGING = false
    
    /**
     * Model download timeout (in milliseconds).
     * Large models may take time to download on slow networks.
     */
    const val MODEL_DOWNLOAD_TIMEOUT_MS = 300_000L // 5 minutes

    /**
     * Default maximum size for LRU caches.
     * Balances memory usage with caching benefits.
     */
    const val DEFAULT_LRU_CACHE_SIZE = 50
}
