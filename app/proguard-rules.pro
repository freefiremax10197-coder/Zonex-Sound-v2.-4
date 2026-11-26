# R8 Full-Mode Obfuscation & Security Rules for Zonex Voice Pro
-repackageclasses 'z'
-allowaccessmodification
-overloadaggressively
-optimizationpasses 5

# Strip debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep Shizuku AIDL and UserService entry points (instantiated via reflection by Shizuku server)
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
    @androidx.annotation.Keep <init>(...);
}
-keep class com.example.IShizukuAudioService { *; }
-keep class com.example.IShizukuAudioService$** { *; }
-keep class com.example.shizuku.ShizukuAudioUserService { *; }
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Keep Firebase Realtime Database & Auth models
-keepclassmembers class com.example.data.model.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*

# Keep Kotlin Coroutines & Serialization metadata
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# OkHttp / Coil
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn coil.**
