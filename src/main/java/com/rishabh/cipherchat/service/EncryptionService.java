package com.rishabh.cipherchat.service;

import java.security.PrivateKey;

public interface EncryptionService {

    // this method generates a new random AES key when a new conversation is created
    public byte[] generateAesKey();

    // this method encrypts the conversation AES key with the participant's public
    // RSA key so that only the participant can decrypt it with their private key
    public String encryptForUser(byte[] data, String base64PublicKey);
    // this method encrypts messages

    String encryptWithAesKey(String plainText, byte[] key);
    // this method decrypts messages

    String decryptWithAesKey(String cipherText, byte[] key);

    // decrypts text with RSA public key
    byte[] decryptWithPrivateKey(String cipherText, PrivateKey privateKey);

}
