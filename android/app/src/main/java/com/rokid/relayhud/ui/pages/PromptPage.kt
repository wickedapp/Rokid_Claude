package com.rokid.relayhud

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PromptPage(
    state: HudState,
    prompt: PermissionPrompt,
    s: Strings,
    connected: Boolean,
) {
    val terminal = state.terminal
    val identity = if (terminal == null) prompt.title else "${displayToolName(terminal.tool)} / ${terminal.title} / ${prompt.title}"
    TerminalScaffold(
        header = "$identity (${prompt.secondsLeft}s)",
        connected = connected,
        actionLabel = "[ ENTER ${s.confirmAction} ]",
    ) {
        Column(Modifier.fillMaxSize()) {
            if (prompt.summary.isNotBlank()) {
                Text(
                    prompt.summary,
                    style = TerminalTokens.Text,
                    modifier = Modifier.padding(TerminalTokens.Space8),
                )
            }
            prompt.options.forEachIndexed { index, option ->
                TerminalMenuRow(option, focused = index == prompt.highlight)
            }
        }
    }
}
