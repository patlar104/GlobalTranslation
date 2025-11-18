# GlobalTranslation Architecture

This document provides a concise, developer-oriented view of the GlobalTranslation app’s multi-module clean architecture
and the provider pattern used throughout. For ML Kit implementation details, see ML_KIT_ARCHITECTURE.md.

## Module Structure

```text
:core  (Pure Kotlin domain & contracts)
:data  (Android Library with implementations)
:app   (Android App – UI only)
```

- :core
    - Domain models
    - Provider interfaces (Translation, Text Recognition/OCR, Camera, Speech, TextToSpeech)
    - Repository interfaces
- :data
    - ML Kit and Android implementations of :core interfaces
    - Hilt modules to bind interfaces to implementations
    - Room database, DataStore, and platform integrations
- :app
    - Jetpack Compose UI + ViewModels
    - Depends on :core interfaces; implementations provided by :data via Hilt

## Provider Pattern (Clean Architecture)

Provider interfaces live in :core and are implemented in :data. ViewModels in :app depend only on the interfaces and are
injected via Hilt.

### Example Interface (in :core)

```kotlin
interface TranslationProvider {
    suspend fun translate(text: String, from: String, to: String): Result<String>
    suspend fun areModelsDownloaded(from: String, to: String): Boolean
    suspend fun downloadModels(from: String, to: String): Result<Unit>
    suspend fun deleteModel(languageCode: String): Result<Unit>
    fun cleanup()
}
```

### Hilt Binding (in :data)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    @Binds
    @Singleton
    abstract fun bindTranslationProvider(impl: MlKitTranslationProvider): TranslationProvider
}
```

## Critical Method Distinction: areModelsDownloaded() vs translate()

The TranslationProvider exposes two different flows regarding ML Kit model management that must not be conflated:

1. areModelsDownloaded(from, to): Boolean

    - Purpose: Check model availability without triggering downloads
    - Behavior: Queries the local RemoteModelManager; no network usage
    - Use cases: UI readiness checks, enabling a “Download” call-to-action, pre-flight validation

   ```kotlin
   // Non-destructive status check (no download)
   val ready = translationProvider.areModelsDownloaded("en", "es")
   if (!ready) {
       // Show download button / guidance (WiFi recommended)
   }
   ```

2. translate(text, from, to): `Result<String>`

    - Purpose: Perform translation
    - Behavior: Will auto-download missing models on first use (on WiFi per ML Kit policy)
    - Use cases: Actual translation when the user expects models to be present

   ```kotlin
   // May download models automatically if missing (on WiFi)
   val result = translationProvider.translate("Hello", "en", "es")
   ```

Why this separation matters:

- Prevents unintended large downloads during simple readiness checks
- Gives users control (explicit download action) and better UX
- Keeps code intent clear (checking vs doing)

### Recommended ViewModel Pattern

```kotlin
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val translationProvider: TranslationProvider,
    private val repository: ConversationRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun checkModels(from: String, to: String) {
        viewModelScope.launch {
            val ready = translationProvider.areModelsDownloaded(from, to)
            _ui.value = _ui.value.copy(modelsReady = ready, showDownloadPrompt = !ready)
        }
    }

    fun downloadModels(from: String, to: String) {
        viewModelScope.launch {
            translationProvider.downloadModels(from, to)
                .onSuccess { _ui.value = _ui.value.copy(modelsReady = true, showDownloadPrompt = false) }
                .onFailure { e -> _ui.value = _ui.value.copy(error = "Download failed: ${e.message}") }
        }
    }

    fun translate(text: String, from: String, to: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(getTranslating = true)
            translationProvider.translate(text, from, to)
                .onSuccess { translated ->
                    _ui.value = _ui.value.copy(getTranslating = false)
                    repository.saveConversation(ConversationTurn(text, translated, from, to))
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(getTranslating = false, error = e.message)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        translationProvider.cleanup()
    }
}

data class UiState(
    val getTranslating: Boolean = false,
    val error: String? = null,
    val modelsReady: Boolean = false,
    val showDownloadPrompt: Boolean = false
)
```

## Related Documentation

- ML Kit implementation details and performance characteristics: ML_KIT_ARCHITECTURE.md
- Build setup, versions, and requirements: docs/README.md
- Testing strategy and fake providers: see docs/archive/TESTING_IMPROVEMENTS_SUMMARY.md
