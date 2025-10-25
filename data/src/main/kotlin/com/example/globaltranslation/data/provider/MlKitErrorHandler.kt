package com.example.globaltranslation.data.provider

import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.mlkit.common.MlKitException
import kotlinx.coroutines.delay

/**
 * ML Kit Error Handling Utilities.
 * 
 * Provides standardized error handling and retry logic for ML Kit operations.
 * This ensures consistent behavior across all ML Kit providers and improves
 * reliability by handling transient failures gracefully.
 * 
 * ## Key Features:
 * - Standardized error classification
 * - Retry logic with exponential backoff
 * - Detailed error messages for debugging
 * - Performance monitoring hooks
 * 
 * ## Usage:
 * ```kotlin
 * val result = withMlKitRetry {
 *     performMlKitOperation()
 * }
 * ```
 */
object MlKitErrorHandler {
    
    private const val TAG = "MlKitErrorHandler"
    
    /**
     * Classification of ML Kit errors for better handling.
     */
    enum class ErrorType {
        /** Network-related errors (model download, etc.) */
        NETWORK,
        /** Model not downloaded or unavailable */
        MODEL_UNAVAILABLE,
        /** Input validation errors */
        INVALID_INPUT,
        /** Resource exhaustion (memory, etc.) */
        RESOURCE_EXHAUSTED,
        /** Unknown or unexpected errors */
        UNKNOWN
    }
    
    /**
     * Classifies an exception into a specific error type.
     * This helps determine appropriate retry and recovery strategies.
     * 
     * @param throwable The exception to classify
     * @return ErrorType representing the category of error
     */
    fun classifyError(throwable: Throwable): ErrorType {
        return when (throwable) {
            is MlKitException -> {
                when (throwable.errorCode) {
                    MlKitException.UNAVAILABLE -> ErrorType.MODEL_UNAVAILABLE
                    MlKitException.NETWORK_ISSUE -> ErrorType.NETWORK
                    MlKitException.INVALID_ARGUMENT -> ErrorType.INVALID_INPUT
                    MlKitException.INTERNAL -> ErrorType.RESOURCE_EXHAUSTED
                    else -> ErrorType.UNKNOWN
                }
            }
            is ApiException -> ErrorType.NETWORK
            is IllegalArgumentException -> ErrorType.INVALID_INPUT
            is OutOfMemoryError -> ErrorType.RESOURCE_EXHAUSTED
            else -> ErrorType.UNKNOWN
        }
    }
    
    /**
     * Determines if an error is retryable.
     * 
     * @param errorType The classified error type
     * @return true if the operation should be retried
     */
    fun isRetryable(errorType: ErrorType): Boolean {
        return when (errorType) {
            ErrorType.NETWORK -> true
            ErrorType.MODEL_UNAVAILABLE -> true
            ErrorType.INVALID_INPUT -> false
            ErrorType.RESOURCE_EXHAUSTED -> false
            ErrorType.UNKNOWN -> false
        }
    }
    
    /**
     * Generates a user-friendly error message.
     * 
     * @param errorType The classified error type
     * @param originalMessage The original error message
     * @return A user-friendly error message
     */
    fun getUserFriendlyMessage(errorType: ErrorType, originalMessage: String?): String {
        return when (errorType) {
            ErrorType.NETWORK -> "Network error. Please check your connection and try again."
            ErrorType.MODEL_UNAVAILABLE -> "Translation model not available. Please download the language model first."
            ErrorType.INVALID_INPUT -> "Invalid input: ${originalMessage ?: "Unknown error"}"
            ErrorType.RESOURCE_EXHAUSTED -> "Device resources exhausted. Please try again later."
            ErrorType.UNKNOWN -> "An unexpected error occurred: ${originalMessage ?: "Unknown error"}"
        }
    }
    
    /**
     * Executes an ML Kit operation with retry logic.
     * 
     * Automatically retries transient failures with exponential backoff.
     * Non-retryable errors (like invalid input) fail immediately.
     * 
     * @param maxAttempts Maximum number of attempts (default from config)
     * @param initialDelay Initial delay between retries in ms
     * @param block The operation to execute
     * @return Result of the operation
     */
    suspend fun <T> withMlKitRetry(
        maxAttempts: Int = MlKitConfig.MAX_RETRY_ATTEMPTS,
        initialDelay: Long = MlKitConfig.RETRY_DELAY_MS,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        var lastException: Throwable? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                val result = block()
                if (MlKitConfig.ENABLE_DEBUG_LOGGING && attempt > 0) {
                    Log.d(TAG, "Operation succeeded after ${attempt + 1} attempts")
                }
                return Result.success(result)
            } catch (e: Throwable) {
                lastException = e
                val errorType = classifyError(e)
                
                if (MlKitConfig.ENABLE_DEBUG_LOGGING) {
                    Log.w(TAG, "Attempt ${attempt + 1} failed with error type: $errorType", e)
                }
                
                // Don't retry if error is not retryable or if this was the last attempt
                if (!isRetryable(errorType) || attempt == maxAttempts - 1) {
                    val message = getUserFriendlyMessage(errorType, e.message)
                    return Result.failure(Exception(message, e))
                }
                
                // Wait before retrying with exponential backoff
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        
        // Should never reach here, but handle it gracefully
        return Result.failure(
            lastException ?: Exception("Operation failed after $maxAttempts attempts")
        )
    }
    
    /**
     * Logs performance metrics for ML Kit operations.
     * Useful for monitoring and optimization.
     * 
     * @param operation Name of the operation
     * @param durationMs Duration in milliseconds
     * @param success Whether the operation succeeded
     */
    fun logPerformanceMetric(operation: String, durationMs: Long, success: Boolean) {
        if (MlKitConfig.ENABLE_DEBUG_LOGGING) {
            val status = if (success) "SUCCESS" else "FAILURE"
            Log.d(TAG, "ML Kit Performance: $operation - $status in ${durationMs}ms")
        }
    }
    
    /**
     * Validates text input for ML Kit operations.
     * 
     * @param text The text to validate
     * @param operation Name of the operation for error messages
     * @return Result indicating validation success or failure
     */
    fun validateTextInput(text: String, operation: String): Result<Unit> {
        return when {
            text.isBlank() -> Result.failure(
                IllegalArgumentException("$operation: Text cannot be empty or blank")
            )
            text.length < MlKitConfig.MIN_TEXT_LENGTH -> Result.failure(
                IllegalArgumentException("$operation: Text is too short (minimum ${MlKitConfig.MIN_TEXT_LENGTH} characters)")
            )
            text.length > MlKitConfig.MAX_TEXT_LENGTH -> Result.failure(
                IllegalArgumentException("$operation: Text is too long (maximum ${MlKitConfig.MAX_TEXT_LENGTH} characters)")
            )
            else -> Result.success(Unit)
        }
    }
}
