# ML Kit Provider Architecture

## Overview

This document provides comprehensive documentation for the ML Kit provider implementations in the GlobalTranslation app. It serves as a guide for understanding the architecture, implementation details, and best practices.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                        │
│                      (ViewModels)                           │
└────────────────────┬────────────────────────────────────────┘
                     │ Depends on
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                     Core Module                              │
│                   (Interfaces)                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ TranslationProvider                                   │  │
│  │ TextRecognitionProvider                              │  │
│  │ CameraTranslationProvider                            │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────┘
                     │ Implemented by
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                     Data Module                              │
│              (ML Kit Implementations)                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ MlKitTranslationProvider                             │  │
│  │ MlKitTextRecognitionProvider                         │  │
│  │ MlKitCameraTranslationProvider                       │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ MlKitConfig (Configuration)                          │  │
│  │ MlKitErrorHandler (Error Handling)                   │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────┘
                     │ Uses
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   Google ML Kit SDK                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Translation API                                       │  │
│  │ Text Recognition API                                  │  │
│  │ Vision API                                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Provider Implementations

### 1. MlKitTranslationProvider

**Purpose**: Handles text translation using ML Kit's on-device translation models.

**Key Responsibilities**:
- Manage translator instances with efficient caching
- Download and manage translation models
- Handle model availability checks
- Coordinate translation operations

**Implementation Details**:

```kotlin
@Singleton
class MlKitTranslationProvider @Inject constructor(
    private val languageModelPreferences: LanguageModelPreferences
) : TranslationProvider
```

**Resource Management**:
- **Translator Caching**: Uses `ConcurrentHashMap<String, Translator>` to cache translators
- **Cache Key Format**: `"from-to"` (e.g., `"en-es"`)
- **Model State**: Tracks which models are ready in memory with `ConcurrentHashMap.newKeySet<String>()`
- **Lazy Initialization**: Model manager is created only when first needed

**Thread Safety**:
- All operations are thread-safe using `ConcurrentHashMap` and `Mutex`
- Download operations use double-checked locking to prevent duplicate downloads
- Cleanup method safely closes all translators

**Error Handling**:
- Returns `Result<T>` for all operations
- Validates input before processing
- Catches and wraps exceptions with meaningful messages

**Performance Optimizations**:
- Lazy model manager initialization
- Efficient translator caching (prevents recreation)
- Mutex synchronization for downloads (prevents duplicates)
- Early validation to avoid unnecessary processing

---

### 2. MlKitTextRecognitionProvider

**Purpose**: Performs optical character recognition (OCR) on images using ML Kit's text recognition.

**Key Responsibilities**:
- Recognize text in images across multiple scripts
- Select appropriate recognizer based on language
- Convert ML Kit results to domain models
- Cache recognizers for performance

**Supported Scripts**:
- **Latin** (Default): English and most European languages
- **Chinese**: Simplified and Traditional Chinese
- **Japanese**: Hiragana, Katakana, Kanji
- **Korean**: Hangul
- **Devanagari**: Hindi, Marathi, Nepali, Sanskrit

**Implementation Details**:

```kotlin
@Singleton
class MlKitTextRecognitionProvider @Inject constructor() : TextRecognitionProvider
```

**Script Selection Logic**:
```kotlin
when (languageCode) {
    TranslateLanguage.CHINESE -> ChineseTextRecognizerOptions
    TranslateLanguage.JAPANESE -> JapaneseTextRecognizerOptions
    TranslateLanguage.KOREAN -> KoreanTextRecognizerOptions
    TranslateLanguage.HINDI, BENGALI -> DevanagariTextRecognizerOptions
    else -> TextRecognizerOptions.DEFAULT_OPTIONS (Latin)
}
```

**Resource Management**:
- **Recognizer Caching**: One recognizer per script
- **Lazy Creation**: Recognizers created on first use
- **Cleanup**: All recognizers properly closed

**Performance Optimizations**:
- Recognizers cached to avoid recreation
- Lazy initialization per script
- Efficient script detection
- Minimal object allocations in mapping

---

### 3. MlKitCameraTranslationProvider

**Purpose**: Combines OCR and translation for real-time camera-based translation.

**Key Responsibilities**:
- Process camera frames for text detection
- Group detected text blocks intelligently
- Translate multiple text blocks in parallel
- Return translated text with bounding boxes

**Processing Pipeline**:

```
Camera Frame
    ↓
InputImage
    ↓
Text Recognition (OCR)
    ↓
Text Block Grouping & Filtering
    ↓
Parallel Translation (coroutines)
    ↓
TranslatedTextBlock Results
```

**Implementation Details**:

```kotlin
@Singleton
class MlKitCameraTranslationProvider @Inject constructor(
    private val textRecognitionProvider: MlKitTextRecognitionProvider,
    private val translationProvider: MlKitTranslationProvider
) : CameraTranslationProvider
```

**Parallel Processing**:
- Uses `coroutineScope` to ensure proper cancellation
- `async/awaitAll` for concurrent translation of text blocks
- Bounded parallelism (configuration can limit concurrent operations)

**Error Handling**:
- Early exit on empty results (performance optimization)
- Fallback to original text if translation fails
- Wrapped exceptions with context

**Performance Optimizations**:
- Early exit on empty text detection
- Parallel translation of text blocks
- Efficient text block grouping
- Proper coroutine context management

---

## Configuration: MlKitConfig

Centralized configuration object for all ML Kit providers.

**Key Parameters**:
- `DEFAULT_OPERATION_TIMEOUT_MS`: 30 seconds
- `MAX_RETRY_ATTEMPTS`: 3 attempts
- `MAX_CACHED_TRANSLATORS`: 10 translators
- `MAX_CACHED_RECOGNIZERS`: 5 recognizers
- `MAX_TEXT_LENGTH`: 5,000 characters
- `MAX_PARALLEL_TRANSLATIONS`: 20 concurrent operations

**Usage**:
```kotlin
val timeout = MlKitConfig.DEFAULT_OPERATION_TIMEOUT_MS
val maxRetries = MlKitConfig.MAX_RETRY_ATTEMPTS
```

---

## Error Handling: MlKitErrorHandler

Standardized error handling for ML Kit operations.

**Error Classification**:
- `NETWORK`: Network-related errors
- `MODEL_UNAVAILABLE`: Model not downloaded
- `INVALID_INPUT`: Input validation errors
- `RESOURCE_EXHAUSTED`: Memory/resource issues
- `UNKNOWN`: Unexpected errors

**Retry Logic**:
- Automatic retry for transient failures (network, model unavailable)
- Exponential backoff: `delay * (2 ^ attempt)`
- No retry for permanent failures (invalid input, resource exhausted)

**Usage**:
```kotlin
val result = MlKitErrorHandler.withMlKitRetry {
    performMlKitOperation()
}
```

**Input Validation**:
```kotlin
MlKitErrorHandler.validateTextInput(text, "Translation")
    .onFailure { return it }
```

---

## Best Practices

### 1. Resource Management

**DO**:
- Always call `cleanup()` when provider is no longer needed
- Use dependency injection for provider instances
- Rely on cached instances (singleton scope)

**DON'T**:
- Create multiple provider instances
- Forget to clean up resources
- Keep references after cleanup

### 2. Error Handling

**DO**:
- Use `MlKitErrorHandler.withMlKitRetry()` for operations
- Validate input before processing
- Provide user-friendly error messages

**DON'T**:
- Ignore errors silently
- Use raw try-catch without classification
- Retry non-retryable errors

### 3. Performance

**DO**:
- Use configuration constants from `MlKitConfig`
- Process operations in parallel when possible
- Exit early on invalid or empty input

**DON'T**:
- Hard-code configuration values
- Process sequentially when parallel is possible
- Continue processing after detecting errors

### 4. Thread Safety

**DO**:
- Trust the thread-safe implementations
- Use coroutines for async operations
- Leverage built-in synchronization

**DON'T**:
- Add additional synchronization unnecessarily
- Use threads instead of coroutines
- Access internal state directly

---

## Testing Strategy

### Unit Tests

Test individual provider methods with mocked dependencies:
- Input validation
- Error handling
- Resource cleanup
- Configuration usage

### Integration Tests

Test with real ML Kit SDK:
- Model download/availability
- Translation accuracy
- Text recognition accuracy
- Performance benchmarks

### Example Test:
```kotlin
@Test
fun translate_with_empty_text_returns_failure() = runTest {
    val result = provider.translate("", "en", "es")
    
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
}
```

---

## Performance Characteristics

### MlKitTranslationProvider
- **First Translation**: 2-5 seconds (model download + initialization)
- **Subsequent Translations**: 100-500ms (cached translator)
- **Memory**: ~50MB per active translator
- **Cache Size**: Up to 10 translators (configurable)

### MlKitTextRecognitionProvider
- **First Recognition**: 1-3 seconds (model initialization)
- **Subsequent Recognition**: 200-800ms (cached recognizer)
- **Memory**: ~30MB per recognizer
- **Cache Size**: Up to 5 recognizers (one per script)

### MlKitCameraTranslationProvider
- **Full Pipeline**: 1-5 seconds (depends on text complexity)
- **OCR Phase**: 200-800ms
- **Translation Phase**: 100-500ms per text block (parallel)
- **Memory**: ~100MB peak (combined models)

---

## Troubleshooting

### Common Issues

**Issue**: Translation fails with "Model not available"
- **Cause**: Translation model not downloaded
- **Solution**: Call `downloadModels()` or ensure WiFi connectivity

**Issue**: Text recognition returns empty results
- **Cause**: Wrong script/language selected or poor image quality
- **Solution**: Verify language code, improve lighting/focus

**Issue**: High memory usage
- **Cause**: Too many cached translators/recognizers
- **Solution**: Adjust cache limits in `MlKitConfig`

**Issue**: Slow performance
- **Cause**: Sequential processing or first-time model loading
- **Solution**: Use parallel processing, pre-load models

---

## Future Enhancements

1. **Model Management**:
   - Automatic model updates
   - Model size optimization
   - Custom model support

2. **Performance**:
   - Adaptive cache sizing
   - Predictive model loading
   - Result caching

3. **Features**:
   - Batch translation support
   - Translation history
   - Offline dictionary fallback

4. **Quality**:
   - Translation confidence scores
   - Alternative translations
   - Language detection

---

## References

- [ML Kit Translation Documentation](https://developers.google.com/ml-kit/language/translation)
- [ML Kit Text Recognition Documentation](https://developers.google.com/ml-kit/vision/text-recognition/v2)
- [Clean Architecture Principles](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
