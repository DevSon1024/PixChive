# Keep data classes used for JSON serialization
-keep class com.devson.pixchive.data.** { *; }

# Keep DocumentFile related classes
-keep class androidx.documentfile.provider.** { *; }

# Gson rules for serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep all model classes that are serialized
-keep class com.devson.pixchive.data.CachedFolderData { *; }
-keep class com.devson.pixchive.data.CachedChapter { *; }
-keep class com.devson.pixchive.data.CachedImageInfo { *; }