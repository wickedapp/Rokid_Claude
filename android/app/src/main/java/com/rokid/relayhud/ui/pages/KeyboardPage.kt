package com.rokid.relayhud

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun KeyboardPage(state: HudState) {
    Column(Modifier.fillMaxSize().padding(TerminalTokens.Space8)) {
        Text("> ${state.textInput.ifBlank { "_" }}", style = TerminalTokens.Text)
    }
}
