package com.example.globaltranslation.data.provider

import com.example.globaltranslation.core.provider.BoundingBox
import com.example.globaltranslation.core.provider.DetectedText
import com.example.globaltranslation.core.provider.TextBlock
import com.example.globaltranslation.core.provider.TextLine
import com.example.globaltranslation.core.provider.TextRecognitionProvider
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ML Kit implementation of TextRecognitionProvider.
 * 
 * This provider performs optical character recognition (OCR) on images using
 * Google's ML Kit Text Recognition v2 API with multi-script support.
 * Extends CachedResourceProvider to eliminate duplication in resource management.
 * 
 * ## Architecture:
 * - **Singleton**: Shared instance across the app
 * - **Multi-Script**: Supports Latin, Chinese, Japanese, Korean, Devanagari
 * - **Cached**: One recognizer per script type (max 5) via base class
 * - **Lazy**: Recognizers created only when needed
 * 
 * ## Supported Scripts:
 * | Script      | Languages                              | Model Size |
 * |-------------|----------------------------------------|------------|
 * | Latin       | English, Spanish, French, etc.        | ~20MB      |
 * | Chinese     | Simplified & Traditional Chinese       | ~25MB      |
 * | Japanese    | Hiragana, Katakana, Kanji             | ~25MB      |
 * | Korean      | Hangul                                 | ~25MB      |
 * | Devanagari  | Hindi, Marathi, Nepali, Sanskrit      | ~25MB      |
 * 
 * ## Performance:
 * - First recognition: 1-3 seconds (model loading)
 * - Cached recognition: 200-800ms per image
 * - Memory per recognizer: ~30MB
 * 
 * ## Example Usage:
 * ```kotlin
 * @Inject lateinit var textRecognition: MlKitTextRecognitionProvider
 * 
 * // Recognize text in English/Latin script
 * val result = textRecognition.recognizeText(inputImage, "en")
 * result.onSuccess { detectedText ->
 *     detectedText.textBlocks.forEach { block ->
 *         println("Text: ${block.text}")
 *         println("Bounding box: ${block.boundingBox}")
 *     }
 * }
 * 
 * // Recognize text in Chinese
 * val chineseResult = textRecognition.recognizeText(inputImage, "zh")
 * ```
 * 
 * @see com.example.globaltranslation.core.provider.TextRecognitionProvider
 * @see CachedResourceProvider for resource management implementation
 * @see MlKitConfig for configuration constants
 */
@Singleton
class MlKitTextRecognitionProvider @Inject constructor() 
    : CachedResourceProvider<String, TextRecognizer>(), TextRecognitionProvider {
    
    override suspend fun recognizeText(imageData: Any, languageCode: String?): Result<DetectedText> {
        // Use utility for validation
        ProviderUtils.validateImageData<DetectedText>(imageData)?.let { return it }
        
        return try {
            // Get appropriate recognizer for the language (uses base class caching)
            val key = languageCode ?: "latin"
            val recognizer = getOrCreateResource(key)
            val result = recognizer.process(imageData as InputImage).await()
            
            val textBlocks = result.textBlocks.map { block ->
                TextBlock(
                    text = block.text,
                    boundingBox = block.boundingBox?.let {
                        BoundingBox(it.left, it.top, it.right, it.bottom)
                    } ?: BoundingBox(0, 0, 0, 0),
                    lines = block.lines.map { line ->
                        TextLine(
                            text = line.text,
                            boundingBox = line.boundingBox?.let {
                                BoundingBox(it.left, it.top, it.right, it.bottom)
                            } ?: BoundingBox(0, 0, 0, 0)
                        )
                    }
                )
            }
            
            Result.success(
                DetectedText(
                    text = result.text,
                    textBlocks = textBlocks
                )
            )
        } catch (e: Exception) {
            Result.failure(
                ProviderUtils.wrapException(
                    ProviderUtils.formatProviderError("TextRecognition", "recognizeText", e),
                    e
                )
            )
        }
    }
    
    /**
     * Creates the appropriate text recognizer for the given language code.
     * Called by base class when resource is not in cache.
     * 
     * @param key Language code or "latin" for default
     * @return TextRecognizer configured for the specified script
     */
    override fun createResource(key: String): TextRecognizer {
        return when (key) {
            TranslateLanguage.CHINESE -> {
                // Chinese text recognition
                TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            }
            TranslateLanguage.JAPANESE -> {
                // Japanese text recognition
                TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            }
            TranslateLanguage.KOREAN -> {
                // Korean text recognition
                TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            }
            TranslateLanguage.HINDI, TranslateLanguage.BENGALI -> {
                // Devanagari script recognition (Hindi, Bengali, etc.)
                TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            }
            else -> {
                // Default to Latin script for English and other Latin-based languages
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            }
        }
    }
}

