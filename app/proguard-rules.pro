# Keep application class
-keep class com.devson.pixchive.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# Navigation Compose
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment

# Lifecycle
-keep class androidx.lifecycle.** { *; }

# Gson (for JSON parsing)
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Anggrayudi Storage Library
-keep class com.anggrayudi.storage.** { *; }
-dontwarn com.anggrayudi.storage.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# DataStore
-keep class androidx.datastore.*.** { *; }

# EXIF
-keep class androidx.exifinterface.** { *; }

# Telephoto (Zoomable)
-keep class me.saket.telephoto.** { *; }
-dontwarn me.saket.telephoto.**
