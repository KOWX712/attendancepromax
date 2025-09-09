package io.github.kowx712.mmuautoqr.utils;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {
    private static final String TRANSFORMATION = "AES";
    private static final String KEY_ALIAS = "AttendanceAppKey";
    private static final String TAG = "EncryptionUtils";

    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(TRANSFORMATION);
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            byte[] keyBytes = secretKey.getEncoded();
            return Base64.encodeToString(keyBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error generating key", e);
            return null;
        }
    }

    public static String encrypt(String data, String keyString) {
        try {
            byte[] keyBytes = Base64.decode(keyString, Base64.DEFAULT);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, TRANSFORMATION);

            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedData = cipher.doFinal(data.getBytes());
            return Base64.encodeToString(encryptedData, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting data", e); // Replaced printStackTrace
            return data;
        }
    }

    public static String decrypt(String encryptedData, String keyString) {
        try {
            byte[] keyBytes = Base64.decode(keyString, Base64.DEFAULT);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, TRANSFORMATION);

            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decryptedData = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT));
            return new String(decryptedData);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting data", e);
            return encryptedData;
        }
    }

    public static String simpleEncrypt(String data) {
        try {
            return Base64.encodeToString(data.getBytes(), Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error in simpleEncrypt", e);
            return data;
        }
    }

    public static String simpleDecrypt(String encryptedData) {
        try {
            return new String(Base64.decode(encryptedData, Base64.DEFAULT));
        } catch (Exception e) {
            Log.e(TAG, "Error in simpleDecrypt", e);
            return encryptedData;
        }
    }
}