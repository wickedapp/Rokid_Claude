package com.rokid.relayhud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun HeaderBar(text: String, connected: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TerminalTokens.HeaderHeight)
            .background(TerminalTokens.Background)
            .terminalBottomDivider()
            .padding(horizontal = TerminalTokens.Space4),
        horizontalArrangement = Arrangement.spacedBy(TerminalTokens.Space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = TerminalTokens.Text,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (connected) "●" else "○",
            style = if (connected) TerminalTokens.Text else TerminalTokens.MutedText,
            maxLines = 1,
            softWrap = false,
        )
    }
}
