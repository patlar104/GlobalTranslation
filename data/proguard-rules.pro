# ProGuard rules for :data module
# Keep all public APIs that are used by :app module

# Keep all data module classes
-keep class com.example.globaltranslation.data.** { *; }
-keep interface com.example.globaltranslation.data.** { *; }

# Room ProGuard rules
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }

# Keep Room generated classes
-keep class **_Impl { *; }
-keep class **Dao_Impl { *; }

# Hilt ProGuard rules for data module
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep class * extends dagger.internal.Factory { *; }

# Keep injection constructors
-keepclasseswithmembernames class * {
    @javax.inject.Inject <init>(...);
}

# DataStore
-keep class androidx.datastore.*.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ML Kit ProGuard rules
# Keep all ML Kit model classes
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }

# Keep ML Kit translation API
-keep class com.google.mlkit.nl.translate.** { *; }
-dontwarn com.google.mlkit.nl.translate.**

# Keep ML Kit text recognition API
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.vision.text.chinese.** { *; }
-keep class com.google.mlkit.vision.text.devanagari.** { *; }
-keep class com.google.mlkit.vision.text.japanese.** { *; }
-keep class com.google.mlkit.vision.text.korean.** { *; }
-keep class com.google.mlkit.vision.text.latin.** { *; }
-dontwarn com.google.mlkit.vision.text.**

# Keep ML Kit common classes
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-dontwarn com.google.mlkit.common.**

# Keep Android TTS and Speech Recognition
-keep class android.speech.** { *; }
-keep class android.speech.tts.** { *; }

# Keep Kotlin coroutines for ML Kit
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep R8 optimizations for ML Kit
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
