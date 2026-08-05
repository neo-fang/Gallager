package app.gallager.android.network

import app.gallager.android.model.EncryptedPayload
import app.gallager.android.model.PairedHost
import app.gallager.android.model.PairingResult
import app.gallager.android.model.PaneSummary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

object GallagerProtocol {
    const val APP_VERSION = "2.0.0"
    const val MIN_HOST_VERSION = "2.0"

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun pairingCompletion(
        pairingCode: String,
        deviceId: String,
        deviceName: String,
        publicKey: String,
        publicKeyId: String,
    ): String = buildJsonObject {
        put("pairingCode", pairingCode)
        put("deviceId", deviceId)
        put("deviceName", deviceName)
        put("publicKey", publicKey)
        put("publicKeyId", publicKeyId)
    }.toString()

    fun parsePairingResponse(body: String, serverUrl: String): PairingResult {
        val root = json.parseToJsonElement(body).jsonObject
        root.enumPayload("paired")?.let { info ->
            return PairingResult(
                host = PairedHost(
                    pairId = info.requiredString("pairId"),
                    hostName = info.requiredString("partnerDeviceName"),
                    username = info.string("partnerUsername").orEmpty(),
                    partnerPublicKey = info.requiredString("partnerPublicKey"),
                    partnerPublicKeyId = info.requiredString("partnerPublicKeyId"),
                    serverUrl = normalizeWebSocketBase(serverUrl),
                ),
            )
        }
        root.enumPayload("error")?.let { error ->
            return PairingResult(error = error.string("message") ?: "Pairing failed")
        }
        return PairingResult(error = "Unexpected pairing response")
    }

    fun registerViewer(
        pairId: String,
        deviceId: String,
        deviceName: String,
        publicKey: String,
        publicKeyId: String,
    ): String = frame(
        "registerViewer",
        buildJsonObject {
            put("pairId", pairId)
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("publicKey", publicKey)
            put("publicKeyId", publicKeyId)
        },
    )

    fun peerHello(): String = frame(
        "peerHello",
        buildJsonObject {
            put("appVersion", APP_VERSION)
            put("minRequiredPartnerVersion", MIN_HOST_VERSION)
        },
    )

    fun requestSessionState(): String = frame("requestSessionState")

    fun ping(): String = frame("ping")

    fun pong(): String = frame("pong")

    fun startTerminalStream(paneId: String): String = commandFrame(
        paneId,
        "startTerminalStream",
        buildJsonObject { },
    )

    fun stopTerminalStream(paneId: String): String = commandFrame(
        paneId,
        "stopTerminalStream",
        buildJsonObject { },
    )

    fun sendRawInput(paneId: String, bytes: ByteArray): String = commandFrame(
        paneId,
        "sendRawInput",
        buildJsonObject {
            put("dataBase64", Base64.getEncoder().encodeToString(bytes))
        },
    )

    fun encrypted(payload: EncryptedPayload): String = frame(
        "encrypted",
        buildJsonObject {
            put(
                "payload",
                buildJsonObject {
                    put("ciphertext", Base64.getEncoder().encodeToString(payload.ciphertext))
                    put("senderKeyId", payload.senderKeyId)
                    put("version", payload.version)
                },
            )
        },
    )

    fun parseEncryptedPayload(payload: JsonObject): EncryptedPayload {
        val encryptedPayload = payload["payload"]?.jsonObject ?: error("Missing encrypted payload")
        return EncryptedPayload(
            ciphertext = Base64.getDecoder().decode(encryptedPayload.requiredString("ciphertext")),
            senderKeyId = encryptedPayload.requiredString("senderKeyId"),
            version = encryptedPayload["version"]?.jsonPrimitive?.intOrNull ?: 1,
        )
    }

    fun parsePanes(payload: JsonObject): List<PaneSummary> {
        val paneStates = payload["paneStates"] as? JsonObject ?: return emptyList()
        return paneStates.entries.mapNotNull { (paneId, element) ->
            runCatching {
                val pane = element.jsonObject
                val agentSession = pane["agentSession"] as? JsonObject
                PaneSummary(
                    paneId = pane.string("paneId") ?: paneId,
                    sessionName = pane.string("sessionName").orEmpty(),
                    windowName = pane.string("windowName").orEmpty(),
                    terminalTitle = pane.string("terminalTitle"),
                    currentPath = pane.string("currentPath"),
                    gitBranch = pane.string("gitBranch"),
                    pluginId = agentSession?.string("pluginID"),
                    state = agentSession?.get("state")?.enumCaseName() ?: "terminal",
                    customDescription = pane.string("customDescription"),
                    customEmoji = pane.string("customEmoji"),
                )
            }.getOrNull()
        }.sortedWith(compareBy<PaneSummary> { it.sessionName }.thenBy { it.paneId })
    }

    fun terminalUpdate(payload: JsonObject): TerminalUpdate? {
        val paneId = payload.string("paneId") ?: return null
        val update = payload["updateType"] as? JsonObject ?: return null
        val type = update.keys.firstOrNull() ?: return null
        val value = update[type]
        val data = (value as? JsonObject)?.let { it["_0"] ?: it } as? JsonObject
        return when (type) {
            "initialState" -> TerminalUpdate(
                paneId = paneId,
                type = TerminalUpdateType.INITIAL,
                bytes = data?.string("contentBase64")?.let(Base64.getDecoder()::decode),
            )
            "dataChunk" -> TerminalUpdate(
                paneId = paneId,
                type = TerminalUpdateType.CHUNK,
                bytes = data?.string("dataBase64")?.let(Base64.getDecoder()::decode),
            )
            "streamEnd" -> TerminalUpdate(paneId, TerminalUpdateType.END)
            else -> null
        }
    }

    fun normalizeWebSocketBase(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        return when {
            trimmed.startsWith("https://") -> "wss://${trimmed.removePrefix("https://")}".removeSuffix("/api/ws")
            trimmed.startsWith("http://") -> "ws://${trimmed.removePrefix("http://")}".removeSuffix("/api/ws")
            trimmed.startsWith("wss://") || trimmed.startsWith("ws://") -> trimmed.removeSuffix("/api/ws")
            else -> "wss://$trimmed".removeSuffix("/api/ws")
        }
    }

    fun normalizeHttpBase(input: String): String {
        val ws = normalizeWebSocketBase(input)
        return when {
            ws.startsWith("wss://") -> "https://${ws.removePrefix("wss://")}".trimEnd('/')
            else -> "http://${ws.removePrefix("ws://")}".trimEnd('/')
        }
    }

    private fun commandFrame(paneId: String, commandName: String, commandPayload: JsonObject): String {
        val associatedValue = buildJsonObject { put("_0", commandPayload) }
        return frame(
            "command",
            buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("paneId", paneId)
                put("command", buildJsonObject { put(commandName, associatedValue) })
                put("timestamp", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())
            },
        )
    }

    private fun frame(type: String, payload: JsonElement? = null): String = buildJsonObject {
        put("type", type)
        if (payload != null) put("payload", payload)
    }.toString()

    private fun JsonObject.enumPayload(name: String): JsonObject? {
        val wrapper = this[name] as? JsonObject ?: return null
        return (wrapper["_0"] ?: wrapper) as? JsonObject
    }

    private fun JsonElement.enumCaseName(): String =
        (this as? JsonObject)?.keys?.firstOrNull()
            ?: (this as? JsonPrimitive)?.contentOrNull
            ?: "unknown"

    private fun JsonObject.requiredString(name: String): String =
        string(name) ?: error("Missing $name")

    private fun JsonObject.string(name: String): String? {
        val value = this[name] ?: return null
        if (value is JsonNull) return null
        return (value as? JsonPrimitive)?.contentOrNull
    }
}

data class OuterFrame(
    val type: String,
    val payload: JsonObject?,
)

fun parseOuterFrame(text: String): OuterFrame {
    val root = GallagerProtocol.json.parseToJsonElement(text).jsonObject
    return OuterFrame(
        type = root["type"]?.jsonPrimitive?.content ?: error("Missing frame type"),
        payload = root["payload"] as? JsonObject,
    )
}

enum class TerminalUpdateType { INITIAL, CHUNK, END }

data class TerminalUpdate(
    val paneId: String,
    val type: TerminalUpdateType,
    val bytes: ByteArray? = null,
)
