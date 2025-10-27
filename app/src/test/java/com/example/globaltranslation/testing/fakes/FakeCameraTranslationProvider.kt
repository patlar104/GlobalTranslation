package com.example.globaltranslation.testing.fakes

import com.example.globaltranslation.core.provider.BoundingBox
import com.example.globaltranslation.core.provider.CameraTranslationProvider
import com.example.globaltranslation.core.provider.TranslatedTextBlock

class FakeCameraTranslationProvider : CameraTranslationProvider {
    var shouldSucceed = true
    var shouldDetectText = true
    var errorMessage = "Translation failed"
    var translatedPrefix = "Translated"
    var modelsAvailable = true
    
    // Track what was processed
    var lastProcessedImage: Any? = null
    var lastSourceLanguage: String? = null
    var lastTargetLanguage: String? = null
    
    override suspend fun processImage(
        imageData: Any,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<List<TranslatedTextBlock>> {
        lastProcessedImage = imageData
        lastSourceLanguage = sourceLanguage
        lastTargetLanguage = targetLanguage
        
        return if (!shouldSucceed) {
            Result.failure(Exception(errorMessage))
        } else if (!shouldDetectText) {
            Result.success(emptyList())
        } else {
            Result.success(
                listOf(
                    TranslatedTextBlock(
                        originalText = "Test text",
                        translatedText = "$translatedPrefix: Test text",
                        boundingBox = BoundingBox(0, 0, 100, 100),
                        confidence = 0.95f
                    )
                )
            )
        }
    }
    
    override suspend fun areModelsAvailable(
        sourceLanguage: String,
        targetLanguage: String
    ): Boolean {
        return modelsAvailable
    }
}
