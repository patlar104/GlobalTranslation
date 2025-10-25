package com.example.globaltranslation.data.provider

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.globaltranslation.core.provider.SpeechProvider
import com.example.globaltranslation.core.provider.SpeechResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of SpeechProvider using built-in SpeechRecognizer.
 * 
 * Battery optimizations:
 * - Automatic cleanup on flow cancellation
 * - Efficient error handling to prevent resource leaks
 * - Proper lifecycle management of SpeechRecognizer
 */
@Singleton
class AndroidSpeechProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechProvider {
    
    @Volatile
    private var speechRecognizer: SpeechRecognizer? = null
    
    override fun startListening(languageCode: String): Flow<SpeechResult> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SpeechResult.Error("Speech recognition not available"))
            close()
            return@callbackFlow
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        
        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechResult.ReadyForSpeech)
            }
            
            override fun onBeginningOfSpeech() {}
            
            override fun onRmsChanged(rmsdB: Float) {}
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                trySend(SpeechResult.EndOfSpeech)
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    else -> "Speech recognition error: $error"
                }
                trySend(SpeechResult.Error(errorMessage))
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    trySend(SpeechResult.FinalResult(text))
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    trySend(SpeechResult.PartialResult(text))
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        
        speechRecognizer?.setRecognitionListener(recognitionListener)
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        
        speechRecognizer?.startListening(intent)
        
        awaitClose {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }
    
    override fun stopListening() {
        speechRecognizer?.stopListening()
    }
    
    override fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

