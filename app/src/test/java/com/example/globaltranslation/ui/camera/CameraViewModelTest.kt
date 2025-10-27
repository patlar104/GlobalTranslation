package com.example.globaltranslation.ui.camera

import com.example.globaltranslation.testing.MainDispatcherRule
import com.example.globaltranslation.testing.fakes.FakeCameraTranslationProvider
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private fun buildVm(
        cameraProvider: FakeCameraTranslationProvider = FakeCameraTranslationProvider()
    ): Pair<CameraViewModel, FakeCameraTranslationProvider> {
        val vm = CameraViewModel(cameraProvider)
        return Pair(vm, cameraProvider)
    }
    
    // Simple fake input image for testing
    private val fakeInputImage = object : Any() {
        override fun toString() = "FakeInputImage"
    }
    
    @Test
    fun `initial state has default values`() = runTest {
        val (vm, _) = buildVm()
        val state = vm.uiState.first()
        
        assertEquals(TranslateLanguage.ENGLISH, state.sourceLanguageCode)
        assertEquals(TranslateLanguage.SPANISH, state.targetLanguageCode)
        assertTrue(state.detectedTextBlocks.isEmpty())
        assertFalse(state.isProcessing)
        assertFalse(state.isFrozen)
        assertFalse(state.isFlashOn)
        assertNull(state.error)
    }
    
    @Test
    fun `setSourceLanguage updates source language`() = runTest {
        val (vm, _) = buildVm()
        
        vm.setSourceLanguage(TranslateLanguage.FRENCH)
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertEquals(TranslateLanguage.FRENCH, state.sourceLanguageCode)
    }
    
    @Test
    fun `setSourceLanguage enforces English requirement when both non-English`() = runTest {
        val (vm, _) = buildVm()
        
        // Set target to French first
        vm.setTargetLanguage(TranslateLanguage.FRENCH)
        advanceUntilIdle()
        
        // Now try to set source to Spanish (both would be non-English)
        vm.setSourceLanguage(TranslateLanguage.SPANISH)
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertEquals(TranslateLanguage.SPANISH, state.sourceLanguageCode)
        assertEquals(TranslateLanguage.ENGLISH, state.targetLanguageCode) // Reset to English
    }
    
    @Test
    fun `setTargetLanguage updates target language`() = runTest {
        val (vm, _) = buildVm()
        
        vm.setTargetLanguage(TranslateLanguage.GERMAN)
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertEquals(TranslateLanguage.GERMAN, state.targetLanguageCode)
    }
    
    @Test
    fun `setTargetLanguage enforces English requirement when both non-English`() = runTest {
        val (vm, _) = buildVm()
        
        // Set source to French first
        vm.setSourceLanguage(TranslateLanguage.FRENCH)
        advanceUntilIdle()
        
        // Now try to set target to Spanish (both would be non-English)
        vm.setTargetLanguage(TranslateLanguage.SPANISH)
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertEquals(TranslateLanguage.ENGLISH, state.sourceLanguageCode) // Reset to English
        assertEquals(TranslateLanguage.SPANISH, state.targetLanguageCode)
    }
    
    @Test
    fun `swapLanguages swaps source and target`() = runTest {
        val (vm, _) = buildVm()
        
        vm.setSourceLanguage(TranslateLanguage.ENGLISH)
        vm.setTargetLanguage(TranslateLanguage.GERMAN)
        advanceUntilIdle()
        
        vm.swapLanguages()
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertEquals(TranslateLanguage.GERMAN, state.sourceLanguageCode)
        assertEquals(TranslateLanguage.ENGLISH, state.targetLanguageCode)
    }
    
    @Test
    fun `toggleFlash toggles flash state`() = runTest {
        val (vm, _) = buildVm()
        
        assertFalse(vm.uiState.first().isFlashOn)
        
        vm.toggleFlash()
        advanceUntilIdle()
        assertTrue(vm.uiState.first().isFlashOn)
        
        vm.toggleFlash()
        advanceUntilIdle()
        assertFalse(vm.uiState.first().isFlashOn)
    }
    
    @Test
    fun `processCapturedImage success updates state with results`() = runTest {
        val (vm, provider) = buildVm()
        provider.shouldSucceed = true
        provider.shouldDetectText = true
        
        vm.processCapturedImage(fakeInputImage as InputImage)
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertFalse(state.isProcessing)
        assertTrue(state.isFrozen)
        assertTrue(state.detectedTextBlocks.isNotEmpty())
        assertEquals("Translated: Test text", state.detectedTextBlocks.first().translatedText)
        assertNull(state.error)
    }
    
    @Test
    fun `processCapturedImage no text detected shows error`() = runTest {
        val (vm, provider) = buildVm()
        provider.shouldSucceed = true
        provider.shouldDetectText = false
        
        vm.processCapturedImage(fakeInputImage as InputImage)
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertFalse(state.isProcessing)
        assertTrue(state.detectedTextBlocks.isEmpty())
        assertNotNull(state.error)
        assertTrue(state.error?.contains("No text detected") == true)
    }
    
    @Test
    fun `processCapturedImage failure shows error`() = runTest {
        val (vm, provider) = buildVm()
        provider.shouldSucceed = false
        provider.errorMessage = "Network error"
        
        vm.processCapturedImage(fakeInputImage as InputImage)
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertFalse(state.isProcessing)
        assertNotNull(state.error)
        assertTrue(state.error?.contains("Translation failed") == true)
    }
    
    @Test
    fun `processCapturedImage sets processing and frozen states`() = runTest {
        val (vm, _) = buildVm()
        
        vm.processCapturedImage(fakeInputImage as InputImage)
        
        // Check intermediate state (before advanceUntilIdle)
        val processingState = vm.uiState.value
        assertTrue(processingState.isProcessing)
        assertTrue(processingState.isFrozen)
        
        advanceUntilIdle()
        
        val finalState = vm.uiState.first()
        assertFalse(finalState.isProcessing)
        assertTrue(finalState.isFrozen)
    }
    
    @Test
    fun `clearError clears error message`() = runTest {
        val (vm, provider) = buildVm()
        provider.shouldSucceed = false
        
        vm.processCapturedImage(fakeInputImage as InputImage)
        advanceUntilIdle()
        
        assertNotNull(vm.uiState.first().error)
        
        vm.clearError()
        advanceUntilIdle()
        
        assertNull(vm.uiState.first().error)
    }
    
    @Test
    fun `clearResults clears detection results and unfreezes`() = runTest {
        val (vm, provider) = buildVm()
        provider.shouldSucceed = true
        provider.shouldDetectText = true
        
        vm.processCapturedImage(fakeInputImage as InputImage)
        advanceUntilIdle()
        
        assertTrue(vm.uiState.first().detectedTextBlocks.isNotEmpty())
        assertTrue(vm.uiState.first().isFrozen)
        
        vm.clearResults()
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertTrue(state.detectedTextBlocks.isEmpty())
        assertFalse(state.isFrozen)
        assertFalse(state.isProcessing)
        assertNull(state.error)
    }
    
    @Test
    fun `reset unfreezes camera and clears translations`() = runTest {
        val (vm, provider) = buildVm()
        provider.shouldSucceed = true
        
        vm.processCapturedImage(fakeInputImage as InputImage)
        advanceUntilIdle()
        
        vm.reset()
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertFalse(state.isFrozen)
        assertTrue(state.detectedTextBlocks.isEmpty())
        assertNull(state.error)
    }
    
    @Test
    fun `CameraUiState validates language pairs correctly`() {
        val validPair1 = CameraUiState(
            sourceLanguageCode = TranslateLanguage.ENGLISH,
            targetLanguageCode = TranslateLanguage.SPANISH
        )
        assertTrue(validPair1.isValidLanguagePair())
        
        val validPair2 = CameraUiState(
            sourceLanguageCode = TranslateLanguage.FRENCH,
            targetLanguageCode = TranslateLanguage.ENGLISH
        )
        assertTrue(validPair2.isValidLanguagePair())
        
        val invalidPair = CameraUiState(
            sourceLanguageCode = TranslateLanguage.FRENCH,
            targetLanguageCode = TranslateLanguage.SPANISH
        )
        assertFalse(invalidPair.isValidLanguagePair())
    }
    
    @Test
    fun `processCapturedImage uses correct language codes`() = runTest {
        val (vm, provider) = buildVm()
        
        vm.setSourceLanguage(TranslateLanguage.FRENCH)
        vm.setTargetLanguage(TranslateLanguage.ENGLISH)
        advanceUntilIdle()
        
        vm.processCapturedImage(fakeInputImage as InputImage)
        advanceUntilIdle()
        
        assertEquals(TranslateLanguage.FRENCH, provider.lastSourceLanguage)
        assertEquals(TranslateLanguage.ENGLISH, provider.lastTargetLanguage)
    }
}
