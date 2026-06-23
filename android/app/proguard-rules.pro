# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers @kotlinx.serialization.Serializable class * { *; }
-keep,includedescriptorclasses class app.ascend.**$$serializer { *; }
-keepclassmembers class app.ascend.** {
    *** Companion;
}

# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
