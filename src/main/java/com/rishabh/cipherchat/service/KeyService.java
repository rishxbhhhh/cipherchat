package com.rishabh.cipherchat.service;

import java.security.KeyPair;
import java.security.PrivateKey;

import com.rishabh.cipherchat.entity.User;

public interface KeyService {
    // Creates RSA key pair
    public KeyPair generateKeyPair();

    // Encrypts private key using masterKey and AES-GCM
    public String encryptPrivateKey(byte[] privateKeyBytes);

    // Decrypts private key using masterKey and AES-GCM
    public byte[] decryptPrivateKey(String encrypted);

    // Reconstructs user's RSA private key from encrypted storage
    public PrivateKey reconstructPrivateKey(User user);
}
