---
applyTo: '**/*'
---

# Copilot Instructions for GlobalTranslation Android App

## Project Overview

**PRODUCTION-READY** Android translation app with **multi-module clean architecture** using Jetpack Compose and ML Kit.
Core features complete: live conversation translation, text input translation, camera translation, and language
management. Architecture refactored into :core, :data, and :app modules for optimal testability and maintainability. *
*Now includes comprehensive 16KB page size support** for ARM64 devices and Google Play compliance.

## Critical Build Setup (MUST READ FIRST)

### Stable Build Configuration

- **AGP Version**: Uses stable AGP 8.13.0 with reliable Hilt integration
- **Kotlin Version**: Kotlin 2.2.20 (latest stable)
- **KSP Version**: 2.2.20-2.0.2 (matches Kotlin version - note: KSP versioning changed from 1.0.x to 2.0.x)
- **Hilt Version**: 2.57.2
- **JVM Target**: 21 (must be aligned between Java and Kotlin compileOptions - uses LTS version)
- **Build command**: Use `.\gradlew build` (PowerShell) or `./gradlew build` (bash)
- **16KB Page Size**: Full ARM64 support with Google Play compliance

### Dependency Management

- **Version Catalogs**: All dependencies in `gradle/libs.versions.toml` using `libs.` references
- **Required Plugins**: `kotlin.android`, `kotlin.compose`, `ksp`, `hilt` all properly configured
- **ML Kit**: Only translation (`mlkit-translate:17.0.3`) - speech recognition removed due to version conflicts
- **Critical**: JVM target must match in both `compileOptions` and `kotlinOptions` to avoid compatibility errors

## Current Architecture - Multi-Module Clean Architecture

### Module Structure ✅

```
:core (Pure Kotlin, no Android)
├── model/ (ConversationTurn)
├── provider/ (5 interfaces: Translation, Speech, TTS, OCR, Camera)
├── repository/ (ConversationRepository)
└── util/ (TextBlockGroupingUtil + tests)

:data (Android Library)
├── provider/ (ML Kit & Android implementations)
├── repository/ (Room-based ConversationRepository)
├── local/ (Room database, DAO, entities)
└── di/ (Hilt modules: DataModule, ProviderModule)

:app (Android App)
├── ui/ (Compose screens + ViewModels)
└── navigation/ (NavigationSuiteScaffold)
```

**Package Name**: `com.example.globaltranslation` (fixed from gloabtranslation)

### Navigation Structure

- **Single Activity**: `MainActivity.kt` with `NavigationSuiteScaffold`
- **Destinations**: Defined in `AppDestinations` enum (CONVERSATION, TEXT_INPUT, CAMERA, LANGUAGES)
- **Adaptive UI**: Uses Material3 adaptive navigation suite for different screen sizes
- **Current state**: ✅ All screens fully implemented and functional

### Hilt Setup ✅ COMPLETE

```kotlin
// Application class configured
@HiltAndroidApp
class GloabTranslationApplication : Application()  // Note: Class name unchanged for backward compatibility

// MainActivity is Hilt-enabled
@AndroidEntryPoint
class MainActivity : ComponentActivity()

// All providers injected from :data module via ProviderModule
// ViewModels use provider interfaces from :core
```

## ✅ Implemented Features - FULLY FUNCTIONAL

### Package Structure

All features have been successfully implemented:

**:core module** (Pure Kotlin):

- `model/` - ✅ ConversationTurn domain model
- `provider/` - ✅ 5 interfaces (Translation, Speech, TTS, OCR, Camera)
- `repository/` - ✅ ConversationRepository interface
- `util/` - ✅ TextBlockGroupingUtil business logic

**:data module** (Android Library):

- `provider/` - ✅ ML Kit & Android implementations (5 providers)
- `repository/` - ✅ Room-based ConversationRepository
- `local/` - ✅ Room database (DAO, entities)
- `di/` - ✅ ProviderModule for dependency injection

**:app module** (Android App):

- `ui/conversation/` - ✅ Live conversation translation with voice I/O + persistence
- `ui/textinput/` - ✅ Manual text translation with history management
- `ui/camera/` - ✅ Real-time OCR translation with CameraX
- `ui/languages/` - ✅ ML Kit model download and management
- `ui/components/` - ✅ Reusable LanguagePicker dialog and button components
- `ui/theme/` - ✅ Material3 Expressive Theme (lavender/purple palette)
- `util/` - ✅ DeviceCompatibility utility for 16KB page size monitoring

### Key Implementation Patterns (When Building Features)

#### Provider Pattern (Clean Architecture)

```kotlin
// Interface definition in :core module
interface TranslationProvider {
    suspend fun translate(text: String, from: String, to: String): Result<String>
    suspend fun areModelsDownloaded(from: String, to: String): Boolean
    suspend fun downloadModels(from: String, to: String): Result<Unit>
    suspend fun deleteModel(languageCode: String): Result<Unit>
    fun cleanup()
}

// Implementation in :data module
@Singleton
class MlKitTranslationProvider @Inject constructor() : TranslationProvider {
    private val activeTranslators = mutableMapOf<String, Translator>()
    
    override suspend fun translate(text: String, from: String, to: String): Result<String> {
        return try {
            val translator = getOrCreateTranslator(from, to)
            val result = translator.translate(text).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // CRITICAL: Checks actual download status without triggering downloads
    override suspend fun areModelsDownloaded(from: String, to: String): Boolean {
        val modelManager = RemoteModelManager.getInstance()
        val fromDownloaded = modelManager.isModelDownloaded(
            TranslateRemoteModel.Builder(from).build()
        ).await()
        val toDownloaded = modelManager.isModelDownloaded(
            TranslateRemoteModel.Builder(to).build()
        ).await()
        return fromDownloaded && toDownloaded
    }
    
    override fun cleanup() {
        activeTranslators.values.forEach { it.close() }
        activeTranslators.clear()
    }
}
```

**ML Kit Translation Best Practices:**

- ✅ **Model Checking**: Use `areModelsDownloaded()` to check status without downloading
- ✅ **WiFi Requirement**: First-time downloads require WiFi connection
- ✅ **Auto-Download**: `translate()` automatically downloads models if missing (on WiFi)
- ✅ **Error Messages**: Include guidance about WiFi and model downloads
- ❌ **DON'T**: Check models by attempting translation (triggers download)
- ❌ **DON'T**: Assume models are available without checking
- ❌ **DON'T**: Forget to handle cellular network errors

#### Speech Recognition Alternative

- **Do NOT use** ML Kit speech - use Android's built-in `SpeechRecognizer`
- No additional dependencies needed, more stable than ML Kit version
- Pattern: `SpeechRecognizer.createSpeechRecognizer(context)`

#### Hilt Provider Binding Pattern

```kotlin
// In :data/di/ProviderModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    
    @Binds
    @Singleton
    abstract fun bindTranslationProvider(
        impl: MlKitTranslationProvider
    ): TranslationProvider
    
    @Binds
    @Singleton
    abstract fun bindSpeechProvider(
        impl: AndroidSpeechProvider
    ): SpeechProvider
    
    @Binds
    @Singleton
    abstract fun bindTextToSpeechProvider(
        impl: AndroidTextToSpeechProvider
    ): TextToSpeechProvider
}
```

**Why @Binds instead of @Provides:**

- Generates less code (abstract methods)
- Enforces interface-based programming
- Better for testing (easy to swap implementations)

#### ViewModel Pattern with Providers (Current Implementation)

**All ViewModels use provider interfaces for clean architecture:**

```kotlin
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val translationProvider: TranslationProvider,
    private val speechProvider: SpeechProvider,
    private val ttsProvider: TextToSpeechProvider,
    private val conversationRepository: ConversationRepository
) : ViewModel() {
    // BEST PRACTICE: Private MutableStateFlow for internal updates
    private val _uiState = MutableStateFlow(ConversationUiState())
    // BEST PRACTICE: Public immutable StateFlow with .asStateFlow()
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()
    
    // Always use viewModelScope for automatic cancellation
    fun translateAndSave(text: String, fromLang: String, toLang: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(getTranslating = true)
            try {
                val result = translationProvider.translate(text, fromLang, toLang)
                result.fold(
                    onSuccess = { translatedText ->
                        val turn = ConversationTurn(
                            originalText = text,
                            translatedText = translatedText,
                            sourceLang = fromLang,
                            targetLang = toLang
                        )
                        
                        // Update UI state
                        _uiState.value = _uiState.value.copy(
                            conversationHistory = _uiState.value.conversationHistory + turn,
                            getTranslating = false
                        )
                        
                        // Persist to Room database
                        conversationRepository.saveConversation(turn)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = exception.message,
                            getTranslating = false
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    getTranslating = false
                )
            }
        }
    }
    
    // BEST PRACTICE: Clean up resources in onCleared()
    override fun onCleared() {
        super.onCleared()
        speechProvider.cleanup()
        ttsProvider.cleanup()
    }
}

// Data class for immutable state
data class ConversationUiState(
    val conversationHistory: List<ConversationTurn> = emptyList(),
    val getTranslating: Boolean = false,
    val error: String? = null
)
```

**Why This Pattern?**

- ✅ Immutable state exposure prevents external modification
- ✅ Single source of truth maintained
- ✅ Thread-safe state updates
- ✅ Compose automatically recomposes on state changes
- ✅ Resource cleanup prevents memory leaks

## Development Workflow

### Build System

- **Kotlin DSL**: All build files use `.gradle.kts` format
- **Namespace**: `com.example.globaltranslation` (typo fixed in architecture refactoring)
- **Target SDK**: 36 (latest Android)
- **Min SDK**: 29 (Android 10+)
- **Plugin Configuration**: All plugins properly declared in `app/build.gradle.kts`:
  ```kotlin
  plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.android)     // Required!
      alias(libs.plugins.kotlin.compose)
      alias(libs.plugins.ksp)
      alias(libs.plugins.hilt)
  }
  ```
- **JVM Target Alignment**: Both Java and Kotlin must target JVM 21:
  ```kotlin
  compileOptions {
      sourceCompatibility = JavaVersion.VERSION_21
      targetCompatibility = JavaVersion.VERSION_21
  }
  kotlin {
      compilerOptions {
          jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
      }
  }
  // Note: kotlinOptions is deprecated, use kotlin.compilerOptions instead
  ```

### File Organization

- Follow the refactoring plan in `docs/planning/docs/planning/Project Plan.md` when implementing features
- Template files should be renamed: `ui/home` → `ui/conversation`, `ui/gallery` → `ui/textinput`, etc.
- Services go in dedicated `services/` package, not mixed with UI code

### Compose Patterns

- Use `@PreviewScreenSizes` for responsive design testing
- Theme defined in `ui/theme/` - leverage existing `GloabTranslationTheme`
- Material3 with adaptive navigation suite for cross-device compatibility

## Project-Specific Conventions

### Current Navigation Pattern

```kotlin
// AppDestinations enum drives navigation in MainActivity.kt
enum class AppDestinations(val label: String, val icon: ImageVector) {
    CONVERSATION("Conversation", Icons.Filled.Mic),
    TEXT_INPUT("Text Input", Icons.Filled.Translate),
    LANGUAGES("Languages", Icons.Filled.Language),
}
```

### Required Permissions ✅ IMPLEMENTED

Already configured in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

## ✅ Implementation Complete - Production Ready

1. **All planned features implemented** - Translation app is fully functional
2. **Complete architecture in place** - Services, ViewModels, UI screens all working
3. **Modern Material3 UI** - Adaptive navigation with custom components
4. **ML Kit fully integrated** - Translation models, download management implemented
5. **Runtime permissions handled** - Microphone permission with visual feedback

## 🎯 Current Codebase Status (Verified)

### Implementation Complete ✅

1. ✅ **Complete service layer** with Hilt dependency injection
    - All services use @Singleton and @Inject
    - Proper resource cleanup in ViewModels
    - Flow-based reactive APIs
    - Model deletion support

2. ✅ **All three main screens** with full functionality
    - ConversationScreen: Live voice translation with TTS
    - TextInputScreen: Manual text translation with history, clipboard copy, and TTS
    - LanguageScreen: Model download, deletion, and status management

3. ✅ **Voice translation** with speech recognition and TTS
    - Real-time speech recognition with Flow
    - Auto-play translation support
    - Comprehensive error handling

4. ✅ **Manual text translation** with complete feature set
    - Translation history with timestamps
    - Copy to clipboard functionality
    - Copy to input functionality (separate buttons with distinct icons)
    - Text-to-speech for original and translated text
    - Language swapping

5. ✅ **Language model management** interface
    - 20+ supported languages
    - Dynamic download status checking
    - WiFi-only model downloads
    - Delete models to free storage space
    - Protected English model (cannot be deleted)

### StateFlow Verification ✅

All ViewModels implement StateFlow best practices:

```kotlin
// ✅ ConversationViewModel - 282 lines
private val _uiState = MutableStateFlow(ConversationUiState())
val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

// ✅ TextInputViewModel - 176 lines  
private val _uiState = MutableStateFlow(TextInputUiState())
val uiState: StateFlow<TextInputUiState> = _uiState.asStateFlow()

// ✅ LanguageViewModel - 209 lines
private val _uiState = MutableStateFlow(LanguageUiState())
val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()
```

### Architecture Verified ✅

```
✅ Project Structure Matches Documentation
✅ All Services Properly Injected
✅ StateFlow Pattern Consistent Across All ViewModels
✅ Resource Cleanup in onCleared()
✅ Coroutines Use viewModelScope
✅ No Deprecated API Usage
✅ Material3 Throughout
```

```

#### Compose UI Testing
```kotlin
// Test specific translation app UI behaviors
@Test
fun `conversation screen shows microphone button when not listening`() {
    composeTestRule.setContent {
        ConversationScreen(
            getListening = false,
            onStartListening = { }
        )
    }
    
    composeTestRule
        .onNodeWithContentDescription("Start listening")
        .assertIsDisplayed()
}

### Required Permissions (When Implementing)
Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

## Project-Specific Conventions

### File Organization

- Follow the refactoring plan in `docs/planning/docs/planning/Project Plan.md` when implementing features
- Template files should be renamed: `ui/home` → `ui/conversation`, `ui/gallery` → `ui/textinput`, etc.
- Services go in dedicated `services/` package, not mixed with UI code

### Compose Patterns

- Use `@PreviewScreenSizes` for responsive design testing
- Theme defined in `ui/theme/` - leverage existing `GloabTranslationTheme`
- Material3 with adaptive navigation suite for cross-device compatibility

## Testing Strategy

### Provider Testing with Fakes

```kotlin
// Create fake provider in test/fake/ directory
class FakeTranslationProvider : TranslationProvider {
    var shouldSucceed = true
    var translationResult = "Translated Text"
    
    override suspend fun translate(text: String, from: String, to: String): Result<String> {
        return if (shouldSucceed) {
            Result.success("$translationResult: $text")
        } else {
            Result.failure(Exception("Translation failed"))
        }
    }
    
    override suspend fun areModelsDownloaded(from: String, to: String) = true
    override suspend fun downloadModels(from: String, to: String) = Result.success(Unit)
    override suspend fun deleteModel(languageCode: String) = Result.success(Unit)
    override fun cleanup() {}
}

// Use in tests via Hilt
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ProviderModule::class]
)
abstract class TestProviderModule {
    @Binds
    @Singleton
    abstract fun bindTranslationProvider(impl: FakeTranslationProvider): TranslationProvider
}
```

### ViewModel Testing with Fake Providers

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ConversationViewModelTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var fakeTranslationProvider: FakeTranslationProvider
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun `translation success updates state correctly`() = runTest {
        fakeTranslationProvider.translationResult = "Hola"
        val viewModel = ConversationViewModel(
            fakeTranslationProvider,
            fakeSpeechProvider,
            fakeTtsProvider,
            fakeRepository
        )
        
        val stateValues = mutableListOf<ConversationUiState>()
        backgroundScope.launch {
            viewModel.uiState.toList(stateValues)
        }
        
        viewModel.translateAndSave("Hello", "en", "es")
        advanceUntilIdle()
        
        // Verify conversation was added
        assertTrue(stateValues.last().conversationHistory.isNotEmpty())
        assertEquals("Hola", stateValues.last().conversationHistory.first().translatedText)
    }
}
```

### Compose UI Testing with Hilt

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ConversationScreenTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Inject
    lateinit var fakeTranslationProvider: FakeTranslationProvider
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun conversationScreen_displaysLanguageSelectors() {
        composeTestRule.onNodeWithText("Conversation").performClick()
        composeTestRule.waitForIdle()
        
        // Verify language selectors exist
        composeTestRule
            .onAllNodesWithContentDescription("Select language")
            .assertCountEquals(2)
    }
}
```

## Architecture Constraints

### State Management Rules

- **Single Source of Truth**: Each screen has one ViewModel holding all UI state
- **No Direct Service Access**: Compose screens never call services directly, only through ViewModels
- **Immutable State**: Use data classes with copy() for state updates
- **Error Handling**: Always wrap service calls in try/catch with proper error states

### ML Kit Integration Constraints

- **Model Management**: Check model availability before translation attempts
- **Offline Handling**: Gracefully handle missing language models with download prompts
- **Memory Management**: Properly close ML Kit clients in ViewModel onCleared()

### Navigation Architecture

- **No Navigation in ViewModels**: Navigation events must be handled at Compose level
- **Shared State**: Use SavedStateHandle for data that survives process death
- **Deep Links**: Plan for deep link support in navigation structure

### Performance Guidelines

- **LazyColumn**: Use for any list with >10 items (conversation history, language list)
- **Image Loading**: Use Coil for any future image features
- **Background Work**: ML Kit operations must run on background threads via viewModelScope

## 🚀 Implementation Guidelines for Future Development

When extending this completed app:

1. **Architecture is established** - Follow existing MVVM + Hilt patterns
2. **Services are injectable** - Use existing ServicesModule for new dependencies
3. **UI follows Material3** - Extend existing component patterns for consistency
4. **Testing patterns documented** - Follow established ViewModel and service testing patterns
5. **Build system stable** - AGP 8.13.0 + Kotlin 2.2.20 configuration is production-ready
6. **16KB page size ready** - All new features automatically support ARM64 16KB pages

## 📱 16KB Page Size Support (NEW)

### Implementation Complete ✅

- **NDK Configuration**: All ABIs properly configured for 16KB alignment
- **Build Variants**: `sixteenKB` test variant for validation
- **Room Database**: Automatic 16KB page size handling with Room 2.7+
- **Native Libraries**: ML Kit libraries aligned for 16KB compatibility
- **Device Monitoring**: DeviceCompatibility utility for page size detection
- **ProGuard Rules**: Native method preservation and data module protection
- **Data Safety**: Existing user data preserved - no migration required

### Testing 16KB Compatibility

```bash
# Test 16KB build variant
./gradlew :app:assembleSixteenKB

# Test on 16KB emulator (Android 15+ ARM64)
# DeviceCompatibility logs page size info on app startup
```

### Google Play Compliance

- ✅ **Ready for 2025 requirements** - Full ARM64 16KB page size support
- ✅ **Backward compatible** - Works on 4KB devices without issues
- ✅ **Data preservation** - Room automatically handles page size differences
- ✅ **Native library alignment** - All ML Kit libraries properly configured

## Future Enhancement Areas

Potential areas for expansion:

1. Add unit tests for ViewModels and services
2. Implement UI tests for complex user flows
3. Add more language pairs or specialized translation features
4. Integrate additional ML Kit features (handwriting, etc.)