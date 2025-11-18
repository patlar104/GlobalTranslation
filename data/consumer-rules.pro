# Consumer ProGuard rules for :data module
# These rules are applied to consuming modules (:app) to preserve :data module APIs

# Keep all public classes and interfaces that might be referenced by :app
-keep public class com.example.globaltranslation.data.** { public *; }
-keep public interface com.example.globaltranslation.data.** { *; }

# Keep all Hilt-injected classes
-keep class com.example.globaltranslation.data.di.** { *; }
-keep class com.example.globaltranslation.data.provider.** { *; }
-keep class com.example.globaltranslation.data.repository.** { *; }
-keep class com.example.globaltranslation.data.network.** { *; }
-keep class com.example.globaltranslation.data.preferences.** { *; }
-keep class com.example.globaltranslation.data.local.** { *; }
-keep class com.example.globaltranslation.data.migration.** { *; }

# Keep Room generated classes
-keep class **_Impl { *; }
-keep class **Dao_Impl { *; }

# Keep data classes and sealed classes
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# ML Kit consumer rules - ensure ML Kit works properly in consuming modules
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }

# Prevent obfuscation of ML Kit model classes
-keep class com.google.mlkit.nl.translate.TranslateLanguage { *; }
-keep class com.google.mlkit.nl.translate.Translator { *; }
-keep class com.google.mlkit.nl.translate.Translation { *; }
-keep class com.google.mlkit.vision.text.TextRecognizer { *; }
-keep class com.google.mlkit.vision.text.TextRecognition { *; }
-keep class com.google.mlkit.vision.common.InputImage { *; }

# Keep NetworkState sealed class and its subclasses
-keep class com.example.globaltranslation.data.network.NetworkState { *; }
-keep class com.example.globaltranslation.data.network.NetworkState$* { *; }
