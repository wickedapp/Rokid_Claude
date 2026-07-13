package com.rokid.relayhud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds

@Composable
fun TerminalScaffold(
    header: String,
    connected: Boolean,
    actionLabel: String,
    actionFocused: Boolean = false,
    actionContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    TerminalTheme {
        Column(
            modifier
                .fillMaxSize()
                .background(TerminalTokens.Background)
                .padding(
                    start = TerminalTokens.PageInset,
                    end = TerminalTokens.PageInset,
                    top = TerminalTokens.SafeTop,
                    bottom = TerminalTokens.SafeBottom,
                ),
        ) {
            HeaderBar(header, connected)
            Spacer(Modifier.height(TerminalTokens.Space4))
            BodyViewport(Modifier.weight(1f), body)
            Spacer(Modifier.height(TerminalTokens.Space4))
            if (actionContent == null) ActionDock(actionLabel, actionFocused) else actionContent()
        }
    }
}

@Composable
fun BodyViewport(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .background(TerminalTokens.Background)
            .clipToBounds(),
    ) { content() }
}
