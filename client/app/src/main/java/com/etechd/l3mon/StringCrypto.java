package com.etechd.l3mon;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * String Encryption utilitário
 * Uso: StringCrypto.d("string_criptografada_em_base64")
 */
public final class StringCrypto {

    // Chave e IV ofuscados (não deixar em texto claro)
    private static final byte[] KEY = {
            0x4c, 0x33, 0x4d, 0x30, 0x4e, 0x5f, 0x4b, 0x33,
            0x59, 0x5f, 0x32, 0x30, 0x32, 0x36, 0x21, 0x40
    }; // 16 bytes

    private static final byte[] IV = {
            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x30, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66
    }; // 16 bytes

    private StringCrypto() {
        // Utility class
    }

    /**
     * Descriptografa uma string
     */
    public static String d(String encryptedBase64) {
        try {
            byte[] encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Criptografa uma string (use apenas em desenvolvimento)
     */
    public static String e(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }
}