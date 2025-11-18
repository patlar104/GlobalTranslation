plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Pure Kotlin, no Android dependencies
    implementation(libs.kotlinx.coroutines.core)
    
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

