package com.example.globaltranslation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.globaltranslation.util.DeviceCompatibility
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.globaltranslation.ui.camera.CameraScreen
import com.example.globaltranslation.ui.conversation.ConversationScreen
import com.example.globaltranslation.ui.languages.LanguageScreen
import com.example.globaltranslation.ui.textinput.TextInputScreen
import com.example.globaltranslation.ui.theme.GlobalTranslationTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Log device compatibility information for 16KB page size support
        DeviceCompatibility.logPageSizeInfo()
        
        setContent {
            GlobalTranslationTheme {
                GloabTranslationApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun GloabTranslationApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CONVERSATION) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // Animated content transition between screens with improved animations
            // Strategy:
            // - Camera screen: Use fade transitions for smooth camera lifecycle management
            // - Other screens: Use slide + fade with spring physics for polished feel
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        // Use different animations based on screen type
                        // Camera screen uses fade for smoother transitions
                        val isCameraInvolved = targetState == AppDestinations.CAMERA || 
                                              initialState == AppDestinations.CAMERA
                        
                        if (isCameraInvolved) {
                            // Smooth fade for camera transitions to avoid jarring camera start/stop
                            // Longer duration (400ms) provides smooth visual continuity
                            fadeIn(
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) togetherWith fadeOut(
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            )
                        } else {
                            // Smooth slide + fade for other screens
                            // Spring animations create natural, bouncy motion
                            slideInHorizontally(
                                initialOffsetX = { it / 4 }, // Reduced from 1/3 to 1/4 for subtler slide
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { -it / 4 }, // Reduced from 1/3 to 1/4 for subtler slide
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeOut(
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            )
                        }
                    },
                    label = "screen_transition"
                ) { destination ->
                    when (destination) {
                        AppDestinations.CONVERSATION -> {
                            ConversationScreen(
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppDestinations.TEXT_INPUT -> {
                            TextInputScreen(
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppDestinations.CAMERA -> {
                            CameraScreen(
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppDestinations.LANGUAGES -> {
                            LanguageScreen(
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    CONVERSATION("Conversation", Icons.Filled.Mic),
    TEXT_INPUT("Text Input", Icons.Filled.Translate),
    CAMERA("Camera", Icons.Filled.CameraAlt),
    LANGUAGES("Languages", Icons.Filled.Language),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GlobalTranslationTheme {
        Greeting("Android")
    }
}
