package com.rishabh.cipherchat.service.impl;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rishabh.cipherchat.service.KeyService;

@Service
public class KeyServiceImpl implements KeyService {
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
    };

    // Encrypts private key using masterKey and AES

    @Override
    public String encryptPrivateKey(byte[] privateKeyBytes) {
        try {
            var secret = new SecretKeySpec(masterKey.getBytes(), 0, 32, "AES");
            var cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            return Base64.getEncoder().encodeToString(cipher.doFinal(privateKeyBytes));
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting private key.", e);
        }
    };

    // Decrypts private key using masterKey and AES

    @Override
    public byte[] decryptPrivateKey(String encrypted) {
        try {
            var secret = new SecretKeySpec(masterKey.getBytes(), 0, 32, "AES");
            var cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secret);
            return cipher.doFinal(Base64.getDecoder().decode(encrypted));
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting private key.", e);
        }
    };
}