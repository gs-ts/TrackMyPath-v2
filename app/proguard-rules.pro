
# Google Places SDK
# https://developers.google.com/maps/documentation/places/android-sdk/release-notes#November_20_2025
-keepclassmembers class com.google.android.libraries.places.internal.** {
    <init>();
}

# Coil 3
-keep class coil3.util.DecoderServiceLoaderTarget { *; }
-keep class coil3.util.FetcherServiceLoaderTarget { *; }
-keep class coil3.util.ServiceLoaderComponentRegistry { *; }
-keep class * implements coil3.util.DecoderServiceLoaderTarget { *; }
-keep class * implements coil3.util.FetcherServiceLoaderTarget { *; }
