# Quick Reference: New Utilities and Patterns

## New Core Utilities

### LanguagePairKey - Type-Safe Language Pairs

```kotlin
// Create a language pair key
val key = LanguagePairKey.create("en", "es")

// Parse from string
val parsed = LanguagePairKey.parse("en-es")

// Check if contains language
if (key.containsLanguage("en")) {
    // Process
}

// Get source/target
val from = key.sourceLanguage  // "en"
val to = key.targetLanguage    // "es"
```

### ResultExtensions - Functional Error Handling

```kotlin
// Chain operations with early failure
result
    .andThen { processStep1(it) }
    .andThen { processStep2(it) }
    .orDefault(fallbackValue)

// Side effects without modifying result
result
    .onSuccessAction { logSuccess(it) }
    .onFailureAction { logError(it) }

// Transform both success and failure
result.mapBoth(
    onSuccess = { value -> transform(value) },
    onFailure = { error -> wrapError(error) }
)

// Recover from failure
result.recoverWith { error -> 
    when (error) {
        is NetworkError -> fetchFromCache()
        else -> defaultValue
    }
}
```

### FlowExtensions - Stream Processing

```kotlin
// Map with error handling
flow.mapToResult { riskyOperation(it) }

// Chain result transformations
resultFlow.mapResult { transform(it) }

// Filter to success values only
resultFlow.filterSuccess()

// Catch errors to Results
flow.catchToResult()
```

## New Data Utilities

### LruCache - Memory-Efficient Caching

```kotlin
// Create cache with max size
private val cache = LruCache<String, Translation>(maxSize = 100)

// Get value
suspend fun getTranslation(key: String): Translation? {
    return cache.get(key)
}

// Put value (automatic eviction when full)
suspend fun cacheTranslation(key: String, value: Translation) {
    cache.put(key, value)
}

// Get or compute
suspend fun getOrTranslate(text: String): Translation {
    return cache.getOrPut(text) {
        performExpensiveTranslation(text)
    }
}

// Remove value
suspend fun invalidate(key: String) {
    cache.remove(key)
}

// Clear cache
suspend fun clearCache() {
    cache.clear()
}
```

### ExpiringCache - Time-Based Expiration

```kotlin
// Create cache with 5-minute TTL
private val cache = ExpiringCache<String, Token>(
    ttlMillis = 5 * 60 * 1000
)

// Get value (returns null if expired)
fun getToken(key: String): Token? {
    return cache.get(key)
}

// Put value (automatic expiration)
fun cacheToken(key: String, token: Token) {
    cache.put(key, token)
}

// Get or compute
fun getOrFetchToken(key: String): Token {
    return cache.getOrPut(key) {
        fetchTokenFromServer(key)
    }
}

// Periodic cleanup (call in background)
fun cleanupExpiredEntries() {
    cache.cleanup()
}
```

## Updated Provider Patterns

### Battery-Efficient Resource Management

```kotlin
@Singleton
class MyProvider @Inject constructor() {
    // Lazy initialization - only created when needed
    private val resource: ExpensiveResource by lazy {
        ExpensiveResource.create()
    }
    
    // Volatile for thread-safe singleton
    @Volatile
    private var instance: SomeResource? = null
    
    // Thread-safe caching
    private val cache = ConcurrentHashMap<String, Result>()
    
    // Always implement cleanup
    fun cleanup() {
        resource.dispose()
        instance = null
        cache.clear()
    }
}
```

### Efficient Coroutine Patterns

```kotlin
// Use mutex for synchronization
private val mutex = Mutex()

suspend fun criticalSection() {
    mutex.withLock {
        // Protected code
    }
}

// Double-checked locking pattern
suspend fun getOrCreate(key: String): Value {
    // Fast path - no lock
    cache[key]?.let { return it }
    
    // Slow path - with lock
    mutex.withLock {
        // Double-check after acquiring lock
        cache[key]?.let { return it }
        
        val value = create()
        cache[key] = value
        return value
    }
}
```

## Best Practices

### Type Safety

```kotlin
// ✅ Good: Use LanguagePairKey
fun translate(pair: LanguagePairKey, text: String): Result<String>

// ❌ Avoid: Raw strings for language pairs
fun translate(from: String, to: String, text: String): Result<String>
```

### Error Handling

```kotlin
// ✅ Good: Functional composition
repository.getData()
    .onSuccessAction { logMetrics(it) }
    .andThen { validate(it) }
    .andThen { process(it) }
    .orDefault(fallback)

// ❌ Avoid: Nested callbacks
repository.getData().fold(
    onSuccess = { data ->
        logMetrics(data)
        validate(data).fold(
            onSuccess = { validated ->
                process(validated).fold(
                    onSuccess = { result -> result },
                    onFailure = { fallback }
                )
            },
            onFailure = { fallback }
        )
    },
    onFailure = { fallback }
)
```

### Caching Strategy

```kotlin
// ✅ Good: Bounded cache with clear policy
private val translationCache = LruCache<LanguagePairKey, String>(maxSize = 100)

// ✅ Good: Expiring cache for temporary data
private val authTokenCache = ExpiringCache<String, Token>(ttlMillis = 5.minutes)

// ❌ Avoid: Unbounded caching
private val cache = mutableMapOf<String, Translation>()  // No size limit!
```

### Resource Management

```kotlin
// ✅ Good: Lazy initialization with cleanup
class MyProvider {
    private val resource by lazy { createExpensiveResource() }
    
    fun cleanup() {
        if (::resource.isInitialized) {
            resource.dispose()
        }
    }
}

// ❌ Avoid: Eager initialization
class MyProvider {
    private val resource = createExpensiveResource()  // Created immediately!
}
```

## Common Patterns

### Caching Translations

```kotlin
@Singleton
class CachedTranslationProvider @Inject constructor(
    private val delegate: TranslationProvider
) : TranslationProvider {
    
    private val cache = LruCache<String, String>(maxSize = 200)
    
    override suspend fun translate(
        text: String, 
        from: String, 
        to: String
    ): Result<String> {
        val key = "${LanguagePairKey.create(from, to)}-$text"
        
        return cache.getOrPut(key) {
            delegate.translate(text, from, to)
                .onFailureAction { error -> 
                    logTranslationError(error)
                }
                .getOrThrow()
        }.let { Result.success(it) }
    }
}
```

### Retry with Exponential Backoff

```kotlin
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> Result<T>
): Result<T> {
    var currentDelay = initialDelay
    repeat(maxAttempts) { attempt ->
        block()
            .onSuccessAction { return Result.success(it) }
            .onFailureAction { error ->
                if (attempt == maxAttempts - 1) {
                    return Result.failure(error)
                }
                delay(currentDelay)
                currentDelay = (currentDelay * factor)
                    .toLong()
                    .coerceAtMost(maxDelay)
            }
    }
    return Result.failure(IllegalStateException("Should never reach here"))
}
```

### Flow Error Handling

```kotlin
// Collect with error handling
dataFlow
    .mapToResult { processData(it) }
    .collect { result ->
        result.fold(
            onSuccess = { updateUI(it) },
            onFailure = { showError(it) }
        )
    }

// Filter only successful results
dataFlow
    .mapToResult { riskyOperation(it) }
    .filterSuccess()
    .collect { validData -> process(validData) }
```

## Performance Tips

1. **Use inline value classes** for type safety with zero cost
2. **Cache frequently accessed data** with proper bounds
3. **Initialize resources lazily** to defer expensive operations
4. **Use @Volatile** for thread-safe singletons
5. **Implement proper cleanup** to prevent resource leaks
6. **Prefer Flow over LiveData** for better composability
7. **Use mutex for synchronization** instead of synchronized blocks
8. **Apply double-checked locking** for expensive initialization

## Testing Utilities

```kotlin
// Test with fake cache
class FakeCache<K : Any, V : Any> : LruCache<K, V>(maxSize = Int.MAX_VALUE) {
    val accessLog = mutableListOf<K>()
    
    override suspend fun get(key: K): V? {
        accessLog.add(key)
        return super.get(key)
    }
}

// Test Result extensions
@Test
fun `test error recovery`() {
    val result = Result.failure<Int>(Exception("Test"))
    val recovered = result.recoverWith { 42 }
    assertEquals(42, recovered)
}
```

## Migration Guide

### From String Pairs to LanguagePairKey

```kotlin
// Before
fun getTranslation(from: String, to: String): String? {
    val key = "$from-$to"
    return cache[key]
}

// After
fun getTranslation(pair: LanguagePairKey): String? {
    return cache[pair.value]
}
```

### From Imperative to Functional Error Handling

```kotlin
// Before
val result = repository.getData()
if (result.isSuccess) {
    val data = result.getOrThrow()
    logSuccess(data)
    return process(data)
} else {
    logError(result.exceptionOrNull()!!)
    return fallback
}

// After
return repository.getData()
    .onSuccessAction { logSuccess(it) }
    .onFailureAction { logError(it) }
    .andThen { process(it) }
    .orDefault(fallback)
```

### From Unbounded to Bounded Caching

```kotlin
// Before
private val cache = mutableMapOf<String, Translation>()

fun cache(key: String, value: Translation) {
    cache[key] = value  // Grows forever!
}

// After
private val cache = LruCache<String, Translation>(maxSize = 100)

suspend fun cache(key: String, value: Translation) {
    cache.put(key, value)  // Auto-evicts when full
}
```

## Summary

These new utilities provide:
- ✅ Type safety with zero cost
- ✅ Cleaner error handling
- ✅ Efficient resource management
- ✅ Better battery efficiency
- ✅ Thread-safe operations
- ✅ Memory-bounded caching

All while maintaining 100% backward compatibility!
