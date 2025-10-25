package com.example.globaltranslation.ui.camera

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.globaltranslation.ui.components.MultiDevicePreview
import com.example.globaltranslation.ui.components.DesignVariantPreview
import com.example.globaltranslation.ui.components.PreviewScaffold
import com.example.globaltranslation.ui.components.UiCheckPreview
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.core.content.PermissionChecker
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Camera screen for real-time text recognition and translation.
 * Uses CameraX for camera preview and ML Kit for text recognition.
 * Implements lifecycle-aware camera management for smooth navigation.
 */
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PermissionChecker.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    // Track if this screen is actively visible for proper camera lifecycle
    var isScreenActive by remember { mutableStateOf(true) }
    
    DisposableEffect(lifecycleOwner) {
        isScreenActive = true
        onDispose {
            isScreenActive = false
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        when {
            hasCameraPermission -> {
                // State to hold image capture controller
                var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
                
                CameraPreview(
                    onImageCaptureReady = { capture -> imageCapture = capture },
                    isFlashOn = uiState.isFlashOn,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay UI
                var showLanguagePicker by remember { mutableStateOf(false) }
                
                val executor = remember { Executors.newSingleThreadExecutor() }
                
                DisposableEffect(Unit) {
                    onDispose {
                        executor.shutdown()
                    }
                }
                
                CameraOverlay(
                    uiState = uiState,
                    onFlashToggle = { viewModel.toggleFlash() },
                    onLanguagePickerClick = { showLanguagePicker = true },
                    onClearResults = { viewModel.clearResults() },
                    onCaptureClick = {
                        // Capture photo when button is tapped
                        imageCapture?.let { capture ->
                            capture.takePicture(
                                executor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    @androidx.camera.core.ExperimentalGetImage
                                    override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val inputImage = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            viewModel.processCapturedImage(inputImage)
                                        }
                                        imageProxy.close()
                                    }
                                    
                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Language picker dialog
                if (showLanguagePicker) {
                    CameraLanguagePickerDialog(
                        currentSourceLanguage = uiState.sourceLanguageCode,
                        currentTargetLanguage = uiState.targetLanguageCode,
                        onLanguagesSelected = { sourceCode, targetCode ->
                            viewModel.setSourceLanguage(sourceCode)
                            viewModel.setTargetLanguage(targetCode)
                            showLanguagePicker = false
                        },
                        onDismiss = { showLanguagePicker = false }
                    )
                }
            }
            else -> {
                // Permission request UI
                CameraPermissionRequest(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Camera preview component using CameraX.
 * Sets up preview and image capture (no continuous analysis).
 * Implements smooth lifecycle transitions for better UX.
 */
@Composable
private fun CameraPreview(
    onImageCaptureReady: (ImageCapture) -> Unit,
    isFlashOn: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    // Animate the preview appearance for smooth transitions
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "camera_preview_alpha"
    )
    
    // Update flash when isFlashOn changes
    LaunchedEffect(isFlashOn) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }
    
    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            // Preview use case
            val preview = CameraXPreview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            // Image capture use case (for taking photos)
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                // Apply initial flash state
                camera?.cameraControl?.enableTorch(isFlashOn)
                
                // Notify that image capture is ready
                onImageCaptureReady(imageCapture)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
        
        onDispose {
            // Properly unbind all use cases and release camera resources
            try {
                camera?.cameraControl?.enableTorch(false) // Turn off flash
                cameraProvider?.unbindAll()
                camera = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    AndroidView(
        factory = { previewView },
        modifier = modifier.graphicsLayer { this.alpha = alpha }
    )
}

/**
 * Overlay UI for camera controls and detected text display.
 */
@Composable
private fun CameraOverlay(
    uiState: CameraUiState,
    onFlashToggle: () -> Unit,
    onLanguagePickerClick: () -> Unit,
    onClearResults: () -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CameraOverlayContent(
        uiState = uiState,
        onFlashToggle = onFlashToggle,
        onLanguagePickerClick = onLanguagePickerClick,
        onClearResults = onClearResults,
        onCaptureClick = onCaptureClick,
        modifier = modifier
    )
}

@Composable
private fun CameraOverlayContent(
    uiState: CameraUiState,
    onFlashToggle: () -> Unit,
    onLanguagePickerClick: () -> Unit,
    onClearResults: () -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate overlay entrance for smooth UX
    val overlayAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "overlay_alpha"
    )
    
    Column(
        modifier = modifier
            .padding(16.dp)
            .graphicsLayer { alpha = overlayAlpha },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash toggle
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (uiState.isFlashOn) "Flash On" else "Flash Off",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Language selector - Shows source -> target with improved styling
            val languageLabel = "${getLanguageName(uiState.sourceLanguageCode)} → ${getLanguageName(uiState.targetLanguageCode)}"
            Surface(
                onClick = onLanguagePickerClick,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .testTag("camera_language_chip")
                    .semantics { contentDescription = "Select languages ($languageLabel)" }
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
                        text = languageLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Document-style translation display (merged content) with smooth animation
        AnimatedVisibility(
            visible = uiState.detectedTextBlocks.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(400)) + 
                    expandVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(300)) + 
                   shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Take remaining space
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header with clear button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Translation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onClearResults,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear results",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Scrollable document view - merge all translations
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Merge all original text into one document
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Original",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.detectedTextBlocks
                                        .joinToString("\n\n") { it.originalText },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Merge all translations into one document
                        item {
                            HorizontalDivider()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Translation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.detectedTextBlocks
                                        .mapNotNull { it.translatedText }
                                        .joinToString("\n\n"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Processing indicator with smooth animation
        AnimatedVisibility(
            visible = uiState.isProcessing,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Error message or info card with smooth transitions
        AnimatedVisibility(
            visible = uiState.error != null,
            enter = fadeIn(animationSpec = tween(300)) + 
                    expandVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200)) + 
                   shrinkVertically(animationSpec = tween(200))
        ) {
            uiState.error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Download models for: ${getLanguageName(uiState.sourceLanguageCode)} and ${getLanguageName(uiState.targetLanguageCode)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Help message when no text is detected with smooth animation
        AnimatedVisibility(
            visible = uiState.detectedTextBlocks.isEmpty() && !uiState.isProcessing && !uiState.isFrozen && uiState.error == null,
            enter = fadeIn(animationSpec = tween(400)) + 
                    expandVertically(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(300)) + 
                   shrinkVertically(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Point camera at text",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap the button below to capture and translate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Capture/Resume button at the bottom (Google Lens style) with smooth transitions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = uiState.isFrozen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) togetherWith fadeOut(animationSpec = tween(200)) + scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )
                },
                label = "capture_button_transition"
            ) { isFrozen ->
                if (!isFrozen) {
                    // Capture button
                    FloatingActionButton(
                        onClick = onCaptureClick,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Capture and translate",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    // Resume camera button
                    FloatingActionButton(
                        onClick = onClearResults,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Resume camera",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Camera permission request UI.
 */
@Composable
private fun CameraPermissionRequest(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To use camera translation, please grant camera permission.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

/**
 * Language picker dialog for camera translation.
 * Allows selecting source and target languages.
 */
@Composable
private fun CameraLanguagePickerDialog(
    currentSourceLanguage: String,
    currentTargetLanguage: String,
    onLanguagesSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSource by remember { mutableStateOf(currentSourceLanguage) }
    var selectedTarget by remember { mutableStateOf(currentTargetLanguage) }
    val isSameLanguageSelected by remember {
        derivedStateOf { selectedSource == selectedTarget }
    }
    
    // Use string literals for preview compatibility (ML Kit constants are "en", "es", etc.)
    val languages = remember {
        mapOf(
            "en" to "English",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "it" to "Italian",
            "pt" to "Portuguese",
            "zh" to "Chinese",
            "ja" to "Japanese",
            "ko" to "Korean",
            "ru" to "Russian",
            "ar" to "Arabic",
            "hi" to "Hindi"
        )
    }
    val languageEntries = remember(languages) { languages.entries.toList() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Languages",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            // Swap selected languages quickly
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
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Single smooth-scrolling list for both sections
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
                    items(languageEntries, key = { it.key }) { (code, name) ->
                        LanguageSelectionCard(
                            name = name,
                            isSelected = code == selectedSource,
                            onClick = { selectedSource = code }
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
                    items(languageEntries, key = { it.key + "_target" }) { (code, name) ->
                        LanguageSelectionCard(
                            name = name,
                            isSelected = code == selectedTarget,
                            onClick = { selectedTarget = code }
                        )
                    }
                }
                
                // Tiny warning when both selections are the same
                if (isSameLanguageSelected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
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

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onLanguagesSelected(selectedSource, selectedTarget) },
                        enabled = !isSameLanguageSelected,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
                        Text("Apply")
                    }
                }
            }
        }
    }
}

/**
 * Modern language selection card with animation.
 */
@Composable
private fun LanguageSelectionCard(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.98f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ---- Preview support ----

private class CameraUiStatePreviewProvider : PreviewParameterProvider<CameraUiState> {
    override val values: Sequence<CameraUiState> = sequenceOf(
        CameraUiState(),
        CameraUiState(isFlashOn = true),
        CameraUiState(
            detectedTextBlocks = listOf(
                DetectedTextBlock("HELLO WORLD", "HOLA MUNDO", Rect(0f, 0f, 10f, 10f)),
                DetectedTextBlock("WELCOME", "BIENVENIDO", Rect(0f, 0f, 10f, 10f))
            ),
            isFrozen = true
        ),
        CameraUiState(error = "Models missing. Connect to WiFi to download.")
    )
}

@Preview(name = "Camera Overlay States", showBackground = true)
@PreviewScreenSizes
@MultiDevicePreview
@DesignVariantPreview
@Composable
fun CameraOverlayStatesPreview(
    @PreviewParameter(CameraUiStatePreviewProvider::class) state: CameraUiState
) {
    PreviewScaffold {
        // Using a simple placeholder background to mimic camera backdrop
        Box(modifier = Modifier.fillMaxSize()) {
            CameraOverlayContent(
                uiState = state,
                onFlashToggle = {},
                onLanguagePickerClick = {},
                onClearResults = {},
                onCaptureClick = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Live interactive overlay preview (no actual camera). Simulates flash, language change, capture, and clear.
@Preview(name = "Camera Overlay Live", showBackground = true)
@MultiDevicePreview
@DesignVariantPreview
@Composable
fun CameraOverlayLivePreview() {
    val state = remember { mutableStateOf(CameraUiState()) }

    fun cycleLanguages() {
        // Use string literals instead of ML Kit constants for preview compatibility
        val next = when (state.value.targetLanguageCode) {
            "es" -> "fr"  // Spanish -> French
            "fr" -> "de"  // French -> German
            else -> "es"  // Default to Spanish
        }
        state.value = state.value.copy(targetLanguageCode = next)
    }

    fun simulateCapture() {
        state.value = state.value.copy(
            isFrozen = true,
            isProcessing = false,
            error = null,
            detectedTextBlocks = listOf(
                DetectedTextBlock("HELLO WORLD", "HOLA MUNDO", Rect(0f, 0f, 100f, 40f)),
                DetectedTextBlock("WELCOME", "BIENVENIDO", Rect(0f, 50f, 120f, 90f))
            )
        )
    }

    fun clear() {
        state.value = state.value.copy(
            detectedTextBlocks = emptyList(),
            isFrozen = false,
            isProcessing = false,
            error = null
        )
    }

    PreviewScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraOverlayContent(
                uiState = state.value,
                onFlashToggle = { state.value = state.value.copy(isFlashOn = !state.value.isFlashOn) },
                onLanguagePickerClick = { cycleLanguages() },
                onClearResults = { clear() },
                onCaptureClick = { simulateCapture() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// UI-check preview with extreme text lengths and frozen state
@UiCheckPreview
@Composable
fun CameraOverlayUiCheckPreview() {
    val state = CameraUiState(
        isFlashOn = true,
        isFrozen = true,
        detectedTextBlocks = listOf(
            DetectedTextBlock(
                originalText = "THIS IS AN EXTREMELY LONG BLOCK OF ORIGINAL TEXT INTENDED TO STRESS WRAPPING, SPACING, AND SCROLLING IN THE DOCUMENT VIEW.",
                translatedText = "ESTE ES UN BLOQUE EXTREMADAMENTE LARGO DE TEXTO ORIGINAL DESTINADO A PROBAR EL AJUSTE DE LÍNEA, ESPACIADO Y DESPLAZAMIENTO.",
                boundingBox = Rect(0f, 0f, 200f, 100f)
            ),
            DetectedTextBlock(
                originalText = "SECOND LONG PARAGRAPH WITH MORE CONTENT TO SEE HOW MULTIPLE ITEMS ARE MERGED AND DISPLAYED IN THE PREVIEW.",
                translatedText = "SEGUNDO PÁRRAFO LARGO CON MÁS CONTENIDO PARA VER CÓMO SE COMBINAN Y MUESTRAN VARIOS ELEMENTOS EN LA VISTA.",
                boundingBox = Rect(0f, 110f, 200f, 200f)
            )
        )
    )
    PreviewScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraOverlayContent(
                uiState = state,
                onFlashToggle = {},
                onLanguagePickerClick = {},
                onClearResults = {},
                onCaptureClick = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Helper function to get language display name.
 * Uses string literals for preview compatibility (ML Kit constants are "en", "es", etc.)
 */
private fun getLanguageName(languageCode: String): String {
    return when (languageCode) {
        "en" -> "English"
        "es" -> "Spanish"
        "fr" -> "French"
        "de" -> "German"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        "zh" -> "Chinese"
        "ja" -> "Japanese"
        "ko" -> "Korean"
        "ru" -> "Russian"
        "ar" -> "Arabic"
        "hi" -> "Hindi"
        else -> languageCode
    }
}

