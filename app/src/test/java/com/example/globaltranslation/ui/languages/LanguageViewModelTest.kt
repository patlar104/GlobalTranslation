package com.example.globaltranslation.ui.languages

import com.example.globaltranslation.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LanguageViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    @Test
    fun `getSupportedLanguages returns expected languages`() {
        val supportedLanguages = getSupportedLanguages()
        
        // Verify we have the expected number of supported languages
        assertEquals(20, supportedLanguages.size)
        
        // Verify key languages are present
        assertTrue(supportedLanguages.any { it.code == "en" && it.name == "English" })
        assertTrue(supportedLanguages.any { it.code == "es" && it.name == "Spanish" })
        assertTrue(supportedLanguages.any { it.code == "fr" && it.name == "French" })
        assertTrue(supportedLanguages.any { it.code == "de" && it.name == "German" })
        assertTrue(supportedLanguages.any { it.code == "it" && it.name == "Italian" })
        assertTrue(supportedLanguages.any { it.code == "pt" && it.name == "Portuguese" })
        assertTrue(supportedLanguages.any { it.code == "zh" && it.name == "Chinese" })
        assertTrue(supportedLanguages.any { it.code == "ja" && it.name == "Japanese" })
        assertTrue(supportedLanguages.any { it.code == "ko" && it.name == "Korean" })
        assertTrue(supportedLanguages.any { it.code == "ru" && it.name == "Russian" })
        
        // Verify all language codes are unique
        val codes = supportedLanguages.map { it.code }
        assertEquals(codes.size, codes.distinct().size)
        
        // Verify all language names are unique
        val names = supportedLanguages.map { it.name }
        assertEquals(names.size, names.distinct().size)
    }
    
    @Test
    fun `DownloadStatus enum has all expected values`() {
        val statuses = DownloadStatus.values()
        
        // Verify simplified enum values (PREPARING, FINALIZING, COMPLETE removed for honesty)
        assertTrue(statuses.contains(DownloadStatus.IDLE))
        assertTrue(statuses.contains(DownloadStatus.DOWNLOADING))
        assertTrue(statuses.contains(DownloadStatus.FAILED))
        assertTrue(statuses.contains(DownloadStatus.PAUSED))
        
        // Verify we have exactly 4 values (simplified from 7)
        assertEquals(4, statuses.size)
    }
    
    @Test
    fun `LanguageModel data class has correct properties`() {
        val model = LanguageModel(
            code = "en",
            name = "English", 
            isDownloaded = true,
            isDownloading = false,
            downloadProgress = 0.5f,
            downloadStatus = DownloadStatus.DOWNLOADING
        )
        
        assertEquals("en", model.code)
        assertEquals("English", model.name)
        assertTrue(model.isDownloaded)
        assertEquals(false, model.isDownloading)
        assertEquals(0.5f, model.downloadProgress)
        assertEquals(DownloadStatus.DOWNLOADING, model.downloadStatus)
    }
}