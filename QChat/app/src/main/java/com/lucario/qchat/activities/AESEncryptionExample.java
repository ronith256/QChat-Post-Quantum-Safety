package com.lucario.qchat.activities;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class AESEncryptionExample {

    public static void main(String[] args) {
        try {
            // Convert the string to bytes
            String keyString = "à¤øèçæåä";
            byte[] keyBytes = keyString.getBytes("UTF-8");

            // Ensure the key is 128, 192, or 256 bits long (16, 24, or 32 bytes)
            keyBytes = Arrays.copyOf(keyBytes, 16); // Adjust the length as needed

            // Create a SecretKey object
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // Create a cipher instance for AES encryption
            Cipher cipher = Cipher.getInstance("AES");

            // Encrypt a sample string
            String plaintext = "Hello, AES Encryption!";
            byte[] encryptedBytes = encrypt(plaintext, secretKey, cipher);

            // Display the encrypted string
            System.out.println("Encrypted: " + Base64.getEncoder().encodeToString(encryptedBytes));

            // Decrypt the encrypted string
            String decryptedText = decrypt(encryptedBytes, secretKey, cipher);

            // Display the decrypted string
            System.out.println("Decrypted: " + decryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] encrypt(String plaintext, SecretKey secretKey, Cipher cipher) throws Exception {
        // Initialize the cipher for encryption
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Encrypt the plaintext
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));

        return encryptedBytes;
    }

    private static String decrypt(byte[] encryptedBytes, SecretKey secretKey, Cipher cipher) throws Exception {
        // Initialize the cipher for decryption
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Decrypt the encrypted bytes
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, "UTF-8");
    }
}

