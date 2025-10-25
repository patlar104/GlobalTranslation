package com.example.globaltranslation.data.provider

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.globaltranslation.core.provider.TextToSpeechProvider
import com.example.globaltranslation.core.provider.TtsEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of TextToSpeechProvider using Android TTS.
 * 
 * Battery optimizations:
 * - Lazy TTS initialization (only when first needed)
 * - Proper lifecycle management to prevent resource leaks
 * - Efficient reuse of TTS instance across multiple calls
 */
@Singleton
class AndroidTextToSpeechProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeechProvider {
    
    @Volatile
    private var tts: TextToSpeech? = null
    
    @Volatile
    private var isInitialized = false
    
    override fun speak(text: String, languageCode: String): Flow<TtsEvent> = callbackFlow {
        fun speakInternal(speakText: String, langCode: String) {
            val locale = Locale.forLanguageTag(langCode)
            tts?.language = locale
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    trySend(TtsEvent.Started)
                }
                
                override fun onDone(utteranceId: String?) {
                    trySend(TtsEvent.Completed)
                    close()
                }
                
                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun onError(utteranceId: String?) {
                    trySend(TtsEvent.Error("TTS error"))
                    close()
                }
                
                override fun onError(utteranceId: String?, errorCode: Int) {
                    trySend(TtsEvent.Error("TTS error: $errorCode"))
                    close()
                }
            })
            
            tts?.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "utterance_id")
        }
        
        // Initialize TTS if needed
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    speakInternal(text, languageCode)
                } else {
                    trySend(TtsEvent.Error("TTS initialization failed"))
                    close()
                }
            }
        } else {
            speakInternal(text, languageCode)
        }
        
        awaitClose {
            // Flow closed, but keep TTS alive for reuse
        }
    }
    
    override fun stop() {
        tts?.stop()
    }
    
    override fun cleanup() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

