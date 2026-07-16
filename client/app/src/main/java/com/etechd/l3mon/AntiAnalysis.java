package com.etechd.l3mon;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Debug;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;

/**
 * Anti-Analysis Module - String Encryption reforçada
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

    // ==================== EMULADOR / VM ====================
    public static boolean isEmulator() {
        try {
            return Build.FINGERPRINT.startsWith(StringCrypto.d("4p/xMggNqApsKCYqUxspNA==")) || // generic
                    Build.MODEL.contains(StringCrypto.d("QMKqOPFCBt71zaAFPPmKkQ==")) || // google_sdk
                    Build.MANUFACTURER.contains(StringCrypto.d("jBDijLWwxWTBQbjIIFXZTg==")) || // Genymotion
                    Build.HARDWARE.contains(StringCrypto.d("wmApkfFZJAN8vbvAutnwDA==")) || // goldfish
                    Build.HARDWARE.contains(StringCrypto.d("HsfjAD3MAMunOMQ5zQ9h1w==")) || // ranchu
                    hasQemuProperties();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasQemuProperties() {
        String[] props = { "ro.kernel.qemu", "ro.hardware", "ro.boot.hardware" };
        for (String prop : props) {
            String value = getSystemProperty(prop);
            if (value != null && (value.contains(StringCrypto.d("PtzQ4ygpLqaADIkQAMacOg==")) // qemu
                    || value.contains(StringCrypto.d("wmApkfFZJAN8vbvAutnwDA==")))) { // goldfish
                return true;
            }
        }
        return false;
    }

    // ==================== ROOT + MAGISK + ZYGISK ====================
    public static boolean isRooted() {
        String[] paths = {
                "/system/app/" + StringCrypto.d("eW4D9vkU1GUVMuA2m/VYbg=="), // Superuser.apk
                "/sbin/" + StringCrypto.d("YoNretLgNKMc/J/+bSoWeQ=="),
                "/system/bin/" + StringCrypto.d("YoNretLgNKMc/J/+bSoWeQ=="),
                "/system/xbin/" + StringCrypto.d("YoNretLgNKMc/J/+bSoWeQ=="),
                "/data/local/xbin/" + StringCrypto.d("YoNretLgNKMc/J/+bSoWeQ=="),
                "/data/local/bin/" + StringCrypto.d("YoNretLgNKMc/J/+bSoWeQ=="),
                "/su/bin/" + StringCrypto.d("YoNretLgNKMc/J/+bSoWeQ=="),
                "/magisk"
        };
        for (String path : paths) {
            if (new File(path).exists())
                return true;
        }
        return canExecuteCommand(StringCrypto.d("YoNretLgNKMc/J/+bSoWeQ==")) // su
                || isMagiskDetected()
                || isZygiskDetected();
    }

    private static boolean isMagiskDetected() {
        return new File("/data/adb/" + StringCrypto.d("EaaChdbjGCKDYynoet+qzw==")).exists() // magisk
                || checkMagiskHide();
    }

    private static boolean isZygiskDetected() {
        return new File("/data/adb/" + StringCrypto.d("NpqXcEueHmsjJ7NQ4juoxA==")).exists(); // zygisk
    }

    private static boolean checkMagiskHide() {
        try {
            String value = getSystemProperty("ro.boot." + StringCrypto.d("EaaChdbjGCKDYynoet+qzw==")); // magisk
            return value != null && value.contains("1");
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== FRIDA ====================
    public static boolean isFridaDetected() {
        String[] fridaPaths = {
                "/data/local/tmp/" + StringCrypto.d("6r4U5bAT8wypLZXXgXXdVw=="), // frida-server
                "/data/local/tmp/" + StringCrypto.d("mJ7+nLWgj6QtZNJbJFjdew==") + ".server", // re.frida
                "/sdcard/" + StringCrypto.d("XbbpHsUuMTWGNpYYtsM5Bw==") + "-"
                        + StringCrypto.d("OGamoR4D7jS9xesDUVIL7A=="), // frida-gadget
                "/data/app/" + StringCrypto.d("XbbpHsUuMTWGNpYYtsM5Bw==")
        };
        for (String path : fridaPaths) {
            if (new File(path).exists())
                return true;
        }
        return isFridaInMaps();
    }

    private static boolean isFridaInMaps() {
        try {
            File maps = new File("/proc/self/maps");
            if (maps.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(maps)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(StringCrypto.d("XbbpHsUuMTWGNpYYtsM5Bw==")) // frida
                                || line.contains(StringCrypto.d("OGamoR4D7jS9xesDUVIL7A==")) // gadget
                                || line.contains(StringCrypto.d("mJ7+nLWgj6QtZNJbJFjdew=="))) { // re.frida
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    // ==================== XPOSED / LSPOSED ====================
    public static boolean isXposedDetected() {
        return System.getProperty(StringCrypto.d("fTSk8ganeduwDK7pcOQiwg==")) != null // vxp
                || isPackageInstalled(
                        StringCrypto.d("285qMcGhDydJC/Hhds8sP6G16zdUYMq5oygNRujTIFyssBNqB7rN2VHG7us4m/sm")) // de.robv...
                || isPackageInstalled(StringCrypto.d("xZjuT5IUdbS8AA5v4jCqDd1K2DmHNAhijVrErsRZj4s=")) // org.meowcat...
                || isPackageInstalled(StringCrypto.d("J1h3hOxgRIy4A9ltZn1wvt9j6lfmQfHLV1pWQAq8Bhs=")); // com.topjohnwu.magisk
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

    // ==================== INTEGRIDADE ====================
    public static boolean verifyAPKIntegrity() {
        try {
            Signature[] signatures = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
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