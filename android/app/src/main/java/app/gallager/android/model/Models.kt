package app.gallager.android.model

import app.gallager.android.terminal.TerminalRender

data class KeyMaterial(
    val privateKey: ByteArray,
    val publicKey: ByteArray,
    val keyId: String,
) {
    override fun equals(other: Any?): Boolean =
        other is KeyMaterial &&
            privateKey.contentEquals(other.privateKey) &&
            publicKey.contentEquals(other.publicKey) &&
            keyId == other.keyId

    override fun hashCode(): Int =
        31 * (31 * privateKey.contentHashCode() + publicKey.contentHashCode()) + keyId.hashCode()
}

data class PairedHost(
    val pairId: String,
    val hostName: String,
    val username: String,
    val partnerPublicKey: String,
    val partnerPublicKeyId: String,
    val serverUrl: String,
)

data class PairingResult(
    val host: PairedHost? = null,
    val error: String? = null,
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    HOST_OFFLINE,
    ERROR,
}

data class PaneSummary(
    val paneId: String,
    val sessionName: String,
    val windowName: String,
    val terminalTitle: String?,
    val currentPath: String?,
    val gitBranch: String?,
    val pluginId: String?,
    val state: String,
    val customDescription: String?,
    val customEmoji: String?,
) {
    val displayName: String
        get() = customDescription
            ?.takeIf { it.isNotBlank() }
            ?: terminalTitle?.takeIf { it.isNotBlank() }
            ?: windowName.takeIf { it.isNotBlank() }
            ?: sessionName.takeIf { it.isNotBlank() }
            ?: paneId
}

data class RelaySnapshot(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val statusMessage: String = "Disconnected",
    val hostConnected: Boolean = false,
    val hostName: String? = null,
    val panes: List<PaneSummary> = emptyList(),
    val terminalContent: Map<String, TerminalRender> = emptyMap(),
    val error: String? = null,
)

data class EncryptedPayload(
    val ciphertext: ByteArray,
    val senderKeyId: String,
    val version: Int = 1,
)
