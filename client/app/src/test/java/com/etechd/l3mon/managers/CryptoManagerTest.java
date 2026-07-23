package com.etechd.l3mon.managers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CryptoManager AES-256 CBC encryption/decryption.
 *
 * android.util.Base64 is mocked via Mockito static mocking (Mockito 5.x
 * inline mock maker) to delegate to java.util.Base64 — no Robolectric needed.
 */
public class CryptoManagerTest {

    private MockedStatic<android.util.Base64> base64Mock;

    @Before
    public void setUp() {
        base64Mock = mockStatic(android.util.Base64.class);

        // Redirect android.util.Base64 → java.util.Base64 for test JVM
        base64Mock.when(() -> android.util.Base64.encodeToString(any(byte[].class), anyInt()))
                .thenAnswer(inv -> java.util.Base64.getEncoder()
                        .encodeToString(inv.getArgument(0)));

        base64Mock.when(() -> android.util.Base64.decode(any(String.class), anyInt()))
                .thenAnswer(inv -> {
                    try {
                        return java.util.Base64.getDecoder().decode(inv.<String>getArgument(0));
                    } catch (IllegalArgumentException e) {
                        throw e; // propagate so CryptoManager.catch() returns null
                    }
                });
    }

    @After
    public void tearDown() {
        if (base64Mock != null)
            base64Mock.close();
    }

    // ---------------------------------------------------------------
    // encrypt / decrypt round-trip
    // ---------------------------------------------------------------

    @Test
    public void encryptThenDecrypt_returnsOriginalValue() {
        String plaintext = "hello world";
        String encrypted = CryptoManager.encrypt(plaintext);

        assertNotNull("Encrypted value must not be null", encrypted);
        assertNotEquals("Ciphertext must differ from plaintext", plaintext, encrypted);
        assertEquals("Decrypted value must match original", plaintext, CryptoManager.decrypt(encrypted));
    }

    @Test
    public void encryptThenDecrypt_withSpecialChars_returnsOriginalValue() {
        String plaintext = "áéíóú !@#$% çñ";
        assertEquals(plaintext, CryptoManager.decrypt(CryptoManager.encrypt(plaintext)));
    }

    @Test
    public void encryptThenDecrypt_withLongString_succeeds() {
        String plaintext = "A".repeat(512);
        assertEquals(plaintext, CryptoManager.decrypt(CryptoManager.encrypt(plaintext)));
    }

    // ---------------------------------------------------------------
    // encrypt — determinism (fixed IV → same ciphertext)
    // ---------------------------------------------------------------

    @Test
    public void encrypt_sameInput_producesSameCiphertext() {
        String plaintext = "deterministic";
        assertEquals("Fixed IV must yield identical ciphertext",
                CryptoManager.encrypt(plaintext), CryptoManager.encrypt(plaintext));
    }

    @Test
    public void encrypt_differentInputs_produceDifferentCiphertexts() {
        assertNotEquals(CryptoManager.encrypt("alpha"), CryptoManager.encrypt("beta"));
    }

    // ---------------------------------------------------------------
    // edge cases — must never propagate exceptions
    // ---------------------------------------------------------------

    @Test
    public void encrypt_nullInput_returnsNull() {
        assertNull(CryptoManager.encrypt(null));
    }

    @Test
    public void decrypt_invalidBase64_returnsNull() {
        // java.util.Base64 throws IllegalArgumentException for invalid input,
        // which CryptoManager.catch() converts to null
        assertNull(CryptoManager.decrypt("not!!valid!!base64"));
    }
}
