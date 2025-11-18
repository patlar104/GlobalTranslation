package com.example.globaltranslation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.globaltranslation.ui.languages.SupportedLanguage
import com.example.globaltranslation.ui.languages.getSupportedLanguages

/**
 * Language picker dialog component that can be reused across screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerDialog(
    modifier: Modifier = Modifier,
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Language"
) {
    val supportedLanguages = remember { getSupportedLanguages() }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Language list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(supportedLanguages) { language ->
                        LanguagePickerItem(
                            language = language,
                            isSelected = language.code == selectedLanguageCode,
                            onClick = {
                                onLanguageSelected(language.code)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified two-language picker dialog with swap and same-language prevention.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualLanguagePickerDialog(
    modifier: Modifier = Modifier,
    currentSourceLanguage: String,
    currentTargetLanguage: String,
    onLanguagesSelected: (String, String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Languages"
) {
    var selectedSource by remember { mutableStateOf(currentSourceLanguage) }
    var selectedTarget by remember { mutableStateOf(currentTargetLanguage) }
    val isSame by remember { derivedStateOf { selectedSource == selectedTarget } }

    val supportedLanguages = remember { getSupportedLanguages() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header with swap + close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val tmp = selectedSource
                            selectedSource = selectedTarget
                            selectedTarget = tmp
                        }) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap languages",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Combined smooth-scrolling list for both sections
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    item {
                        Text(
                            text = "From",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(supportedLanguages, key = { it.code }) { language ->
                        LanguagePickerItem(
                            language = language,
                            isSelected = language.code == selectedSource,
                            onClick = { selectedSource = language.code }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    item {
                        Text(
                            text = "To",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(supportedLanguages, key = { it.code + "_to" }) { language ->
                        LanguagePickerItem(
                            language = language,
                            isSelected = language.code == selectedTarget,
                            onClick = { selectedTarget = language.code }
                        )
                    }
                }

                if (isSame) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onLanguagesSelected(selectedSource, selectedTarget) }, enabled = !isSame) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

/**
 * Single chip that shows "From → To" and opens DualLanguagePickerDialog.
 */
@Composable
fun TwoLanguagePickerButton(
    sourceLanguageCode: String,
    targetLanguageCode: String,
    onLanguagesSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Select Languages"
) {
    var showDialog by remember { mutableStateOf(false) }
    val languages = remember { getSupportedLanguages() }
    val label = remember(sourceLanguageCode, targetLanguageCode) {
        val from = languages.find { it.code == sourceLanguageCode }?.name ?: sourceLanguageCode.uppercase()
        val to = languages.find { it.code == targetLanguageCode }?.name ?: targetLanguageCode.uppercase()
        "$from → $to"
    }

    Surface(
        onClick = { showDialog = true },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    if (showDialog) {
        DualLanguagePickerDialog(
            currentSourceLanguage = sourceLanguageCode,
            currentTargetLanguage = targetLanguageCode,
            onLanguagesSelected = { source, target ->
                onLanguagesSelected(source, target)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            title = title
        )
    }
}

@Composable
private fun LanguagePickerItem(
    language: SupportedLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Language icon placeholder
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                Text(
                    text = language.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Language picker button that opens the dialog when clicked.
 */
@Composable
fun LanguagePickerButton(
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Select Language"
) {
    var showDialog by remember { mutableStateOf(false) }
    
    val selectedLanguageName = remember(selectedLanguageCode) {
        getSupportedLanguages().find { it.code == selectedLanguageCode }?.name 
            ?: selectedLanguageCode.uppercase()
    }
    
    FilterChip(
        selected = false,
        onClick = { showDialog = true },
        label = {
            Text(
                text = selectedLanguageName,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Language,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier
    )
    
    if (showDialog) {
        LanguagePickerDialog(
            selectedLanguageCode = selectedLanguageCode,
            onLanguageSelected = onLanguageSelected,
            onDismiss = { showDialog = false },
            title = title
        )
    }
}
