# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Android sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:
#-dontwarn butterknife.**
#-dontwarn okio.**
#-dontwarn java.util.zip.**
#-dontnote java.util.zip.**

# http://proguard.sourceforge.net/index.html#/manual/examples.html
#-keep public class * extends android.app.Application
#-keep public class * extends Activity
#-keep public class * extends Service
#-keep public class * extends ImageView


# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
