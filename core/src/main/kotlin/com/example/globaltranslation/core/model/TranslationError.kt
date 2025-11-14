package com.example.globaltranslation.core.model

/**
 * Sealed hierarchy for translation errors.
 * Uses typed error information instead of generic messages,
 * allowing the UI layer to handle each error type distinctly with type safety.
 */
sealed interface TranslationError {
    /**
     * No text was detected in the image.
     * UI can provide a user-friendly message like "No text detected. Try again with clearer text."
     */
    data object NoTextDetected : TranslationError
    
    /**
     * Translation failed with an underlying cause.
     * @param cause The throwable that caused the failure, if available
     */
    data class TranslationFailed(val cause: Throwable? = null) : TranslationError
    
    /**
     * Invalid language pair was provided for translation.
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     */
    data class InvalidLanguagePair(
        val sourceLanguage: String,
        val targetLanguage: String
    ) : TranslationError
    
    /**
     * Network-related error occurred (e.g., no WiFi for model download).
     * UI can provide guidance about WiFi and model downloads.
     */
    data object NetworkError : TranslationError
    
    /**
     * Unknown or unexpected error occurred.
     * @param cause The throwable that caused the failure, if available
     */
    data class UnknownError(val cause: Throwable? = null) : TranslationError
}
