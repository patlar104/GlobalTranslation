package com.example.globaltranslation.core.util

/**
 * Result extension utilities for cleaner error handling and composition.
 * 
 * These inline functions provide zero-cost abstractions for common Result operations,
 * following modern Kotlin best practices for railway-oriented programming.
 * 
 * ## Design Principles:
 * - All functions are inline for zero runtime cost
 * - Functions are chainable for fluent composition
 * - No exceptions thrown except for truly exceptional conditions
 * - Type-safe error propagation
 * 
 * ## Example Usage:
 * ```kotlin
 * val result = doSomething()
 *     .mapBoth(
 *         onSuccess = { it.uppercase() },
 *         onFailure = { CustomError(it) }
 *     )
 *     .onSuccessAction { log("Success: $it") }
 *     .recoverWith { error ->
 *         when (error) {
 *             is NetworkError -> fallbackValue
 *             else -> throw error
 *         }
 *     }
 * ```
 */

/**
 * Maps both success and failure values to a new result.
 * Useful for transforming errors while preserving the Result wrapper.
 * 
 * @param onSuccess Transform function for success values
 * @param onFailure Transform function for failure exceptions
 * @return Result with transformed value or exception
 */
inline fun <T, R> Result<T>.mapBoth(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> Throwable
): Result<R> = fold(
    onSuccess = { Result.success(onSuccess(it)) },
    onFailure = { Result.failure(onFailure(it)) }
)

/**
 * Executes a side effect on success without modifying the result.
 * Useful for logging, caching, analytics, or other side effects.
 * 
 * The action is only executed if the result is successful.
 * Any exception thrown by the action is caught and ignored to maintain result integrity.
 * 
 * @param action Side effect to execute on success
 * @return Original result unchanged
 */
inline fun <T> Result<T>.onSuccessAction(action: (T) -> Unit): Result<T> {
    onSuccess { value ->
        try {
            action(value)
        } catch (e: Exception) {
            // Silently ignore errors in side effects to not affect the result
        }
    }
    return this
}

/**
 * Executes a side effect on failure without modifying the result.
 * Useful for logging errors, tracking analytics, or error reporting.
 * 
 * The action is only executed if the result is a failure.
 * Any exception thrown by the action is caught and ignored to maintain result integrity.
 * 
 * @param action Side effect to execute on failure
 * @return Original result unchanged
 */
inline fun <T> Result<T>.onFailureAction(action: (Throwable) -> Unit): Result<T> {
    onFailure { error ->
        try {
            action(error)
        } catch (e: Exception) {
            // Silently ignore errors in side effects to not affect the result
        }
    }
    return this
}

/**
 * Recovers from failure by providing a default value.
 * More concise than getOrElse for common use cases.
 * 
 * @param defaultValue Value to use if result is failure
 * @return Success value or default value
 */
fun <T> Result<T>.orDefault(defaultValue: T): T = getOrElse { defaultValue }

/**
 * Recovers from failure by computing a default value from the exception.
 * Allows error-specific recovery strategies.
 * 
 * @param compute Function to compute default from exception
 * @return Success value or computed default
 */
inline fun <T> Result<T>.recoverWith(compute: (Throwable) -> T): T = getOrElse(compute)

/**
 * Chains multiple Result operations, short-circuiting on first failure.
 * Similar to flatMap but more readable for multiple operations.
 * 
 * Also known as "bind" in functional programming.
 * 
 * @param transform Function that takes success value and returns new Result
 * @return Result of transform or original failure
 */
inline fun <T, R> Result<T>.andThen(transform: (T) -> Result<R>): Result<R> = 
    fold(
        onSuccess = transform,
        onFailure = { Result.failure(it) }
    )

/**
 * Zips two Results together, succeeding only if both succeed.
 * Useful for combining independent operations that must all succeed.
 * 
 * If either result is a failure, returns the first failure encountered.
 * 
 * @param other The other Result to zip with
 * @param transform Function to combine success values
 * @return Result of combined values or first failure
 */
inline fun <T, U, R> Result<T>.zip(
    other: Result<U>,
    transform: (T, U) -> R
): Result<R> = andThen { t ->
    other.map { u -> transform(t, u) }
}

/**
 * Maps a success value to a nullable type, treating null as failure.
 * Useful when a successful result might legitimately be null.
 * 
 * @param transform Function that returns nullable value
 * @param onNull Exception to use if transform returns null
 * @return Result with non-null value or failure
 */
inline fun <T, R : Any> Result<T>.mapNotNull(
    transform: (T) -> R?,
    onNull: () -> Throwable = { NullPointerException("Transform returned null") }
): Result<R> = andThen { value ->
    transform(value)?.let { Result.success(it) } ?: Result.failure(onNull())
}

/**
 * Filters a Result based on a predicate, converting unmatched success to failure.
 * 
 * @param predicate Condition that success value must satisfy
 * @param onPredicateFailed Exception to use if predicate fails
 * @return Original result if predicate passes, failure otherwise
 */
inline fun <T> Result<T>.filter(
    predicate: (T) -> Boolean,
    onPredicateFailed: (T) -> Throwable = { IllegalArgumentException("Predicate failed for value: $it") }
): Result<T> = andThen { value ->
    if (predicate(value)) Result.success(value)
    else Result.failure(onPredicateFailed(value))
}

/**
 * Returns the success value or null if failed.
 * More concise than getOrNull() for nullable return scenarios.
 */
fun <T> Result<T>.orNull(): T? = getOrNull()

