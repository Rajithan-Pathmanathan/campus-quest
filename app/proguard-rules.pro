# Proguard configuration for Campus Quest

# Keep Room generated classes
-keep class androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Keep Domain models for Room and Firestore serialization
-keepclassmembers class com.campusquest.domain.model.** { *; }
-keepclassmembers class com.campusquest.data.local.entity.** { *; }
-keepclassmembers class com.campusquest.data.remote.dto.** { *; }

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# Keep Material Design & Navigation SafeArgs
-keep class com.campusquest.ui.**Directions { *; }
-keep class com.campusquest.ui.**Args { *; }
