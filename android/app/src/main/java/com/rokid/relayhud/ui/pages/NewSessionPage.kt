package com.rokid.relayhud

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NewSessionPage(state: HudState) {
    Column(Modifier.fillMaxSize()) {
        newSessionOptions().forEachIndexed { index, option ->
            TerminalMenuRow(option, focused = index == state.newSessionIndex)
        }
    }
}
