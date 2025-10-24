package com.muratcangzm.preferences.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random

private const val KS = "AndroidKeyStore"
private const val ALIAS = "ghost_prefs_key"
private const val TRANS = "AES/GCM/NoPadding"
private const val IV_LEN = 12
private const val TAG_BITS = 128

internal object AesGcmCipher {
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KS).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS)
        val spec = KeyGenParameterSpec.Builder(
            ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        gen.init(spec)
        return gen.generateKey()
    }

    fun encrypt(plain: ByteArray): ByteArray {
        val iv = Random.Default.nextBytes(IV_LEN)
        val c = Cipher.getInstance(TRANS).apply {
            init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        }
        return iv + c.doFinal(plain)
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > IV_LEN) { "Invalid blob" }
        val iv = blob.copyOfRange(0, IV_LEN)
        val body = blob.copyOfRange(IV_LEN, blob.size)
        val c = Cipher.getInstance(TRANS).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        }
        return c.doFinal(body)
    }
}