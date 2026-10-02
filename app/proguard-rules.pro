# Varagh release rules. Most libraries (Retrofit, OkHttp, kotlinx.serialization, Room, Hilt,
# WorkManager, Tink, Coil) ship their own consumer rules; only app-specific needs are listed here.

# Keep line numbers for readable crash reports, hide original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization: @Serializable classes are looked up through generated serializers
# (navigation routes, backup file, network DTOs).
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.mid.varagh.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.mid.varagh.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit service interface (suspend functions + generic return types).
-keep,allowobfuscation,allowshrinking interface com.mid.varagh.core.network.VaraghApi
-keepattributes Signature, Exceptions
