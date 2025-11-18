package com.example.globaltranslation.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.globaltranslation.core.provider.SpeechProvider
import com.example.globaltranslation.core.provider.SpeechResult
import com.example.globaltranslation.core.provider.TextToSpeechProvider
import com.example.globaltranslation.core.provider.TranslationProvider
import com.example.globaltranslation.core.provider.TtsEvent
import com.example.globaltranslation.core.repository.ConversationRepository
import com.example.globaltranslation.model.ConversationTurn
import com.google.mlkit.nl.translate.TranslateLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the conversation screen, managing live translation conversation state.
 * Migrated to use :data providers for clean architecture.
 * Includes optional conversation persistence via ConversationRepository.
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val translationProvider: TranslationProvider,
    private val speechProvider: SpeechProvider,
    private val ttsProvider: TextToSpeechProvider,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    // Track the history collection job for proper cleanup
    private var historyCollectionJob: Job? = null

    init {
        // Load conversation history from repository on startup
        loadConversationHistory()
    }

    companion object {
        // Configurable delay for auto-hiding history after refresh (5 seconds)
        private const val AUTO_HIDE_HISTORY_DELAY_MS = 5000L
    }

    /**
     * Loads conversation history from the repository.
     * Properly managed with Job tracking to prevent memory leaks.
     */
    private fun loadConversationHistory() {
        // Cancel any existing collection
        historyCollectionJob?.cancel()

        historyCollectionJob = viewModelScope.launch {
            conversationRepository.getConversations().collect { savedConversations ->
                _uiState.value = _uiState.value.copy(
                    savedHistory = savedConversations
                )
            }
        }
    }

    /**
     * Starts listening for speech input in the specified language.
     */
    fun startListening(forLanguage: String) {
        if (_uiState.value.isListening) return

        _uiState.value = _uiState.value.copy(
            isListening = true,
            error = null
        )

        viewModelScope.launch {
            val languageCode = getLanguageCodeForSpeech(forLanguage)

            speechProvider.startListening(languageCode).collect { result ->
                when (result) {
                    is SpeechResult.ReadyForSpeech -> {
                        _uiState.value = _uiState.value.copy(
                            isListeningReady = true,
                            isDetectingSpeech = true
                        )
                    }

                    is SpeechResult.PartialResult -> {
                        _uiState.value = _uiState.value.copy(partialSpeechText = result.text)
                    }

                    is SpeechResult.FinalResult -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            isListeningReady = false,
                            isDetectingSpeech = false,
                            partialSpeechText = ""
                        )
                        translateAndAddToConversation(result.text, forLanguage)
                    }

                    is SpeechResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            isListeningReady = false,
                            isDetectingSpeech = false,
                            partialSpeechText = "",
                            error = result.message
                        )
                    }

                    is SpeechResult.EndOfSpeech -> {
                        _uiState.value = _uiState.value.copy(isDetectingSpeech = false)
                    }
                }
            }
        }
    }

    /**
     * Stops the current speech recognition session.
     */
    fun stopListening() {
        speechProvider.stopListening()
        _uiState.value = _uiState.value.copy(
            isListening = false,
            isListeningReady = false,
            isDetectingSpeech = false,
            partialSpeechText = ""
        )
    }

    /**
     * Translates text and adds it to the conversation history.
     */
    private fun translateAndAddToConversation(text: String, sourceLanguage: String) {
        val targetLanguage = getTargetLanguage(sourceLanguage)

        // Validate language pair (ML Kit requires English as source or target)
        if (!validLanguagePair(sourceLanguage, targetLanguage)) {
            _uiState.value = _uiState.value.copy(
                error = "ML Kit requires English as source or target language. Please select English for one side."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isTranslating = true)

        viewModelScope.launch {
            try {
                val result = translationProvider.translate(text, sourceLanguage, targetLanguage)

                result.fold(
                    onSuccess = { translatedText ->
                        val turn = ConversationTurn(
                            originalText = text,
                            translatedText = translatedText,
                            sourceLang = sourceLanguage,
                            targetLang = targetLanguage
                        )

                        val updatedConversation = _uiState.value.conversationHistory + turn
                        _uiState.value = _uiState.value.copy(
                            conversationHistory = updatedConversation,
                            isTranslating = false
                        )

                        // Persist the conversation turn (optional feature)
                        try {
                            conversationRepository.saveConversation(turn)
                        } catch (e: Exception) {
                            // Persistence failure doesn't break the UI
                            // Could log this for debugging
                        }

                        // Optionally speak the translation
                        if (_uiState.value.autoPlayTranslation) {
                            speakText(translatedText, targetLanguage)
                        }
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isTranslating = false,
                            error = "Translation failed: ${exception.message}. Ensure models are downloaded and you're on WiFi for first-time downloads."
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTranslating = false,
                    error = "Translation error: ${e.message}"
                )
            }
        }
    }

    /**
     * Speaks the given text using TTS.
     */
    fun speakText(text: String, languageCode: String) {
        viewModelScope.launch {
            ttsProvider.speak(text, languageCode).collect { event ->
                when (event) {
                    is TtsEvent.Started -> {
                        _uiState.value = _uiState.value.copy(isSpeaking = true)
                    }

                    is TtsEvent.Completed -> {
                        _uiState.value = _uiState.value.copy(isSpeaking = false)
                    }

                    is TtsEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSpeaking = false,
                            error = "TTS error: ${event.message}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Changes the source language for speech recognition.
     */
    fun setSourceLanguage(languageCode: String) {
        _uiState.value = _uiState.value.copy(sourceLanguage = languageCode)
    }

    /**
     * Changes the target language for translation.
     */
    fun setTargetLanguage(languageCode: String) {
        _uiState.value = _uiState.value.copy(targetLanguage = languageCode)
    }

    /**
     * Swaps source and target languages.
     */
    fun swapLanguages() {
        val currentSource = _uiState.value.sourceLanguage
        val currentTarget = _uiState.value.targetLanguage
        _uiState.value = _uiState.value.copy(
            sourceLanguage = currentTarget,
            targetLanguage = currentSource
        )
    }

    /**
     * Toggles auto-play of translations.
     */
    fun toggleAutoPlay() {
        _uiState.value = _uiState.value.copy(
            autoPlayTranslation = !_uiState.value.autoPlayTranslation
        )
    }

    /**
     * Toggles the saved history drawer visibility.
     */
    fun toggleSavedHistory() {
        _uiState.value = _uiState.value.copy(
            showSavedHistory = !_uiState.value.showSavedHistory
        )
    }

    /**
     * Refreshes the conversation history when user pulls to refresh.
     */
    fun refreshConversationHistory() {
        _uiState.value = _uiState.value.copy(
            isRefreshing = true,
            showSavedHistory = true // Show history when refreshing
        )

        viewModelScope.launch {
            try {
                // Reload conversation history from repository
                // The repository flow will automatically update the UI state
                // No artificial delay needed - let the actual data fetch determine timing
                _uiState.value = _uiState.value.copy(isRefreshing = false)

                // Auto-hide history after refresh completes
                kotlinx.coroutines.delay(AUTO_HIDE_HISTORY_DELAY_MS)
                if (!_uiState.value.isRefreshing) {
                    _uiState.value = _uiState.value.copy(showSavedHistory = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Failed to refresh history: ${e.message}"
                )
            }
        }
    }

    /**
     * Manually hides the saved history.
     */
    fun hideSavedHistory() {
        _uiState.value = _uiState.value.copy(showSavedHistory = false)
    }

    /**
     * Deletes a saved conversation turn.
     */
    fun deleteSavedConversation(timestamp: Long) {
        viewModelScope.launch {
            try {
                conversationRepository.deleteConversation(timestamp)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete conversation: ${e.message}"
                )
            }
        }
    }

    /**
     * Clears the conversation history.
     */
    fun clearConversation() {
        _uiState.value = _uiState.value.copy(conversationHistory = emptyList())

        // Also clear persisted conversations
        viewModelScope.launch {
            try {
                conversationRepository.clearAll()
            } catch (e: Exception) {
                // Persistence failure doesn't break the UI
            }
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun getTargetLanguage(sourceLanguage: String): String {
        return if (sourceLanguage == _uiState.value.sourceLanguage) {
            _uiState.value.targetLanguage
        } else {
            _uiState.value.sourceLanguage
        }
    }

    /**
     * Converts ML Kit language code to language tag for speech recognition.
     * Speech recognition expects locale tags like "en-US", "es-ES", etc.
     */
    private fun getLanguageCodeForSpeech(languageCode: String): String {
        return when (languageCode) {
            TranslateLanguage.ENGLISH -> "en-US"
            TranslateLanguage.SPANISH -> "es-ES"
            TranslateLanguage.FRENCH -> "fr-FR"
            TranslateLanguage.GERMAN -> "de-DE"
            TranslateLanguage.ITALIAN -> "it-IT"
            TranslateLanguage.PORTUGUESE -> "pt-PT"
            TranslateLanguage.CHINESE -> "zh-CN"
            TranslateLanguage.JAPANESE -> "ja-JP"
            TranslateLanguage.KOREAN -> "ko-KR"
            TranslateLanguage.RUSSIAN -> "ru-RU"
            TranslateLanguage.ARABIC -> "ar-SA"
            TranslateLanguage.HINDI -> "hi-IN"
            else -> "en-US"
        }
    }

    /**
     * Validates that at least one language is English (required for ML Kit).
     * ML Kit only supports translation pairs with English.
     */
    private fun validLanguagePair(sourceLanguage: String, targetLanguage: String): Boolean {
        return sourceLanguage == TranslateLanguage.ENGLISH || targetLanguage == TranslateLanguage.ENGLISH
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel the history collection job
        historyCollectionJob?.cancel()
        historyCollectionJob = null

        // Cleanup providers
        speechProvider.cleanup()
        ttsProvider.cleanup()
    }
}

/**
 * UI state for the conversation screen.
 */
data class ConversationUiState(
    val conversationHistory: List<ConversationTurn> = emptyList(),
    val savedHistory: List<ConversationTurn> = emptyList(),
    val showSavedHistory: Boolean = false,
    val isRefreshing: Boolean = false,
    val sourceLanguage: String = TranslateLanguage.ENGLISH,
    val targetLanguage: String = TranslateLanguage.SPANISH,
    val isListening: Boolean = false,
    val isListeningReady: Boolean = false,
    val isDetectingSpeech: Boolean = false,
    val partialSpeechText: String = "",
    val isTranslating: Boolean = false,
    val isSpeaking: Boolean = false,
    val autoPlayTranslation: Boolean = true,
    val error: String? = null
) {
    /**
     * Validates that at least one language is English (required for ML Kit).
     */
    val isValidLanguagePair: Boolean
        get() = sourceLanguage == TranslateLanguage.ENGLISH || targetLanguage == TranslateLanguage.ENGLISH
}
