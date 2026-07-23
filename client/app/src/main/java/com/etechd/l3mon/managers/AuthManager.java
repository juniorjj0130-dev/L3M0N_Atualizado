package com.etechd.l3mon.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * AuthManager - Gerencia a autenticação JWT no Cliente Android
 */
public class AuthManager {
    private static final String PREF_NAME = "L3MON_AUTH";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String TAG = "AuthManager";

    private final SharedPreferences prefs;

    public AuthManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Salva o token JWT recebido do servidor
     */
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
        Log.d(TAG, "Token JWT salvo com sucesso");
    }

    /**
     * Recupera o token JWT para as requisições
     */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /**
     * Verifica se o cliente possui um token salvo
     */
    public boolean isAuthenticated() {
        return getToken() != null;
    }

    /**
     * Remove o token (logout/desautenticação)
     */
    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }
}