# Add project specific ProGuard rules here.
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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-keepclasseswithmembers class **.R$* {
    public static final int define_*;
}
# keep snmp4j classes or crash
#noinspection ShrinkerUnresolvedReference
-keep class org.snmp4j.** {*;}

# handle jackson databind
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** {
*;
}
-keepnames interface com.fasterxml.jackson.** {
*;
}
-dontwarn com.fasterxml.jackson.databind.**
-keep class org.codehaus.** { *; }
