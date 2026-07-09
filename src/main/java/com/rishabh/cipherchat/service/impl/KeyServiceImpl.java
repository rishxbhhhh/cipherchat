package com.rishabh.cipherchat.service.impl;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rishabh.cipherchat.entity.User;
import com.rishabh.cipherchat.service.KeyService;

@Service
public class KeyServiceImpl implements KeyService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecureRandom random = new SecureRandom();

    @Value("${cipherchat.crypto.master-key}")
    private String masterKey;

    // Creates RSA key pair
    @Override
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Error generating key pair.", e);
        }
    }

    // Encrypts private key using masterKey and AES-GCM
    @Override
    public String encryptPrivateKey(byte[] privateKeyBytes) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);

            var secret = new SecretKeySpec(masterKey.getBytes(), 0, 32, "AES");
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(privateKeyBytes);
            byte[] combined = new byte[GCM_IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, GCM_IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting private key.", e);
        }
    }

    // Decrypts private key using masterKey and AES-GCM
    @Override
    public byte[] decryptPrivateKey(String encrypted) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decoded, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            var secret = new SecretKeySpec(masterKey.getBytes(), 0, 32, "AES");
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting private key.", e);
        }
    }

    // Reconstructs user's RSA private key from encrypted storage
    @Override
    public PrivateKey reconstructPrivateKey(User user) {
        try {
            byte[] privateKeyBytes = decryptPrivateKey(user.getPrivateKeyEncrypted());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct private key for user.", e);
        }
    }
}