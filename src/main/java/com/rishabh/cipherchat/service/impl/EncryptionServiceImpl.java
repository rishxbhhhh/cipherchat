package com.rishabh.cipherchat.service.impl;

import java.security.KeyFactory;
import java.security.PrivateKey;

import org.springframework.stereotype.Service;

import com.rishabh.cipherchat.service.EncryptionService;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecureRandom random = new SecureRandom();

    // this method generates a new random AES key when a new conversation is created
    @Override
    public byte[] generateAesKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey().getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("AES key generation failed for new conversation.", e);
        }
    }

    // this method encrypts the AES conversation key (generate above) with the participant's public RSA key so that only the participant can decrypt it with their private key
    @Override
    public String encryptForUser(byte[] data, String base64PublicKey) {
        try {
            byte[] pubBytes = Base64.getDecoder().decode(base64PublicKey);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(pubBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            PublicKey publicKey = kf.generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            return Base64.getEncoder().encodeToString(cipher.doFinal(data));

        } catch (Exception e) {
            throw new RuntimeException("RSA encryption failed for participant.", e);
        }
    }

    // this method encrypts messages
    @Override
    public String encryptWithAesKey(String plainText, byte[] keyBytes) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    // this method decrypts messages
    @Override
    public String decryptWithAesKey(String cipherText, byte[] keyBytes) {
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - IV_LENGTH];

            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
            System.arraycopy(decoded, IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    // decrypts text with RSA public key
    @Override
    public byte[] decryptWithPrivateKey(String cipherText, PrivateKey privateKey) {
        try {
            byte[] data = Base64.getDecoder().decode(cipherText);

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            return cipher.doFinal(data);

        } catch (Exception e) {
            throw new RuntimeException("RSA private key decryption failed", e);
        }
    }

}