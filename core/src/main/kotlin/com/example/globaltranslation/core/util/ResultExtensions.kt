package com.example.globaltranslation.core.util

/**
 * Result extension utilities for cleaner error handling and composition.
 * These inline functions provide zero-cost abstractions for common Result operations.
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
 * Useful for logging, caching, or other side effects.
 * 
 * @param action Side effect to execute on success
 * @return Original result unchanged
 */
inline fun <T> Result<T>.onSuccessAction(action: (T) -> Unit): Result<T> {
    onSuccess(action)
    return this
}

/**
 * Executes a side effect on failure without modifying the result.
 * Useful for logging or error tracking.
 * 
 * @param action Side effect to execute on failure
 * @return Original result unchanged
 */
inline fun <T> Result<T>.onFailureAction(action: (Throwable) -> Unit): Result<T> {
    onFailure(action)
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
 * @param transform Function that takes success value and returns new Result
 * @return Result of transform or original failure
 */
inline fun <T, R> Result<T>.andThen(transform: (T) -> Result<R>): Result<R> = 
    fold(
        onSuccess = transform,
        onFailure = { Result.failure(it) }
    )
