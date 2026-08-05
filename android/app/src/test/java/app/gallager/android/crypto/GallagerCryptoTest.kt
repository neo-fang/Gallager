package app.gallager.android.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GallagerCryptoTest {
    @Test
    fun pairedDevicesDeriveSameKeyAndExchangeCiphertext() {
        val android = GallagerCrypto.generate()
        val mac = GallagerCrypto.generate()

        android.establishSession(mac.keyMaterial.publicKey, "pair-123")
        mac.establishSession(android.keyMaterial.publicKey, "pair-123")

        val plaintext = "{\"type\":\"requestSessionState\"}".toByteArray()
        val payload = android.encrypt(plaintext)

        assertTrue(android.isSessionEstablished)
        assertArrayEquals(plaintext, mac.decrypt(payload))
        assertFalse(payload.ciphertext.contentEquals(plaintext))
    }

    @Test(expected = Exception::class)
    fun tamperedCiphertextIsRejected() {
        val one = GallagerCrypto.generate()
        val two = GallagerCrypto.generate()
        one.establishSession(two.keyMaterial.publicKey, "pair-123")
        two.establishSession(one.keyMaterial.publicKey, "pair-123")

        val payload = one.encrypt("secret".toByteArray())
        payload.ciphertext[payload.ciphertext.lastIndex] =
            (payload.ciphertext.last().toInt() xor 1).toByte()
        two.decrypt(payload)
    }
}
