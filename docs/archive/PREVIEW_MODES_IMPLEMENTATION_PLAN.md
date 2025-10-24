# 🎨 Android Studio Preview Modes - Implementation Plan

## 📋 Executive Summary

This document outlines the plan to implement comprehensive Android Studio preview mode support across all screens in the GlobalTranslation app. Android Studio provides three key preview modes:

1. **Standard Preview Mode** - Static UI previews with multiple device/theme variants
2. **Interactive Mode** - Live interaction with composables in the preview pane
3. **UI Check Mode** - Accessibility and design system validation

## 🔍 Current State Analysis

### ✅ What's Already Implemented

#### ConversationScreen
- ✅ `@PreviewScreenSizes` for basic multi-device previews
- ✅ `@PreviewParameter` with `ConversationUiStatePreviewProvider` (5 states)
- ✅ `ConversationScreenLivePreview` with interactive state using `remember { mutableStateOf() }`
- ✅ Custom `@MultiDevicePreview` annotation (phone/tablet, light/dark)
- ✅ Shared preview data (`previewConversations`, `previewLanguages`)

#### TextInputScreen
- ✅ `@PreviewScreenSizes` for basic multi-device previews
- ✅ `@PreviewParameter` with `TextInputUiStatePreviewProvider` (4 states)
- ✅ Extracted `TextInputScreenContent` composable for preview testing
- ✅ Uses shared preview data

#### CameraScreen
- ✅ `@PreviewScreenSizes` for overlay previews
- ✅ `@PreviewParameter` with `CameraUiStatePreviewProvider` (5 states)
- ✅ Extracted `CameraOverlayContent` for overlay-only preview (camera preview excluded)
- ✅ Uses shared preview data

#### LanguageScreen
- ⚠️ **Basic preview only** - Empty preview stub
- ❌ No `@PreviewParameter` provider
- ❌ No interactive preview
- ❌ No state variants

### ❌ What's Missing

Status update (this pass):

- Phase 1 is implemented across all screens with standardized annotations and theming wrappers.
- New helper annotations: `DesignVariantPreview`, `FontScalePreview`, `DynamicColorPreview`.
- New `PreviewScaffold` wrapper applied across previews for consistent theming.

Gaps remaining:

1. Interactive Mode Full Support
    - ✅ ConversationScreen and LanguageScreen have live previews
    - ✅ TextInputScreen and CameraScreen have live previews

2. UI Check Mode Support
     - Add accessibility validation previews and semantics audits

#### 1. Advanced Preview Annotations (ALL SCREENS)
- ✅ Implemented via composite `DesignVariantPreview` and helper sets (`FontScalePreview`, `DynamicColorPreview`)
- ✅ `@PreviewLightDark` applied through `DesignVariantPreview`
- ✅ `@PreviewFontScale` applied through `DesignVariantPreview`
- ✅ `@PreviewDynamicColors` applied through `DesignVariantPreview`
- ⚠️ Ensure live previews also include `@PreviewScreenSizes` where useful

#### 2. Interactive Mode Full Support

- ✅ ConversationScreen and LanguageScreen have live previews
- ✅ TextInputScreen and CameraScreen have live previews

#### 3. UI Check Mode Support

- Add accessibility validation previews
- Audit `contentDescription` usage
- Validate touch target sizes (48dp minimum)
- Add color contrast validation previews
- ❌ Missing touch target size checks (48dp minimum)
- ❌ No color contrast validation previews

#### 4. LanguageScreen Specific Issues

- Implemented: extracted content, preview provider, state previews, and live preview
- ❌ Empty preview - needs fake data
- ❌ No state provider for different language states
- ❌ No interactive download/delete testing
- ❌ Carousel preview not implemented

---

## 🎯 Implementation Plan

### Phase 1: Complete Standard Preview Support (All Screens)

Status: COMPLETED

- Added `@DesignVariantPreview` to all state and interactive previews
- Ensured `@MultiDevicePreview` and `@PreviewScreenSizes` are present where appropriate
- Standardized theming via `PreviewScaffold { ... }`

Goal: Ensure every screen has comprehensive standard previews with multiple states, devices, themes, and accessibility configurations.

- Added `@DesignVariantPreview` to all state and interactive previews
- Ensured `@MultiDevicePreview` and `@PreviewScreenSizes` are present where appropriate
- Standardized theming via `PreviewScaffold { ... }`

**Goal**: Ensure every screen has comprehensive standard previews with multiple states, devices, themes, and accessibility configurations.

#### 1.1 Add Missing Preview Annotations

**Files to Modify**: All `*Screen.kt` files
Files Modified: All `*Screen.kt` files

```kotlin
// Add to ALL preview functions
@PreviewScreenSizes
@PreviewFontScale
@PreviewLightDark
@PreviewDynamicColors
@Composable
private fun [Screen]Preview(
    @PreviewParameter([Screen]UiStatePreviewProvider::class) state: [Screen]UiState
) {
    GlobalTranslationTheme(dynamicColor = true) { // Enable dynamic colors
        Surface {
            [Screen]Content(
                uiState = state,
                // ... other params with no-op callbacks
            )
        }
    }
}
```

**Required Imports**:
Required Imports:
```kotlin
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
```

#### 1.2 Create Comprehensive PreviewParameterProviders

**ConversationScreen** - Already has 5 states ✅
**TextInputScreen** - Already has 4 states ✅
**CameraScreen** - Already has 5 states ✅

**LanguageScreen** - **NEEDS IMPLEMENTATION** ❌
LanguageScreen - IMPLEMENTED ✅

```kotlin
// Add to LanguageScreen.kt
private class LanguageUiStatePreviewProvider : PreviewParameterProvider<LanguageUiState> {
    override val values: Sequence<LanguageUiState> = sequenceOf(
                LanguageModel(TranslateLanguage.SPANISH, "Spanish", false, false),
                LanguageModel(TranslateLanguage.FRENCH, "French", false, false)
            )
        ),
        // State 2: Some models downloaded
        LanguageUiState(
            availableLanguages = listOf(
                LanguageModel(TranslateLanguage.ENGLISH, "English", true, false),
                LanguageModel(TranslateLanguage.SPANISH, "Spanish", true, false),
                LanguageModel(TranslateLanguage.FRENCH, "French", false, false)
            )
        ),
        // State 3: Downloading in progress
        LanguageUiState(
            availableLanguages = listOf(
                LanguageModel(TranslateLanguage.ENGLISH, "English", true, false),
                LanguageModel(TranslateLanguage.FRENCH, "French", false, true)
            )
        ),
        // State 4: Error state
        LanguageUiState(
            error = "Failed to download French model. Check WiFi connection."
        ),
        // State 5: All popular models downloaded
        LanguageUiState(
            availableLanguages = listOf(
                LanguageModel(TranslateLanguage.ENGLISH, "English", true, false),
                LanguageModel(TranslateLanguage.SPANISH, "Spanish", true, false),
                LanguageModel(TranslateLanguage.FRENCH, "French", true, false),
                LanguageModel(TranslateLanguage.GERMAN, "German", true, false),
                LanguageModel(TranslateLanguage.ITALIAN, "Italian", true, false)
            )
        )
    )
}

See actual implementation in `app/src/main/java/com/example/globaltranslation/ui/languages/LanguageScreen.kt`.
@Preview(name = "Language States", showBackground = true)
@PreviewScreenSizes
@PreviewFontScale
@PreviewLightDark
@Composable
private fun LanguageScreenStatesPreview(
    @PreviewParameter(LanguageUiStatePreviewProvider::class) state: LanguageUiState
) {
    GlobalTranslationTheme {
        Surface {
            // Use fake ViewModel or create LanguageScreenContent composable
            LanguageScreen() // Need to extract content composable
        }
    }
}
```

#### 1.3 Extract Content Composables for Testing

**Required Changes**:

1. **LanguageScreen.kt** - Extract `LanguageScreenContent`
   - Separate `LanguageScreen()` (with ViewModel) from `LanguageScreenContent()` (pure composable)
   - Pass state and callbacks as parameters
   - Use in preview with no-op callbacks

2. **ConversationScreen.kt** - Already has `ConversationScreenContent` ✅
3. **TextInputScreen.kt** - Already has `TextInputScreenContent` ✅
4. **CameraScreen.kt** - Already has `CameraOverlayContent` ✅

---

### Phase 2: Interactive Mode Full Support

**Goal**: Enable "Run Interactive Mode" in Android Studio for all screens with live state management.

#### 2.1 Add Live Interactive Previews (ALL SCREENS)

**Pattern** (Already implemented in ConversationScreen):
```kotlin
@MultiDevicePreview
@Composable
private fun [Screen]LivePreview() {
    val state = remember {
        mutableStateOf(
            [Screen]UiState(
                // Initial state with preview data
            )
        )
    }
    
    GlobalTranslationTheme {
        Surface {
            [Screen]Content(
                uiState = state.value,
                onAction1 = { 
                    state.value = state.value.copy(/* update state */)
                },
                onAction2 = { 
                    state.value = state.value.copy(/* update state */)
                },
                // ... all other callbacks with state updates
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

**Implementation Checklist**:

- ✅ ConversationScreen - `ConversationScreenLivePreview`
- ✅ TextInputScreen - `TextInputScreenLivePreview`
- ✅ CameraScreen - `CameraOverlayLivePreview`
- ✅ LanguageScreen - `LanguageScreenLivePreview`


#### 2.2 TextInputScreen Interactive Preview

```kotlin
@MultiDevicePreview
@Composable
private fun TextInputScreenLivePreview() {
    var inputText by remember { mutableStateOf("") }
    val state = remember {
        mutableStateOf(
            TextInputUiState(
                sourceLanguage = previewLanguages[0],
                targetLanguage = previewLanguages[1],
                translationHistory = previewConversations.take(2)
            )
        )
    }
    
    GlobalTranslationTheme {
        Surface {
            TextInputScreenContent(
                uiState = state.value,
                onTextChange = { inputText = it },
                onTranslate = { text ->
                    // Simulate translation
                    val turn = ConversationTurn(
                        text,
                        "Translation of: $text",
                        state.value.sourceLanguage,
                        state.value.targetLanguage
                    )
                    state.value = state.value.copy(
                        translationHistory = listOf(turn) + state.value.translationHistory,
                        currentTranslation = turn
                    )
                },
                onSourceLanguageChange = { code ->
                    state.value = state.value.copy(sourceLanguage = code)
                },
                onTargetLanguageChange = { code ->
                    state.value = state.value.copy(targetLanguage = code)
                },
                onSwapLanguages = {
                    state.value = state.value.copy(
                        sourceLanguage = state.value.targetLanguage,
                        targetLanguage = state.value.sourceLanguage
                    )
                },
                onCopyToClipboard = { /* no-op in preview */ },
                onCopyToInput = { text -> inputText = text },
                onSpeak = { _, _ -> /* no-op in preview */ },
                onClearHistory = {
                    state.value = state.value.copy(translationHistory = emptyList())
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

#### 2.3 CameraScreen Interactive Preview

```kotlin
@MultiDevicePreview
@Composable
private fun CameraOverlayLivePreview() {
    val state = remember {
        mutableStateOf(
            CameraUiState(
                sourceLanguage = previewLanguages[0],
                targetLanguage = previewLanguages[1],
                detectedTextBlocks = listOf(
                    DetectedTextBlock("Hello", "Hola", 0.1f, 0.2f, 0.3f, 0.1f),
                    DetectedTextBlock("World", "Mundo", 0.1f, 0.35f, 0.3f, 0.1f)
                ),
                isFlashOn = false
            )
        )
    }
    
    GlobalTranslationTheme {
        Surface {
            CameraOverlayContent(
                uiState = state.value,
                onFlashToggle = {
                    state.value = state.value.copy(isFlashOn = !state.value.isFlashOn)
                },
                onLanguagePickerClick = { /* no-op in preview */ },
                onClearResults = {
                    state.value = state.value.copy(detectedTextBlocks = emptyList())
                },
                onCaptureClick = {
                    // Simulate new detection
                    state.value = state.value.copy(
                        detectedTextBlocks = listOf(
                            DetectedTextBlock("Preview", "Vista previa", 0.2f, 0.5f, 0.4f, 0.1f)
                        )
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

#### 2.4 LanguageScreen Interactive Preview

```kotlin
@MultiDevicePreview
@Composable
private fun LanguageScreenLivePreview() {
    val state = remember {
        mutableStateOf(
            LanguageUiState(
                availableLanguages = listOf(
                    LanguageModel(TranslateLanguage.ENGLISH, "English", true, false),
                    LanguageModel(TranslateLanguage.SPANISH, "Spanish", false, false),
                    LanguageModel(TranslateLanguage.FRENCH, "French", false, false),
                    LanguageModel(TranslateLanguage.GERMAN, "German", true, false)
                )
            )
        )
    }
    
    GlobalTranslationTheme {
        Surface {
            LanguageScreenContent(
                uiState = state.value,
                onDownloadModel = { languageCode ->
                    // Simulate download
                    state.value = state.value.copy(
                        availableLanguages = state.value.availableLanguages.map {
                            if (it.code == languageCode) it.copy(isDownloading = true)
                            else it
                        }
                    )
                    // Simulate completion after visual feedback
                    kotlinx.coroutines.GlobalScope.launch {
                        kotlinx.coroutines.delay(1000)
                        // Update to downloaded (in real preview, this won't persist)
                    }
                },
                onDeleteModel = { languageCode ->
                    state.value = state.value.copy(
                        availableLanguages = state.value.availableLanguages.map {
                            if (it.code == languageCode) it.copy(isDownloaded = false)
                            else it
                        }
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

---

### Phase 3: UI Check Mode Support

**Goal**: Enable "Run UI Check" in Android Studio for accessibility and design system validation.

#### 3.1 Accessibility Annotations Audit

**Required Actions**:

1. Audit all interactive elements for `contentDescription`
2. Ensure all buttons have semantic descriptions
3. Add `semantics { }` blocks for custom gestures
4. Verify minimum touch target sizes (48dp)

**Example Pattern**:

```kotlin
IconButton(
    onClick = onFlashToggle,
    modifier = Modifier
        .semantics {
            contentDescription = if (isFlashOn) "Turn flash off" else "Turn flash on"
        }
) {
    Icon(
        if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
        contentDescription = null // Handled by parent semantics
    )
}
```

#### 3.2 Design System Validation Previews

Create dedicated preview functions for design system checks:

```kotlin
@Preview(name = "Color Contrast - Light", group = "UI Check")
@Composable
private fun ColorContrastLightPreview() {
    GlobalTranslationTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(16.dp)) {
                Text("Primary on Surface", color = MaterialTheme.colorScheme.primary)
                Text("OnSurface on Surface", color = MaterialTheme.colorScheme.onSurface)
                Text("Secondary on Surface", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Preview(name = "Color Contrast - Dark", group = "UI Check")
@Composable
private fun ColorContrastDarkPreview() {
    GlobalTranslationTheme(darkTheme = true) {
        Surface {
            Column(Modifier.padding(16.dp)) {
                Text("Primary on Surface", color = MaterialTheme.colorScheme.primary)
                Text("OnSurface on Surface", color = MaterialTheme.colorScheme.onSurface)
                Text("Secondary on Surface", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Preview(name = "Touch Targets", group = "UI Check", showBackground = true)
@Composable
private fun TouchTargetSizesPreview() {
    // Show all buttons/interactive elements with 48dp minimum indicators
    Surface {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Microphone")
            }
            Spacer(Modifier.height(8.dp))
            Text("Minimum 48dp touch target ✅")
        }
    }
}
```

#### 3.3 Font Scaling Validation

Create previews specifically for large text testing:

```kotlin
@Preview(name = "Font Scale 1.0x", group = "Accessibility", fontScale = 1.0f)
@Preview(name = "Font Scale 1.5x", group = "Accessibility", fontScale = 1.5f)
@Preview(name = "Font Scale 2.0x", group = "Accessibility", fontScale = 2.0f)
@Composable
private fun FontScalingPreview() {
    GlobalTranslationTheme {
        Surface {
            [Screen]Content(
                uiState = /* typical state */,
                // ... callbacks
            )
        }
    }
}
```

---

### Phase 4: Shared Preview Utilities Enhancement

**Goal**: Centralize and enhance preview utilities for consistency.

#### 4.1 Expand PreviewData.kt

```kotlin
// File: app/src/main/java/com/example/globaltranslation/ui/components/PreviewData.kt

// Add language models for LanguageScreen
val previewLanguageModels = listOf(
    LanguageModel(TranslateLanguage.ENGLISH, "English", true, false),
    LanguageModel(TranslateLanguage.SPANISH, "Spanish", true, false),
    LanguageModel(TranslateLanguage.FRENCH, "French", false, false),
    LanguageModel(TranslateLanguage.GERMAN, "German", false, false),
    LanguageModel(TranslateLanguage.ITALIAN, "Italian", true, false),
    LanguageModel(TranslateLanguage.PORTUGUESE, "Portuguese", false, false)
)

// Add detected text blocks for CameraScreen
val previewDetectedTextBlocks = listOf(
    DetectedTextBlock("Hello", "Hola", 0.1f, 0.2f, 0.3f, 0.1f),
    DetectedTextBlock("World", "Mundo", 0.1f, 0.35f, 0.3f, 0.1f),
    DetectedTextBlock("Translation", "Traducción", 0.5f, 0.5f, 0.4f, 0.1f)
)

// Add error states
object PreviewErrors {
    const val TRANSLATION_FAILED = "Translation failed. Please check your internet connection."
    const val MODEL_DOWNLOAD_FAILED = "Failed to download model. WiFi connection required."
    const val PERMISSION_DENIED = "Microphone permission is required for voice translation."
    const val CAMERA_PERMISSION_DENIED = "Camera permission is required for camera translation."
}
```

#### 4.2 Create PreviewScaffolds.kt

```kotlin
// File: app/src/main/java/com/example/globaltranslation/ui/components/PreviewScaffolds.kt

/**
 * Scaffold for previewing screen content with consistent structure.
 */
@Composable
fun ScreenPreviewScaffold(
    title: String = "Preview",
    content: @Composable () -> Unit
) {
    GlobalTranslationTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(title) })
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                content()
            }
        }
    }
}

/**
 * Scaffold for accessibility testing with visible grid overlay.
 */
@Composable
fun AccessibilityPreviewScaffold(
    showTouchTargetOverlay: Boolean = true,
    content: @Composable () -> Unit
) {
    Box {
        content()
        if (showTouchTargetOverlay) {
            // Draw 48dp grid overlay for touch target validation
            Canvas(Modifier.fillMaxSize()) {
                val gridSize = 48.dp.toPx()
                // Draw grid lines...
            }
        }
    }
}
```

#### 4.3 Enhance Previews.kt Annotations

```kotlin
// File: app/src/main/java/com/example/globaltranslation/ui/components/Previews.kt

/**
 * Complete multi-device, multi-theme preview set.
 * Includes phone, tablet, landscape, light, and dark modes.
 */
@Preview(name = "Phone - Light", group = "Devices", device = "spec:width=411dp,height=891dp,dpi=480", showBackground = true)
@Preview(name = "Phone - Dark", group = "Devices", device = "spec:width=411dp,height=891dp,dpi=480", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Phone - Landscape", group = "Devices", device = "spec:width=891dp,height=411dp,dpi=480", showBackground = true)
@Preview(name = "Tablet - Light", group = "Devices", device = "spec:width=1280dp,height=800dp,dpi=240", showBackground = true)
@Preview(name = "Tablet - Dark", group = "Devices", device = "spec:width=1280dp,height=800dp,dpi=240", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Desktop - Light", group = "Devices", device = "spec:width=1920dp,height=1080dp,dpi=160", showBackground = true)
annotation class MultiDevicePreview

/**
 * Font scaling preview set for accessibility testing.
 * Tests 1.0x (default), 1.5x (large), and 2.0x (extra large) font scales.
 */
@Preview(name = "Font 1.0x", group = "Accessibility", fontScale = 1.0f, showBackground = true)
@Preview(name = "Font 1.5x", group = "Accessibility", fontScale = 1.5f, showBackground = true)
@Preview(name = "Font 2.0x", group = "Accessibility", fontScale = 2.0f, showBackground = true)
annotation class FontScalePreview

/**
 * Dynamic color preview set with system color themes.
 */
@Preview(name = "Dynamic - Light", group = "Dynamic Colors", showBackground = true)
@Preview(name = "Dynamic - Dark", group = "Dynamic Colors", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
annotation class DynamicColorPreview
```

---

## 📝 Implementation Checklist

### Phase 1: Standard Preview Support

- [ ] Add `@PreviewFontScale`, `@PreviewLightDark`, `@PreviewDynamicColors` to all screens
- [ ] Create `LanguageUiStatePreviewProvider` with 5 states
- [ ] Extract `LanguageScreenContent` composable
- [ ] Update `LanguageScreenPreview` to use preview parameter
- [ ] Verify all screens compile with new annotations

### Phase 2: Interactive Mode

- [ ] Add `TextInputScreenLivePreview` with state management
- [ ] Add `CameraOverlayLivePreview` with state management
- [ ] Add `LanguageScreenLivePreview` with state management
- [ ] Test interactive mode in Android Studio for all screens
- [ ] Document interactive preview usage in README

### Phase 3: UI Check Mode

- [ ] Audit all screens for missing `contentDescription`
- [ ] Add `semantics {}` blocks to custom interactive elements
- [ ] Create color contrast validation previews
- [ ] Create touch target size validation previews
- [ ] Create font scaling validation previews
- [ ] Run "UI Check" in Android Studio and fix issues

### Phase 4: Shared Utilities

- [ ] Expand `PreviewData.kt` with language models and text blocks
- [ ] Create `PreviewScaffolds.kt` with reusable preview wrappers
- [ ] Enhance `Previews.kt` with font scale and dynamic color annotations
- [ ] Document preview utilities in developer guide

---

## 🧪 Testing Strategy

### Manual Testing in Android Studio

1. **Standard Preview**:
   - Open each `*Screen.kt` file
   - Verify preview panel shows multiple device/theme variants
   - Check that all preview parameters render correctly

2. **Interactive Mode**:
   - Click "Run Interactive Mode" (play icon in preview)
   - Test button clicks, text input, language selection
   - Verify state updates reflect in UI immediately

3. **UI Check Mode**:
   - Click "Run UI Check" in preview pane
   - Review accessibility warnings (missing descriptions, touch targets)
   - Fix all critical and high-priority issues

### Automated Testing

```kotlin
// Example test for preview providers
class PreviewProviderTest {
    @Test
    fun `ConversationUiStatePreviewProvider returns 5 states`() {
        val provider = ConversationUiStatePreviewProvider()
        assertEquals(5, provider.values.count())
    }
    
    @Test
    fun `All preview states are valid`() {
        val provider = ConversationUiStatePreviewProvider()
        provider.values.forEach { state ->
            // Verify state validity
            assertNotNull(state)
        }
    }
}
```

---

## 📚 Documentation Requirements

### Developer Guide Section

Create `docs/PREVIEW_MODE_GUIDE.md`:

```markdown
# Android Studio Preview Modes Guide

## Using Standard Previews
- Open any `*Screen.kt` file
- Preview panel shows all device/theme variants
- Use dropdown to filter by group (Devices, Accessibility, etc.)

## Using Interactive Mode
1. Locate `*LivePreview` functions
2. Click play icon in preview pane
3. Interact with UI elements directly
4. State changes update in real-time

## Using UI Check Mode
1. Open preview pane
2. Click "Run UI Check" button
3. Review accessibility reports
4. Fix issues and re-run check
```

### README Update

Add section to main `README.md`:

```markdown
## 🎨 Development with Android Studio Previews

GlobalTranslation supports all three Android Studio preview modes:

- **Standard Preview**: Multi-device, multi-theme, font scaling previews
- **Interactive Mode**: Live UI interaction with state management
- **UI Check Mode**: Accessibility and design system validation

See `docs/PREVIEW_MODE_GUIDE.md` for detailed usage instructions.
```

---

## 🚀 Rollout Plan

### Week 1: Foundation

- Implement Phase 1 (Standard Preview Support)
- Focus on LanguageScreen completion
- Add missing annotations to existing screens

### Week 2: Interactivity

- Implement Phase 2 (Interactive Mode)
- Add live previews to TextInput, Camera, Language screens
- Test interactive mode thoroughly

### Week 3: Validation

- Implement Phase 3 (UI Check Mode)
- Audit and fix accessibility issues
- Create validation previews

### Week 4: Enhancement & Documentation

- Implement Phase 4 (Shared Utilities)
- Write developer guide
- Update README and documentation

---

## 📊 Success Metrics

- ✅ All 4 screens have `@PreviewScreenSizes`, `@PreviewFontScale`, `@PreviewLightDark`, `@PreviewDynamicColors`
- ✅ All 4 screens have `@PreviewParameter` providers with 4-5 states each
- ✅ All 4 screens have interactive live preview functions
- ✅ "Run Interactive Mode" works for all screens in Android Studio
- ✅ "Run UI Check" passes with no critical accessibility issues
- ✅ Developer guide documented and committed

---

## 🎯 Future Enhancements

### Phase 5 (Optional): Preview Screenshots

- Automate preview screenshot generation
- Use for documentation and PR reviews
- Create visual regression testing pipeline

### Phase 6 (Optional): Preview Performance Testing

- Measure preview render times
- Optimize slow preview functions
- Implement preview-specific performance benchmarks

---

## 📞 Support

For questions or issues with preview implementation:
 
1. Check Android Studio documentation: [Compose tooling](https://developer.android.com/jetpack/compose/tooling)
2. Review existing preview implementations in `ConversationScreen.kt`
3. Consult `ui/components/Previews.kt` for annotation examples

---

**Document Version**: 1.0  
**Last Updated**: October 15, 2025  
**Author**: GitHub Copilot  
**Status**: Ready for Implementation
