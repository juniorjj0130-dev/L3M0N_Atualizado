package com.etechd.l3mon.managers;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {

    // Chave AES-256 (32 bytes). Mude para uma chave forte!
    private static final String SECRET_KEY = "SUA_CHAVE_SECRETA_32_BYTES_AQUI!!"; // 32 caracteres
    private static final String INIT_VECTOR = "RandomInitVector"; // 16 caracteres

    /**
     * Criptografa uma string usando AES-256 CBC
     */
    public static String encrypt(String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Descriptografa uma string criptografada com AES-256
     */
    public static String decrypt(String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP));
            return new String(original);

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}