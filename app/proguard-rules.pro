# Add project specific ProGuard rules here.

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.musimind.**$$serializer { *; }
-keepclassmembers class com.musimind.** {
    *** Companion;
}
-keepclasseswithmembers class com.musimind.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# TarsosDSP
-keep class be.tarsos.dsp.** { *; }
