package app.gallager.android.network

import app.gallager.android.model.EncryptedPayload
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Base64

class GallagerProtocolTest {
    @Test
    fun parsesSwiftSynthesizedPairingEnum() {
        val body = """
            {
              "paired": {
                "_0": {
                  "pairId": "pair-1",
                  "partnerDeviceName": "Studio Mac",
                  "partnerPublicKey": "AQID",
                  "partnerPublicKeyId": "host-key",
                  "partnerUsername": "neo"
                }
              }
            }
        """.trimIndent()

        val host = GallagerProtocol.parsePairingResponse(body, "https://relay.example.com").host

        assertNotNull(host)
        assertEquals("pair-1", host?.pairId)
        assertEquals("wss://relay.example.com", host?.serverUrl)
        assertEquals("neo", host?.username)
    }

    @Test
    fun emitsSwiftCompatibleCommandAssociatedValue() {
        val frame = parseOuterFrame(GallagerProtocol.sendRawInput("%7", byteArrayOf(0x0d)))
        val command = frame.payload?.get("command")?.jsonObject
        val encoded = command?.get("sendRawInput")?.jsonObject?.get("_0")?.jsonObject

        assertEquals("command", frame.type)
        assertEquals("%7", frame.payload?.get("paneId")?.toString()?.trim('"'))
        assertEquals("DQ==", encoded?.get("dataBase64")?.toString()?.trim('"'))
    }

    @Test
    fun encryptedFrameRoundTripsBase64() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val frame = parseOuterFrame(
            GallagerProtocol.encrypted(EncryptedPayload(bytes, "android-key")),
        )
        val parsed = GallagerProtocol.parseEncryptedPayload(requireNotNull(frame.payload))

        assertArrayEquals(bytes, parsed.ciphertext)
        assertEquals("android-key", parsed.senderKeyId)
        assertEquals(1, parsed.version)
    }

    @Test
    fun parsesSwiftTerminalEnumPayload() {
        val content = Base64.getEncoder().encodeToString("hello".toByteArray())
        val payload = GallagerProtocol.json.parseToJsonElement(
            """{
              "id":"00000000-0000-0000-0000-000000000000",
              "paneId":"%1",
              "timestamp":"2026-08-05T00:00:00Z",
              "updateType":{"initialState":{"_0":{"width":80,"height":24,"contentBase64":"$content"}}}
            }""",
        ).jsonObject

        val update = GallagerProtocol.terminalUpdate(payload)

        assertEquals(TerminalUpdateType.INITIAL, update?.type)
        assertEquals("hello", update?.bytes?.toString(Charsets.UTF_8))
    }
}
