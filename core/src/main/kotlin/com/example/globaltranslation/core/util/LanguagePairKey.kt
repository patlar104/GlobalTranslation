package com.example.globaltranslation.core.util

/**
 * Inline value class for language pair keys.
 * Provides type safety and zero-cost abstraction for language pair operations.
 * 
 * Benefits:
 * - Type safety: Prevents mixing up language codes with other strings
 * - Performance: No runtime overhead (compiles to String)
 * - Code clarity: Makes intent explicit in function signatures
 */
@JvmInline
value class LanguagePairKey private constructor(val value: String) {
    
    companion object {
        /**
         * Creates a language pair key from source and target language codes.
         * @param from Source language code (e.g., "en")
         * @param to Target language code (e.g., "es")
         * @return LanguagePairKey representing the pair
         */
        fun create(from: String, to: String): LanguagePairKey {
            require(from.isNotBlank()) { "Source language code cannot be blank" }
            require(to.isNotBlank()) { "Target language code cannot be blank" }
            return LanguagePairKey("$from-$to")
        }
        
        /**
         * Parses a language pair key string into a LanguagePairKey.
         * @param key String in format "from-to"
         * @return LanguagePairKey or null if invalid format
         */
        fun parse(key: String): LanguagePairKey? {
            val parts = key.split("-")
            return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                LanguagePairKey(key)
            } else {
                null
            }
        }
    }
    
    /**
     * Checks if this language pair contains the given language code.
     * @param languageCode Language code to check (e.g., "en", "es")
     * @return true if the language code is part of this pair
     */
    fun containsLanguage(languageCode: String): Boolean {
        return value.startsWith("$languageCode-") || value.endsWith("-$languageCode")
    }
    
    /**
     * Gets the source language code from this pair.
     */
    val sourceLanguage: String
        get() = value.substringBefore("-")
    
    /**
     * Gets the target language code from this pair.
     */
    val targetLanguage: String
        get() = value.substringAfter("-")
    
    override fun toString(): String = value
}
