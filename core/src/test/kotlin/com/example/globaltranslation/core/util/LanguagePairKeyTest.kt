package com.example.globaltranslation.core.util

import org.junit.Assert.*
import org.junit.Test

class LanguagePairKeyTest {
    
    @Test
    fun `create() creates valid language pair key`() {
        val key = LanguagePairKey.create("en", "es")
        assertEquals("en-es", key.value)
    }
    
    @Test
    fun `create() throws for blank source language`() {
        assertThrows(IllegalArgumentException::class.java) {
            LanguagePairKey.create("", "es")
        }
    }
    
    @Test
    fun `create() throws for blank target language`() {
        assertThrows(IllegalArgumentException::class.java) {
            LanguagePairKey.create("en", "")
        }
    }
    
    @Test
    fun `parse() parses valid key string`() {
        val key = LanguagePairKey.parse("en-es")
        assertNotNull(key)
        assertEquals("en-es", key?.value)
    }
    
    @Test
    fun `parse() returns null for invalid format`() {
        assertNull(LanguagePairKey.parse("invalid"))
        assertNull(LanguagePairKey.parse("en-es-fr"))
        assertNull(LanguagePairKey.parse("-es"))
        assertNull(LanguagePairKey.parse("en-"))
    }
    
    @Test
    fun `containsLanguage() detects source language`() {
        val key = LanguagePairKey.create("en", "es")
        assertTrue(key.containsLanguage("en"))
    }
    
    @Test
    fun `containsLanguage() detects target language`() {
        val key = LanguagePairKey.create("en", "es")
        assertTrue(key.containsLanguage("es"))
    }
    
    @Test
    fun `containsLanguage() returns false for non-existent language`() {
        val key = LanguagePairKey.create("en", "es")
        assertFalse(key.containsLanguage("fr"))
    }
    
    @Test
    fun `sourceLanguage extracts correct code`() {
        val key = LanguagePairKey.create("en", "es")
        assertEquals("en", key.sourceLanguage)
    }
    
    @Test
    fun `targetLanguage extracts correct code`() {
        val key = LanguagePairKey.create("en", "es")
        assertEquals("es", key.targetLanguage)
    }
    
    @Test
    fun `toString() returns key value`() {
        val key = LanguagePairKey.create("en", "es")
        assertEquals("en-es", key.toString())
    }
    
    @Test
    fun `multiple language codes work correctly`() {
        val key1 = LanguagePairKey.create("zh", "ja")
        val key2 = LanguagePairKey.create("ar", "hi")
        
        assertEquals("zh", key1.sourceLanguage)
        assertEquals("ja", key1.targetLanguage)
        assertEquals("ar", key2.sourceLanguage)
        assertEquals("hi", key2.targetLanguage)
    }
}
