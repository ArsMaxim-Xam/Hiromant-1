# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Annotation

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Room database ProGuard rules ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.aistudio.hiromant.kxsrwa.data.local.** { *; }

# --- Moshi JSON serialization models ---
-keep class com.aistudio.hiromant.kxsrwa.data.remote.** { *; }
-keepclassmembers class com.aistudio.hiromant.kxsrwa.data.remote.** {
    <fields>;
    <init>(...);
}

# --- Keep Retrofit & OkHttp classes ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes *Annotation*
-keepclassmembernames class * {
    @retrofit2.http.* <methods>;
}

-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# --- Keep Moshi internals ---
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }

