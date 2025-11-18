package com.example.globaltranslation.ui.textinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.globaltranslation.core.provider.TextToSpeechProvider
import com.example.globaltranslation.core.provider.TranslationProvider
import com.example.globaltranslation.core.repository.ConversationRepository
import com.example.globaltranslation.model.ConversationTurn
import com.google.mlkit.nl.translate.TranslateLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the text input screen, managing manual text translation.
 * Migrated to use :data providers for clean architecture.
 */
@HiltViewModel
class TextInputViewModel @Inject constructor(
    private val translationProvider: TranslationProvider,
    private val ttsProvider: TextToSpeechProvider,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextInputUiState())
    val uiState: StateFlow<TextInputUiState> = _uiState.asStateFlow()

    init {
        // Load translation history from repository on startup
        loadTranslationHistory()
    }

    /**
     * Loads translation history from the repository.
     */
    private fun loadTranslationHistory() {
        viewModelScope.launch {
            conversationRepository.getConversations().collect { savedConversations ->
                // Use ConversationTurn directly
                _uiState.value = _uiState.value.copy(translationHistory = savedConversations)
            }
        }
    }

    /**
     * Updates the input text as user types.
     */
    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    /**
     * Translates the current input text.
     */
    fun translateText() {
        val currentState = _uiState.value
        val textToTranslate = currentState.inputText.trim()

        if (textToTranslate.isEmpty()) {
            _uiState.value = currentState.copy(error = "Please enter text to translate")
            return
        }

        // Validate language pair (ML Kit requires English as source or target)
        if (!validLanguagePair(currentState.sourceLanguage, currentState.targetLanguage)) {
            _uiState.value = currentState.copy(
                error = "ML Kit requires English as source or target language. Please select English for one side."
            )
            return
        }

        _uiState.value = currentState.copy(
            isTranslating = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val result = translationProvider.translate(
                    text = textToTranslate,
                    from = currentState.sourceLanguage,
                    to = currentState.targetLanguage
                )

                result.fold(
                    onSuccess = { translatedText ->
                        val translation = ConversationTurn(
                            originalText = textToTranslate,
                            translatedText = translatedText,
                            sourceLang = currentState.sourceLanguage,
                            targetLang = currentState.targetLanguage,
                            timestamp = System.currentTimeMillis()
                        )

                        val updatedHistory = listOf(translation) + currentState.translationHistory

                        _uiState.value = currentState.copy(
                            currentTranslation = translation,
                            translationHistory = updatedHistory,
                            isTranslating = false
                        )

                        // Persist the translation to repository
                        viewModelScope.launch {
                            try {
                                conversationRepository.saveConversation(translation)
                            } catch (e: Exception) {
                                // Persistence failure doesn't break the UI
                            }
                        }
                    },
                    onFailure = { exception ->
                        _uiState.value = currentState.copy(
                            isTranslating = false,
                            error = "Translation failed: ${exception.message}. Ensure models are downloaded and you're on WiFi for first-time downloads."
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isTranslating = false,
                    error = "Translation error: ${e.message}"
                )
            }
        }
    }

    /**
     * Sets the source language for translation.
     */
    fun setSourceLanguage(languageCode: String) {
        _uiState.value = _uiState.value.copy(sourceLanguage = languageCode)
    }

    /**
     * Sets the target language for translation.
     */
    fun setTargetLanguage(languageCode: String) {
        _uiState.value = _uiState.value.copy(targetLanguage = languageCode)
    }

    /**
     * Swaps source and target languages.
     */
    fun swapLanguages() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            sourceLanguage = currentState.targetLanguage,
            targetLanguage = currentState.sourceLanguage,
            inputText = currentState.currentTranslation?.translatedText ?: currentState.inputText
        )
    }

    /**
     * Clears the current input text.
     */
    fun clearInput() {
        _uiState.value = _uiState.value.copy(
            inputText = "",
            currentTranslation = null
        )
    }

    /**
     * Clears the translation history.
     */
    fun clearHistory() {
        _uiState.value = _uiState.value.copy(translationHistory = emptyList())
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Copies text from a previous translation to input.
     */
    fun copyToInput(translation: ConversationTurn) {
        _uiState.value = _uiState.value.copy(
            inputText = translation.originalText,
            sourceLanguage = translation.sourceLang,
            targetLanguage = translation.targetLang
        )
    }

    /**
     * Copies text to input (overloaded version for text-only).
     */
    fun copyToInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    /**
     * Speaks the given text using text-to-speech.
     */
    fun speakText(text: String, languageCode: String) {
        viewModelScope.launch {
            ttsProvider.speak(text, languageCode).collect { event ->
                // Handle TTS events if needed (e.g., show speaking indicator)
            }
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
        ttsProvider.cleanup()
    }
}

/**
 * UI state for the text input screen.
 */
data class TextInputUiState(
    val inputText: String = "",
    val sourceLanguage: String = TranslateLanguage.ENGLISH,
    val targetLanguage: String = TranslateLanguage.SPANISH,
    val currentTranslation: ConversationTurn? = null,
    val translationHistory: List<ConversationTurn> = emptyList(),
    val isTranslating: Boolean = false,
    val error: String? = null
) {
    /**
     * Validates that at least one language is English (required for ML Kit).
     */
    val isValidLanguagePair: Boolean
        get() = sourceLanguage == TranslateLanguage.ENGLISH || targetLanguage == TranslateLanguage.ENGLISH
}
