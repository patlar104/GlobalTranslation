package com.example.globaltranslation.core.error

/**
 * Sealed class hierarchy representing all possible translation errors.
 * 
 * Using sealed classes instead of exceptions provides:
 * - Type-safe error handling (compiler ensures all cases are handled)
 * - Better composability with Result types
 * - Clearer separation between expected errors and exceptional conditions
 * - No stack trace overhead for expected failures
 * 
 * ## Usage with Result:
 * ```kotlin
 * suspend fun translate(text: String): Result<String> {
 *     return if (text.isBlank()) {
 *         Result.failure(TranslationError.Input.Empty.toException())
 *     } else {
 *         // ... perform translation
 *         Result.success(translatedText)
 *     }
 * }
 * 
 * // Pattern matching on errors
 * result.fold(
 *     onSuccess = { text -> println(text) },
 *     onFailure = { error ->
 *         when (val translationError = error.toTranslationError()) {
 *             is TranslationError.Network -> showNetworkError()
 *             is TranslationError.ModelNotDownloaded -> promptDownload()
 *             is TranslationError.Input.Empty -> showValidationError()
 *         }
 *     }
 * )
 * ```
 */
sealed class TranslationError : Exception() {
    /**
     * Converts this error to a user-friendly message.
     */
    abstract override val message: String
    
    /**
     * Input validation errors
     */
    sealed class Input : TranslationError() {
        data object Empty : Input() {
            override val message = "Text cannot be empty"
        }
        
        data class TooLong(val length: Int, val maxLength: Int) : Input() {
            override val message = "Text is too long ($length characters, maximum is $maxLength)"
        }
        
        data class UnsupportedCharacters(val invalidChars: String) : Input() {
            override val message = "Text contains unsupported characters: $invalidChars"
        }
    }
    
    /**
     * Language-related errors
     */
    sealed class Language : TranslationError() {
        data class Unsupported(val languageCode: String) : Language() {
            override val message = "Language '$languageCode' is not supported"
        }
        
        data class SameSourceAndTarget(val languageCode: String) : Language() {
            override val message = "Source and target language cannot be the same: $languageCode"
        }
        
        data object InvalidPair : Language() {
            override val message = "This language pair is not supported. ML Kit requires English as source or target."
        }
    }
    
    /**
     * Model management errors
     */
    sealed class Model : TranslationError() {
        data class NotDownloaded(val languageCode: String) : Model() {
            override val message = "Translation model for '$languageCode' is not downloaded"
        }
        
        data class DownloadFailed(val languageCode: String, val reason: String?) : Model() {
            override val message = "Failed to download model for '$languageCode'${reason?.let { ": $it" } ?: ""}"
        }
        
        data class Corrupted(val languageCode: String) : Model() {
            override val message = "Translation model for '$languageCode' is corrupted"
        }
        
        data class DeleteFailed(val languageCode: String, val reason: String?) : Model() {
            override val message = "Failed to delete model for '$languageCode'${reason?.let { ": $it" } ?: ""}"
        }
    }
    
    /**
     * Network-related errors
     */
    sealed class Network : TranslationError() {
        data object NoConnection : Network() {
            override val message = "No internet connection available"
        }
        
        data object RequiresWiFi : Network() {
            override val message = "WiFi connection required for model download"
        }
        
        data object Timeout : Network() {
            override val message = "Network request timed out"
        }
        
        data class RequestFailed(val statusCode: Int) : Network() {
            override val message = "Network request failed with status $statusCode"
        }
    }
    
    /**
     * Service/API errors
     */
    sealed class Service : TranslationError() {
        data object Unavailable : Service() {
            override val message = "Translation service is temporarily unavailable"
        }
        
        data object RateLimited : Service() {
            override val message = "Too many requests. Please try again later."
        }
        
        data class ApiError(val code: String, val details: String?) : Service() {
            override val message = "API error $code${details?.let { ": $it" } ?: ""}"
        }
    }
    
    /**
     * Resource/system errors
     */
    sealed class Resource : TranslationError() {
        data object InsufficientStorage : Resource() {
            override val message = "Not enough storage space for model download"
        }
        
        data object InsufficientMemory : Resource() {
            override val message = "Not enough memory to load translation model"
        }
        
        data class PermissionDenied(val permission: String) : Resource() {
            override val message = "Permission denied: $permission"
        }
    }
    
    /**
     * Generic/unknown errors
     */
    data class Unknown(override val cause: Throwable? = null) : TranslationError() {
        override val message = "An unexpected error occurred${cause?.message?.let { ": $it" } ?: ""}"
    }
    
    /**
     * Converts this error to an Exception for Result.failure()
     */
    fun toException(): Exception = this
}

/**
 * Extension to convert Throwable to TranslationError.
 */
fun Throwable.toTranslationError(): TranslationError = when (this) {
    is TranslationError -> this
    else -> when {
        message?.contains("empty", ignoreCase = true) == true -> TranslationError.Input.Empty
        message?.contains("not downloaded", ignoreCase = true) == true -> TranslationError.Model.NotDownloaded("unknown")
        message?.contains("network", ignoreCase = true) == true -> TranslationError.Network.NoConnection
        else -> TranslationError.Unknown(this)
    }
}
