package com.example.globaltranslation.ui.conversation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.globaltranslation.R
import com.example.globaltranslation.model.ConversationTurn
import com.example.globaltranslation.ui.components.LanguagePickerButton
import com.example.globaltranslation.ui.components.TwoLanguagePickerButton
import com.example.globaltranslation.ui.theme.GlobalTranslationTheme
import com.example.globaltranslation.ui.components.MultiDevicePreview
import com.example.globaltranslation.ui.components.previewConversations
import com.example.globaltranslation.ui.components.previewLanguages
import com.example.globaltranslation.ui.components.DesignVariantPreview
import com.example.globaltranslation.ui.components.PreviewScaffold
import com.example.globaltranslation.ui.components.UiCheckPreview
import com.google.mlkit.nl.translate.TranslateLanguage as MlKitTranslateLanguage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip

// --- Live interactive preview ---
@MultiDevicePreview
@DesignVariantPreview
@Composable
fun ConversationScreenLivePreview() {
    val state = remember {
        mutableStateOf(
            ConversationUiState(
                conversationHistory = previewConversations,
                sourceLanguage = previewLanguages[0],
                targetLanguage = previewLanguages[1],
                autoPlayTranslation = true
            )
        )
    }
    PreviewScaffold {
        ConversationScreenContent(
                uiState = state.value,
                hasAudioPermission = true,
                onRequestAudioPermission = {},
                onRefresh = {},
                onSpeakText = { _, _ -> },
                onDeleteSaved = {},
                onHideSaved = {},
                onSourceLanguageChange = { code -> state.value = state.value.copy(sourceLanguage = code) },
                onTargetLanguageChange = { code -> state.value = state.value.copy(targetLanguage = code) },
                onSwapLanguages = {
                    state.value = state.value.copy(
                        sourceLanguage = state.value.targetLanguage,
                        targetLanguage = state.value.sourceLanguage
                    )
                },
                onAutoPlayToggle = {
                    state.value = state.value.copy(autoPlayTranslation = !state.value.autoPlayTranslation)
                },
                onStartListening = {},
                onStopListening = {},
                onClearConversation = {
                    state.value = state.value.copy(conversationHistory = emptyList())
                },
                modifier = Modifier.fillMaxSize()
        )
    }
}

// (imports block removed - now only at top)

/**
 * Main conversation screen for live translation.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ConversationScreen(
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Permission handling
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PermissionChecker.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            // Permission denied, show error
            viewModel.clearError() // Clear any existing error first
        }
    }
    
    // Show error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // In a real app, you'd show a SnackbarHostState here
            // For now, we'll auto-clear after showing
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
    
    ConversationScreenContent(
        uiState = uiState,
        hasAudioPermission = hasAudioPermission,
        onRequestAudioPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onRefresh = viewModel::refreshConversationHistory,
        onSpeakText = viewModel::speakText,
        onDeleteSaved = viewModel::deleteSavedConversation,
        onHideSaved = viewModel::hideSavedHistory,
        onSourceLanguageChange = viewModel::setSourceLanguage,
        onTargetLanguageChange = viewModel::setTargetLanguage,
        onSwapLanguages = viewModel::swapLanguages,
        onAutoPlayToggle = viewModel::toggleAutoPlay,
        onStartListening = { viewModel.startListening(uiState.sourceLanguage) },
        onStopListening = viewModel::stopListening,
        onClearConversation = viewModel::clearConversation,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
private fun ConversationScreenContent(
    uiState: ConversationUiState,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRefresh: () -> Unit,
    onSpeakText: (String, String) -> Unit,
    onDeleteSaved: (Long) -> Unit,
    onHideSaved: () -> Unit,
    onSourceLanguageChange: (String) -> Unit,
    onTargetLanguageChange: (String) -> Unit,
    onSwapLanguages: () -> Unit,
    onAutoPlayToggle: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClearConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = onRefresh
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Saved history
            if ((uiState.showSavedHistory || uiState.isRefreshing) && uiState.savedHistory.isNotEmpty()) {
                SavedHistorySection(
                    savedHistory = uiState.savedHistory,
                    onSpeakText = onSpeakText,
                    onDelete = onDeleteSaved,
                    onHide = onHideSaved,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Pull hint
            if (!uiState.showSavedHistory && !uiState.isRefreshing && uiState.savedHistory.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Pull down",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pull down to view ${uiState.savedHistory.size} saved translations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Warning banner
            if (!uiState.isValidLanguagePair) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "ML Kit requires English as source or target language",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Language selection and controls
            LanguageSelectionRow(
                sourceLanguage = uiState.sourceLanguage,
                targetLanguage = uiState.targetLanguage,
                onSourceLanguageChange = onSourceLanguageChange,
                onTargetLanguageChange = onTargetLanguageChange,
                onSwapLanguages = onSwapLanguages,
                autoPlayEnabled = uiState.autoPlayTranslation,
                onAutoPlayToggle = onAutoPlayToggle,
                modifier = Modifier.padding(16.dp)
            )

            // Conversation history
            ConversationHistory(
                conversationHistory = uiState.conversationHistory,
                onSpeakText = onSpeakText,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // Speech input area
            SpeechInputArea(
                isListening = uiState.isListening,
                isListeningReady = uiState.isListeningReady,
                isDetectingSpeech = uiState.isDetectingSpeech,
                partialText = uiState.partialSpeechText,
                isTranslating = uiState.isTranslating,
                hasPermission = hasAudioPermission,
                isValidLanguagePair = uiState.isValidLanguagePair,
                onStartListening = {
                    if (hasAudioPermission) onStartListening() else onRequestAudioPermission()
                },
                onStopListening = onStopListening,
                onClearHistory = onClearConversation,
                modifier = Modifier.padding(16.dp)
            )

            // Error display
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Pull refresh indicator
        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

// ---- Preview support ----

private class ConversationUiStatePreviewProvider : PreviewParameterProvider<ConversationUiState> {
    override val values: Sequence<ConversationUiState> = sequenceOf(
        ConversationUiState(),
        ConversationUiState(
            conversationHistory = listOf(
                ConversationTurn("Hello", "Hola", MlKitTranslateLanguage.ENGLISH, MlKitTranslateLanguage.SPANISH),
                ConversationTurn("How are you?", "¿Cómo estás?", MlKitTranslateLanguage.ENGLISH, MlKitTranslateLanguage.SPANISH)
            ),
            autoPlayTranslation = true
        ),
        ConversationUiState(
            sourceLanguage = MlKitTranslateLanguage.FRENCH,
            targetLanguage = MlKitTranslateLanguage.GERMAN
        ),
        ConversationUiState(
            isListening = true,
            isListeningReady = true,
            partialSpeechText = "Bonjour tout le monde",
            sourceLanguage = MlKitTranslateLanguage.FRENCH,
            targetLanguage = MlKitTranslateLanguage.ENGLISH
        ),
        ConversationUiState(
            savedHistory = listOf(
                ConversationTurn("Thanks", "Gracias", MlKitTranslateLanguage.ENGLISH, MlKitTranslateLanguage.SPANISH)
            ),
            showSavedHistory = true
        )
    )
}

// UI-check preview with extreme font scale/RTL and stress states
@UiCheckPreview
@Composable
fun ConversationScreenUiCheckPreview() {
    val longText = "THIS IS A VERY LONG PREVIEW TEXT TO STRESS WRAPPING, ACCESSIBILITY, AND LAYOUT BEHAVIOR IN THE CONVERSATION SCREEN PREVIEW. " +
            "IT SHOULD ENSURE NO CLIPPING OCCURS AND TOUCH TARGETS REMAIN 48DP+ WHERE REQUIRED."
    val state = ConversationUiState(
        conversationHistory = listOf(
            ConversationTurn(
                originalText = longText,
                translatedText = "ESTE ES UN TEXTO MUY LARGO PARA PROBAR EL AJUSTE Y LA ACCESIBILIDAD.",
                sourceLang = MlKitTranslateLanguage.ENGLISH,
                targetLang = MlKitTranslateLanguage.SPANISH
            )
        ),
        // Choose a non-English pair to trigger invalid pair banner
        sourceLanguage = MlKitTranslateLanguage.FRENCH,
        targetLanguage = MlKitTranslateLanguage.GERMAN,
        isListening = false,
        isListeningReady = false,
        isDetectingSpeech = false,
        partialSpeechText = longText,
        isTranslating = false,
        isRefreshing = false,
        savedHistory = listOf(
            ConversationTurn("Thanks", "Gracias", MlKitTranslateLanguage.ENGLISH, MlKitTranslateLanguage.SPANISH)
        ),
        showSavedHistory = true
    )
    PreviewScaffold {
        ConversationScreenContent(
            uiState = state,
            hasAudioPermission = false, // Force permission warning state
            onRequestAudioPermission = {},
            onRefresh = {},
            onSpeakText = { _, _ -> },
            onDeleteSaved = {},
            onHideSaved = {},
            onSourceLanguageChange = {},
            onTargetLanguageChange = {},
            onSwapLanguages = {},
            onAutoPlayToggle = {},
            onStartListening = {},
            onStopListening = {},
            onClearConversation = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Conversation States", showBackground = true)
@PreviewScreenSizes
@MultiDevicePreview
@DesignVariantPreview
@Composable
fun ConversationScreenStatesPreview(
    @PreviewParameter(ConversationUiStatePreviewProvider::class) state: ConversationUiState
) {
    PreviewScaffold {
        ConversationScreenContent(
                uiState = state,
                hasAudioPermission = true,
                onRequestAudioPermission = {},
                onRefresh = {},
                onSpeakText = { _, _ -> },
                onDeleteSaved = {},
                onHideSaved = {},
                onSourceLanguageChange = {},
                onTargetLanguageChange = {},
                onSwapLanguages = {},
                onAutoPlayToggle = {},
                onStartListening = {},
                onStopListening = {},
                onClearConversation = {},
                modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun LanguageSelectionRow(
    sourceLanguage: String,
    targetLanguage: String,
    onSourceLanguageChange: (String) -> Unit,
    onTargetLanguageChange: (String) -> Unit,
    onSwapLanguages: () -> Unit,
    autoPlayEnabled: Boolean,
    onAutoPlayToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sameWarning by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            TwoLanguagePickerButton(
                sourceLanguageCode = sourceLanguage,
                targetLanguageCode = targetLanguage,
                onLanguagesSelected = { from: String, to: String ->
                    sameWarning = from == to
                    if (!sameWarning) {
                        onSourceLanguageChange(from)
                        onTargetLanguageChange(to)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("conversation_languages")
            )
            
            // Auto-play toggle
            IconButton(onClick = onAutoPlayToggle) {
                Icon(
                    if (autoPlayEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (autoPlayEnabled) "Auto-play enabled" else "Auto-play disabled",
                    tint = if (autoPlayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
            if (sameWarning) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "From and To can't be the same.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(
    language: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Text(
                text = getLanguageDisplayName(language),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        modifier = modifier
    )
}

@Composable
private fun ConversationHistory(
    conversationHistory: List<ConversationTurn>,
    onSpeakText: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new items are added
    LaunchedEffect(conversationHistory.size) {
        if (conversationHistory.isNotEmpty()) {
            listState.animateScrollToItem(conversationHistory.size - 1)
        }
    }
    
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(conversationHistory) { turn ->
            ConversationTurnItem(
                turn = turn,
                onSpeakOriginal = { onSpeakText(turn.originalText, turn.sourceLang) },
                onSpeakTranslation = { onSpeakText(turn.translatedText, turn.targetLang) }
            )
        }
    }
}

@Composable
private fun ConversationTurnItem(
    turn: ConversationTurn,
    onSpeakOriginal: () -> Unit,
    onSpeakTranslation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Original text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getLanguageDisplayName(turn.sourceLang),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = turn.originalText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(onClick = onSpeakOriginal) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak original text",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Translated text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getLanguageDisplayName(turn.targetLang),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = turn.translatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(onClick = onSpeakTranslation) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak translation",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeechInputArea(
    isListening: Boolean,
    isListeningReady: Boolean,
    isDetectingSpeech: Boolean,
    partialText: String,
    isTranslating: Boolean,
    hasPermission: Boolean,
    isValidLanguagePair: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Partial text display
        if (partialText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = partialText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Status text
        Text(
            text = when {
                !isValidLanguagePair -> "Invalid language pair (English required)"
                !hasPermission -> "Microphone permission required"
                isTranslating -> "Translating..."
                isDetectingSpeech -> "Listening..."
                isListeningReady -> "Say something"
                isListening -> "Starting..."
                else -> "Tap to start speaking"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (isValidLanguagePair && hasPermission) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear history button
            OutlinedIconButton(
                onClick = onClearHistory,
                enabled = !isListening && !isTranslating,
                modifier = Modifier
                    .testTag("clear_conversation_btn")
                    .semantics { contentDescription = "Clear conversation" }
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null
                )
            }
            
            // Main microphone button
            val micContentDescription = when {
                !isValidLanguagePair -> "Invalid language pair"
                !hasPermission -> "Microphone permission required"
                isDetectingSpeech -> "Detecting speech"
                isListening -> "Stop listening"
                else -> "Start listening"
            }

            FloatingActionButton(
                onClick = if (isListening) onStopListening else onStartListening,
                modifier = Modifier
                    .size(72.dp)
                    .testTag("mic_btn")
                    .semantics { contentDescription = micContentDescription },
                containerColor = when {
                    !isValidLanguagePair -> MaterialTheme.colorScheme.errorContainer
                    !hasPermission -> MaterialTheme.colorScheme.error
                    isDetectingSpeech -> MaterialTheme.colorScheme.error
                    isListening -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                when {
                    !isValidLanguagePair -> {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    isTranslating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    isListening -> {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    !hasPermission -> {
                        Icon(
                            Icons.Default.MicOff,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedHistorySection(
    modifier: Modifier = Modifier,
    savedHistory: List<ConversationTurn>,
    onSpeakText: (String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onHide: () -> Unit,
    isRefreshing: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRefreshing) "Refreshing..." else "Saved Translations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRefreshing) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${savedHistory.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isRefreshing) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onHide,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Hide history",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Scrollable list of saved items
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = savedHistory,
                    key = { it.timestamp }
                ) { turn ->
                    SavedHistoryItem(
                        turn = turn,
                        onSpeakOriginal = { onSpeakText(turn.originalText, turn.sourceLang) },
                        onSpeakTranslation = { onSpeakText(turn.translatedText, turn.targetLang) },
                        onDelete = { onDelete(turn.timestamp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedHistoryItem(
    turn: ConversationTurn,
    onSpeakOriginal: () -> Unit,
    onSpeakTranslation: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Original text with delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getLanguageDisplayName(turn.sourceLang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = turn.originalText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Row {
                    IconButton(onClick = onSpeakOriginal) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            // Translated text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getLanguageDisplayName(turn.targetLang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = turn.translatedText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                IconButton(onClick = onSpeakTranslation) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak translation",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// Helper function to convert language codes to display names
private fun getLanguageDisplayName(languageCode: String): String {
    return when (languageCode) {
        MlKitTranslateLanguage.ENGLISH -> "English"
        MlKitTranslateLanguage.SPANISH -> "Spanish"
        MlKitTranslateLanguage.FRENCH -> "French"
        MlKitTranslateLanguage.GERMAN -> "German"
        MlKitTranslateLanguage.ITALIAN -> "Italian"
        MlKitTranslateLanguage.PORTUGUESE -> "Portuguese"
        MlKitTranslateLanguage.CHINESE -> "Chinese"
        MlKitTranslateLanguage.JAPANESE -> "Japanese"
        MlKitTranslateLanguage.KOREAN -> "Korean"
        MlKitTranslateLanguage.RUSSIAN -> "Russian"
        MlKitTranslateLanguage.ARABIC -> "Arabic"
        MlKitTranslateLanguage.HINDI -> "Hindi"
        else -> languageCode.uppercase()
    }
}

@PreviewScreenSizes
@Composable
private fun ConversationScreenPreview() {
    GlobalTranslationTheme {
        Surface {
            // Preview with mock data would go here
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Conversation Screen Preview")
            }
        }
    }
}
