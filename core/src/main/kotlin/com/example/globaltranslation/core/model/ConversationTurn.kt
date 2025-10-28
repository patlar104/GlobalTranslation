package com.example.globaltranslation.core.model

/**
 * Represents a single turn in a conversation with original text, translation, and language info.
 * 
 * This is an immutable domain model following modern Kotlin best practices:
 * - Pure Kotlin with no Android dependencies (enables unit testing without Android runtime)
 * - Value semantics with data class (structural equality, copy, toString, etc.)
 * - Non-null by default for safer code
 * - Timestamp defaults to creation time for convenience
 * 
 * ## Usage:
 * ```kotlin
 * val turn = ConversationTurn(
 *     originalText = "Hello",
 *     translatedText = "Hola",
 *     sourceLang = "en",
 *     targetLang = "es"
 * )
 * 
 * // Create a modified copy
 * val updatedTurn = turn.copy(translatedText = "¡Hola!")
 * ```
 * 
 * @property originalText The original untranslated text (non-empty recommended)
 * @property translatedText The translated text (non-empty recommended)
 * @property sourceLang ISO 639-1 language code (e.g., "en", "es", "fr")
 * @property targetLang ISO 639-1 language code (e.g., "en", "es", "fr")
 * @property timestamp Unix epoch milliseconds when turn was created
 */
data class ConversationTurn(
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(originalText.isNotBlank()) { "Original text cannot be blank" }
        require(translatedText.isNotBlank()) { "Translated text cannot be blank" }
        require(sourceLang.isNotBlank()) { "Source language cannot be blank" }
        require(targetLang.isNotBlank()) { "Target language cannot be blank" }
        require(sourceLang != targetLang) { "Source and target languages must be different" }
    }
    
    /**
     * Returns true if this turn represents a bidirectional language pair swap of the other turn.
     * Useful for detecting reverse translations.
     */
    fun isReversePairOf(other: ConversationTurn): Boolean =
        sourceLang == other.targetLang && targetLang == other.sourceLang
    
    /**
     * Returns a reversed copy with source and target languages swapped.
     * The text content remains unchanged (does not perform translation).
     */
    fun reversed(): ConversationTurn = copy(
        originalText = translatedText,
        translatedText = originalText,
        sourceLang = targetLang,
        targetLang = sourceLang
    )
}

