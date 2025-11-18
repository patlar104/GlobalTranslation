package com.example.globaltranslation.model

/**
 * Sealed hierarchy for translation errors.
 * This allows the UI to handle different error types appropriately.
 */
sealed interface TranslationError {
    data class NoTextDetected(val message: String) : TranslationError
    data class TranslationFailed(val message: String) : TranslationError
    data class InvalidLanguagePair(val message: String) : TranslationError
    data class NetworkError(val message: String) : TranslationError
    data class UnknownError(val message: String) : TranslationError
}
