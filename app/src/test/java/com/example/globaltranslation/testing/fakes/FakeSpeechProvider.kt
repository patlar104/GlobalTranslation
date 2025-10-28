package com.example.globaltranslation.testing.fakes

import com.example.globaltranslation.core.provider.SpeechProvider
import com.example.globaltranslation.core.provider.SpeechResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeSpeechProvider : SpeechProvider {
    private val _events = MutableSharedFlow<SpeechResult>(replay = 1, extraBufferCapacity = 10)
    
    var isListening = false
        private set
    
    fun emitEvent(event: SpeechResult) {
        _events.tryEmit(event)
    }
    
    fun emitResult(text: String) {
        _events.tryEmit(SpeechResult.FinalResult(text))
    }
    
    fun emitError(message: String) {
        _events.tryEmit(SpeechResult.Error(message))
    }
    
    override fun startListening(languageCode: String): Flow<SpeechResult> {
        isListening = true
        _events.tryEmit(SpeechResult.ReadyForSpeech)
        return _events
    }
    
    override fun stopListening() {
        isListening = false
        _events.tryEmit(SpeechResult.EndOfSpeech)
    }
    
    override fun cleanup() {
        isListening = false
    }
}