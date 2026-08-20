package app.gallager.android

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.gallager.android.data.PairingStore
import app.gallager.android.model.PairedHost
import app.gallager.android.model.PaneSummary
import app.gallager.android.model.RelaySnapshot
import app.gallager.android.network.RelayClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GallagerUiState(
    val pairedHost: PairedHost? = null,
    val relay: RelaySnapshot = RelaySnapshot(),
    val selectedPaneId: String? = null,
    val pairingInProgress: Boolean = false,
    val error: String? = null,
)

class GallagerViewModel(private val application: GallagerApplication) : ViewModel() {
    private val _uiState = MutableStateFlow(
        GallagerUiState(pairedHost = application.pairingStore.loadHost()),
    )
    val uiState: StateFlow<GallagerUiState> = _uiState.asStateFlow()

    val defaultDeviceName: String = application.pairingStore.defaultDeviceName
    val defaultServerUrl: String = PairingStore.DEFAULT_SERVER_URL

    private var relayClient: RelayClient? = null
    private var relayCollection: Job? = null
    private var terminalSnapshotRefresh: Job? = null
    private var streamingPaneId: String? = null
    private val requestedHistoryLines = mutableMapOf<String, Int>()

    init {
        _uiState.value.pairedHost?.let(::connect)
    }

    fun pair(serverUrl: String, code: String, deviceName: String) {
        if (_uiState.value.pairingInProgress) return
        val normalizedCode = code.trim().uppercase()
        if (!normalizedCode.matches(Regex("[A-Z]{6}"))) {
            _uiState.update { it.copy(error = "Pairing code must contain six letters") }
            return
        }
        if (deviceName.isBlank()) {
            _uiState.update { it.copy(error = "Device name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(pairingInProgress = true, error = null) }
            runCatching {
                application.pairingApi.complete(
                    serverUrl = serverUrl,
                    pairingCode = normalizedCode,
                    deviceId = application.pairingStore.deviceId,
                    deviceName = deviceName.trim(),
                    publicKey = Base64.encodeToString(application.crypto.keyMaterial.publicKey, Base64.NO_WRAP),
                    publicKeyId = application.crypto.keyMaterial.keyId,
                )
            }.onSuccess { result ->
                val host = result.host
                if (host != null) {
                    application.pairingStore.setDeviceName(deviceName)
                    application.pairingStore.saveHost(host)
                    _uiState.update {
                        it.copy(
                            pairedHost = host,
                            pairingInProgress = false,
                            error = null,
                        )
                    }
                    connect(host)
                } else {
                    _uiState.update {
                        it.copy(pairingInProgress = false, error = result.error ?: "Pairing failed")
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(pairingInProgress = false, error = error.message ?: "Pairing failed")
                }
            }
        }
    }

    fun retryConnection() {
        _uiState.value.pairedHost?.let(::connect)
    }

    fun selectPane(pane: PaneSummary) {
        val previous = streamingPaneId
        if (previous != null && previous != pane.paneId) relayClient?.stopTerminalStream(previous)
        _uiState.update { it.copy(selectedPaneId = pane.paneId) }
        if (previous != pane.paneId) {
            streamingPaneId = pane.paneId
            relayClient?.startTerminalStream(pane.paneId)
        }
    }

    fun closeTerminal() {
        // Keep the one active stream subscribed while the user visits the
        // session list. Re-entering the same pane can then reuse its current
        // screen instead of racing stop/start commands through the relay.
        _uiState.update { it.copy(selectedPaneId = null) }
    }

    fun sendInput(bytes: ByteArray) {
        val paneId = _uiState.value.selectedPaneId ?: return
        relayClient?.sendInput(paneId, bytes)
    }

    /**
     * Re-captures the authoritative host screen after a remote TUI scroll.
     * Full-screen apps redraw asynchronously, so waiting briefly avoids
     * capturing the pane before the wheel event has been rendered.
     */
    fun refreshTerminalSnapshot() {
        val paneId = _uiState.value.selectedPaneId ?: return
        if (!_uiState.value.relay.hostConnected) return
        terminalSnapshotRefresh?.cancel()
        terminalSnapshotRefresh = viewModelScope.launch {
            delay(180)
            if (_uiState.value.selectedPaneId == paneId) {
                relayClient?.startTerminalStream(paneId)
            }
        }
    }

    /** Requests a progressively deeper authoritative terminal snapshot. */
    fun requestEarlierHistory(): Boolean {
        val paneId = _uiState.value.selectedPaneId ?: return false
        if (!_uiState.value.relay.hostConnected) return false
        val current = requestedHistoryLines[paneId] ?: 0
        val next = when {
            current < 1_000 -> 1_000
            current < 2_500 -> 2_500
            current < 5_000 -> 5_000
            current < 10_000 -> 10_000
            else -> return false
        }
        requestedHistoryLines[paneId] = next
        relayClient?.startTerminalStream(paneId, scrollbackLines = next)
        return true
    }

    fun createSession(name: String, workingDirectory: String?, configDir: String?, pluginId: String) {
        relayClient?.createSession(
            name = name.trim(),
            workingDirectory = workingDirectory?.trim(),
            configDir = configDir?.trim(),
            pluginId = pluginId,
        )
    }

    fun createWindow(sessionName: String, workingDirectory: String?) {
        relayClient?.createWindow(sessionName, workingDirectory?.trim())
    }

    fun splitPane(horizontal: Boolean) {
        val paneId = _uiState.value.selectedPaneId ?: return
        relayClient?.splitPane(paneId, horizontal)
    }

    fun closeWindow(pane: PaneSummary) {
        relayClient?.killWindow(pane.windowId)
        closeTerminal()
    }

    fun closeSession(pane: PaneSummary) {
        relayClient?.killSession(pane.sessionName)
        closeTerminal()
    }

    fun clearCommandFeedback() {
        relayClient?.clearCommandFeedback()
    }

    fun unpair() {
        val host = _uiState.value.pairedHost ?: return
        viewModelScope.launch {
            relayCollection?.cancel()
            relayClient?.destroy()
            relayClient = null
            streamingPaneId = null
            requestedHistoryLines.clear()
            runCatching { application.pairingApi.unpair(host.serverUrl, host.pairId) }
            application.pairingStore.clearHost()
            application.crypto.clearSession()
            _uiState.value = GallagerUiState()
        }
    }

    private fun connect(host: PairedHost) {
        relayCollection?.cancel()
        relayClient?.destroy()
        streamingPaneId = null
        val client = RelayClient(
            client = application.httpClient,
            crypto = application.crypto,
            host = host,
            deviceId = application.pairingStore.deviceId,
            deviceName = application.pairingStore.defaultDeviceName,
        )
        relayClient = client
        relayCollection = viewModelScope.launch {
            client.snapshot.collectLatest { snapshot ->
                _uiState.update { state -> state.copy(relay = snapshot) }
            }
        }
        client.connect()
    }

    override fun onCleared() {
        relayClient?.destroy()
        super.onCleared()
    }

    class Factory(private val application: GallagerApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GallagerViewModel::class.java))
            return GallagerViewModel(application) as T
        }
    }
}
