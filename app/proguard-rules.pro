# Proguard Rules for Shario
-keepattributes *Annotation*
-dontwarn java.lang.invoke.**
-dontwarn sun.misc.Unsafe

# Kotlinx serialization / Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep data models
-keep class org.zeroshare.app.domain.model.** { *; }
-keep class org.zeroshare.app.data.engine.** { *; }
