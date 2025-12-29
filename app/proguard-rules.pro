# Add project specific ProGuard rules here.
#
# By default, the Android Gradle Plugin adds some standard rules for you:
# https://developer.android.com/studio/build/shrink-code
#
# Keep rules should be tightened as the app grows. Start permissive, then restrict.

# Retrofit / Gson (if used with reflection)
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn javax.annotation.**

# OkHttp / SSE
-dontwarn okhttp3.**
-dontwarn okio.**


