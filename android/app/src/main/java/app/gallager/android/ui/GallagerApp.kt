package app.gallager.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gallager.android.GallagerViewModel
import app.gallager.android.model.ConnectionStatus
import app.gallager.android.model.PaneSummary

@Composable
fun GallagerApp(viewModel: GallagerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPane = state.selectedPaneId?.let { selected ->
        state.relay.panes.firstOrNull { it.paneId == selected }
    }

    when {
        state.pairedHost == null -> PairingScreen(
            defaultServerUrl = viewModel.defaultServerUrl,
            defaultDeviceName = viewModel.defaultDeviceName,
            loading = state.pairingInProgress,
            error = state.error,
            onPair = viewModel::pair,
        )
        selectedPane != null -> TerminalScreen(
            pane = selectedPane,
            terminalText = state.relay.terminalText[selectedPane.paneId].orEmpty(),
            connected = state.relay.hostConnected,
            onBack = viewModel::closeTerminal,
            onSend = viewModel::sendInput,
        )
        else -> SessionsScreen(
            hostName = state.pairedHost?.hostName ?: "Mac",
            status = state.relay.status,
            statusMessage = state.relay.statusMessage,
            panes = state.relay.panes,
            error = state.relay.error,
            onRefresh = viewModel::retryConnection,
            onSelectPane = viewModel::selectPane,
            onUnpair = viewModel::unpair,
        )
    }
}

@Composable
private fun PairingScreen(
    defaultServerUrl: String,
    defaultDeviceName: String,
    loading: Boolean,
    error: String?,
    onPair: (String, String, String) -> Unit,
) {
    var serverUrl by remember { mutableStateOf(defaultServerUrl) }
    var pairingCode by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf(defaultDeviceName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GallagerBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = GallagerAccent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Connect to Gallager",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Enter the six-letter code from Settings → Remote Access on your Mac.",
            color = GallagerMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = pairingCode,
            onValueChange = { value ->
                pairingCode = value.filter(Char::isLetter).uppercase().take(6)
            },
            label = { Text("Pairing code") },
            placeholder = { Text("ABCDEF") },
            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Device name") },
            leadingIcon = { Icon(Icons.Outlined.Computer, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Relay server") },
            supportingText = { Text("Use the same hosted or self-hosted relay as the Mac") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = error,
                color = GallagerDanger,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onPair(serverUrl, pairingCode, deviceName) },
            enabled = !loading && pairingCode.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(10.dp))
                Text("Pairing…")
            } else {
                Text("Pair device")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionsScreen(
    hostName: String,
    status: ConnectionStatus,
    statusMessage: String,
    panes: List<PaneSummary>,
    error: String?,
    onRefresh: () -> Unit,
    onSelectPane: (PaneSummary) -> Unit,
    onUnpair: () -> Unit,
) {
    var showUnpairConfirmation by remember { mutableStateOf(false) }

    if (showUnpairConfirmation) {
        AlertDialog(
            onDismissRequest = { showUnpairConfirmation = false },
            title = { Text("Unpair this device?") },
            text = { Text("Gallager will remove this Android device from the relay. You will need a new code to connect again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnpairConfirmation = false
                        onUnpair()
                    },
                ) {
                    Text("Unpair", color = GallagerDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        containerColor = GallagerBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(hostName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        ConnectionLabel(status, statusMessage)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.semantics { contentDescription = "Reconnect" },
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                    }
                    IconButton(
                        onClick = { showUnpairConfirmation = true },
                        modifier = Modifier.semantics { contentDescription = "Unpair device" },
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GallagerBackground),
            )
        },
    ) { padding ->
        when {
            panes.isNotEmpty() -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = "SESSIONS",
                        color = GallagerMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                    )
                }
                items(panes, key = { it.paneId }) { pane ->
                    SessionCard(pane = pane, onClick = { onSelectPane(pane) })
                }
            }
            else -> EmptySessions(
                modifier = Modifier.padding(padding),
                status = status,
                statusMessage = statusMessage,
                error = error,
                onRefresh = onRefresh,
            )
        }
    }
}

@Composable
private fun SessionCard(pane: PaneSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Open ${pane.displayName}" },
        colors = CardDefaults.cardColors(containerColor = GallagerSurface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(pane.state)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pane.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                val metadata = listOfNotNull(
                    pane.pluginId,
                    pane.gitBranch?.let { "git:$it" },
                    pane.currentPath?.substringAfterLast('/')?.takeIf { it.isNotBlank() },
                ).joinToString("  ·  ")
                Text(
                    text = metadata.ifBlank { pane.paneId },
                    color = GallagerMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatePill(pane.state)
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = GallagerMuted,
            )
        }
    }
}

@Composable
private fun EmptySessions(
    modifier: Modifier,
    status: ConnectionStatus,
    statusMessage: String,
    error: String?,
    onRefresh: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (status == ConnectionStatus.CONNECTING) Icons.Outlined.Link else Icons.Outlined.Code,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = if (error == null) GallagerMuted else GallagerDanger,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (status == ConnectionStatus.CONNECTING) "Connecting" else "No sessions yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = error ?: statusMessage,
                color = GallagerMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onRefresh, modifier = Modifier.height(48.dp)) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reconnect")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerminalScreen(
    pane: PaneSummary,
    terminalText: String,
    connected: Boolean,
    onBack: () -> Unit,
    onSend: (ByteArray) -> Unit,
) {
    BackHandler(onBack = onBack)
    val scrollState = rememberScrollState()
    var input by remember(pane.paneId) { mutableStateOf("") }
    LaunchedEffect(terminalText.length) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            pane.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (connected) "Live · ${pane.paneId}" else "Mac disconnected",
                            color = if (connected) GallagerAccent else GallagerWarning,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back to sessions" },
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GallagerSurface),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GallagerSurface)
                    .imePadding(),
            ) {
                TerminalKeyRow(onSend)
                HorizontalDivider(color = GallagerBorder)
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Send text to terminal") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        enabled = connected && input.isNotEmpty(),
                        onClick = {
                            onSend(input.toByteArray(Charsets.UTF_8))
                            input = ""
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .semantics { contentDescription = "Send terminal input" },
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                    }
                }
            }
        },
    ) { padding ->
        SelectionContainer {
            Text(
                text = terminalText.ifBlank { "Waiting for terminal stream…" },
                color = if (terminalText.isBlank()) GallagerMuted else Color(0xFFE2E8F0),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun TerminalKeyRow(onSend: (ByteArray) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TerminalKey("Esc") { onSend(byteArrayOf(0x1b)) }
        TerminalKey("Ctrl-C") { onSend(byteArrayOf(0x03)) }
        TerminalKey("Tab") { onSend(byteArrayOf(0x09)) }
        TerminalKey("←") { onSend(byteArrayOf(0x1b, 0x5b, 0x44)) }
        TerminalKey("↑") { onSend(byteArrayOf(0x1b, 0x5b, 0x41)) }
        TerminalKey("↓") { onSend(byteArrayOf(0x1b, 0x5b, 0x42)) }
        TerminalKey("→") { onSend(byteArrayOf(0x1b, 0x5b, 0x43)) }
        TerminalKey("Backspace") { onSend(byteArrayOf(0x7f)) }
        TerminalKey("Enter") { onSend(byteArrayOf(0x0d)) }
    }
}

@Composable
private fun TerminalKey(label: String, action: () -> Unit) {
    FilledTonalButton(
        onClick = action,
        modifier = Modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 13.dp),
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
private fun ConnectionLabel(status: ConnectionStatus, message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    when (status) {
                        ConnectionStatus.CONNECTED -> GallagerAccent
                        ConnectionStatus.CONNECTING -> GallagerWarning
                        ConnectionStatus.ERROR -> GallagerDanger
                        else -> GallagerMuted
                    },
                ),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = message,
            color = GallagerMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusDot(state: String) {
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(RoundedCornerShape(50))
            .background(stateColor(state)),
    )
}

@Composable
private fun StatePill(state: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = stateColor(state).copy(alpha = 0.14f),
    ) {
        Text(
            text = stateLabel(state),
            color = stateColor(state),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun stateColor(state: String): Color = when (state) {
    "working" -> Color(0xFF38BDF8)
    "awaitingPlanApproval", "awaitingPermission", "awaitingReplies" -> GallagerWarning
    "doneWorking" -> GallagerAccent
    "idle" -> GallagerMuted
    else -> GallagerBorder
}

private fun stateLabel(state: String): String = when (state) {
    "working" -> "Working"
    "awaitingPlanApproval" -> "Plan"
    "awaitingPermission" -> "Permission"
    "awaitingReplies" -> "Question"
    "doneWorking" -> "Done"
    "idle" -> "Idle"
    else -> "Terminal"
}
