plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.example.globaltranslation.data"
    compileSdk = 36
    
    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Ensure all ABIs are properly configured for 16KB page size
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    buildFeatures {
        buildConfig = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs = listOf("-Xannotation-default-target=param-property")
        }
    }
    
    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false  // Don't minify library modules - let app module handle it
            proguardFiles("proguard-rules.pro")
            consumerProguardFiles("consumer-rules.pro")
        }

        // 16KB page size testing build variant (matches app module)
        create("sixteenKB") {
            initWith(getByName("debug"))
            isMinifyEnabled = false
            matchingFallbacks += listOf("debug")
        }
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    lint {
        // Enable additional lint checks for better code quality
        abortOnError = false
        checkDependencies = true

        // Generate reports
        xmlReport = true
        htmlReport = true
        textReport = true

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
            "MemberExtensionConflict"
        )

        // Set specific checks to informational severity
        informational += setOf(
            "SyntheticAccessor",
            "SelectableText",
            "DuplicateStrings",
            "TypographyQuotes",
            "UnusedIds",
            "StringFormatTrivial",
            "WrongThreadInterprocedural"
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))
    
    // Android core
    implementation(libs.androidx.core.ktx)
    
    // ML Kit
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.mlkit.text.recognition.japanese)
    implementation(libs.mlkit.text.recognition.korean)
    implementation(libs.mlkit.text.recognition.devanagari)
    implementation(libs.kotlinx.coroutines.play.services)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}

