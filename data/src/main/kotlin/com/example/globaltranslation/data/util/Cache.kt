package com.example.globaltranslation.data.util

import com.example.globaltranslation.data.provider.MlKitConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Memory-efficient cache with automatic eviction.
 * Implements LRU (Least Recently Used) eviction policy to prevent unbounded growth.
 * Thread-safe and optimized for concurrent access.
 *
 * Battery benefits:
 * - Reduces redundant network/disk operations
 * - Configurable size limits to prevent memory pressure
 * - Efficient concurrent access without blocking
 *
 * @param T Type of cached values
 * @param maxSize Maximum number of entries before eviction (default from MlKitConfig)
 */
class LruCache<K : Any, V : Any>(
    private val maxSize: Int = MlKitConfig.DEFAULT_LRU_CACHE_SIZE
) {
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val mutex = Mutex()
    
    /**
     * Entry in the cache with access timestamp for LRU tracking.
     */
    private data class CacheEntry<V>(
        val value: V,
        var lastAccessTime: Long = System.currentTimeMillis()
    )
    
    /**
     * Gets a value from the cache.
     * Updates access time for LRU tracking.
     * 
     * @param key Cache key
     * @return Cached value or null if not found
     */
    suspend fun get(key: K): V? {
        val entry = cache[key] ?: return null
        
        // Update access time
        mutex.withLock {
            entry.lastAccessTime = System.currentTimeMillis()
        }
        
        return entry.value
    }
    
    /**
     * Puts a value in the cache.
     * Evicts least recently used entry if cache is full.
     * 
     * @param key Cache key
     * @param value Value to cache
     */
    suspend fun put(key: K, value: V) {
        mutex.withLock {
            // Evict if cache is full
            if (cache.size >= maxSize && !cache.containsKey(key)) {
                evictLeastRecentlyUsed()
            }
            
            cache[key] = CacheEntry(value)
        }
    }
    
    /**
     * Removes a value from the cache.
     * 
     * @param key Cache key
     * @return Removed value or null if not found
     */
    suspend fun remove(key: K): V? {
        return mutex.withLock {
            cache.remove(key)?.value
        }
    }
    
    /**
     * Clears all entries from the cache.
     */
    suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
    
    /**
     * Gets the current size of the cache.
     */
    fun size(): Int = cache.size
    
    /**
     * Checks if the cache contains a key.
     */
    fun containsKey(key: K): Boolean = cache.containsKey(key)
    
    /**
     * Evicts the least recently used entry.
     * Should be called while holding the mutex.
     * 
     * Note: This implementation uses O(n) linear scan for eviction.
     * For typical cache sizes (50-100 entries), this is acceptable.
     * For larger caches, consider using LinkedHashMap with access order.
     */
    private fun evictLeastRecentlyUsed() {
        val lruEntry = cache.entries.minByOrNull { it.value.lastAccessTime }
        lruEntry?.let { cache.remove(it.key) }
    }
    
    /**
     * Gets or computes a value.
     * If key exists, returns cached value.
     * Otherwise, computes value, caches it, and returns it.
     * Thread-safe with double-checked locking to prevent duplicate computation.
     * 
     * @param key Cache key
     * @param compute Function to compute value if not cached
     * @return Cached or computed value
     */
    suspend fun getOrPut(key: K, compute: suspend () -> V): V {
        // Fast path: check cache first (no lock needed)
        get(key)?.let { return it }
        
        // Slow path: acquire lock and double-check
        mutex.withLock {
            // Double-check after acquiring lock to avoid duplicate computation
            val existingEntry = cache[key]
            if (existingEntry != null) {
                existingEntry.lastAccessTime = System.currentTimeMillis()
                return existingEntry.value
            }
            
            // Evict if cache is full
            if (cache.size >= maxSize) {
                evictLeastRecentlyUsed()
            }
            
            // Compute value while holding lock to prevent duplicates
            val value = compute()
            cache[key] = CacheEntry(value)
            return value
        }
    }
}

/**
 * Simple time-based expiration cache.
 * Entries automatically expire after a specified duration.
 * Useful for caching API responses or temporary data.
 * 
 * @param T Type of cached values
 * @param ttlMillis Time-to-live in milliseconds (default: 5 minutes)
 */
class ExpiringCache<K : Any, V : Any>(
    private val ttlMillis: Long = 5 * 60 * 1000 // 5 minutes
) {
    private val cache = ConcurrentHashMap<K, ExpiringEntry<V>>()
    
    private data class ExpiringEntry<V>(
        val value: V,
        val expirationTime: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expirationTime
    }
    
    /**
     * Gets a value from the cache if not expired.
     * Automatically removes expired entries.
     */
    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        
        return if (entry.isExpired()) {
            cache.remove(key)
            null
        } else {
            entry.value
        }
    }
    
    /**
     * Puts a value in the cache with automatic expiration.
     */
    fun put(key: K, value: V) {
        val expirationTime = System.currentTimeMillis() + ttlMillis
        cache[key] = ExpiringEntry(value, expirationTime)
    }
    
    /**
     * Removes expired entries from the cache.
     * Should be called periodically to prevent memory leaks.
     * Thread-safe operation with synchronized access.
     */
    @Synchronized
    fun cleanup() {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries
            .filter { it.value.expirationTime < now }
            .map { it.key }
        
        expiredKeys.forEach { cache.remove(it) }
    }
    
    /**
     * Clears all entries from the cache.
     */
    fun clear() {
        cache.clear()
    }
    
    /**
     * Gets or computes a value with expiration.
     */
    fun getOrPut(key: K, compute: () -> V): V {
        get(key)?.let { return it }
        
        val value = compute()
        put(key, value)
        return value
    }
}
