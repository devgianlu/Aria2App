-dontobfuscate

-dontwarn okio.**
-dontwarn okhttp3.internal.huc.StreamedRequestBody
-dontwarn okhttp3.internal.huc.OkHttpURLConnection
-keep class okhttp3.internal.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
-dontwarn org.conscrypt.OpenSSLProvider
-dontwarn org.conscrypt.Conscrypt

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-dontwarn com.pavelsikun.vintagechroma.ChromaPreferenceCompat
-dontwarn com.mikepenz.aboutlibraries.ui.item.HeaderItem