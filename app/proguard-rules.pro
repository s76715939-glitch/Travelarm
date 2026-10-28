# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * implements androidx.room.RoomDatabase

# Moshi / Retrofit Models
-keep class com.sstechzone.travelarm.data.model.** { *; }
-keep class com.sstechzone.travelarm.data.local.** { *; }

# Osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# OkHttp optional dependencies
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
