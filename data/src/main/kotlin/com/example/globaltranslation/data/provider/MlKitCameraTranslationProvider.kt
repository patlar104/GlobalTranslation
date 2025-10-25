package com.example.globaltranslation.data.provider

import com.example.globaltranslation.core.provider.BoundingBox
import com.example.globaltranslation.core.provider.CameraTranslationProvider
import com.example.globaltranslation.core.provider.TranslatedTextBlock
import com.example.globaltranslation.core.util.TextBlockGroupingUtil
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ML Kit implementation of CameraTranslationProvider.
 * Combines OCR and translation for camera-based text translation.
 * 
 * Battery optimizations:
 * - Efficient parallel translation with proper coroutine context
 * - Early exit on empty results to avoid unnecessary processing
 * - Optimized error handling to prevent resource leaks
 */
@Singleton
class MlKitCameraTranslationProvider @Inject constructor(
    private val textRecognitionProvider: MlKitTextRecognitionProvider,
    private val translationProvider: MlKitTranslationProvider
) : CameraTranslationProvider {
    
    override suspend fun processImage(
        imageData: Any,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<List<TranslatedTextBlock>> {
        if (imageData !is InputImage) {
            return Result.failure(IllegalArgumentException("imageData must be InputImage"))
        }
        
        return try {
            // Step 1: Recognize text with appropriate script recognizer
            val detectedText = textRecognitionProvider.recognizeText(imageData, sourceLanguage)
                .getOrThrow()
            
            if (detectedText.textBlocks.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Step 2: Filter and group text blocks
            val processedBlocks = TextBlockGroupingUtil.filterAndGroup(detectedText.textBlocks)
            
            if (processedBlocks.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Step 3: Translate in parallel
            val translatedBlocks = coroutineScope {
                processedBlocks.map { textBlock ->
                    async {
                        val translationResult = translationProvider.translate(
                            text = textBlock.text,
                            from = sourceLanguage,
                            to = targetLanguage
                        )
                        
                        TranslatedTextBlock(
                            originalText = textBlock.text,
                            translatedText = translationResult.getOrNull() ?: textBlock.text,
                            boundingBox = textBlock.boundingBox,
                            confidence = 1.0f
                        )
                    }
                }.awaitAll()
            }
            
            Result.success(translatedBlocks)
        } catch (e: Exception) {
            Result.failure(Exception("Camera translation error: ${e.message}"))
        }
    }
    
    override suspend fun areModelsAvailable(
        sourceLanguage: String,
        targetLanguage: String
    ): Boolean {
        return translationProvider.areModelsDownloaded(sourceLanguage, targetLanguage)
    }
}

