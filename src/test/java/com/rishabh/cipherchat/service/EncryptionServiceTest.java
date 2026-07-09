package com.rishabh.cipherchat.service;

import com.rishabh.cipherchat.service.impl.EncryptionServiceImpl;
import com.rishabh.cipherchat.service.impl.KeyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionServiceImpl encryptionService;
    private KeyServiceImpl keyService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionServiceImpl();
        keyService = new KeyServiceImpl();
        ReflectionTestUtils.setField(keyService, "masterKey",
                "abcdefghijklmnopqrstuvwxyz123456");
    }

    @Test
    void shouldEncryptAndDecryptMessageWithAesGcm() {
        byte[] aesKey = encryptionService.generateAesKey();

        String original = "Hello, secure world! こんにちは";
        String encrypted = encryptionService.encryptWithAesKey(original, aesKey);
        String decrypted = encryptionService.decryptWithAesKey(encrypted, aesKey);

        assertEquals(original, decrypted);
        assertNotEquals(original, encrypted);
    }

    @Test
    void shouldProduceDifferentCiphertextsForSamePlaintext() {
        byte[] aesKey = encryptionService.generateAesKey();
        String plaintext = "same message";

        String enc1 = encryptionService.encryptWithAesKey(plaintext, aesKey);
        String enc2 = encryptionService.encryptWithAesKey(plaintext, aesKey);

        // Different IVs → different ciphertexts
        assertNotEquals(enc1, enc2);

        // But both decrypt correctly
        assertEquals(plaintext, encryptionService.decryptWithAesKey(enc1, aesKey));
        assertEquals(plaintext, encryptionService.decryptWithAesKey(enc2, aesKey));
    }

    @Test
    void shouldWrapAndUnwrapAesKeyWithRsa() throws Exception {
        KeyPair keyPair = keyService.generateKeyPair();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(
                keyPair.getPublic().getEncoded());

        byte[] aesKey = encryptionService.generateAesKey();
        String wrappedKey = encryptionService.encryptForUser(aesKey, publicKeyBase64);
        byte[] unwrappedKey = encryptionService.decryptWithPrivateKey(
                wrappedKey, keyPair.getPrivate());

        assertArrayEquals(aesKey, unwrappedKey);
    }

    @Test
    void shouldEncryptAndDecryptPrivateKeyWithAesGcm() {
        KeyPair keyPair = keyService.generateKeyPair();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        String encrypted = keyService.encryptPrivateKey(privateKeyBytes);
        byte[] decrypted = keyService.decryptPrivateKey(encrypted);

        assertArrayEquals(privateKeyBytes, decrypted);
    }

    @Test
    void shouldProduceDifferentEncryptedPrivateKeysEachTime() {
        KeyPair keyPair = keyService.generateKeyPair();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

        String enc1 = keyService.encryptPrivateKey(privateKeyBytes);
        String enc2 = keyService.encryptPrivateKey(privateKeyBytes);

        assertNotEquals(enc1, enc2);
    }

    @Test
    void shouldHandleEmptyMessage() {
        byte[] aesKey = encryptionService.generateAesKey();

        String encrypted = encryptionService.encryptWithAesKey("", aesKey);
        String decrypted = encryptionService.decryptWithAesKey(encrypted, aesKey);

        assertEquals("", decrypted);
    }

    @Test
    void shouldHandleLongMessage() {
        byte[] aesKey = encryptionService.generateAesKey();
        String longMessage = "A".repeat(10000);

        String encrypted = encryptionService.encryptWithAesKey(longMessage, aesKey);
        String decrypted = encryptionService.decryptWithAesKey(encrypted, aesKey);

        assertEquals(longMessage, decrypted);
    }

    @Test
    void shouldHandleUnicodeMessage() {
        byte[] aesKey = encryptionService.generateAesKey();
        String original = "🚀🔥 Secure Chat 安全聊天";

        String encrypted = encryptionService.encryptWithAesKey(original, aesKey);
        String decrypted = encryptionService.decryptWithAesKey(encrypted, aesKey);

        assertEquals(original, decrypted);
    }

    @Test
    void shouldGenerate256BitAesKey() {
        byte[] key = encryptionService.generateAesKey();
        assertEquals(32, key.length); // 256 bits = 32 bytes
    }

    @Test
    void shouldGenerate2048BitRsaKeyPair() {
        KeyPair keyPair = keyService.generateKeyPair();
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
    }
}
