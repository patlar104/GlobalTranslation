package com.example.globaltranslation.data.provider

import java.util.concurrent.ConcurrentHashMap
import java.io.Closeable

/**
 * Base class for ML Kit providers that manage cached resources.
 * 
 * Provides common functionality for:
 * - Thread-safe resource caching using ConcurrentHashMap
 * - Automatic cleanup of cached resources
 * - Resource lifecycle management
 * 
 * ## Purpose:
 * Eliminates code duplication across ML Kit providers by centralizing
 * the common pattern of caching and cleaning up ML Kit resources
 * (Translators, TextRecognizers, etc.).
 * 
 * ## Type Parameters:
 * - K: Cache key type (e.g., String for language codes)
 * - R: Resource type that implements Closeable (e.g., Translator, TextRecognizer)
 * 
 * ## Benefits:
 * - Reduces code duplication
 * - Ensures consistent resource management
 * - Thread-safe by design
 * - Simplifies provider implementations
 * 
 * ## Example Usage:
 * ```kotlin
 * @Singleton
 * class MlKitTranslationProvider @Inject constructor() 
 *     : CachedResourceProvider<String, Translator>() {
 *     
 *     override fun createResource(key: String): Translator {
 *         // Create and return translator
 *     }
 * }
 * ```
 */
abstract class CachedResourceProvider<K : Any, R : Closeable> {
    
    /**
     * Thread-safe cache for resources.
     * Uses ConcurrentHashMap for lock-free reads and thread-safe writes.
     */
    protected val resourceCache = ConcurrentHashMap<K, R>()
    
    /**
     * Gets or creates a resource for the given key.
     * If the resource exists in cache, returns it.
     * Otherwise, creates a new resource, caches it, and returns it.
     * 
     * @param key The cache key
     * @return The cached or newly created resource
     */
    protected fun getOrCreateResource(key: K): R {
        return resourceCache.getOrPut(key) {
            createResource(key)
        }
    }
    
    /**
     * Creates a new resource for the given key.
     * Subclasses must implement this to provide resource-specific creation logic.
     * 
     * @param key The cache key
     * @return A new resource instance
     */
    protected abstract fun createResource(key: K): R
    
    /**
     * Cleans up all cached resources.
     * Closes each resource and clears the cache.
     * 
     * This method is thread-safe and can be called multiple times safely.
     * Implements the common cleanup pattern used across all ML Kit providers.
     */
    fun cleanup() {
        resourceCache.values.forEach { resource ->
            try {
                resource.close()
            } catch (e: Exception) {
                // Log but don't propagate exceptions during cleanup
                // This ensures all resources are attempted to be closed
            }
        }
        resourceCache.clear()
    }
    
    /**
     * Gets the current number of cached resources.
     * Useful for monitoring and debugging.
     * 
     * @return Number of resources in cache
     */
    fun getCacheSize(): Int = resourceCache.size
    
    /**
     * Removes a specific resource from the cache and closes it.
     * 
     * @param key The cache key to remove
     * @return true if resource was removed, false if key didn't exist
     */
    protected fun removeResource(key: K): Boolean {
        val resource = resourceCache.remove(key)
        return if (resource != null) {
            try {
                resource.close()
            } catch (e: Exception) {
                // Log but continue
            }
            true
        } else {
            false
        }
    }
    
    /**
     * Checks if a resource is cached for the given key.
     * 
     * @param key The cache key
     * @return true if resource exists in cache
     */
    protected fun hasResource(key: K): Boolean = resourceCache.containsKey(key)
}
