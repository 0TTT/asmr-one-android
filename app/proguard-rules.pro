# Add project specific ProGuard rules here.
# Retrofit / Gson / kotlinx-serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.asmrone.app.data.model.** { *; }

# kotlinx.serialization
-keepclassmembers class com.asmrone.app.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.asmrone.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
