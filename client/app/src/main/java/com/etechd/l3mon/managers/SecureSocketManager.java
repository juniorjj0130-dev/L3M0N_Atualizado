package com.etechd.l3mon.managers;

import android.util.Log;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SecureSocketManager {

    private static final String TAG = "SecureSocket";
    private static Socket socket;

    // Coloque aqui o hash do certificado do seu servidor (SHA256)
    private static final String CERTIFICATE_PIN = "sha256/xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx=";

    public static Socket getSecureSocket(String serverUrl) {
        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionDelay = 3000;
            options.secure = true; // Força HTTPS/WSS

            // Certificate Pinning com OkHttp
            CertificatePinner certificatePinner = new CertificatePinner.Builder()
                    .add("seudominio.com", CERTIFICATE_PIN) // Coloque seu domínio aqui
                    .build();

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .certificatePinner(certificatePinner)
                    .build();

            options.callFactory = okHttpClient;
            options.webSocketFactory = okHttpClient;

            socket = IO.socket(serverUrl, options);
            return socket;

        } catch (URISyntaxException e) {
            Log.e(TAG, "Erro ao criar socket seguro: " + e.getMessage());
            return null;
        }
    }
}