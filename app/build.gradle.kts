plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.globaltranslation"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.globaltranslation"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.globaltranslation.HiltTestRunner"
        
        ndk {
            // Ensure all ABIs are properly configured for 16KB page size
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    signingConfigs {
        create("release") {
            // ⚠️ TEMPORARY CONFIGURATION: Optional signing for debugging purposes
            // ⚠️ This allows building without a keystore for testing
            // ⚠️ TODO: Add proper release keystore for production
            //
            // For production releases:
            // 1. Generate a release keystore: keytool -genkey -v -keystore release.keystore
            // 2. Store credentials securely (e.g., environment variables, CI/CD secrets)
            // 3. Update this configuration to use the release keystore
            //
            // Optional debug keystore configuration (only used if file exists)
            val debugKeystorePath = "${System.getProperty("user.home")}/.android/debug.keystore"
            val debugKeystoreFile = file(debugKeystorePath)
            
            if (debugKeystoreFile.exists()) {
                storeFile = debugKeystoreFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
            // If keystore doesn't exist, signing config remains empty (unsigned build for testing)
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            // Removed applicationIdSuffix to prevent package name conflicts

            // Enable code shrinking in debug for faster iteration
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            // Only use signing config if keystore exists, otherwise create unsigned build
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile?.exists() == true) {
                signingConfig = releaseSigningConfig
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // R8 optimizations for better performance
            ndk {
                debugSymbolLevel = "FULL"
            }
        }

        // 16KB page size testing build variant
        create("sixteenKB") {
            initWith(getByName("debug"))
            isDebuggable = true
            applicationIdSuffix = ".sixteenkb"
            matchingFallbacks += listOf("debug")
        }
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }

    lint {
        // Enable additional lint checks for better code quality
        abortOnError = false
        checkDependencies = true

        // Use custom lint configuration from lint.xml
        checkReleaseBuilds = true

        // Generate reports
        xmlReport = true
        htmlReport = true
        textReport = false

        // Enable baseline for incremental improvements
        baseline = file("lint-baseline.xml")

        // Warning configuration
        warningsAsErrors = false

        // Enable all additional checks from libraries
        checkAllWarnings = true

        // Explicitly disable only truly problematic checks
        disable += setOf(
            "LintError" // Disable internal lint errors that may occur with new AGP versions
        )

        // Enable important checks explicitly
        enable += setOf(
            "StopShip",
            "LogConditional",
            "ExpensiveAssertion",
            "SyntheticAccessor",
            "KotlinPropertyAccess",
            "NoHardKeywords",
            "UnknownNullness",
            "AppCompatMethod",
            "ThreadConstraint",
            "SelectableText",
            "LambdaLast",
            "NoOp",
            "EasterEgg",
            "MemberExtensionConflict"
        )

        // Set specific checks to error severity
        error += setOf(
            "StopShip",
            "ThreadConstraint"
        )

        // Set specific checks to warning severity
        warning += setOf(
            "OldTargetApi",
            "LogConditional",
            "ExpensiveAssertion",
            "KotlinPropertyAccess",
            "NoHardKeywords",
            "UnknownNullness",
            "AppCompatMethod",
            "NegativeMargin",
            "LambdaLast",
            "NoOp",
            "EasterEgg",
            "Registered",
            "PermissionNamingConvention",
            "MangledCRLF",
            "MemberExtensionConflict",
            "ComposableLambdaParameterNaming",
            "ComposableLambdaParameterPosition",
            "UnsupportedChromeOsHardware"
        )

        // Set specific checks to informational severity
        informational += setOf(
            "SyntheticAccessor",
            "SelectableText",
            "DuplicateStrings",
            "TypographyQuotes",
            "ConvertToWebp",
            "UnusedIds",
            "IconExpectedSize",
            "StringFormatTrivial",
            "WrongThreadInterprocedural",
            "AppLinksAutoVerify",
            "KotlincFE10",
            "MinSdkTooLow"
        )
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material)
    
    // CameraX dependencies with Compose support
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.compose)
    
    // ML Kit (for TranslateLanguage constants used in UI and InputImage for camera)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.text.recognition)
    
    // Material Icons (required since Compose BOM 2025.10.00)
    implementation(libs.androidx.compose.material.icons.core)
    
    // Hilt dependencies
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
