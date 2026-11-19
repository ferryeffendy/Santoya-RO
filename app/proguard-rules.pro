# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line number information for readable crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database Entities, DAOs, and TypeConverters
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.example.data.entity.** { *; }
-keep class com.example.data.dao.** { *; }

# Firebase Firestore & Auth reflection / serialization safety
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Moshi / JSON Backup & Export models
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# Coroutines & OkHttp warnings suppression
-dontwarn kotlinx.coroutines.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Firebase App Check & AI Logic
-keep class com.google.firebase.appcheck.** { *; }
-keep class com.google.firebase.ai.** { *; }

# Strip verbose and debug Android logs from release bytecode
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
