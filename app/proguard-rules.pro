# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ===== Optimization Configuration =====
# R8 full mode optimizations (compatible with AGP 8.13.1+)
-optimizationpasses 5
-dontpreverify

# Hilt ProGuard rules - allow optimization but keep structure
-keep,allowobfuscation,allowoptimization @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.Module class *
-keep @dagger.hilt.InstallIn class *

# Keep Hilt generated components (R8 needs these for DI)
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class **_HiltModules
-keep class **_HiltComponents { *; }
-keep class **_HiltComponents$*

# Keep Inject constructors and fields (required for DI)
-keepclasseswithmembernames class * {
    @javax.inject.Inject <init>(...);
}
-keepclasseswithmembernames class * {
    @javax.inject.Inject <fields>;
}

# Keep the Application class
-keep class com.example.globaltranslation.GlobalTranslationApplication { *; }

# ML Kit keep rules
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep ML Kit model classes
-keep class com.google.mlkit.nl.translate.** { *; }
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.common.** { *; }

# Keep native method signatures for 16KB page size compatibility
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Room keep rules - minimal rules, let R8 optimize
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Kotlin coroutines - only keep what's needed for reflection
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# DataStore - only keep protobuf classes
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep UI state classes and core models (used in StateFlow)
-keep,allowobfuscation class com.example.globaltranslation.ui.**.*UiState
-keep,allowobfuscation class com.example.globaltranslation.core.model.**
-keep,allowobfuscation class com.example.globaltranslation.core.provider.**

# Data module - rely on consumer ProGuard rules from :data module
# Only keep what's absolutely necessary for reflection/DI
-keep,allowobfuscation class * extends androidx.room.RoomDatabase$Callback

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# OkHttp and Conscrypt (used by Google Play Services)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn com.android.org.conscrypt.**

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile