package app.gallager.android.network

import app.gallager.android.model.EncryptedPayload
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    @Test
    fun parsesTerminalDimensionChanges() {
        val payload = GallagerProtocol.json.parseToJsonElement(
            """{
              "paneId":"%6",
              "updateType":{"dimensionChange":{"_0":{"width":225,"height":61}}}
            }""",
        ).jsonObject

        val update = GallagerProtocol.terminalUpdate(payload)

        assertEquals(TerminalUpdateType.DIMENSION, update?.type)
        assertEquals(225, update?.width)
        assertEquals(61, update?.height)
    }

    @Test
    fun emitsSwiftCompatibleCreateAndSplitCommands() {
        val create = parseOuterFrame(
            GallagerProtocol.createTmuxSession(
                sessionName = "mobile",
                workingDirectory = "/Users/neo/project",
                configDir = "/Users/neo/.codex-alt",
                pluginId = "codex",
            ).message,
        )
        val createPayload = create.payload
            ?.get("command")?.jsonObject
            ?.get("createTmuxSession")?.jsonObject
            ?.get("_0")?.jsonObject

        assertEquals("", create.payload?.get("paneId")?.jsonPrimitive?.content)
        assertEquals("mobile", createPayload?.get("sessionName")?.jsonPrimitive?.content)
        assertEquals(120, createPayload?.get("width")?.jsonPrimitive?.content?.toInt())
        assertEquals("/Users/neo/project", createPayload?.get("workingDirectory")?.jsonPrimitive?.content)
        assertEquals("/Users/neo/.codex-alt", createPayload?.get("configDir")?.jsonPrimitive?.content)
        assertEquals("codex", createPayload?.get("pluginID")?.jsonPrimitive?.content)

        val split = parseOuterFrame(GallagerProtocol.splitTmuxPane("%7", horizontal = false).message)
        val splitPayload = split.payload
            ?.get("command")?.jsonObject
            ?.get("splitTmuxPane")?.jsonObject
            ?.get("_0")?.jsonObject
        assertEquals("%7", split.payload?.get("paneId")?.jsonPrimitive?.content)
        assertEquals("vertical", splitPayload?.get("direction")?.jsonPrimitive?.content)
    }

    @Test
    fun parsesCommandResponseFromMac() {
        val payload = GallagerProtocol.json.parseToJsonElement(
            """{
              "commandId":"00000000-0000-0000-0000-000000000005",
              "success":true,
              "paneId":"%12"
            }""",
        ).jsonObject

        val response = GallagerProtocol.parseCommandResponse(payload)

        assertEquals(true, response.success)
        assertEquals("%12", response.paneId)
    }

    @Test
    fun parsesWindowIdentityForManagementActions() {
        val payload = GallagerProtocol.json.parseToJsonElement(
            """{
              "paneStates": {
                "%3": {
                  "paneId":"%3",
                  "sessionName":"work",
                  "windowIndex":2,
                  "paneIndex":1,
                  "windowName":"editor"
                }
              }
            }""",
        ).jsonObject

        val pane = GallagerProtocol.parsePanes(payload).single()

        assertEquals("work:2", pane.windowId)
        assertEquals(1, pane.paneIndex)
    }

    @Test
    fun parsesProjectsAndPluginPresentationsFromMac() {
        val sessionPayload = GallagerProtocol.json.parseToJsonElement(
            """{
              "pairId":"pair-1",
              "homeDirectory":"/Users/neo",
              "paneStates":{},
              "agentProjects":[
                {
                  "name":"vaka",
                  "path":"/Users/neo/llm-develop/vaka",
                  "lastUsed":"2026-08-05T06:30:00Z",
                  "configDir":"/Users/neo/.codex-vaka",
                  "pluginID":"codex"
                },
                {
                  "name":"docs",
                  "path":"/Users/neo/docs",
                  "pluginID":"claude-code"
                }
              ]
            }""",
        ).jsonObject

        val projects = GallagerProtocol.parseProjects(sessionPayload)

        assertEquals(2, projects.size)
        assertEquals("codex:/Users/neo/llm-develop/vaka", projects[0].id)
        assertEquals("/Users/neo/.codex-vaka", projects[0].configDir)

        val presentationPayload = GallagerProtocol.json.parseToJsonElement(
            """{
              "pairId":"pair-1",
              "presentations":[
                {
                  "id":"codex",
                  "version":"1.0.0",
                  "displayName":"Codex",
                  "shortName":"codex",
                  "color":"#22D3EE"
                }
              ]
            }""",
        ).jsonObject

        val presentation = GallagerProtocol.parsePluginPresentations(presentationPayload).single()

        assertEquals("codex", presentation.id)
        assertEquals("#22D3EE", presentation.color)
    }
}
