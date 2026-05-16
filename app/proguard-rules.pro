# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Model classes (keep for Firestore serialization)
-keep class com.taskify.pro.model.** { *; }

# Gson (used internally by Firestore)
-keep class com.google.gson.** { *; }
-keep class com.google.firebase.firestore.** { *; }
