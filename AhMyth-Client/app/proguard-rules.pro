# Socket.IO & Engine.IO
-keep class io.socket.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface io.socket.** { *; }

# Keep our core service and receiver to prevent manifest issues
-keep class com.google.android.sys.security.MainService { *; }
-keep class com.google.android.sys.security.MyReceiver { *; }
-keep class com.google.android.sys.security.NotificationService { *; }

# Prevent shrinking of important methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Ignore ProGuard warnings from these packages
-dontwarn io.socket.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.json.**
