package com.example.globaltranslation.data.provider

/**
 * Common provider utilities to reduce code duplication.
 * 
 * Contains shared functionality used across multiple providers:
 * - Input validation
 * - Error message formatting
 * - Common guard clauses
 * 
 * ## Purpose:
 * Centralizes repetitive validation and error handling logic that appears
 * across different provider implementations, improving code maintainability
 * and consistency.
 * 
 * ## Benefits:
 * - Eliminates duplicate validation code
 * - Ensures consistent error messages
 * - Single source of truth for common operations
 * - Easier to maintain and test
 * 
 * ## Example Usage:
 * ```kotlin
 * // Instead of repeating validation in each provider:
 * if (imageData !is InputImage) {
 *     return Result.failure(IllegalArgumentException("imageData must be InputImage"))
 * }
 * 
 * // Use utility method:
 * ProviderUtils.validateImageData(imageData)?.let { return it }
 * ```
 */
object ProviderUtils {
    
    /**
     * Validates that imageData is an InputImage.
     * 
     * @param imageData The image data to validate
     * @return Result.failure if invalid, null if valid
     */
    fun <T> validateImageData(imageData: Any): Result<T>? {
        return if (imageData !is com.google.mlkit.vision.common.InputImage) {
            Result.failure(IllegalArgumentException(
                "Invalid image data type. Expected InputImage but got ${imageData::class.simpleName}"
            ))
        } else {
            null
        }
    }
    
    /**
     * Validates that text is not blank.
     * 
     * @param text The text to validate
     * @param operation Name of the operation for error message
     * @return Result.failure if invalid, null if valid
     */
    fun <T> validateTextNotBlank(text: String, operation: String = "Operation"): Result<T>? {
        return if (text.isBlank()) {
            Result.failure(IllegalArgumentException(
                "$operation: Text cannot be empty or blank"
            ))
        } else {
            null
        }
    }
    
    /**
     * Creates a standardized error message for provider operations.
     * 
     * @param providerName Name of the provider (e.g., "Translation")
     * @param operation Name of the operation (e.g., "translate")
     * @param cause The underlying exception
     * @return Formatted error message
     */
    fun formatProviderError(providerName: String, operation: String, cause: Throwable): String {
        return "$providerName provider error during $operation: ${cause.message ?: cause::class.simpleName}"
    }
    
    /**
     * Wraps an exception with additional context.
     * 
     * @param message Context message
     * @param cause Original exception
     * @return New exception with context
     */
    fun wrapException(message: String, cause: Throwable): Exception {
        return Exception(message, cause)
    }
}
