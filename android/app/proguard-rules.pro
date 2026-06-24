# --- Keep attributes needed by Retrofit + kotlinx.serialization under R8 full mode ---
# Signature/EnclosingMethod let Retrofit read suspend return types & generic
# parameters; without them, release builds can throw "Unable to create converter".
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# --- kotlinx.serialization ---
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers @kotlinx.serialization.Serializable class * { *; }
-keep,includedescriptorclasses class app.ascend.**$$serializer { *; }
-keepclassmembers class app.ascend.** {
    *** Companion;
}
# Belt-and-suspenders: keep the wire models themselves (small, correctness > shrink).
-keep @kotlinx.serialization.Serializable class app.ascend.data.remote.** { *; }

# --- Retrofit ---
# Retrofit 2.11 ships its own consumer rules, but keep these as a safety net.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
