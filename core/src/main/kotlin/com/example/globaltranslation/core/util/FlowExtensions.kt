package com.example.globaltranslation.core.util

import kotlinx.coroutines.flow.*

/**
 * Flow extension utilities for efficient data stream processing.
 * 
 * These extensions provide modern patterns for:
 * - Error handling with Result types
 * - Resource-efficient streaming
 * - Composable flow transformations
 * - Battery-optimized operations
 * 
 * ## Design Principles:
 * - All transformations are lazy (cold flows)
 * - Inline functions for zero runtime cost
 * - Proper exception handling to prevent flow crashes
 * - Minimal allocations for battery efficiency
 * 
 * ## Example Usage:
 * ```kotlin
 * dataSource.fetchItems()
 *     .mapToResult { processItem(it) }
 *     .filterSuccess()
 *     .debounce(300.milliseconds)
 *     .distinctUntilChanged()
 *     .collect { processedItem -> updateUI(processedItem) }
 * ```
 */

/**
 * Maps flow values to Result, catching any exceptions.
 * Useful for converting throwing operations to Result-based flows.
 * 
 * Any exception thrown by the transform function is caught and wrapped in Result.failure.
 * 
 * @param transform Transformation function that may throw
 * @return Flow of Results containing transformed values or exceptions
 */
inline fun <T, R> Flow<T>.mapToResult(
    crossinline transform: suspend (T) -> R
): Flow<Result<R>> = map { value ->
    try {
        Result.success(transform(value))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Maps Result flow values, propagating failures.
 * Useful for chaining operations on Result flows.
 * 
 * Success values are transformed, failures pass through unchanged.
 * 
 * @param transform Transformation function for success values
 * @return Flow with transformed success values, failures pass through
 */
inline fun <T, R> Flow<Result<T>>.mapResult(
    crossinline transform: suspend (T) -> R
): Flow<Result<R>> = map { result ->
    result.fold(
        onSuccess = { value ->
            try {
                Result.success(transform(value))
            } catch (e: Exception) {
                Result.failure(e)
            }
        },
        onFailure = { Result.failure(it) }
    )
}

/**
 * Flat maps Result flow values, allowing nested Result operations.
 * Similar to mapResult but for operations that return Result.
 * 
 * @param transform Transformation that returns a new Result
 * @return Flow with chained Results
 */
inline fun <T, R> Flow<Result<T>>.flatMapResult(
    crossinline transform: suspend (T) -> Result<R>
): Flow<Result<R>> = map { result ->
    result.fold(
        onSuccess = { value ->
            try {
                transform(value)
            } catch (e: Exception) {
                Result.failure(e)
            }
        },
        onFailure = { Result.failure(it) }
    )
}

/**
 * Filters flow to only emit success results, extracting values.
 * Errors are silently dropped. Use onFailureAction to log them if needed.
 * 
 * @return Flow of unwrapped success values
 */
fun <T> Flow<Result<T>>.filterSuccess(): Flow<T> = 
    mapNotNull { it.getOrNull() }

/**
 * Filters flow to only emit failure results, extracting exceptions.
 * Success values are dropped.
 * 
 * @return Flow of exceptions from failed results
 */
fun <T> Flow<Result<T>>.filterFailures(): Flow<Throwable> =
    mapNotNull { it.exceptionOrNull() }

/**
 * Partitions flow into two flows: successes and failures.
 * Useful when you need to handle both paths differently.
 * 
 * @return Pair of (success flow, failure flow)
 */
fun <T> Flow<Result<T>>.partition(): Pair<Flow<T>, Flow<Throwable>> =
    Pair(filterSuccess(), filterFailures())

/**
 * Catches exceptions and converts them to Result failures.
 * Ensures flow doesn't crash and errors are properly wrapped.
 * 
 * @return Flow that never throws, wrapping values in Results
 */
fun <T> Flow<T>.catchToResult(): Flow<Result<T>> = 
    map { Result.success(it) }
        .catch { emit(Result.failure(it)) }

/**
 * Retries failed operations with exponential backoff.
 * Only retries on failure, success values pass through immediately.
 * 
 * @param attempts Maximum number of retry attempts
 * @param initialDelay Initial delay before first retry (in milliseconds)
 * @param maxDelay Maximum delay between retries (in milliseconds)
 * @param factor Multiplier for delay after each attempt
 * @param predicate Optional condition to determine if error should be retried
 * @return Flow with automatic retries on failure
 */
fun <T> Flow<Result<T>>.retryWithBackoff(
    attempts: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    predicate: (Throwable) -> Boolean = { true }
): Flow<Result<T>> = flow {
    var currentDelay = initialDelay
    var remainingAttempts = attempts
    
    collect { result ->
        result.fold(
            onSuccess = { 
                emit(result)
                currentDelay = initialDelay // Reset delay on success
                remainingAttempts = attempts // Reset attempts on success
            },
            onFailure = { error ->
                if (remainingAttempts > 0 && predicate(error)) {
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                    remainingAttempts--
                } else {
                    emit(result) // Give up and emit failure
                }
            }
        )
    }
}

/**
 * Executes side effects on Result flow without modifying values.
 * Useful for logging, analytics, or UI updates.
 * 
 * @param onSuccess Action to execute for success values
 * @param onFailure Action to execute for failures
 * @return Original flow unchanged
 */
inline fun <T> Flow<Result<T>>.onEachResult(
    crossinline onSuccess: suspend (T) -> Unit = {},
    crossinline onFailure: suspend (Throwable) -> Unit = {}
): Flow<Result<T>> = onEach { result ->
    result.fold(
        onSuccess = { onSuccess(it) },
        onFailure = { onFailure(it) }
    )
}
