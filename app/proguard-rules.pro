# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.myradio.data.model.**$$serializer { *; }
-keepclassmembers class com.example.myradio.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.myradio.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Google Cast ---
-keep class com.example.myradio.cast.CastOptionsProvider { *; }

# --- Media3 / ExoPlayer (consumer rules handle most, but keep custom service) ---
-keep class com.example.myradio.playback.PlaybackService { *; }

# --- Widget ---
-keep class com.example.myradio.widget.RadioWidgetProvider { *; }
