package app.gallager.android.crypto

import app.gallager.android.model.EncryptedPayload
import app.gallager.android.model.KeyMaterial
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class GallagerCrypto(
    val keyMaterial: KeyMaterial,
    private val random: SecureRandom = SecureRandom(),
) {
    @Volatile
    private var sessionKey: ByteArray? = null

    val isSessionEstablished: Boolean
        get() = sessionKey != null

    fun establishSession(partnerPublicKey: ByteArray, pairId: String) {
        require(pairId.isNotBlank()) { "Pair ID must not be blank" }
        require(partnerPublicKey.size == X25519_KEY_SIZE) { "X25519 public key must be 32 bytes" }

        val agreement = X25519Agreement()
        agreement.init(X25519PrivateKeyParameters(keyMaterial.privateKey, 0))
        val sharedSecret = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(X25519PublicKeyParameters(partnerPublicKey, 0), sharedSecret, 0)

        val orderedKeys = listOf(keyMaterial.publicKey, partnerPublicKey)
            .sortedWith(::compareUnsigned)
        val info = pairId.toByteArray(Charsets.UTF_8) + orderedKeys[0] + orderedKeys[1]

        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(
            HKDFParameters(
                sharedSecret,
                PROTOCOL_SALT.toByteArray(Charsets.UTF_8),
                info,
            ),
        )
        sessionKey = ByteArray(SESSION_KEY_SIZE).also { hkdf.generateBytes(it, 0, it.size) }
        sharedSecret.fill(0)
    }

    fun clearSession() {
        sessionKey?.fill(0)
        sessionKey = null
    }

    fun encrypt(plaintext: ByteArray): EncryptedPayload {
        val key = sessionKey?.copyOf() ?: error("E2EE session is not established")
        val nonce = ByteArray(NONCE_SIZE).also(random::nextBytes)
        val cipher = Cipher.getInstance("ChaCha20-Poly1305", PROVIDER)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "ChaCha20"), IvParameterSpec(nonce))
        val ciphertextAndTag = cipher.doFinal(plaintext)
        key.fill(0)
        return EncryptedPayload(
            ciphertext = nonce + ciphertextAndTag,
            senderKeyId = keyMaterial.keyId,
        )
    }

    fun decrypt(payload: EncryptedPayload): ByteArray {
        require(payload.version == ENCRYPTION_VERSION) { "Unsupported encryption version ${payload.version}" }
        require(payload.ciphertext.size >= NONCE_SIZE + TAG_SIZE) { "Encrypted payload is too short" }
        val key = sessionKey?.copyOf() ?: error("E2EE session is not established")
        val nonce = payload.ciphertext.copyOfRange(0, NONCE_SIZE)
        val ciphertextAndTag = payload.ciphertext.copyOfRange(NONCE_SIZE, payload.ciphertext.size)
        val cipher = Cipher.getInstance("ChaCha20-Poly1305", PROVIDER)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "ChaCha20"), IvParameterSpec(nonce))
        return try {
            cipher.doFinal(ciphertextAndTag)
        } finally {
            key.fill(0)
        }
    }

    companion object {
        const val PROTOCOL_SALT = "ClaudeSpy-E2EE-v1"
        const val ENCRYPTION_VERSION = 1
        private const val X25519_KEY_SIZE = 32
        private const val SESSION_KEY_SIZE = 32
        private const val NONCE_SIZE = 12
        private const val TAG_SIZE = 16
        private val PROVIDER = BouncyCastleProvider()

        fun generate(random: SecureRandom = SecureRandom()): GallagerCrypto {
            val privateKey = X25519PrivateKeyParameters(random)
            val privateBytes = privateKey.encoded
            val publicBytes = privateKey.generatePublicKey().encoded
            return GallagerCrypto(
                KeyMaterial(
                    privateKey = privateBytes,
                    publicKey = publicBytes,
                    keyId = UUID.randomUUID().toString().uppercase(),
                ),
                random,
            )
        }

        fun fromPrivateKey(privateKey: ByteArray, keyId: String): GallagerCrypto {
            require(privateKey.size == X25519_KEY_SIZE) { "X25519 private key must be 32 bytes" }
            val privateParameters = X25519PrivateKeyParameters(privateKey, 0)
            return GallagerCrypto(
                KeyMaterial(
                    privateKey = privateKey.copyOf(),
                    publicKey = privateParameters.generatePublicKey().encoded,
                    keyId = keyId,
                ),
            )
        }

        private fun compareUnsigned(left: ByteArray, right: ByteArray): Int {
            for (index in 0 until minOf(left.size, right.size)) {
                val comparison = (left[index].toInt() and 0xff).compareTo(right[index].toInt() and 0xff)
                if (comparison != 0) return comparison
            }
            return left.size.compareTo(right.size)
        }
    }
}
