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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ML Kit implementation of TextRecognitionProvider.
 * Uses ML Kit Text Recognition v2 for OCR with multi-script support.
 * Automatically selects the appropriate recognizer based on the language code.
 * 
 * Battery optimizations:
 * - Efficient caching of recognizers to avoid recreation
 * - Lazy initialization of recognizers (only when needed)
 * - Proper cleanup on provider disposal
 */
@Singleton
class MlKitTextRecognitionProvider @Inject constructor() : TextRecognitionProvider {
    
    // Cache recognizers per language code to avoid recreation
    private val recognizers = ConcurrentHashMap<String, TextRecognizer>()
    
    override suspend fun recognizeText(imageData: Any, languageCode: String?): Result<DetectedText> {
        if (imageData !is InputImage) {
            return Result.failure(IllegalArgumentException("imageData must be InputImage"))
        }
        
        return try {
            // Get appropriate recognizer for the language
            val recognizer = getRecognizerForLanguage(languageCode)
            val result = recognizer.process(imageData).await()
            
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
            Result.failure(e)
        }
    }
    
    /**
     * Gets or creates the appropriate text recognizer for the given language code.
     * Caches recognizers to avoid recreation.
     */
    private fun getRecognizerForLanguage(languageCode: String?): TextRecognizer {
        val key = languageCode ?: "latin"
        
        return recognizers.getOrPut(key) {
            when (languageCode) {
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
    
    override fun cleanup() {
        recognizers.values.forEach { it.close() }
        recognizers.clear()
    }
}

