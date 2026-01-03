# Add project specific ProGuard rules here.

# Keep application class
-keep class com.devson.pixchive.** { *; }
-keepnames class com.devson.pixchive.** { *; }

# ===== CRITICAL: ViewModel and Lifecycle =====
# Keep all ViewModels - REQUIRED to prevent the errors you're seeing
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Keep ViewModel constructors and prevent obfuscation
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep all lifecycle classes
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# Keep viewModelScope and lifecycle runtime
-keep class androidx.lifecycle.ViewModelKt { *; }
-keep class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.ViewModelProvider { *; }
-keep class androidx.lifecycle.ViewModelProvider$Factory { *; }
-keep class androidx.lifecycle.viewmodel.** { *; }

# ===== Room Database =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-dontwarn androidx.room.paging.**

# Keep Room generated implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class *

# ===== Kotlin Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# ===== Jetpack Compose =====
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.foundation.** { *; }
-dontwarn androidx.compose.**

# Keep Composable functions
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ===== Navigation Compose =====
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment
-keepnames class * extends androidx.navigation.Navigator

# ===== Coil Image Loading =====
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ===== Gson (for JSON parsing) =====
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===== Anggrayudi Storage Library =====
-keep class com.anggrayudi.storage.** { *; }
-dontwarn com.anggrayudi.storage.**

# ===== DataStore Preferences =====
-keep class androidx.datastore.*.** { *; }
-keep class androidx.datastore.preferences.** { *; }

# ===== EXIF Interface =====
-keep class androidx.exifinterface.** { *; }

# ===== Telephoto/Zoomable =====
-keep class me.saket.telephoto.** { *; }
-keep class net.engawapg.lib.zoomable.** { *; }
-dontwarn me.saket.telephoto.**
-dontwarn net.engawapg.lib.zoomable.**

# ===== Accompanist Permissions =====
-keep class com.google.accompanist.permissions.** { *; }
-dontwarn com.google.accompanist.**

# ===== DocumentFile =====
-keep class androidx.documentfile.** { *; }

# ===== Android Core =====
-keep class androidx.core.** { *; }
-keep interface androidx.core.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom View constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Activity methods used in XML
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementation
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# General optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Keep source file and line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
