package app.gallager.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gallager.android.GallagerViewModel
import app.gallager.android.model.AgentProject
import app.gallager.android.model.ConnectionStatus
import app.gallager.android.model.PaneSummary
import app.gallager.android.model.PluginPresentation
import app.gallager.android.terminal.TerminalRender
import app.gallager.android.terminal.TerminalMouseScroll
import app.gallager.android.terminal.TerminalStyle

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
            terminalContent = state.relay.terminalContent[selectedPane.paneId] ?: TerminalRender(),
            connected = state.relay.hostConnected,
            commandInProgress = state.relay.commandInProgress,
            commandFeedback = state.relay.commandFeedback,
            onBack = viewModel::closeTerminal,
            onSend = viewModel::sendInput,
            onRequestHistory = viewModel::requestEarlierHistory,
            onRefreshTerminal = viewModel::refreshTerminalSnapshot,
            onCreateWindow = { path -> viewModel.createWindow(selectedPane.sessionName, path) },
            onSplit = viewModel::splitPane,
            onCloseWindow = { viewModel.closeWindow(selectedPane) },
            onCloseSession = { viewModel.closeSession(selectedPane) },
            onFeedbackShown = viewModel::clearCommandFeedback,
        )
        else -> SessionsScreen(
            hostName = state.pairedHost?.hostName ?: "Mac",
            status = state.relay.status,
            statusMessage = state.relay.statusMessage,
            panes = state.relay.panes,
            projects = state.relay.projects,
            projectsLoaded = state.relay.projectsLoaded,
            homeDirectory = state.relay.homeDirectory,
            pluginPresentations = state.relay.pluginPresentations,
            error = state.relay.error,
            connected = state.relay.hostConnected,
            commandInProgress = state.relay.commandInProgress,
            commandFeedback = state.relay.commandFeedback,
            onRefresh = viewModel::retryConnection,
            onSelectPane = viewModel::selectPane,
            onCreateSession = viewModel::createSession,
            onCreateWindow = { pane, path -> viewModel.createWindow(pane.sessionName, path) },
            onCloseSession = viewModel::closeSession,
            onFeedbackShown = viewModel::clearCommandFeedback,
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
            text = "Connect to CtrlX",
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
    projects: List<AgentProject>,
    projectsLoaded: Boolean,
    homeDirectory: String,
    pluginPresentations: Map<String, PluginPresentation>,
    error: String?,
    connected: Boolean,
    commandInProgress: Boolean,
    commandFeedback: String?,
    onRefresh: () -> Unit,
    onSelectPane: (PaneSummary) -> Unit,
    onCreateSession: (String, String?, String?, String) -> Unit,
    onCreateWindow: (PaneSummary, String?) -> Unit,
    onCloseSession: (PaneSummary) -> Unit,
    onFeedbackShown: () -> Unit,
    onUnpair: () -> Unit,
) {
    var showUnpairConfirmation by remember { mutableStateOf(false) }
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var windowTarget by remember { mutableStateOf<PaneSummary?>(null) }
    var closeTarget by remember { mutableStateOf<PaneSummary?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(commandFeedback) {
        commandFeedback?.let {
            snackbarHostState.showSnackbar(it)
            onFeedbackShown()
        }
    }

    if (showNewSessionDialog) {
        NewSessionSheet(
            projects = projects,
            projectsLoaded = projectsLoaded,
            homeDirectory = homeDirectory,
            pluginPresentations = pluginPresentations,
            loading = commandInProgress,
            onDismiss = { showNewSessionDialog = false },
            onSelect = { project ->
                showNewSessionDialog = false
                if (project == null) {
                    onCreateSession("terminal", null, null, "codex")
                } else {
                    onCreateSession(project.name, project.path, project.configDir, project.pluginId)
                }
            },
        )
    }

    windowTarget?.let { pane ->
        NewWindowDialog(
            sessionName = pane.sessionName,
            initialPath = pane.currentPath,
            loading = commandInProgress,
            onDismiss = { windowTarget = null },
            onCreate = { path ->
                windowTarget = null
                onCreateWindow(pane, path)
            },
        )
    }

    closeTarget?.let { pane ->
        ConfirmDestructiveDialog(
            title = "Close session?",
            message = "This closes every terminal in ${pane.sessionName}. Running processes will be stopped.",
            confirmLabel = "Close session",
            onDismiss = { closeTarget = null },
            onConfirm = {
                closeTarget = null
                onCloseSession(pane)
            },
        )
    }

    if (showUnpairConfirmation) {
        AlertDialog(
            onDismissRequest = { showUnpairConfirmation = false },
            title = { Text("Unpair this device?") },
            text = { Text("CtrlX will remove this Android device from the relay. You will need a new code to connect again.") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (connected) {
                FloatingActionButton(
                    onClick = { if (!commandInProgress) showNewSessionDialog = true },
                    containerColor = GallagerAccent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.semantics { contentDescription = "Create terminal session" },
                ) {
                    if (commandInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                    }
                }
            }
        },
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
                    SessionCard(
                        pane = pane,
                        actionsEnabled = connected && !commandInProgress,
                        onClick = { onSelectPane(pane) },
                        onCreateWindow = { windowTarget = pane },
                        onCloseSession = { closeTarget = pane },
                    )
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
private fun SessionCard(
    pane: PaneSummary,
    actionsEnabled: Boolean,
    onClick: () -> Unit,
    onCreateWindow: () -> Unit,
    onCloseSession: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
            Box {
                IconButton(
                    enabled = actionsEnabled,
                    onClick = { menuExpanded = true },
                    modifier = Modifier.semantics { contentDescription = "Session actions" },
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = GallagerMuted)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("New terminal window") },
                        leadingIcon = { Icon(Icons.Outlined.AddBox, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onCreateWindow()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Close session", color = GallagerDanger) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = GallagerDanger) },
                        onClick = {
                            menuExpanded = false
                            onCloseSession()
                        },
                    )
                }
            }
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
internal fun TerminalScreen(
    pane: PaneSummary,
    terminalContent: TerminalRender,
    connected: Boolean,
    commandInProgress: Boolean,
    commandFeedback: String?,
    onBack: () -> Unit,
    onSend: (ByteArray) -> Unit,
    onRequestHistory: () -> Boolean,
    onRefreshTerminal: () -> Unit,
    onCreateWindow: (String?) -> Unit,
    onSplit: (Boolean) -> Unit,
    onCloseWindow: () -> Unit,
    onCloseSession: () -> Unit,
    onFeedbackShown: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var followTerminalTail by remember(pane.paneId) { mutableStateOf(true) }
    var historyRequestInFlight by remember(pane.paneId) { mutableStateOf(false) }
    var historyRequestToken by remember(pane.paneId) { mutableStateOf(0) }
    var historyRequestOldMax by remember(pane.paneId) { mutableStateOf<Int?>(null) }
    var historyRequestOldValue by remember(pane.paneId) { mutableStateOf(0) }
    var input by remember(pane.paneId) { mutableStateOf("") }
    var actionMenuExpanded by remember { mutableStateOf(false) }
    var showNewWindowDialog by remember { mutableStateOf(false) }
    var closeAction by remember { mutableStateOf<String?>(null) }
    val remoteTuiScrolling = connected &&
        (terminalContent.mouseTrackingActive || pane.prefersRemoteTuiScroll)
    val latestRenderRevision by rememberUpdatedState(terminalContent.renderRevision)
    var remoteScrollRefreshToken by remember(pane.paneId) { mutableStateOf(0) }
    var remoteScrollRevisionBeforeDrag by remember(pane.paneId) { mutableStateOf(0L) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(commandFeedback) {
        commandFeedback?.let {
            snackbarHostState.showSnackbar(it)
            onFeedbackShown()
        }
    }

    if (showNewWindowDialog) {
        NewWindowDialog(
            sessionName = pane.sessionName,
            initialPath = pane.currentPath,
            loading = commandInProgress,
            onDismiss = { showNewWindowDialog = false },
            onCreate = { path ->
                showNewWindowDialog = false
                onCreateWindow(path)
            },
        )
    }

    closeAction?.let { action ->
        val closeSession = action == "session"
        ConfirmDestructiveDialog(
            title = if (closeSession) "Close session?" else "Close terminal window?",
            message = if (closeSession) {
                "This closes every terminal in ${pane.sessionName}. Running processes will be stopped."
            } else {
                "This closes ${pane.windowName.ifBlank { pane.windowId }} and stops its running processes."
            },
            confirmLabel = if (closeSession) "Close session" else "Close window",
            onDismiss = { closeAction = null },
            onConfirm = {
                closeAction = null
                if (closeSession) onCloseSession() else onCloseWindow()
            },
        )
    }
    val insertText = {
        if (connected && input.isNotEmpty()) {
            followTerminalTail = true
            onSend(input.toByteArray(Charsets.UTF_8))
            input = ""
        }
    }
    val insertTextAndEnter = {
        if (connected && input.isNotEmpty()) {
            // A submitted prompt and its response are written at the bottom of
            // the host screen. Make that part of the wider Mac viewport visible
            // on the phone before waiting for the next terminal redraw.
            followTerminalTail = true
            onSend((input + "\r").toByteArray(Charsets.UTF_8))
            input = ""
        }
    }
    LaunchedEffect(scrollState, pane.paneId) {
        scrollState.interactionSource.interactions.collect { interaction ->
            if (remoteTuiScrolling) return@collect
            when (interaction) {
                is DragInteraction.Start -> followTerminalTail = false
                is DragInteraction.Stop,
                is DragInteraction.Cancel -> {
                    followTerminalTail = scrollState.value >= scrollState.maxValue - 48
                    if (
                        interaction is DragInteraction.Stop &&
                        !followTerminalTail &&
                        scrollState.value <= 48 &&
                        !historyRequestInFlight &&
                        onRequestHistory()
                    ) {
                        historyRequestOldMax = scrollState.maxValue
                        historyRequestOldValue = scrollState.value
                        historyRequestInFlight = true
                        historyRequestToken++
                    }
                }
            }
        }
    }
    LaunchedEffect(pane.paneId, terminalContent.renderRevision, remoteTuiScrolling) {
        if (followTerminalTail) {
            if (remoteTuiScrolling) {
                withFrameNanos { }
                withFrameNanos { }
            }
            scrollState.scrollTo(scrollState.maxValue)
        }
    }
    LaunchedEffect(pane.paneId, remoteScrollRefreshToken) {
        if (remoteScrollRefreshToken == 0) return@LaunchedEffect
        val revisionBeforeDrag = remoteScrollRevisionBeforeDrag
        kotlinx.coroutines.delay(500)
        // iOS relies on the live terminal stream and never reboots the stream
        // after a pan. Do the same unless the host produced no redraw at all;
        // only then use a delayed snapshot as a recovery fallback.
        if (latestRenderRevision == revisionBeforeDrag) onRefreshTerminal()
    }
    LaunchedEffect(pane.paneId, terminalContent.snapshotGeneration) {
        val oldMax = historyRequestOldMax ?: return@LaunchedEffect
        // Let Compose measure the replacement snapshot before compensating for
        // the newly prepended rows. This keeps the previously visible text under
        // the user's finger instead of jumping to the oldest row.
        withFrameNanos { }
        withFrameNanos { }
        val preserved = historyRequestOldValue + (scrollState.maxValue - oldMax)
        scrollState.scrollTo(preserved.coerceIn(0, scrollState.maxValue))
        historyRequestOldMax = null
        historyRequestInFlight = false
    }
    LaunchedEffect(pane.paneId, historyRequestToken) {
        val token = historyRequestToken
        if (token == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(10_000)
        if (historyRequestToken == token) historyRequestInFlight = false
    }

    Scaffold(
        containerColor = TerminalBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                actions = {
                    Box {
                        IconButton(
                            enabled = connected && !commandInProgress,
                            onClick = { actionMenuExpanded = true },
                            modifier = Modifier.semantics { contentDescription = "Terminal actions" },
                        ) {
                            if (commandInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.MoreVert, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = actionMenuExpanded,
                            onDismissRequest = { actionMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("New terminal window") },
                                leadingIcon = { Icon(Icons.Outlined.AddBox, contentDescription = null) },
                                onClick = {
                                    actionMenuExpanded = false
                                    showNewWindowDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Split right") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = null) },
                                onClick = {
                                    actionMenuExpanded = false
                                    onSplit(true)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Split below") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = null) },
                                onClick = {
                                    actionMenuExpanded = false
                                    onSplit(false)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Close window", color = GallagerDanger) },
                                leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null, tint = GallagerDanger) },
                                onClick = {
                                    actionMenuExpanded = false
                                    closeAction = "window"
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Close session", color = GallagerDanger) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = GallagerDanger) },
                                onClick = {
                                    actionMenuExpanded = false
                                    closeAction = "session"
                                },
                            )
                        }
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { insertTextAndEnter() }),
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        enabled = connected && input.isNotEmpty(),
                        onClick = insertText,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = GallagerAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = GallagerSurfaceRaised,
                            disabledContentColor = GallagerMuted,
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .semantics { contentDescription = "Insert text without Enter" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    FilledIconButton(
                        enabled = connected && input.isNotEmpty(),
                        onClick = insertTextAndEnter,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = GallagerSurfaceRaised,
                            contentColor = GallagerAccent,
                            disabledContainerColor = GallagerSurfaceRaised,
                            disabledContentColor = GallagerMuted,
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .semantics { contentDescription = "Insert text and press Enter" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardReturn,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val terminalInteractionModifier = if (remoteTuiScrolling) {
            Modifier.pointerInput(
                pane.paneId,
                terminalContent.columns,
                terminalContent.rows,
            ) {
                var accumulatedY = 0f
                var didSendScroll = false
                var lastDragY = 0f
                var renderRevisionAtDragStart = latestRenderRevision
                var lastColumn = terminalContent.columns / 2
                var lastRow = terminalContent.rows / 2
                val lineThreshold = 16.dp.toPx().coerceAtLeast(1f)

                fun sendScroll(deltaY: Float, events: Int) {
                    if (deltaY == 0f || events <= 0) return
                    didSendScroll = true
                    onSend(
                        TerminalMouseScroll.encodeVerticalDrag(
                            deltaY = deltaY,
                            column = lastColumn,
                            row = lastRow,
                            columns = terminalContent.columns,
                            rows = terminalContent.rows,
                            events = events,
                        ),
                    )
                }

                fun finishDrag() {
                    // Compose starts this callback only after touch slop has
                    // been crossed. If a deliberate drag is shorter than one
                    // line, still forward one wheel event so it never feels
                    // like the terminal ignored the gesture.
                    if (!didSendScroll && lastDragY != 0f) sendScroll(lastDragY, 1)
                    accumulatedY = 0f
                    lastDragY = 0f
                    followTerminalTail = scrollState.value >= scrollState.maxValue - 48
                    if (didSendScroll && latestRenderRevision == renderRevisionAtDragStart) {
                        remoteScrollRevisionBeforeDrag = renderRevisionAtDragStart
                        remoteScrollRefreshToken++
                    }
                }

                detectVerticalDragGestures(
                    onDragStart = {
                        accumulatedY = 0f
                        didSendScroll = false
                        lastDragY = 0f
                        renderRevisionAtDragStart = latestRenderRevision
                        followTerminalTail = false
                    },
                    onDragCancel = ::finishDrag,
                    onDragEnd = ::finishDrag,
                ) { change, dragAmount ->
                    if (dragAmount == 0f) return@detectVerticalDragGestures
                    change.consume()

                    // First consume the drag inside the Mac-sized snapshot.
                    // Unlike the old HEAD/TAIL anchor this preserves every
                    // intermediate offset, so the content follows the finger.
                    // Only the unconsumed distance at an edge becomes a remote
                    // TUI wheel event, extending history in either direction.
                    val requestedLocalDelta = -dragAmount
                    val consumedLocalDelta = scrollState.dispatchRawDelta(requestedLocalDelta)
                    val remoteDragAmount = -(requestedLocalDelta - consumedLocalDelta)
                    lastDragY = remoteDragAmount

                    // Match iOS: address the cell underneath the finger rather
                    // than a fixed cell in the pane. Include both local scroll
                    // offsets because the Mac terminal is usually wider and
                    // taller than the phone viewport.
                    val contentWidth = (horizontalScrollState.maxValue + size.width).coerceAtLeast(1)
                    val contentHeight = (scrollState.maxValue + size.height).coerceAtLeast(1)
                    val contentX = horizontalScrollState.value + change.position.x
                    val contentY = scrollState.value + change.position.y
                    lastColumn = ((contentX / contentWidth) * terminalContent.columns)
                        .toInt()
                        .coerceIn(0, (terminalContent.columns - 1).coerceAtLeast(0))
                    lastRow = ((contentY / contentHeight) * terminalContent.rows)
                        .toInt()
                        .coerceIn(0, (terminalContent.rows - 1).coerceAtLeast(0))

                    if (remoteDragAmount == 0f) return@detectVerticalDragGestures

                    // Reset immediately when the user reverses direction.
                    if (accumulatedY != 0f && (accumulatedY > 0f) != (remoteDragAmount > 0f)) {
                        accumulatedY = 0f
                    }
                    accumulatedY += remoteDragAmount

                    var events = 0
                    while (kotlin.math.abs(accumulatedY) >= lineThreshold) {
                        events++
                        accumulatedY -= if (accumulatedY > 0f) lineThreshold else -lineThreshold
                    }
                    if (events == 0) return@detectVerticalDragGestures

                    sendScroll(remoteDragAmount, events)
                }
            }
        } else {
            Modifier
        }
        // Keep the TUI gesture recognizer outside SelectionContainer, matching
        // the iOS terminal's outer pan recognizer. Text selection can otherwise
        // win the pointer contest and silently swallow remote wheel gestures.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(terminalInteractionModifier),
        ) {
            SelectionContainer {
                Text(
                    text = if (terminalContent.text.isBlank()) {
                        buildAnnotatedString { append("Waiting for terminal stream…") }
                    } else {
                        terminalAnnotatedString(terminalContent)
                    },
                    color = if (terminalContent.text.isBlank()) GallagerMuted else TerminalDefaultForeground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    softWrap = false,
                    modifier = Modifier
                        .testTag("terminal-transcript")
                        .fillMaxSize()
                        // In a remote TUI, disable local gesture handling but retain
                        // the scroll layout so the phone can expose every row of a
                        // terminal snapshot that is taller than its viewport.
                        .verticalScroll(scrollState, enabled = !remoteTuiScrolling)
                        .horizontalScroll(horizontalScrollState)
                        .padding(12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewSessionSheet(
    projects: List<AgentProject>,
    projectsLoaded: Boolean,
    homeDirectory: String,
    pluginPresentations: Map<String, PluginPresentation>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSelect: (AgentProject?) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filteredProjects = remember(projects, search) {
        val query = search.trim()
        if (query.isEmpty()) {
            projects
        } else {
            projects.filter { project ->
                project.name.contains(query, ignoreCase = true) ||
                    project.path.contains(query, ignoreCase = true) ||
                    project.pluginId.contains(query, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GallagerBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "New Session",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search projects…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 560.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (search.isBlank()) {
                    item {
                        ProjectPickerRow(
                            title = "New Terminal",
                            subtitle = if (homeDirectory.isBlank()) {
                                "Start in home directory"
                            } else {
                                "Start in ${abbreviateHome(homeDirectory, homeDirectory)}"
                            },
                            badge = null,
                            badgeColor = GallagerAccent,
                            loading = loading,
                            icon = { Icon(Icons.Outlined.Terminal, contentDescription = null, tint = GallagerMuted) },
                            onClick = { onSelect(null) },
                        )
                    }
                    item {
                        Text(
                            "PROJECTS",
                            color = GallagerMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                        )
                    }
                }

                when {
                    !projectsLoaded -> item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Loading projects…", color = GallagerMuted)
                        }
                    }
                    filteredProjects.isEmpty() -> item {
                        Text(
                            if (search.isBlank()) "No projects were reported by the Mac" else "No matching projects",
                            color = GallagerMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                    else -> items(filteredProjects, key = { it.id }) { project ->
                        val presentation = pluginPresentations[project.pluginId]
                        ProjectPickerRow(
                            title = project.name,
                            subtitle = abbreviateHome(project.path, homeDirectory),
                            badge = presentation?.shortName ?: project.pluginId,
                            badgeColor = presentation?.color.toComposeColor() ?: GallagerAccent,
                            loading = loading,
                            icon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, tint = GallagerAccent) },
                            onClick = { onSelect(project) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectPickerRow(
    title: String,
    subtitle: String,
    badge: String?,
    badgeColor: Color,
    loading: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !loading, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GallagerSurface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    badge?.let {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = badgeColor.copy(alpha = 0.16f),
                        ) {
                            Text(
                                it,
                                color = badgeColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    color = GallagerMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = GallagerMuted)
            }
        }
    }
}

private fun abbreviateHome(path: String, homeDirectory: String): String =
    if (homeDirectory.isNotBlank() && path.startsWith(homeDirectory)) {
        "~${path.removePrefix(homeDirectory)}"
    } else {
        path
    }

private fun String?.toComposeColor(): Color? {
    val value = this?.trim()?.removePrefix("#") ?: return null
    if (value.length != 6) return null
    val rgb = value.toLongOrNull(16) ?: return null
    return Color((0xFF000000L or rgb).toInt())
}

@Composable
private fun NewWindowDialog(
    sessionName: String,
    initialPath: String?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String?) -> Unit,
) {
    var path by remember(initialPath) { mutableStateOf(initialPath.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New terminal window") },
        text = {
            Column {
                Text("Create a new window in $sessionName.", color = GallagerMuted)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Working directory (optional)") },
                    leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !loading, onClick = { onCreate(path.trim().ifBlank { null }) }) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ConfirmDestructiveDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = GallagerDanger) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun terminalAnnotatedString(content: TerminalRender) = buildAnnotatedString {
    append(content.text)
    content.spans.forEach { span ->
        addStyle(
            style = span.style.toComposeStyle(),
            start = span.start.coerceIn(0, length),
            end = span.end.coerceIn(0, length),
        )
    }
}

private fun TerminalStyle.toComposeStyle(): SpanStyle {
    val styledForeground = foreground?.let(::Color) ?: TerminalDefaultForeground
    val styledBackground = background?.let(::Color)
    val resolvedForeground = if (inverse) styledBackground ?: TerminalBackground else styledForeground
    val resolvedBackground = if (inverse) styledForeground else styledBackground
    return SpanStyle(
        color = if (dim) resolvedForeground.copy(alpha = 0.58f) else resolvedForeground,
        background = resolvedBackground ?: Color.Unspecified,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (underline) TextDecoration.Underline else TextDecoration.None,
    )
}

private val TerminalDefaultForeground = Color(0xFFE2E8F0)
private val TerminalBackground = Color(0xFF181818)

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
