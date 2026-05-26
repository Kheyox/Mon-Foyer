# App classes — keep all (Firestore, ViewModels, data models)
-keep class com.bibliostudio.monfoyer.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Glance (widget)
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Credentials API
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-dontwarn androidx.credentials.**

# JSON / serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
