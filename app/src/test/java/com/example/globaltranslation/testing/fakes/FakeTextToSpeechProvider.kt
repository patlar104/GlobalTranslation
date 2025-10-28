package com.example.globaltranslation.testing.fakes

import com.example.globaltranslation.core.provider.TextToSpeechProvider
import com.example.globaltranslation.core.provider.TtsEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeTextToSpeechProvider : TextToSpeechProvider {
    val speaks = mutableListOf<Pair<String, String>>()
    private val events = MutableSharedFlow<TtsEvent>(replay = 1, extraBufferCapacity = 10)

    fun emitEvent(event: TtsEvent) {
        events.tryEmit(event)
    }

    override fun speak(text: String, languageCode: String): Flow<TtsEvent> {
        speaks += text to languageCode
        return events
    }

    override fun stop() {}

    override fun cleanup() {}
}
