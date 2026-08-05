package app.gallager.android.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.gallager.android.model.KeyMaterial
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getOrCreate(): GallagerCrypto {
        val encrypted = preferences.getString(KEY_PRIVATE_KEY, null)
        val iv = preferences.getString(KEY_PRIVATE_KEY_IV, null)
        val keyId = preferences.getString(KEY_ID, null)
        if (encrypted != null && iv != null && keyId != null) {
            return runCatching {
                GallagerCrypto.fromPrivateKey(
                    decrypt(Base64.decode(encrypted, Base64.NO_WRAP), Base64.decode(iv, Base64.NO_WRAP)),
                    keyId,
                )
            }.getOrElse {
                preferences.edit().clear().apply()
                createAndPersist()
            }
        }
        return createAndPersist()
    }

    private fun createAndPersist(): GallagerCrypto {
        val crypto = GallagerCrypto.generate()
        val (encrypted, iv) = encrypt(crypto.keyMaterial.privateKey)
        preferences.edit()
            .putString(KEY_PRIVATE_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_PRIVATE_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .putString(KEY_ID, crypto.keyMaterial.keyId)
            .apply()
        return crypto
    }

    private fun encrypt(plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        return cipher.doFinal(plaintext) to cipher.iv
    }

    private fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun wrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFERENCES_NAME = "gallager_android_keys"
        private const val KEY_PRIVATE_KEY = "private_key"
        private const val KEY_PRIVATE_KEY_IV = "private_key_iv"
        private const val KEY_ID = "key_id"
        private const val KEYSTORE_ALIAS = "gallager_android_wrapping_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
