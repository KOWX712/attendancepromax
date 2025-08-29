package com.example.autoqr.utils;

import android.util.Base64;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {
    private static final String TRANSFORMATION = "AES";
    private static final String KEY_ALIAS = "AttendanceAppKey";

    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(TRANSFORMATION);
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            byte[] keyBytes = secretKey.getEncoded();
            return Base64.encodeToString(keyBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encrypt(String data, String keyString) {
        try {
            byte[] keyBytes = Base64.decode(keyString, Base64.DEFAULT);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, TRANSFORMATION);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedData = cipher.doFinal(data.getBytes());
            return Base64.encodeToString(encryptedData, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return data;
        }
    }

    public static String decrypt(String encryptedData, String keyString) {
        try {
            byte[] keyBytes = Base64.decode(keyString, Base64.DEFAULT);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, TRANSFORMATION);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decryptedData = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT));
            return new String(decryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return encryptedData;
        }
    }

    public static String simpleEncrypt(String data) {
        try {
            return Base64.encodeToString(data.getBytes(), Base64.DEFAULT);
        } catch (Exception e) {
            return data;
        }
    }

    public static String simpleDecrypt(String encryptedData) {
        try {
            return new String(Base64.decode(encryptedData, Base64.DEFAULT));
        } catch (Exception e) {
            return encryptedData;
        }
    }
}