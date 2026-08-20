package app.gallager.android.network

import android.util.Base64
import app.gallager.android.crypto.GallagerCrypto
import app.gallager.android.model.ConnectionStatus
import app.gallager.android.model.PairedHost
import app.gallager.android.model.RelaySnapshot
import app.gallager.android.terminal.TerminalTranscript
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class RelayClient(
    private val client: OkHttpClient,
    private val crypto: GallagerCrypto,
    private val host: PairedHost,
    private val deviceId: String,
    private val deviceName: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _snapshot = MutableStateFlow(
        RelaySnapshot(
            status = ConnectionStatus.DISCONNECTED,
            hostName = host.hostName,
        ),
    )
    val snapshot: StateFlow<RelaySnapshot> = _snapshot.asStateFlow()

    private val transcripts = mutableMapOf<String, TerminalTranscript>()
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var keepAliveJob: Job? = null
    private var reconnectAttempt = 0
    private var activePaneId: String? = null
    private var activeScrollbackLines: Int? = null
    private val pendingCommands = ConcurrentHashMap<String, String>()
    @Volatile private var shouldReconnect = false

    fun connect() {
        shouldReconnect = true
        reconnectAttempt = 0
        establishStoredSession()
        openSocket()
    }

    fun close() {
        shouldReconnect = false
        reconnectJob?.cancel()
        keepAliveJob?.cancel()
        pendingCommands.clear()
        webSocket?.close(1000, "Client closed")
        webSocket = null
        crypto.clearSession()
        _snapshot.value = _snapshot.value.copy(
            status = ConnectionStatus.DISCONNECTED,
            statusMessage = "Disconnected",
            hostConnected = false,
            commandInProgress = false,
        )
    }

    fun destroy() {
        close()
        scope.cancel()
    }

    fun startTerminalStream(paneId: String, scrollbackLines: Int? = null) {
        if (activePaneId != paneId) activeScrollbackLines = null
        activePaneId = paneId
        if (scrollbackLines != null) activeScrollbackLines = scrollbackLines
        sendEncrypted(GallagerProtocol.startTerminalStream(paneId, scrollbackLines))
    }

    fun stopTerminalStream(paneId: String) {
        if (activePaneId == paneId) {
            activePaneId = null
            activeScrollbackLines = null
        }
        sendEncrypted(GallagerProtocol.stopTerminalStream(paneId))
    }

    fun sendInput(paneId: String, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        sendEncrypted(GallagerProtocol.sendRawInput(paneId, bytes))
    }

    fun createSession(name: String, workingDirectory: String?, configDir: String?, pluginId: String) {
        sendManagedCommand(
            GallagerProtocol.createTmuxSession(
                sessionName = name,
                workingDirectory = workingDirectory,
                configDir = configDir,
                pluginId = pluginId,
            ),
            "Session created",
        )
    }

    fun createWindow(sessionName: String, workingDirectory: String?) {
        sendManagedCommand(
            GallagerProtocol.createTmuxWindow(sessionName, workingDirectory),
            "Terminal window created",
        )
    }

    fun splitPane(paneId: String, horizontal: Boolean) {
        sendManagedCommand(
            GallagerProtocol.splitTmuxPane(paneId, horizontal),
            if (horizontal) "Pane split to the right" else "Pane split below",
        )
    }

    fun killWindow(windowId: String) {
        sendManagedCommand(GallagerProtocol.killTmuxWindow(windowId), "Terminal window closed")
    }

    fun killSession(sessionName: String) {
        sendManagedCommand(GallagerProtocol.killTmuxSession(sessionName), "Session closed")
    }

    fun clearCommandFeedback() {
        _snapshot.value = _snapshot.value.copy(commandFeedback = null, commandFailed = false)
    }

    private fun openSocket() {
        if (!shouldReconnect) return
        _snapshot.value = _snapshot.value.copy(
            status = ConnectionStatus.CONNECTING,
            statusMessage = "Connecting…",
            error = null,
        )

        val base = GallagerProtocol.normalizeHttpBase(host.serverUrl).toHttpUrl()
        val url = base.newBuilder()
            .addPathSegments("api/ws")
            .addQueryParameter("pairId", host.pairId)
            .addQueryParameter("deviceType", "viewer")
            .addQueryParameter("deviceId", deviceId)
            .addQueryParameter("clientVersion", GallagerProtocol.APP_VERSION)
            .build()
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, Listener())
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            this@RelayClient.webSocket = webSocket
            reconnectAttempt = 0
            sendRegistration()
            scope.launch {
                delay(1_000)
                if (this@RelayClient.webSocket === webSocket) sendRegistration()
            }
            keepAliveJob?.cancel()
            keepAliveJob = scope.launch {
                while (isActive && shouldReconnect) {
                    delay(20_000)
                    sendPlain(GallagerProtocol.ping())
                }
            }
            _snapshot.value = _snapshot.value.copy(
                status = ConnectionStatus.CONNECTED,
                statusMessage = "Relay connected",
            )
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch { handleText(text) }
        }

        override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
            scope.launch { handleText(bytes.utf8()) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            handleDisconnect(reason.ifBlank { "Connection closed" })
        }

        override fun onFailure(webSocket: WebSocket, error: Throwable, response: Response?) {
            handleDisconnect(error.message ?: "WebSocket failed")
        }
    }

    private fun sendRegistration() {
        sendPlain(
            GallagerProtocol.registerViewer(
                pairId = host.pairId,
                deviceId = deviceId,
                deviceName = deviceName,
                publicKey = Base64.encodeToString(crypto.keyMaterial.publicKey, Base64.NO_WRAP),
                publicKeyId = crypto.keyMaterial.keyId,
            ),
        )
    }

    private suspend fun handleText(text: String) {
        runCatching {
            val frame = parseOuterFrame(text)
            if (frame.type == "encrypted") {
                val payload = frame.payload ?: error("Missing encrypted frame payload")
                val plaintext = crypto.decrypt(GallagerProtocol.parseEncryptedPayload(payload))
                handleText(plaintext.toString(Charsets.UTF_8))
                return
            }

            when (frame.type) {
                "viewerRegistered" -> handleViewerRegistered(frame.payload)
                "hostConnected" -> handleHostConnected(frame.payload)
                "peerHello" -> handlePeerHello(frame.payload)
                "sessionState" -> handleSessionState(frame.payload)
                "terminalStream" -> handleTerminalStream(frame.payload)
                "hostDisconnected" -> _snapshot.value = _snapshot.value.copy(
                    status = ConnectionStatus.HOST_OFFLINE,
                    statusMessage = "Mac is offline",
                    hostConnected = false,
                )
                "hostSubscriptionInactive" -> failTerminal("Mac relay subscription is inactive")
                "unpaired" -> {
                    shouldReconnect = false
                    failTerminal("Pairing was removed on the Mac")
                }
                "ping" -> sendPlain(GallagerProtocol.pong())
                "commandResponse" -> handleCommandResponse(frame.payload)
                "pluginPresentations" -> handlePluginPresentations(frame.payload)
                "pong", "agentSessionStatus", "agentNotification" -> Unit
                "error" -> handleServerError(frame.payload)
            }
        }.onFailure { error ->
            _snapshot.value = _snapshot.value.copy(error = error.message ?: "Protocol error")
        }
    }

    private fun handleViewerRegistered(payload: JsonObject?) {
        payload ?: return
        val success = (payload["success"] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull() ?: false
        if (!success) {
            failTerminal(payload.string("error") ?: "Viewer registration failed")
            return
        }
        val publicKey = payload.string("hostPublicKey")
        if (publicKey != null) establishAndHello(publicKey)
        _snapshot.value = _snapshot.value.copy(
            hostName = payload.string("hostDeviceName") ?: _snapshot.value.hostName,
        )
    }

    private fun handleHostConnected(payload: JsonObject?) {
        val publicKey = payload?.string("publicKey") ?: return
        establishAndHello(publicKey)
        _snapshot.value = _snapshot.value.copy(
            hostName = payload.string("deviceName") ?: _snapshot.value.hostName,
        )
    }

    private fun establishAndHello(publicKeyBase64: String) {
        crypto.establishSession(Base64.decode(publicKeyBase64, Base64.DEFAULT), host.pairId)
        sendEncrypted(GallagerProtocol.peerHello())
    }

    private fun handlePeerHello(payload: JsonObject?) {
        payload ?: return
        val hostVersion = payload.string("appVersion").orEmpty()
        val requiredViewer = payload.string("minRequiredPartnerVersion").orEmpty()
        if (requiredViewer.isNotBlank() && compareVersions(GallagerProtocol.APP_VERSION, requiredViewer) < 0) {
            failTerminal("Android app must be updated to $requiredViewer or later")
            return
        }
        if (compareVersions(hostVersion, GallagerProtocol.MIN_HOST_VERSION) < 0) {
            failTerminal("Mac Gallager $hostVersion is too old; version ${GallagerProtocol.MIN_HOST_VERSION}+ is required")
            return
        }
        _snapshot.value = _snapshot.value.copy(
            status = ConnectionStatus.CONNECTED,
            statusMessage = "Connected to Mac",
            hostConnected = true,
            error = null,
        )
        sendEncrypted(GallagerProtocol.requestSessionState())
        activePaneId?.let {
            sendEncrypted(GallagerProtocol.startTerminalStream(it, activeScrollbackLines))
        }
    }

    private fun handleSessionState(payload: JsonObject?) {
        payload ?: return
        _snapshot.value = _snapshot.value.copy(
            panes = GallagerProtocol.parsePanes(payload),
            projects = GallagerProtocol.parseProjects(payload),
            projectsLoaded = true,
            homeDirectory = payload.string("homeDirectory").orEmpty(),
        )
    }

    private fun handlePluginPresentations(payload: JsonObject?) {
        payload ?: return
        _snapshot.value = _snapshot.value.copy(
            pluginPresentations = GallagerProtocol.parsePluginPresentations(payload)
                .associateBy { it.id },
        )
    }

    private fun handleCommandResponse(payload: JsonObject?) {
        payload ?: return
        val response = GallagerProtocol.parseCommandResponse(payload)
        val successMessage = pendingCommands.remove(response.commandId) ?: return
        _snapshot.value = _snapshot.value.copy(
            commandInProgress = pendingCommands.isNotEmpty(),
            commandFeedback = if (response.success) successMessage else response.error ?: "Command failed",
            commandFailed = !response.success,
        )
        if (response.success) {
            sendEncrypted(GallagerProtocol.requestSessionState())
            scope.launch {
                delay(350)
                sendEncrypted(GallagerProtocol.requestSessionState())
            }
        }
    }

    private fun handleTerminalStream(payload: JsonObject?) {
        payload ?: return
        val update = GallagerProtocol.terminalUpdate(payload) ?: return
        val transcript = transcripts.getOrPut(update.paneId) { TerminalTranscript() }
        when (update.type) {
            TerminalUpdateType.INITIAL -> transcript.reset(
                bytes = update.bytes ?: ByteArray(0),
                columns = update.width,
                rows = update.height,
            )
            TerminalUpdateType.CHUNK -> transcript.feed(update.bytes ?: ByteArray(0))
            TerminalUpdateType.DIMENSION -> transcript.resize(
                columns = update.width ?: return,
                rows = update.height ?: return,
            )
            TerminalUpdateType.END -> Unit
        }
        _snapshot.value = _snapshot.value.copy(
            terminalContent = _snapshot.value.terminalContent + (update.paneId to transcript.render()),
        )
    }

    private fun handleServerError(payload: JsonObject?) {
        val code = payload?.string("code")
        val message = payload?.string("message") ?: "Relay error"
        if (code == "INVALID_PAIR" || code == "CLIENT_TOO_OLD") shouldReconnect = false
        failTerminal(message)
    }

    private fun establishStoredSession() {
        runCatching {
            crypto.establishSession(Base64.decode(host.partnerPublicKey, Base64.DEFAULT), host.pairId)
        }.onFailure {
            failTerminal("Could not restore encryption session: ${it.message}")
        }
    }

    private fun sendEncrypted(innerMessage: String) {
        runCatching {
            require(crypto.isSessionEstablished) { "Encryption session is not ready" }
            val payload = crypto.encrypt(innerMessage.toByteArray(Charsets.UTF_8))
            sendPlain(GallagerProtocol.encrypted(payload))
        }.onFailure {
            _snapshot.value = _snapshot.value.copy(error = it.message ?: "Could not encrypt command")
        }
    }

    private fun sendManagedCommand(request: CommandRequest, successMessage: String) {
        if (!_snapshot.value.hostConnected) {
            _snapshot.value = _snapshot.value.copy(
                commandFeedback = "Mac is not connected",
                commandFailed = true,
            )
            return
        }
        pendingCommands[request.id] = successMessage
        _snapshot.value = _snapshot.value.copy(
            commandInProgress = true,
            commandFeedback = null,
            commandFailed = false,
        )
        sendEncrypted(request.message)
        scope.launch {
            delay(COMMAND_TIMEOUT_MILLIS)
            if (pendingCommands.remove(request.id) != null) {
                _snapshot.value = _snapshot.value.copy(
                    commandInProgress = pendingCommands.isNotEmpty(),
                    commandFeedback = "Mac did not respond. Please try again.",
                    commandFailed = true,
                )
            }
        }
    }

    private fun sendPlain(text: String) {
        webSocket?.send(text)
    }

    private fun handleDisconnect(reason: String) {
        if (webSocket == null && reconnectJob?.isActive == true) return
        webSocket = null
        keepAliveJob?.cancel()
        pendingCommands.clear()
        _snapshot.value = _snapshot.value.copy(
            status = ConnectionStatus.DISCONNECTED,
            statusMessage = reason,
            hostConnected = false,
            commandInProgress = false,
        )
        if (!shouldReconnect) return
        reconnectAttempt++
        val delaySeconds = min(60, 1 shl min(6, reconnectAttempt - 1))
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(TimeUnit.SECONDS.toMillis(delaySeconds.toLong()))
            if (shouldReconnect) openSocket()
        }
    }

    private fun failTerminal(message: String) {
        _snapshot.value = _snapshot.value.copy(
            status = ConnectionStatus.ERROR,
            statusMessage = message,
            hostConnected = false,
            error = message,
        )
    }

    private fun compareVersions(left: String, right: String): Int {
        if (left.isBlank()) return -1
        val a = left.split('.').map { it.toIntOrNull() ?: 0 }
        val b = right.split('.').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = a.getOrElse(index) { 0 }.compareTo(b.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.contentOrNull

    companion object {
        private const val COMMAND_TIMEOUT_MILLIS = 15_000L
    }
}
