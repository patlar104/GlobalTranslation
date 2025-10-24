---
applyTo: '**/*'
---

# Copilot Instructions for GlobalTranslation Android App

> **📚 Documentation Hub**: All project documentation is organized in [`docs/`](../docs/README.md)
> - **Planning**: [`docs/planning/`](../docs/planning/) - Project Plan and Feature Plan
> - **AI Agents**: [`docs/ai-agents/`](../docs/ai-agents/) - Detailed instructions and quick reference
> - **Archive**: [`docs/archive/`](../docs/archive/) - Historical summaries and implementation reports

## Project Overview
**PRODUCTION-READY** Android translation app with **multi-module clean architecture** using Jetpack Compose and ML Kit. Core features complete: live conversation translation, text input translation, camera translation, and language management. Architecture refactored into :core, :data, and :app modules for optimal testability and maintainability. **Now includes comprehensive 16KB page size support** for ARM64 devices and Google Play compliance.

## Critical Build Setup (MUST READ FIRST)

### Stable Build Configuration
- **AGP Version**: 8.13.0 with reliable Hilt integration
- **Kotlin Version**: 2.2.20 (latest stable)
- **KSP Version**: 2.2.20-2.0.2 (matches Kotlin version - note: KSP versioning changed from 1.0.x to 2.0.x)
- **Hilt Version**: 2.57.2
- **JVM Target**: 11 (must be aligned between Java and Kotlin compileOptions)
- **Build command**: `.\gradlew build` (PowerShell) or `./gradlew build` (bash)
- **16KB Page Size**: Full ARM64 support with Google Play compliance

### Essential Plugin Configuration
```kotlin
// In app/build.gradle.kts - ALL plugins must be present:
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)     // REQUIRED! Not optional
    alias(libs.plugins.kotlin.compose)     // Compose compiler
    alias(libs.plugins.ksp)                // For Hilt annotation processing
    alias(libs.plugins.hilt)               // Hilt DI
}
```

### Critical JVM Target Alignment
```kotlin
// Both MUST be JVM 11 - mismatch causes build failure
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
// Note: Old kotlinOptions { jvmTarget = "11" } is deprecated
```

## Multi-Module Clean Architecture

### Module Structure ✅
```
:core (Pure Kotlin, no Android dependencies)
├── model/ (ConversationTurn domain model)
├── provider/ (5 interfaces: Translation, Speech, TTS, OCR, Camera)
├── repository/ (ConversationRepository interface)
└── util/ (TextBlockGroupingUtil business logic + tests)

:data (Android Library - implementations)
├── provider/ (ML Kit & Android implementations of :core interfaces)
├── repository/ (Room-based ConversationRepository implementation)
├── local/ (Room database: DAO, entities, migrations)
├── di/ (Hilt modules: DataModule, ProviderModule)
├── network/ (Future: cloud translation APIs)
└── preferences/ (DataStore for app settings)

:app (Android App - UI only)
├── ui/ (Compose screens + ViewModels)
├── util/ (DeviceCompatibility for 16KB page size)
└── MainActivity.kt (NavigationSuiteScaffold host)
```

### Package Name & Navigation
- **Package**: `com.example.globaltranslation` (typo corrected from gloabtranslation)
- **Navigation**: Single Activity with `NavigationSuiteScaffold` for adaptive UI
- **Destinations**: CONVERSATION, TEXT_INPUT, CAMERA, LANGUAGES (defined in AppDestinations enum)

## Provider Pattern (Clean Architecture Core)

### Interface Definition Pattern (:core module)
```kotlin
// Example: TranslationProvider interface
interface TranslationProvider {
    suspend fun translate(text: String, from: String, to: String): Result<String>
    suspend fun areModelsDownloaded(from: String, to: String): Boolean
    suspend fun downloadModels(from: String, to: String): Result<Unit>
    suspend fun deleteModel(languageCode: String): Result<Unit>
    fun cleanup()
}
```

### Implementation Pattern (:data module)
```kotlin
@Singleton
class MlKitTranslationProvider @Inject constructor() : TranslationProvider {
    private val activeTranslators = mutableMapOf<String, Translator>()
    
    // CRITICAL: areModelsDownloaded() checks without triggering downloads
    override suspend fun areModelsDownloaded(from: String, to: String): Boolean {
        val modelManager = RemoteModelManager.getInstance()
        return modelManager.isModelDownloaded(TranslateRemoteModel.Builder(from).build()).await() &&
               modelManager.isModelDownloaded(TranslateRemoteModel.Builder(to).build()).await()
    }
    
    override fun cleanup() {
        activeTranslators.values.forEach { it.close() }
        activeTranslators.clear()
    }
}
```

### Hilt Binding Pattern (:data/di/ProviderModule.kt)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    
    @Binds @Singleton
    abstract fun bindTranslationProvider(impl: MlKitTranslationProvider): TranslationProvider
    
    @Binds @Singleton  
    abstract fun bindSpeechProvider(impl: AndroidSpeechProvider): SpeechProvider
    
    // Use @Binds (not @Provides) for interface binding - generates less code
}
```

## ViewModel Pattern with StateFlow

### Established Pattern (All ViewModels Follow This)
```kotlin
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val translationProvider: TranslationProvider,  // :core interface
    private val speechProvider: SpeechProvider,           // :core interface
    private val ttsProvider: TextToSpeechProvider,        // :core interface
    private val conversationRepository: ConversationRepository // :core interface
) : ViewModel() {
    
    // PATTERN: Private MutableStateFlow for internal updates
    private val _uiState = MutableStateFlow(ConversationUiState())
    // PATTERN: Public immutable StateFlow with .asStateFlow()
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()
    
    // Always use viewModelScope for automatic cancellation
    fun translateAndSave(text: String, fromLang: String, toLang: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTranslating = true)
            
            translationProvider.translate(text, fromLang, toLang)
                .fold(
                    onSuccess = { translated ->
                        val turn = ConversationTurn(text, translated, fromLang, toLang)
                        _uiState.value = _uiState.value.copy(
                            conversationHistory = _uiState.value.conversationHistory + turn,
                            isTranslating = false
                        )
                        conversationRepository.saveConversation(turn)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message,
                            isTranslating = false
                        )
                    }
                )
        }
    }
    
    // CRITICAL: Clean up resources in onCleared()
    override fun onCleared() {
        super.onCleared()
        speechProvider.cleanup()
        ttsProvider.cleanup()
    }
}

// PATTERN: Immutable data class for UI state
data class ConversationUiState(
    val conversationHistory: List<ConversationTurn> = emptyList(),
    val isTranslating: Boolean = false,
    val error: String? = null
)
```

## ML Kit Integration Best Practices

### Model Management
- ✅ **Check Status**: Use `areModelsDownloaded()` to check without downloading
- ✅ **WiFi Requirement**: First-time downloads require WiFi connection
- ✅ **Auto-Download**: `translate()` automatically downloads models if missing (on WiFi)
- ✅ **Error Messages**: Include guidance about WiFi and model downloads
- ❌ **DON'T**: Check models by attempting translation (triggers download)
- ❌ **DON'T**: Assume models are available without checking

### Speech Recognition
- **Use Android SpeechRecognizer** - NOT ML Kit speech (more stable, no dependencies)
- Pattern: `SpeechRecognizer.createSpeechRecognizer(context)`
- Handle permissions with Compose built-in `rememberLauncherForActivityResult`

## 16KB Page Size Support (Google Play 2025 Requirement)

### Build Configuration
```kotlin
// In app/build.gradle.kts
android {
    defaultConfig {
        ndk {
            // All ABIs configured for 16KB alignment
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }
    
    buildTypes {
        // 16KB testing build variant
        create("sixteenKB") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".sixteenkb"
        }
    }
    
    packaging {
        // Required for 16KB compatibility
        resources.excludes.add("META-INF/versions/9/previous-compilation-data.bin")
    }
}
```

### Device Compatibility Monitoring
```kotlin
// DeviceCompatibility.kt logs page size on app startup
DeviceCompatibility.logPageSizeInfo()  // Called in MainActivity.onCreate()
```

### Testing Commands
```bash
# Test 16KB build variant
.\gradlew :app:assemblySixteenKB

# Check logs for page size info (DeviceCompatibility logs on startup)
```

## Development Workflow

### Version Catalog Management
- All dependencies in `gradle/libs.versions.toml`
- Use `libs.` references in build files: `implementation(libs.hilt.android)`
- **KSP Version Format**: `2.2.20-2.0.2` (changed from 1.0.x to 2.0.x)

### Testing Strategy
```kotlin
// Create fake providers in test/fake/ directory
class FakeTranslationProvider : TranslationProvider {
    var shouldSucceed = true
    var translationResult = "Translated Text"
    
    override suspend fun translate(text: String, from: String, to: String): Result<String> {
        return if (shouldSucceed) Result.success(translationResult)
        else Result.failure(Exception("Translation failed"))
    }
}

// Use @TestInstallIn to replace production providers
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ProviderModule::class])
abstract class TestProviderModule {
    @Binds @Singleton
    abstract fun bindTranslationProvider(impl: FakeTranslationProvider): TranslationProvider
}
```

## Architecture Constraints

### State Management Rules
- **Single Source of Truth**: Each screen has one ViewModel holding all UI state
- **No Direct Provider Access**: Compose screens never call providers directly, only through ViewModels
- **Immutable State**: Use data classes with copy() for state updates
- **StateFlow Pattern**: Private MutableStateFlow, public StateFlow with asStateFlow()

### Module Dependencies
- **:core** → No Android dependencies (pure Kotlin)
- **:data** → Depends on :core, implements interfaces
- **:app** → Depends on :core for interfaces, :data provides implementations via Hilt

### Navigation Architecture
- **No Navigation in ViewModels**: Navigation events handled at Compose level
- **Adaptive UI**: NavigationSuiteScaffold adapts to phone/tablet/desktop
- **Material3 Expressive Theme**: Lavender/purple palette with large corner radii

## Essential Files to Understand
- `MainActivity.kt` - NavigationSuiteScaffold host with DeviceCompatibility logging
- `GloabTranslationApplication.kt` - @HiltAndroidApp entry point (name unchanged for compatibility)
- `:core/provider/` - All provider interfaces (clean architecture boundaries)
- `:data/di/ProviderModule.kt` - Hilt bindings for provider implementations
- `gradle/libs.versions.toml` - Version catalog with all dependency versions
- `DeviceCompatibility.kt` - 16KB page size monitoring utility

## Permission Handling Pattern

### Modern Compose Permissions (Accompanist Migrated)
```kotlin
// Use built-in Compose permission APIs - Accompanist is deprecated
var hasPermission by remember {
    mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
        == PermissionChecker.PERMISSION_GRANTED
    )
}

val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted -> hasPermission = isGranted }

// Request permission: permissionLauncher.launch(Manifest.permission.CAMERA)
```

## Future Enhancement Guidelines
When extending this app:
1. **Follow established patterns** - Provider interfaces in :core, implementations in :data
2. **Use existing Hilt setup** - Add new providers to ProviderModule
3. **Maintain StateFlow pattern** - Private MutableStateFlow, public StateFlow
4. **Keep 16KB compatibility** - New native libraries must support 16KB pages
5. **Test with fake providers** - Use @TestInstallIn pattern for isolated testing
6. **Use Compose permissions** - Never use deprecated Accompanist libraries

## Future Opportunities (Latest Dependencies)

### CameraX Compose Migration
- **Current**: Custom `CameraPreview` with AndroidView wrapper
- **Upgrade**: Consider migrating to `androidx.camera:camera-compose` CameraPreview composable
- **Benefits**: Better Compose integration, simplified state management, built-in lifecycle handling
```kotlin
// Future migration pattern:
import androidx.camera.compose.CameraPreview
CameraPreview(
    cameraSelector = cameraSelector,
    modifier = Modifier.fillMaxSize()
)
```

### New Material 3 Components (Compose BOM 2025.10.00)
Available for enhancing user experience:
- **`HorizontalCenteredHeroCarousel`** - For showcasing translation history or language examples
- **`SecureTextField` / `OutlinedSecureTextField`** - If adding user authentication features
- **`SearchBar` / `ExpandedFullScreenSearchBar`** - For searching translation history or languages
- **`TimePickerDialog`** - For scheduling translation reminders or history filtering

### Enhanced Animations
- **New `MotionScheme`** - Unified animation system for Material 3 components
- **Auto-sizing Text** - Text composable now supports dynamic sizing for translations
- **Material 3 BasicTextField2** - Enhanced text input with built-in Material styling