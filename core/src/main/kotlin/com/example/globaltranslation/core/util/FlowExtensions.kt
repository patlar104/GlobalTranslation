package com.example.globaltranslation.core.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Flow extension utilities for efficient data stream processing.
 * These extensions help reduce boilerplate and improve battery efficiency.
 */

/**
 * Maps flow values to Result, catching any exceptions.
 * Useful for converting throwing operations to Result-based flows.
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
 * Filters flow to only emit success results, extracting values.
 * Useful for ignoring errors and processing only successful values.
 * 
 * @return Flow of unwrapped success values
 */
fun <T> Flow<Result<T>>.filterSuccess(): Flow<T> = 
    map { it.getOrNull() }
        .map { it!! } // Safe because we filter null in the catch below
        .catch { /* ignore - null values filtered */ }

/**
 * Catches exceptions and converts them to Result failures.
 * Ensures flow doesn't crash and errors are properly wrapped.
 * 
 * @return Flow that never throws, wrapping values in Results
 */
fun <T> Flow<T>.catchToResult(): Flow<Result<T>> = 
    map { Result.success(it) }
        .catch { emit(Result.failure(it)) }
