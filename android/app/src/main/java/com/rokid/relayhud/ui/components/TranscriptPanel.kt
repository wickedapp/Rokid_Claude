package com.rokid.relayhud

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Shared structure reserved for segmented dictation without changing VoiceInput/relay behavior. */
@Composable
fun TranscriptPanel(
    committedSegments: List<String>,
    candidate: String,
    s: Strings,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text("${s.sendPreview} · ${committedSegments.size}", style = TerminalTokens.MutedText)
        committedSegments.forEachIndexed { index, segment ->
            Text("${(index + 1).toString().padStart(2, '0')} $segment", style = TerminalTokens.Text)
        }
        Text(
            s.currentCandidate,
            style = TerminalTokens.MutedText,
            modifier = Modifier.fillMaxWidth().terminalBottomDivider().padding(top = TerminalTokens.Space12),
        )
        Text(candidate.ifBlank { "_" }, style = TerminalTokens.Text, modifier = Modifier.padding(top = TerminalTokens.Space8))
    }
}
