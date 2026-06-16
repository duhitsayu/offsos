package com.vaav.offsos.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    // Derives a 256-bit key using SHA-256
    fun deriveKey(channelName: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(channelName.toByteArray(StandardCharsets.UTF_8))
    }

    // Hash to use as group ID in DB (so we don't store the raw channel name or key directly)
    fun getGroupId(channelName: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(channelName.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun encrypt(data: String, channelName: String): String {
        val key = deriveKey(channelName)
        val secretKey = SecretKeySpec(key, ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        // Let cipher generate IV
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        
        // Prepend IV to encrypted data
        val combined = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedDataWithIv: String, channelName: String): String? {
        try {
            val key = deriveKey(channelName)
            val secretKey = SecretKeySpec(key, ALGORITHM)
            val combined = Base64.decode(encryptedDataWithIv, Base64.NO_WRAP)
            
            if (combined.size < GCM_IV_LENGTH) return null
            
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            
            val encryptedData = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedData, 0, encryptedData.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedData = cipher.doFinal(encryptedData)
            return String(decryptedData, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
