# kotlinx.serialization: keep serializer() companions for our DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class com.musicapp.stemseparator.data.network.** {
    static ** Companion;
}
-keepclasseswithmembers class com.musicapp.stemseparator.data.network.** {
    kotlinx.serialization.KSerializer serializer(...);
}
