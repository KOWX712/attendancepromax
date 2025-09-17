package io.github.kowx712.mmuautoqr.utils

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {
    private const val TRANSFORMATION = "AES"
    private const val TAG = "EncryptionUtils"

    fun generateKey(): String? {
        return try {
            val keyGenerator = KeyGenerator.getInstance(TRANSFORMATION)
            keyGenerator.init(256)
            val secretKey: SecretKey = keyGenerator.generateKey()
            val keyBytes = secretKey.encoded
            Base64.encodeToString(keyBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating key", e)
            null
        }
    }

    fun encrypt(data: String, keyString: String): String {
        return try {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            val secretKey = SecretKeySpec(keyBytes, TRANSFORMATION)

            @SuppressLint("GetInstance") val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val encryptedData = cipher.doFinal(data.toByteArray())
            Base64.encodeToString(encryptedData, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting data", e)
            data
        }
    }

    fun decrypt(encryptedData: String, keyString: String): String {
        return try {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            val secretKey = SecretKeySpec(keyBytes, TRANSFORMATION)

            @SuppressLint("GetInstance") val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val decryptedData = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT))
            String(decryptedData)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting data", e)
            encryptedData
        }
    }
}