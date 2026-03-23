# ========================
# Unmi ProGuard / R8 Rules
# ========================

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }

# ---- Hilt / Dagger ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.* { *; }
-keepclassmembers class * {
    @dagger.hilt.* *;
    @javax.inject.* *;
}

# ---- Retrofit + OkHttp ----
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ---- Kotlinx Serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.unmi.app.**$$serializer { *; }
-keepclassmembers class io.unmi.app.** {
    *** Companion;
}
-keepclasseswithmembers class io.unmi.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Compose ----
-dontwarn androidx.compose.**

# ---- Data classes used in serialization ----
-keep class io.unmi.app.data.network.RdapResult { *; }
-keep class io.unmi.app.data.network.DomainPrice { *; }
-keep class io.unmi.app.data.network.TldPriceInfo { *; }
-keep class io.unmi.app.data.network.ValuationResult { *; }
-keep class io.unmi.app.data.local.datastore.AppSettings { *; }

# ---- Room Entities (keep field names for DB mapping) ----
-keep class io.unmi.app.data.local.db.entity.** { *; }

# ---- Kotlin Coroutines ----
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# ---- Apache POI (large library - strip unused) ----
-dontwarn org.apache.poi.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn com.microsoft.**
-keep class org.apache.poi.ss.** { *; }
-keep class org.apache.poi.xssf.** { *; }

# ---- OpenCSV ----
-dontwarn com.opencsv.**
-keep class com.opencsv.** { *; }

# ---- Security / Crypto ----
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# ---- OSGI (Apache POI transitive) ----
-dontwarn org.osgi.framework.**

# ---- General ----
-dontwarn java.lang.invoke.**
-dontwarn sun.misc.**
