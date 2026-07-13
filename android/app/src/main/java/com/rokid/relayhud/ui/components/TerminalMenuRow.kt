package com.rokid.relayhud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun TerminalMenuRow(
    text: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(TerminalTokens.MenuRowHeight)
            .background(if (focused) TerminalTokens.FocusBackground else TerminalTokens.Background)
            .terminalBottomDivider()
            .padding(horizontal = TerminalTokens.Space4),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            style = if (focused) TerminalTokens.FocusText else TerminalTokens.Text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
