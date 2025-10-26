package com.example.globaltranslation.ui.languages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.globaltranslation.ui.theme.GlobalTranslationTheme
import com.example.globaltranslation.ui.components.MultiDevicePreview
import com.example.globaltranslation.ui.components.DesignVariantPreview
import com.example.globaltranslation.ui.components.PreviewScaffold
import com.example.globaltranslation.ui.components.UiCheckPreview
import com.google.mlkit.nl.translate.TranslateLanguage
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Screen for managing translation language models.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    modifier: Modifier = Modifier,
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Show error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearError()
        }
    }

    LanguageScreenContent(
        uiState = uiState,
        onRefresh = viewModel::refreshLanguages,
        onToggleCellularDownloads = viewModel::toggleCellularDownloads,
        onDownloadLanguage = viewModel::downloadLanguage,
        onDeleteLanguage = viewModel::deleteLanguage,
        onCancelDownload = viewModel::cancelDownload,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageScreenContent(
    uiState: LanguageUiState,
    onRefresh: () -> Unit,
    onToggleCellularDownloads: () -> Unit,
    onDownloadLanguage: (String) -> Unit,
    onDeleteLanguage: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        LanguageScreenHeader(
            isLoading = uiState.isLoading,
            onRefresh = onRefresh
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Popular language pairs carousel
        PopularLanguagePairsCarousel(
            availableLanguages = uiState.availableLanguages,
            onLanguagePairSelected = { (fromLang, toLang) ->
                // Start downloading both models for the selected pair
                // ViewModel will check if already downloaded and skip if needed
                onDownloadLanguage(fromLang)
                onDownloadLanguage(toLang)
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Language Models",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Download language models to enable offline translation. All models are paired with English (e.g., English\u2194Spanish).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Network status indicator
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (uiState.networkState) {
                            is com.example.globaltranslation.data.network.NetworkState.WiFi -> Icons.Default.Wifi
                            is com.example.globaltranslation.data.network.NetworkState.Cellular -> Icons.Default.SignalCellularAlt
                            else -> Icons.Default.SignalWifiOff
                        },
                        contentDescription = "Network status",
                        modifier = Modifier.size(16.dp),
                        tint = when (uiState.networkState) {
                            is com.example.globaltranslation.data.network.NetworkState.WiFi -> MaterialTheme.colorScheme.primary
                            is com.example.globaltranslation.data.network.NetworkState.Cellular -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (uiState.networkState) {
                            is com.example.globaltranslation.data.network.NetworkState.WiFi -> "WiFi connected"
                            is com.example.globaltranslation.data.network.NetworkState.Cellular -> "Cellular connection"
                            is com.example.globaltranslation.data.network.NetworkState.Connected -> "Connected"
                            else -> "No connection"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Settings card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Allow Cellular Downloads",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (uiState.allowCellularDownloads) 
                            "Models can download on mobile data" 
                        else 
                            "Models require WiFi connection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Switch(
                    checked = uiState.allowCellularDownloads,
                    onCheckedChange = { onToggleCellularDownloads() },
                    modifier = Modifier
                        .testTag("cellular_downloads_switch")
                        .semantics { contentDescription = "Toggle cellular downloads" }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Language list
        if (uiState.isLoading && uiState.availableLanguages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.testTag("languages_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(uiState.availableLanguages) { language ->
                    LanguageModelItem(
                        language = language,
                        onDownload = { onDownloadLanguage(language.code) },
                        onDelete = { onDeleteLanguage(language.code) },
                        onCancel = { onCancelDownload(language.code) }
                    )
                }
            }
        }
        
        // Error display
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun LanguageScreenHeader(
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Language Models",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage offline translation models",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(
            onClick = onRefresh,
            enabled = !isLoading,
            modifier = Modifier
                .testTag("languages_refresh_btn")
                .semantics { contentDescription = "Refresh languages" }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun LanguageModelItem(
    language: LanguageModel,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("language_item_${language.code}")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Language flag or icon placeholder
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(end = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Column {
                        Text(
                            text = language.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when {
                                language.isDownloaded -> "Downloaded"
                                language.isDownloading -> "Downloading..."
                                else -> "Not downloaded"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                language.isDownloaded -> MaterialTheme.colorScheme.primary
                                language.isDownloading -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                // Action button
                when {
                    language.code == TranslateLanguage.ENGLISH -> {
                        // English is always available
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Always available",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    language.isDownloading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .testTag("cancel_download_${language.code}")
                                    .semantics {
                                        contentDescription = "Cancel ${language.name} download"
                                    }
                            ) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                    language.isDownloaded -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .testTag("remove_language_${language.code}")
                                    .semantics {
                                        contentDescription = "Remove ${language.name}"
                                    }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove")
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier
                                .testTag("download_language_${language.code}")
                                .semantics {
                                    contentDescription = "Download ${language.name}"
                                },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download")
                        }
                    }
                }
            }
        
        // Show progress bar with actual percentage and status
        if (language.isDownloading || language.downloadStatus != com.example.globaltranslation.ui.languages.DownloadStatus.IDLE) {
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (language.downloadStatus) {
                            com.example.globaltranslation.ui.languages.DownloadStatus.PREPARING -> "Preparing download..."
                            com.example.globaltranslation.ui.languages.DownloadStatus.DOWNLOADING -> "Downloading model..."
                            com.example.globaltranslation.ui.languages.DownloadStatus.FINALIZING -> "Finalizing..."
                            com.example.globaltranslation.ui.languages.DownloadStatus.COMPLETE -> "Complete!"
                            com.example.globaltranslation.ui.languages.DownloadStatus.PAUSED -> "Paused (waiting for network)"
                            com.example.globaltranslation.ui.languages.DownloadStatus.FAILED -> "Failed"
                            else -> "Downloading..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (language.downloadStatus) {
                            com.example.globaltranslation.ui.languages.DownloadStatus.COMPLETE -> MaterialTheme.colorScheme.primary
                            com.example.globaltranslation.ui.languages.DownloadStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
                            com.example.globaltranslation.ui.languages.DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                    
                    // Show percentage if available
                    if (language.downloadProgress != null) {
                        Text(
                            text = "${(language.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = "~10-30 MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                // Progress bar - determinate if we have progress, indeterminate otherwise
                if (language.downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = { language.downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = when (language.downloadStatus) {
                            com.example.globaltranslation.ui.languages.DownloadStatus.COMPLETE -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopularLanguagePairsCarousel(
    availableLanguages: List<LanguageModel>,
    onLanguagePairSelected: (Pair<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val popularPairs = listOf(
        "en" to "es",  // English to Spanish
        "en" to "fr",  // English to French
        "en" to "de",  // English to German
        "en" to "zh",  // English to Chinese
        "en" to "ja",  // English to Japanese
        "es" to "fr",  // Spanish to French
        "fr" to "de"   // French to German
    )
    
    val carouselState = rememberCarouselState(itemCount = { popularPairs.size })
    
    HorizontalCenteredHeroCarousel(
        state = carouselState,
        modifier = modifier
    ) { index ->
        val pair = popularPairs[index]
        
        // Check if both languages are downloaded
        val fromLangDownloaded = availableLanguages.find { it.code == pair.first }?.isDownloaded ?: false
        val toLangDownloaded = availableLanguages.find { it.code == pair.second }?.isDownloaded ?: false
        val bothDownloaded = fromLangDownloaded && toLangDownloaded
        
        Card(
            onClick = { onLanguagePairSelected(pair) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (bothDownloaded) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pair.first.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (bothDownloaded) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (bothDownloaded) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = pair.second.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (bothDownloaded) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (bothDownloaded) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (bothDownloaded) 
                            "Downloaded - Ready to use" 
                        else 
                            "Tap to download",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bothDownloaded) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun LanguageScreenPreview() {
    PreviewScaffold {
        // Basic empty preview for safety
        LanguageScreenContent(
                uiState = LanguageUiState(),
                onRefresh = {},
                onToggleCellularDownloads = {},
                onDownloadLanguage = {},
                onDeleteLanguage = {},
                onCancelDownload = {},
                modifier = Modifier.fillMaxSize()
        )
    }
}

// ---- Preview support for LanguageScreen ----

private class LanguageUiStatePreviewProvider : PreviewParameterProvider<LanguageUiState> {
    override val values: Sequence<LanguageUiState> = sequenceOf(
        // Loading state
        LanguageUiState(isLoading = true),
        // Some downloaded, some not
        LanguageUiState(
            availableLanguages = listOf(
                LanguageModel("en", "English", isDownloaded = true, isDownloading = false),
                LanguageModel("es", "Spanish", isDownloaded = true, isDownloading = false),
                LanguageModel("fr", "French", isDownloaded = false, isDownloading = false),
                LanguageModel("de", "German", isDownloaded = false, isDownloading = false)
            )
        ),
        // Active download with progress
        LanguageUiState(
            availableLanguages = listOf(
                LanguageModel("en", "English", isDownloaded = true, isDownloading = false),
                LanguageModel(
                    code = "ja", name = "Japanese",
                    isDownloaded = false, isDownloading = true,
                    downloadProgress = 0.42f, downloadStatus = DownloadStatus.DOWNLOADING
                )
            )
        ),
        // Error state
        LanguageUiState(
            availableLanguages = listOf(
                LanguageModel("en", "English", isDownloaded = true, isDownloading = false)
            ),
            error = "WiFi required. Will retry on WiFi or enable cellular downloads."
        )
    )
}

@Preview(name = "Language States", showBackground = true)
@PreviewScreenSizes
@MultiDevicePreview
@DesignVariantPreview
@Composable
fun LanguageScreenStatesPreview(
    @PreviewParameter(LanguageUiStatePreviewProvider::class) state: LanguageUiState
) {
    PreviewScaffold {
        LanguageScreenContent(
                uiState = state,
                onRefresh = {},
                onToggleCellularDownloads = {},
                onDownloadLanguage = {},
                onDeleteLanguage = {},
                onCancelDownload = {},
                modifier = Modifier.fillMaxSize()
        )
    }
}

// Live interactive preview for quick interactions
@Preview(name = "Language Live", showBackground = true)
@MultiDevicePreview
@DesignVariantPreview
@Composable
fun LanguageScreenLivePreview() {
    val initial = remember {
        LanguageUiState(
            availableLanguages = listOf(
                LanguageModel("en", "English", isDownloaded = true, isDownloading = false),
                LanguageModel("es", "Spanish", isDownloaded = true, isDownloading = false),
                LanguageModel("fr", "French", isDownloaded = false, isDownloading = false),
                LanguageModel("de", "German", isDownloaded = false, isDownloading = false),
                LanguageModel("ja", "Japanese", isDownloaded = false, isDownloading = false)
            ),
            allowCellularDownloads = false
        )
    }
    val state = remember { mutableStateOf(initial) }
    
    PreviewScaffold {
        LanguageScreenContent(
                uiState = state.value,
                onRefresh = {
                    // Simulate refresh toggling a downloaded flag
                    state.value = state.value.copy(
                        availableLanguages = state.value.availableLanguages.map {
                            if (it.code == "fr") it.copy(isDownloaded = !it.isDownloaded) else it
                        }
                    )
                },
                onToggleCellularDownloads = {
                    state.value = state.value.copy(allowCellularDownloads = !state.value.allowCellularDownloads)
                },
                onDownloadLanguage = { code ->
                    state.value = state.value.copy(
                        availableLanguages = state.value.availableLanguages.map {
                            if (it.code == code) it.copy(isDownloading = true, downloadStatus = DownloadStatus.DOWNLOADING, downloadProgress = 0.4f) else it
                        }
                    )
                },
                onDeleteLanguage = { code ->
                    state.value = state.value.copy(
                        availableLanguages = state.value.availableLanguages.map {
                            if (it.code == code) it.copy(isDownloaded = false) else it
                        }
                    )
                },
                onCancelDownload = { code ->
                    state.value = state.value.copy(
                        availableLanguages = state.value.availableLanguages.map {
                            if (it.code == code) it.copy(isDownloading = false, downloadProgress = null, downloadStatus = DownloadStatus.PAUSED) else it
                        }
                    )
                },
                modifier = Modifier.fillMaxSize()
        )
    }
}

// UI-check preview to stress layout with extreme font scale/RTL and mixed states
@UiCheckPreview
@Composable
fun LanguageScreenUiCheckPreview() {
    val state = LanguageUiState(
        isLoading = false,
        allowCellularDownloads = true,
        availableLanguages = listOf(
            LanguageModel("en", "English", isDownloaded = true, isDownloading = false),
            LanguageModel("es", "Spanish", isDownloaded = true, isDownloading = false),
            LanguageModel(
                code = "fr", name = "French",
                isDownloaded = false, isDownloading = true,
                downloadProgress = 0.65f, downloadStatus = DownloadStatus.DOWNLOADING
            ),
            LanguageModel(
                code = "de", name = "German",
                isDownloaded = false, isDownloading = false
            )
        ),
        error = "WiFi required for first-time downloads. Enable cellular downloads if needed."
    )
    PreviewScaffold {
        LanguageScreenContent(
            uiState = state,
            onRefresh = {},
            onToggleCellularDownloads = {},
            onDownloadLanguage = {},
            onDeleteLanguage = {},
            onCancelDownload = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

