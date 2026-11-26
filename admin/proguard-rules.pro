# R8 Full-Mode Obfuscation & Security Rules for Voice Admin
-repackageclasses 'a'
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

# Keep Admin Firebase models & reflective classes
-keepclassmembers class com.zonex.admin.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn coil.**
