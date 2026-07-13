package com.rokid.relayhud

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ReplyPage(state: HudState, s: Strings) {
    Column(Modifier.fillMaxSize()) {
        replyMethodOptions(s).forEachIndexed { index, option ->
            TerminalMenuRow(option, focused = index == state.replyMenuIndex)
        }
    }
}
