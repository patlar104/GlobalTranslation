package com.example.globaltranslation.ui.camera

import android.graphics.Rect as AndroidRect
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.globaltranslation.core.provider.CameraTranslationProvider
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for camera translation screen.
 * Manages camera state, text recognition, and translation.
 * Migrated to use :data providers for clean architecture.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraTranslationProvider: CameraTranslationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // No more continuous processing - only on-demand capture

    /**
     * Sets the source language for translation.
     * Ensures at least one language is English (ML Kit requirement).
     */
    fun setSourceLanguage(languageCode: String) {
        val currentTarget = _uiState.value.targetLanguageCode

        // If setting source to non-English and target is also non-English, reset target to English
        if (languageCode != TranslateLanguage.ENGLISH && currentTarget != TranslateLanguage.ENGLISH) {
            _uiState.value = _uiState.value.copy(
                sourceLanguageCode = languageCode,
                targetLanguageCode = TranslateLanguage.ENGLISH
            )
        } else {
            _uiState.value = _uiState.value.copy(sourceLanguageCode = languageCode)
        }
    }

    /**
     * Sets the target language for translation.
     * Ensures at least one language is English (ML Kit requirement).
     */
    fun setTargetLanguage(languageCode: String) {
        val currentSource = _uiState.value.sourceLanguageCode

        // If setting target to non-English and source is also non-English, reset source to English
        if (languageCode != TranslateLanguage.ENGLISH && currentSource != TranslateLanguage.ENGLISH) {
            _uiState.value = _uiState.value.copy(
                sourceLanguageCode = TranslateLanguage.ENGLISH,
                targetLanguageCode = languageCode
            )
        } else {
            _uiState.value = _uiState.value.copy(targetLanguageCode = languageCode)
        }
    }

    /**
     * Swaps source and target languages.
     */
    fun swapLanguages() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            sourceLanguageCode = currentState.targetLanguageCode,
            targetLanguageCode = currentState.sourceLanguageCode
        )
    }

    /**
     * Toggles flash on/off.
     */
    fun toggleFlash() {
        _uiState.value = _uiState.value.copy(
            isFlashOn = !_uiState.value.isFlashOn
        )
    }

    /**
     * Processes a captured image for text recognition and translation.
     * This is called with an actual captured photo (Google Lens style).
     */
    fun processCapturedImage(inputImage: InputImage) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isProcessing = true,
                    isFrozen = true,
                    error = null
                )

                // Process the captured image using provider
                val result = cameraTranslationProvider.processImage(
                    imageData = inputImage,
                    sourceLanguage = _uiState.value.sourceLanguageCode,
                    targetLanguage = _uiState.value.targetLanguageCode
                )

                result.fold(
                    onSuccess = { translatedBlocks ->
                        val detectedBlocks = translatedBlocks.map { block ->
                            DetectedTextBlock(
                                originalText = block.originalText,
                                translatedText = block.translatedText,
                                boundingBox = Rect(
                                    block.boundingBox.left.toFloat(),
                                    block.boundingBox.top.toFloat(),
                                    block.boundingBox.right.toFloat(),
                                    block.boundingBox.bottom.toFloat()
                                )
                            )
                        }

                        if (detectedBlocks.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                detectedTextBlocks = detectedBlocks,
                                isProcessing = false,
                                error = null
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isProcessing = false,
                                error = "No text detected. Try again with clearer text."
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            error = "Translation failed: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clears detected text blocks (results) and resumes camera.
     */
    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            detectedTextBlocks = emptyList(),
            isFrozen = false,
            isProcessing = false,
            error = null
        )
    }

    /**
     * Resets the camera (unfreezes and clears translations).
     */
    fun reset() {
        _uiState.value = _uiState.value.copy(
            isFrozen = false,
            detectedTextBlocks = emptyList(),
            error = null
        )
    }
}

/**
 * UI state for camera translation screen.
 * Uses string literals for defaults to support Compose previews.
 */
data class CameraUiState(
    val sourceLanguageCode: String = "en",  // TranslateLanguage.ENGLISH
    val targetLanguageCode: String = "es",  // TranslateLanguage.SPANISH
    val detectedTextBlocks: List<DetectedTextBlock> = emptyList(),
    val isProcessing: Boolean = false,
    val isFrozen: Boolean = false,
    val isFlashOn: Boolean = false,
    val error: String? = null
) {
    /**
     * Validates that at least one language is English (required for ML Kit).
     * ML Kit only supports translation pairs with English.
     */
    fun isValidLanguagePair(): Boolean {
        return sourceLanguageCode == "en" ||  // TranslateLanguage.ENGLISH
                targetLanguageCode == "en"
    }
}

/**
 * Represents a detected and translated text block.
 */
data class DetectedTextBlock(
    val originalText: String,
    val translatedText: String?,
    val boundingBox: Rect
)

