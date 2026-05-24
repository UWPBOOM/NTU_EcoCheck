# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jspecify.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.ntu.electricity.** {
    *** Companion;
}
-keepclasseswithmembers class com.ntu.electricity.** {
    kotlinx.serialization.KSerializer serializer(...);
}
