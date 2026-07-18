package com.etechd.l3mon.managers;

import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

public class SecureSocketManager {

    private static final String TAG = "SecureSocket";
    private static Socket socket;

    public static Socket getSecureSocket(String serverUrl) {
        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionDelay = 3000;
            options.secure = true; // Força HTTPS/WSS

            socket = IO.socket(serverUrl, options);
            return socket;

        } catch (URISyntaxException e) {
            Log.e(TAG, "Erro ao criar socket seguro: " + e.getMessage());
            return null;
        }
    }
}