package app.gallager.android.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaneSummaryTest {
    @Test
    fun agentPaneUsesRemoteScrollingBeforeMouseModeArrives() {
        assertTrue(pane(pluginId = "claude", title = "shell").prefersRemoteTuiScroll)
        assertTrue(pane(pluginId = null, title = "OpenAI Codex").prefersRemoteTuiScroll)
    }

    @Test
    fun ordinaryShellKeepsLocalScrollback() {
        assertFalse(pane(pluginId = null, title = "zsh").prefersRemoteTuiScroll)
    }

    private fun pane(pluginId: String?, title: String) = PaneSummary(
        paneId = "%1",
        sessionName = "terminal",
        windowIndex = 0,
        paneIndex = 0,
        windowName = "terminal",
        terminalTitle = title,
        currentPath = null,
        gitBranch = null,
        pluginId = pluginId,
        state = "running",
        customDescription = null,
        customEmoji = null,
    )
}
