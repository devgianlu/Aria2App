-dontwarn okio.**
-keep class okhttp3.internal.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
-dontwarn org.conscrypt.OpenSSLProvider
-dontwarn org.conscrypt.Conscrypt

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-keepclassmembers public class * extends com.gianlu.aria2app.Tutorial.BaseTutorial {
   public <init>(...);
}