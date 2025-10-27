package com.example.globaltranslation.ui.conversation

import com.example.globaltranslation.core.provider.SpeechResult
import com.example.globaltranslation.core.provider.TtsEvent
import com.example.globaltranslation.model.ConversationTurn
import com.example.globaltranslation.testing.MainDispatcherRule
import com.example.globaltranslation.testing.fakes.FakeConversationRepository
import com.example.globaltranslation.testing.fakes.FakeSpeechProvider
import com.example.globaltranslation.testing.fakes.FakeTextToSpeechProvider
import com.example.globaltranslation.testing.fakes.FakeTranslationProvider
import com.google.mlkit.nl.translate.TranslateLanguage
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
class ConversationViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private fun buildVm(
        translation: FakeTranslationProvider = FakeTranslationProvider(),
        speech: FakeSpeechProvider = FakeSpeechProvider(),
        tts: FakeTextToSpeechProvider = FakeTextToSpeechProvider(),
        repo: FakeConversationRepository = FakeConversationRepository()
    ): TestComponents {
        val vm = ConversationViewModel(translation, speech, tts, repo)
        return TestComponents(vm, translation, speech, tts, repo)
    }
    
    data class TestComponents(
        val vm: ConversationViewModel,
        val translation: FakeTranslationProvider,
        val speech: FakeSpeechProvider,
        val tts: FakeTextToSpeechProvider,
        val repo: FakeConversationRepository
    )
    
    @Test
    fun `initial state has default values`() = runTest {
        val (vm, _, _, _, _) = buildVm()
        val state = vm.uiState.first()
        
        assertEquals(TranslateLanguage.ENGLISH, state.sourceLanguage)
        assertEquals(TranslateLanguage.SPANISH, state.targetLanguage)
        assertTrue(state.conversationHistory.isEmpty())
        assertFalse(state.isListening)
        assertFalse(state.isTranslating)
        assertFalse(state.isSpeaking)
        assertTrue(state.autoPlayTranslation)
        assertNull(state.error)
    }
    
    @Test
    fun `startListening sets listening state`() = runTest {
        val (vm, _, speech, _, _) = buildVm()
        
        vm.startListening(TranslateLanguage.ENGLISH)
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertTrue(state.isListening)
        assertTrue(speech.isListening)
    }
    
    @Test
    fun `startListening when already listening does nothing`() = runTest {
        val (vm, _, speech, _, _) = buildVm()
        
        vm.startListening(TranslateLanguage.ENGLISH)
        advanceUntilIdle()
        
        val firstListeningState = speech.isListening
        
        vm.startListening(TranslateLanguage.SPANISH)
        advanceUntilIdle()
        
        assertEquals(firstListeningState, speech.isListening)
    }
    
    @Test
    fun `speech recognition ReadyForSpeech updates state`() = runTest {
        val (vm, _, speech, _, _) = buildVm()
        
        vm.startListening(TranslateLanguage.ENGLISH)
        speech.emitEvent(SpeechResult.ReadyForSpeech)
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertTrue(state.isListeningReady)
        assertTrue(state.isDetectingSpeech)
    }
    
    @Test
    fun `speech recognition PartialResult updates partial text`() = runTest {
        val (vm, _, speech, _, _) = buildVm()
        
        vm.startListening(TranslateLanguage.ENGLISH)
        speech.emitEvent(SpeechResult.PartialResult("Hello"))
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertEquals("Hello", state.partialSpeechText)
    }
    
    @Test
    fun `speech recognition FinalResult triggers translation`() = runTest {
        val (vm, translation, speech, _, repo) = buildVm()
        translation.translatedPrefix = "Hola"
        
        vm.startListening(TranslateLanguage.ENGLISH)
        speech.emitResult("Hello")
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertFalse(state.isListening)
        assertEquals("", state.partialSpeechText)
        assertTrue(state.conversationHistory.isNotEmpty())
        assertEquals("Hola: Hello", state.conversationHistory.first().translatedText)
        assertTrue(repo.saved.isNotEmpty())
    }
    
    @Test
    fun `speech recognition Error updates error state`() = runTest {
        val (vm, _, speech, _, _) = buildVm()
        
        vm.startListening(TranslateLanguage.ENGLISH)
        speech.emitError("Audio error")
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertFalse(state.isListening)
        assertNotNull(state.error)
        assertTrue(state.error?.contains("Audio error") == true)
    }
    
    @Test
    fun `speech recognition EndOfSpeech updates detecting state`() = runTest {
        val (vm, _, speech, _, _) = buildVm()
        
        vm.startListening(TranslateLanguage.ENGLISH)
        speech.emitEvent(SpeechResult.ReadyForSpeech)
        advanceUntilIdle()
        
        assertTrue(vm.uiState.first().isDetectingSpeech)
        
        speech.emitEvent(SpeechResult.EndOfSpeech)
        advanceUntilIdle()
        
        assertFalse(vm.uiState.first().isDetectingSpeech)
    }
    
    @Test
    fun `stopListening stops speech recognition`() = runTest {
        val (vm, _, speech, _, _) = buildVm()
        
        vm.startListening(TranslateLanguage.ENGLISH)
        advanceUntilIdle()
        
        assertTrue(vm.uiState.first().isListening)
        
        vm.stopListening()
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertFalse(state.isListening)
        assertFalse(state.isListeningReady)
        assertFalse(state.isDetectingSpeech)
        assertEquals("", state.partialSpeechText)
    }
    
    @Test
    fun `translation failure shows error`() = runTest {
        val (vm, translation, speech, _, _) = buildVm()
        translation.shouldSucceed = false
        
        vm.startListening(TranslateLanguage.ENGLISH)
        speech.emitResult("Hello")
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertFalse(state.isTranslating)
        assertNotNull(state.error)
        assertTrue(state.error?.contains("Translation failed") == true)
    }
    
    @Test
    fun `invalid language pair shows error`() = runTest {
        val (vm, _, speech, _, _) = buildVm()
        
        // Set both languages to non-English (invalid for ML Kit)
        vm.setSourceLanguage(TranslateLanguage.FRENCH)
        vm.setTargetLanguage(TranslateLanguage.SPANISH)
        advanceUntilIdle()
        
        vm.startListening(TranslateLanguage.FRENCH)
        speech.emitResult("Bonjour")
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertNotNull(state.error)
        assertTrue(state.error?.contains("English") == true)
    }
    
    @Test
    fun `speakText triggers TTS with Started event`() = runTest {
        val (vm, _, _, tts, _) = buildVm()
        
        vm.speakText("Hello", TranslateLanguage.ENGLISH)
        tts.emitEvent(TtsEvent.Started)
        advanceUntilIdle()
        
        assertTrue(vm.uiState.first().isSpeaking)
    }
    
    @Test
    fun `speakText triggers TTS with Completed event`() = runTest {
        val (vm, _, _, tts, _) = buildVm()
        
        vm.speakText("Hello", TranslateLanguage.ENGLISH)
        tts.emitEvent(TtsEvent.Started)
        advanceUntilIdle()
        
        tts.emitEvent(TtsEvent.Completed)
        advanceUntilIdle()
        
        assertFalse(vm.uiState.first().isSpeaking)
    }
    
    @Test
    fun `speakText TTS Error updates error state`() = runTest {
        val (vm, _, _, tts, _) = buildVm()
        
        vm.speakText("Hello", TranslateLanguage.ENGLISH)
        tts.emitEvent(TtsEvent.Error("TTS failed"))
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertFalse(state.isSpeaking)
        assertNotNull(state.error)
        assertTrue(state.error?.contains("TTS") == true)
    }
    
    @Test
    fun `setSourceLanguage updates source language`() = runTest {
        val (vm, _, _, _, _) = buildVm()
        
        vm.setSourceLanguage(TranslateLanguage.FRENCH)
        advanceUntilIdle()
        
        assertEquals(TranslateLanguage.FRENCH, vm.uiState.first().sourceLanguage)
    }
    
    @Test
    fun `setTargetLanguage updates target language`() = runTest {
        val (vm, _, _, _, _) = buildVm()
        
        vm.setTargetLanguage(TranslateLanguage.GERMAN)
        advanceUntilIdle()
        
        assertEquals(TranslateLanguage.GERMAN, vm.uiState.first().targetLanguage)
    }
    
    @Test
    fun `swapLanguages swaps source and target`() = runTest {
        val (vm, _, _, _, _) = buildVm()
        
        vm.setSourceLanguage(TranslateLanguage.ENGLISH)
        vm.setTargetLanguage(TranslateLanguage.GERMAN)
        advanceUntilIdle()
        
        vm.swapLanguages()
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertEquals(TranslateLanguage.GERMAN, state.sourceLanguage)
        assertEquals(TranslateLanguage.ENGLISH, state.targetLanguage)
    }
    
    @Test
    fun `toggleAutoPlay toggles auto-play state`() = runTest {
        val (vm, _, _, _, _) = buildVm()
        
        assertTrue(vm.uiState.first().autoPlayTranslation)
        
        vm.toggleAutoPlay()
        advanceUntilIdle()
        assertFalse(vm.uiState.first().autoPlayTranslation)
        
        vm.toggleAutoPlay()
        advanceUntilIdle()
        assertTrue(vm.uiState.first().autoPlayTranslation)
    }
    
    @Test
    fun `toggleSavedHistory toggles history visibility`() = runTest {
        val (vm, _, _, _, _) = buildVm()
        
        assertFalse(vm.uiState.first().showSavedHistory)
        
        vm.toggleSavedHistory()
        advanceUntilIdle()
        assertTrue(vm.uiState.first().showSavedHistory)
        
        vm.toggleSavedHistory()
        advanceUntilIdle()
        assertFalse(vm.uiState.first().showSavedHistory)
    }
    
    @Test
    fun `refreshConversationHistory shows history`() = runTest {
        val (vm, _, _, _, _) = buildVm()
        
        vm.refreshConversationHistory()
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertTrue(state.showSavedHistory)
        assertFalse(state.isRefreshing)
    }
    
    @Test
    fun `hideSavedHistory hides history`() = runTest {
        val (vm, _, _, _, _) = buildVm()
        
        vm.toggleSavedHistory()
        advanceUntilIdle()
        assertTrue(vm.uiState.first().showSavedHistory)
        
        vm.hideSavedHistory()
        advanceUntilIdle()
        assertFalse(vm.uiState.first().showSavedHistory)
    }
    
    @Test
    fun `deleteSavedConversation removes conversation from repository`() = runTest {
        val (vm, _, _, _, repo) = buildVm()
        
        val turn = ConversationTurn("Hello", "Hola", "en", "es", timestamp = 12345L)
        repo.saved.add(turn)
        
        vm.deleteSavedConversation(12345L)
        advanceUntilIdle()
        
        assertTrue(repo.deleted.contains(12345L))
    }
    
    @Test
    fun `clearConversation clears history and repository`() = runTest {
        val (vm, translation, speech, _, repo) = buildVm()
        translation.translatedPrefix = "Hola"
        
        vm.startListening(TranslateLanguage.ENGLISH)
        speech.emitResult("Hello")
        advanceUntilIdle()
        
        assertTrue(vm.uiState.first().conversationHistory.isNotEmpty())
        
        vm.clearConversation()
        advanceUntilIdle()
        
        assertTrue(vm.uiState.first().conversationHistory.isEmpty())
        assertTrue(repo.cleared)
    }
    
    @Test
    fun `clearError clears error message`() = runTest {
        val (vm, translation, speech, _, _) = buildVm()
        translation.shouldSucceed = false
        
        vm.startListening(TranslateLanguage.ENGLISH)
        speech.emitResult("Hello")
        advanceUntilIdle()
        
        assertNotNull(vm.uiState.first().error)
        
        vm.clearError()
        advanceUntilIdle()
        
        assertNull(vm.uiState.first().error)
    }
    
    @Test
    fun `autoPlayTranslation triggers TTS on successful translation`() = runTest {
        val (vm, translation, speech, tts, _) = buildVm()
        translation.translatedPrefix = "Hola"
        
        // Ensure auto-play is enabled
        assertTrue(vm.uiState.first().autoPlayTranslation)
        
        vm.startListening(TranslateLanguage.ENGLISH)
        speech.emitResult("Hello")
        advanceUntilIdle()
        
        // TTS should have been called
        tts.emitEvent(TtsEvent.Started)
        advanceUntilIdle()
        
        assertTrue(vm.uiState.first().isSpeaking)
    }
    
    @Test
    fun `ConversationUiState validates language pairs correctly`() {
        val validPair1 = ConversationUiState(
            sourceLanguage = TranslateLanguage.ENGLISH,
            targetLanguage = TranslateLanguage.SPANISH
        )
        assertTrue(validPair1.isValidLanguagePair)
        
        val validPair2 = ConversationUiState(
            sourceLanguage = TranslateLanguage.FRENCH,
            targetLanguage = TranslateLanguage.ENGLISH
        )
        assertTrue(validPair2.isValidLanguagePair)
        
        val invalidPair = ConversationUiState(
            sourceLanguage = TranslateLanguage.FRENCH,
            targetLanguage = TranslateLanguage.SPANISH
        )
        assertFalse(invalidPair.isValidLanguagePair)
    }
    
    @Test
    fun `repository flow updates savedHistory in state`() = runTest {
        val (vm, _, _, _, repo) = buildVm()
        
        val turn = ConversationTurn("Hello", "Hola", "en", "es")
        repo.emitConversation(turn)
        advanceUntilIdle()
        
        val state = vm.uiState.first()
        assertTrue(state.savedHistory.contains(turn))
    }
}
