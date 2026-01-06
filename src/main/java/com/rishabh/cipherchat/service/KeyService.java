package com.rishabh.cipherchat.service;

import java.security.KeyPair;

public interface KeyService {
    // TO-DO: make masterKey as external configuration
    // Creates RSA key pair
    public KeyPair generateKeyPair();

    // Encrypts private key using masterKey and AES
    public String encryptPrivateKey(byte[] privateKeyBytes);

    // Decrypts private key using masterKey and AES
    public byte[] decryptPrivateKey(String encrypted);
}
